package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.ai.common.VectorSearchService;
import com.tcc.pjb.backend.core.digitalizacao.DigitalizacaoProperties;
import com.tcc.pjb.backend.core.digitalizacao.OcrEnginePort;
import com.tcc.pjb.backend.core.digitalizacao.OcrPageResult;
import com.tcc.pjb.backend.core.protocolo.completude.ContextoValidacaoCompletude;
import com.tcc.pjb.backend.core.protocolo.completude.DetectorInteligenteCompletude;
import com.tcc.pjb.backend.core.protocolo.completude.DetectorInteligenteCompletudeImpl;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeInteligenteProperties;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeMetrics;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeValidator;
import com.tcc.pjb.backend.core.protocolo.completude.domain.DocumentoAnalisavel;
import com.tcc.pjb.backend.core.protocolo.completude.domain.FundamentoNormativo;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.FonteNormativaTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.GrauExigibilidade;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.SeveridadeCompletude;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoCondicaoRequisito;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoDocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import com.tcc.pjb.backend.model.repository.protocolo.RequisitoDocumentalEquivalenciaRepository;
import com.tcc.pjb.backend.model.repository.protocolo.RequisitoDocumentalRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class PjbProtocoloCompletudeFase4Test {

    private RequisitoDocumentalRepository requisitoRepo;
    private RequisitoDocumentalEquivalenciaRepository equivalenciaRepo;
    private DetectorInteligenteCompletude detectorMock;
    private ProtocoloCompletudeMetrics metrics;
    private SimpleMeterRegistry meterRegistry;
    private ProtocoloCompletudeValidator validator;

    private OcrEnginePort ocrMock;
    private VectorSearchService vectorMock;
    private DigitalizacaoProperties ocrProps;
    private Environment envMock;
    private ProtocoloCompletudeInteligenteProperties iaProps;

    private static final FundamentoNormativo FUND_IA = new FundamentoNormativo(
            FonteNormativaTipo.REGRA_INTERNA, "DETECTOR-IA-COMPLETUDE",
            "Analise inteligente de conteudo documental via OCR/VectorSearch",
            GrauExigibilidade.RELATIVO, "PJB-IA");

    @BeforeEach
    void setUp() {
        requisitoRepo = mock(RequisitoDocumentalRepository.class);
        equivalenciaRepo = mock(RequisitoDocumentalEquivalenciaRepository.class);
        detectorMock = mock(DetectorInteligenteCompletude.class);
        meterRegistry = new SimpleMeterRegistry();
        metrics = new ProtocoloCompletudeMetrics(meterRegistry);
        validator = new ProtocoloCompletudeValidator(requisitoRepo, equivalenciaRepo, detectorMock, metrics);

        ocrMock = mock(OcrEnginePort.class);
        vectorMock = mock(VectorSearchService.class);
        ocrProps = mock(DigitalizacaoProperties.class);
        envMock = mock(Environment.class);
        iaProps = new ProtocoloCompletudeInteligenteProperties();

        when(requisitoRepo.findVigentesNaData(any(), any())).thenReturn(List.of());
        when(equivalenciaRepo.findByRequisitoId(any())).thenReturn(List.of());
    }

    @Test
    void ia_score_alto_gera_bloqueante() {
        ViolacaoCompletude bloqueante = new ViolacaoCompletude.AssinaturaAusente(
                TipoDocumentoProcessual.PROCURACAO, FUND_IA);
        DocumentoAnalisavel doc = docProcuracao();

        when(detectorMock.disponivel()).thenReturn(true);
        when(detectorMock.analisar(any(), any())).thenReturn(List.of(bloqueante));

        ResultadoValidacao resultado = validator.validar(contextoSimples(), List.of(doc));

        assertThat(resultado.temBloqueante()).isTrue();
        assertThat(resultado.statusResultante()).isEqualTo(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO);
        assertThat(resultado.bloqueantes()).hasSize(1);
        assertThat(resultado.bloqueantes().get(0).codigo()).isEqualTo("ASSINATURA_AUSENTE");
    }

    @Test
    void ia_score_baixo_gera_advertencia_nao_bloqueia() {
        ViolacaoCompletude advertencia = new ViolacaoCompletude.QualidadeDigitalizacaoBaixa(
                TipoDocumentoProcessual.PROCURACAO, 0.60, FUND_IA);
        DocumentoAnalisavel doc = docProcuracao();

        when(detectorMock.disponivel()).thenReturn(true);
        when(detectorMock.analisar(any(), any())).thenReturn(List.of(advertencia));

        ResultadoValidacao resultado = validator.validar(contextoSimples(), List.of(doc));

        assertThat(resultado.temBloqueante()).isFalse();
        assertThat(resultado.statusResultante()).isEqualTo(ProtocoloCompletudeStatus.COMPLETO);
        assertThat(resultado.advertencias()).hasSize(1);
        assertThat(resultado.advertencias().get(0).severidade()).isEqualTo(SeveridadeCompletude.ADVERTENCIA);
    }

    @Test
    void detector_indisponivel_pula_ia_nao_trava_protocolo_incrementa_metrica() {
        when(detectorMock.disponivel()).thenReturn(false);
        DocumentoAnalisavel doc = docProcuracao();

        ResultadoValidacao resultado = validator.validar(contextoSimples(), List.of(doc));

        assertThat(resultado.temBloqueante()).isFalse();
        assertThat(resultado.statusResultante()).isEqualTo(ProtocoloCompletudeStatus.COMPLETO);
        verify(detectorMock, never()).analisar(any(), any());

        double falhasOcr = meterRegistry.counter("pjb.protocolo.completude.ocr_falhas").count();
        assertThat(falhasOcr).isEqualTo(1.0);
    }

    @Test
    void documento_tipo_incorreto_detectado_rg_onde_pede_cnh() {
        ViolacaoCompletude tipoIncorreto = new ViolacaoCompletude.DocumentoTipoIncorreto(
                TipoDocumentoProcessual.DOCUMENTO_IDENTIDADE,
                "DOCUMENTO_IDENTIDADE",
                SeveridadeCompletude.BLOQUEANTE,
                FUND_IA);
        DocumentoAnalisavel doc = docIdentidade();

        when(detectorMock.disponivel()).thenReturn(true);
        when(detectorMock.analisar(any(), any())).thenReturn(List.of(tipoIncorreto));

        ResultadoValidacao resultado = validator.validar(contextoSimples(), List.of(doc));

        assertThat(resultado.bloqueantes()).hasSize(1);
        assertThat(resultado.bloqueantes().get(0).codigo()).isEqualTo("DOC_TIPO_INCORRETO");
    }

    @Test
    void titularidade_inconsistente_detectada_nome_diverge() {
        ViolacaoCompletude titular = new ViolacaoCompletude.TitularidadeInconsistente(
                TipoDocumentoProcessual.PROCURACAO, "FULANO DA SILVA", FUND_IA);
        DocumentoAnalisavel doc = docProcuracao();

        when(detectorMock.disponivel()).thenReturn(true);
        when(detectorMock.analisar(any(), any())).thenReturn(List.of(titular));

        ResultadoValidacao resultado = validator.validar(contextoSimples(), List.of(doc));

        assertThat(resultado.bloqueantes()).hasSize(1);
        assertThat(resultado.bloqueantes().get(0).codigo()).isEqualTo("TITULARIDADE_INCONSISTENTE");
    }

    @Test
    void conteudo_sensivel_nao_vaza_nas_violacoes() {
        String textoSensivel = "CONTEUDO MEDICO SIGILOSO DO PACIENTE";
        byte[] pdfSensivel = textoSensivel.getBytes();

        when(ocrProps.enabled()).thenReturn(true);
        when(ocrProps.idiomaDefault()).thenReturn("por");
        when(envMock.getProperty("pjb.ai.vector.mode", "disabled")).thenReturn("mock");
        when(ocrMock.processar(any(), any())).thenReturn(new OcrPageResult(textoSensivel, 90.0));
        when(vectorMock.searchSimilarResult(any(), any(), eq(1)))
                .thenReturn(new VectorSearchService.VectorSearchResult(
                        "query", Instant.now(), List.of(), Map.of(), Map.of(), "mock"));

        DetectorInteligenteCompletudeImpl impl = new DetectorInteligenteCompletudeImpl(
                ocrMock, vectorMock, ocrProps, envMock, iaProps);

        DocumentoAnalisavel docSensivel = new DocumentoAnalisavel(
                TipoDocumentoProcessual.LAUDO_MEDICO,
                pdfSensivel,
                NivelSigilo.SEGREDO_JUSTICA,
                null,
                null);

        List<ViolacaoCompletude> violacoes = impl.analisar(contextoSimples(), List.of(docSensivel));

        violacoes.forEach(v -> {
            assertThat(v.acaoCorretiva()).doesNotContain(textoSensivel);
            assertThat(v.campo()).doesNotContain(textoSensivel);
            assertThat(v.codigo()).doesNotContain(textoSensivel);
        });
    }

    @Test
    void detector_disponivel_retorna_false_quando_ocr_disabled() {
        when(ocrProps.enabled()).thenReturn(false);
        when(envMock.getProperty("pjb.ai.vector.mode", "disabled")).thenReturn("mock");

        DetectorInteligenteCompletudeImpl impl = new DetectorInteligenteCompletudeImpl(
                ocrMock, vectorMock, ocrProps, envMock, iaProps);

        assertThat(impl.disponivel()).isFalse();
    }

    @Test
    void detector_disponivel_retorna_false_quando_vector_disabled() {
        when(ocrProps.enabled()).thenReturn(true);
        when(envMock.getProperty("pjb.ai.vector.mode", "disabled")).thenReturn("disabled");

        DetectorInteligenteCompletudeImpl impl = new DetectorInteligenteCompletudeImpl(
                ocrMock, vectorMock, ocrProps, envMock, iaProps);

        assertThat(impl.disponivel()).isFalse();
    }

    private ContextoValidacaoCompletude contextoSimples() {
        return new ContextoValidacaoCompletude(
                RitoProcessual.COMUM_ORDINARIO,
                TipoRepresentanteProcessual.ADVOGADO_PRIVADO,
                null,
                Set.of(TipoCondicaoRequisito.SEMPRE),
                List.of(),
                LocalDate.now());
    }

    private DocumentoAnalisavel docProcuracao() {
        return new DocumentoAnalisavel(
                TipoDocumentoProcessual.PROCURACAO,
                "PROCURACAO".getBytes(),
                NivelSigilo.PUBLICO,
                null,
                null);
    }

    private DocumentoAnalisavel docIdentidade() {
        return new DocumentoAnalisavel(
                TipoDocumentoProcessual.DOCUMENTO_IDENTIDADE,
                "IDENTIDADE".getBytes(),
                NivelSigilo.PUBLICO,
                null,
                null);
    }
}
