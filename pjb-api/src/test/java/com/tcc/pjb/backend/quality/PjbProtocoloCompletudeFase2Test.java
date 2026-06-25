package com.tcc.pjb.backend.quality;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.protocolo.completude.ContextoValidacaoCompletude;
import com.tcc.pjb.backend.core.protocolo.completude.DetectorInteligenteCompletude;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeMetrics;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloCompletudeValidator;
import com.tcc.pjb.backend.core.protocolo.completude.ProtocoloPendenciaApplicationService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import com.tcc.pjb.backend.core.protocolo.completude.domain.FundamentoNormativo;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ResultadoValidacao;
import com.tcc.pjb.backend.core.protocolo.completude.domain.ViolacaoCompletude;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.FonteNormativaTipo;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.GrauExigibilidade;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.OrigemValidacao;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoCondicaoRequisito;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.TipoRepresentanteProcessual;
import com.tcc.pjb.backend.model.entity.protocolo.RequisitoDocumental;
import com.tcc.pjb.backend.model.entity.protocolo.RequisitoDocumentalEquivalencia;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.SeveridadeCompletude;
import com.tcc.pjb.backend.model.repository.protocolo.RequisitoDocumentalEquivalenciaRepository;
import com.tcc.pjb.backend.model.repository.protocolo.RequisitoDocumentalRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PjbProtocoloCompletudeFase2Test {

    private RequisitoDocumentalRepository requisitoRepo;
    private RequisitoDocumentalEquivalenciaRepository equivalenciaRepo;
    private ProtocoloCompletudeValidator validator;

    @BeforeEach
    void setUp() {
        requisitoRepo = mock(RequisitoDocumentalRepository.class);
        equivalenciaRepo = mock(RequisitoDocumentalEquivalenciaRepository.class);
        DetectorInteligenteCompletude detector = mock(DetectorInteligenteCompletude.class);
        ProtocoloCompletudeMetrics metrics = new ProtocoloCompletudeMetrics(new SimpleMeterRegistry());
        validator = new ProtocoloCompletudeValidator(requisitoRepo, equivalenciaRepo, detector, metrics);
    }

    @Test
    void protocolo_completo_quando_todos_docs_presentes() {
        RequisitoDocumental req = requisitoIdentidade();
        when(requisitoRepo.findVigentesNaData(eq(RitoProcessual.COMUM_ORDINARIO), any()))
                .thenReturn(List.of(req));
        when(equivalenciaRepo.findByRequisitoId(req.getId())).thenReturn(List.of());

        ContextoValidacaoCompletude ctx = new ContextoValidacaoCompletude(
                RitoProcessual.COMUM_ORDINARIO, TipoRepresentanteProcessual.ADVOGADO_PRIVADO,
                null, Set.of(), List.of(TipoDocumento.DOCUMENTO_IDENTIDADE), LocalDate.now());

        ResultadoValidacao resultado = validator.validar(ctx);

        assertThat(resultado.temBloqueante()).isFalse();
        assertThat(resultado.statusResultante()).isEqualTo(ProtocoloCompletudeStatus.COMPLETO);
        assertThat(resultado.violacoes()).isEmpty();
    }

    @Test
    void protocolo_bloqueado_quando_doc_obrigatorio_ausente() {
        RequisitoDocumental req = requisitoIdentidade();
        when(requisitoRepo.findVigentesNaData(eq(RitoProcessual.COMUM_ORDINARIO), any()))
                .thenReturn(List.of(req));
        when(equivalenciaRepo.findByRequisitoId(req.getId())).thenReturn(List.of());

        ContextoValidacaoCompletude ctx = new ContextoValidacaoCompletude(
                RitoProcessual.COMUM_ORDINARIO, TipoRepresentanteProcessual.ADVOGADO_PRIVADO,
                null, Set.of(), List.of(), LocalDate.now());

        ResultadoValidacao resultado = validator.validar(ctx);

        assertThat(resultado.temBloqueante()).isTrue();
        assertThat(resultado.statusResultante()).isEqualTo(ProtocoloCompletudeStatus.PENDENTE_DOCUMENTACAO);
        assertThat(resultado.bloqueantes()).hasSize(1);
        assertThat(resultado.bloqueantes().get(0).codigo()).isEqualTo("DOC_OBRIGATORIO_AUSENTE");
    }

    @Test
    void equivalente_aceito_gera_advertencia_nao_bloqueante() {
        RequisitoDocumental req = requisitoCtps();
        RequisitoDocumentalEquivalencia eq = equivalenciaContratoTrabalho(req);
        when(requisitoRepo.findVigentesNaData(eq(RitoProcessual.TRABALHISTA_ORDINARIO), any()))
                .thenReturn(List.of(req));
        when(equivalenciaRepo.findByRequisitoId(req.getId())).thenReturn(List.of(eq));

        ContextoValidacaoCompletude ctx = new ContextoValidacaoCompletude(
                RitoProcessual.TRABALHISTA_ORDINARIO, TipoRepresentanteProcessual.ADVOGADO_PRIVADO,
                null, Set.of(), List.of(TipoDocumento.CONTRATO_TRABALHO), LocalDate.now());

        ResultadoValidacao resultado = validator.validar(ctx);

        assertThat(resultado.temBloqueante()).isFalse();
        assertThat(resultado.advertencias()).hasSize(1);
        assertThat(resultado.advertencias().get(0).codigo()).isEqualTo("DOC_TIPO_INCORRETO");
    }

    @Test
    void condicional_procuracao_nao_cobrada_sem_condicao() {
        RequisitoDocumental req = requisitoProcuracao();
        when(requisitoRepo.findVigentesNaData(eq(RitoProcessual.COMUM_ORDINARIO), any()))
                .thenReturn(List.of(req));

        ContextoValidacaoCompletude ctx = new ContextoValidacaoCompletude(
                RitoProcessual.COMUM_ORDINARIO, TipoRepresentanteProcessual.PARTE_SEM_ADVOGADO,
                null, Set.of(), List.of(), LocalDate.now());

        ResultadoValidacao resultado = validator.validar(ctx);

        assertThat(resultado.temBloqueante()).isFalse();
        assertThat(resultado.violacoes()).isEmpty();
    }

    @Test
    void condicional_procuracao_cobrada_quando_advogado_constituido() {
        RequisitoDocumental req = requisitoProcuracao();
        when(requisitoRepo.findVigentesNaData(eq(RitoProcessual.COMUM_ORDINARIO), any()))
                .thenReturn(List.of(req));
        when(equivalenciaRepo.findByRequisitoId(req.getId())).thenReturn(List.of());

        ContextoValidacaoCompletude ctx = new ContextoValidacaoCompletude(
                RitoProcessual.COMUM_ORDINARIO, TipoRepresentanteProcessual.ADVOGADO_PRIVADO,
                null, Set.of(TipoCondicaoRequisito.ADVOGADO_CONSTITUIDO), List.of(), LocalDate.now());

        ResultadoValidacao resultado = validator.validar(ctx);

        assertThat(resultado.temBloqueante()).isTrue();
        assertThat(resultado.bloqueantes().get(0).campo()).isEqualTo("PROCURACAO");
    }

    @Test
    void documento_sensivel_tem_nivel_sensibilidade_preservado() {
        RequisitoDocumental req = requisitoSensivel();
        when(requisitoRepo.findVigentesNaData(eq(RitoProcessual.COMUM_ORDINARIO), any()))
                .thenReturn(List.of(req));
        when(equivalenciaRepo.findByRequisitoId(req.getId())).thenReturn(List.of());

        ContextoValidacaoCompletude ctx = new ContextoValidacaoCompletude(
                RitoProcessual.COMUM_ORDINARIO, null, null, Set.of(), List.of(), LocalDate.now());

        ResultadoValidacao resultado = validator.validar(ctx);

        assertThat(resultado.violacoes()).hasSize(1);
        ViolacaoCompletude v = resultado.violacoes().get(0);
        assertThat(v.nivelSensibilidade().exigeCredencial()).isTrue();
    }

    @Test
    void hash_documentos_determinista() {
        List<String> docs1 = List.of("PROCURACAO", "DOCUMENTO_IDENTIDADE");
        List<String> docs2 = List.of("DOCUMENTO_IDENTIDADE", "PROCURACAO");
        assertThat(ProtocoloPendenciaApplicationService.computeHash(docs1))
                .isEqualTo(ProtocoloPendenciaApplicationService.computeHash(docs2));
    }

    @Test
    void hash_documentos_diferente_para_listas_diferentes() {
        assertThat(ProtocoloPendenciaApplicationService.computeHash(List.of("PROCURACAO")))
                .isNotEqualTo(ProtocoloPendenciaApplicationService.computeHash(List.of("CTPS")));
    }

    private RequisitoDocumental requisitoIdentidade() {
        return RequisitoDocumental.builder()
                .id(1L)
                .tipoDocumento(TipoDocumento.DOCUMENTO_IDENTIDADE)
                .severidade(SeveridadeCompletude.BLOQUEANTE)
                .obrigatorio(true)
                .tipoCondicao(TipoCondicaoRequisito.SEMPRE)
                .fonteNormativaTipo(FonteNormativaTipo.LEI)
                .fonteNormativaIdentificador("CPC art. 319, II")
                .fundamentoResumido("Qualificação das partes")
                .grauExigibilidade(GrauExigibilidade.ABSOLUTO)
                .nivelSensibilidade(NivelSigilo.PUBLICO)
                .versao("v1.0")
                .build();
    }

    private RequisitoDocumental requisitoCtps() {
        return RequisitoDocumental.builder()
                .id(2L)
                .tipoDocumento(TipoDocumento.CTPS)
                .severidade(SeveridadeCompletude.BLOQUEANTE)
                .obrigatorio(true)
                .tipoCondicao(TipoCondicaoRequisito.SEMPRE)
                .fonteNormativaTipo(FonteNormativaTipo.LEI)
                .fonteNormativaIdentificador("CLT art. 41")
                .fundamentoResumido("Vínculo empregatício")
                .grauExigibilidade(GrauExigibilidade.RELATIVO)
                .nivelSensibilidade(NivelSigilo.PUBLICO)
                .versao("v1.0")
                .build();
    }

    private RequisitoDocumental requisitoProcuracao() {
        return RequisitoDocumental.builder()
                .id(3L)
                .tipoDocumento(TipoDocumento.PROCURACAO)
                .severidade(SeveridadeCompletude.BLOQUEANTE)
                .obrigatorio(true)
                .tipoCondicao(TipoCondicaoRequisito.ADVOGADO_CONSTITUIDO)
                .aplicaARepresentante(TipoRepresentanteProcessual.ADVOGADO_PRIVADO)
                .fonteNormativaTipo(FonteNormativaTipo.LEI)
                .fonteNormativaIdentificador("CPC art. 287")
                .fundamentoResumido("Procuração obrigatória para advogado constituído")
                .grauExigibilidade(GrauExigibilidade.DISPENSAVEL_COM_JUSTIFICATIVA)
                .nivelSensibilidade(NivelSigilo.PUBLICO)
                .versao("v1.0")
                .build();
    }

    private RequisitoDocumental requisitoSensivel() {
        return RequisitoDocumental.builder()
                .id(4L)
                .tipoDocumento(TipoDocumento.LAUDO_MEDICO)
                .severidade(SeveridadeCompletude.BLOQUEANTE)
                .obrigatorio(true)
                .tipoCondicao(TipoCondicaoRequisito.SEMPRE)
                .fonteNormativaTipo(FonteNormativaTipo.REGRA_INTERNA)
                .fundamentoResumido("Laudo exigido para ação de interdição")
                .grauExigibilidade(GrauExigibilidade.ABSOLUTO)
                .nivelSensibilidade(NivelSigilo.SEGREDO_JUSTICA)
                .versao("v1.0")
                .build();
    }

    private RequisitoDocumentalEquivalencia equivalenciaContratoTrabalho(RequisitoDocumental req) {
        return RequisitoDocumentalEquivalencia.builder()
                .id(1L)
                .requisito(req)
                .tipoDocumentoAceito(TipoDocumento.CONTRATO_TRABALHO)
                .justificativa("Contrato de trabalho substitui CTPS")
                .severidadeSeSubstituto(SeveridadeCompletude.ADVERTENCIA)
                .build();
    }
}
