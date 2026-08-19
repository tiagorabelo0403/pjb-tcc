package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.api.MarketplaceRepresentacaoResolver;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Cobertura sem Testcontainers/Spring context: valida a decisão de completude documental
 * de {@link ApiMarketplaceService#protocolar} isoladamente, sem depender de Docker.
 *
 * <p>Desde a correção da Fase 2, a completude é calculada a partir do que
 * {@link DocumentoProcessualRepository#findByProcessoId(Long)} devolve como efetivamente
 * persistido — não mais do que o cliente meramente declarou em {@code request.documentos()}.
 * Cada teste, portanto, controla separadamente "o que o cliente anexou na requisição" e
 * "o que o repositório reporta como persistido", estubando este último via
 * {@link #stubDocumentosPersistidos(TipoDocumento...)}.
 */
class ApiMarketplaceServiceCompletudeDocumentalUnitTest {

    private AjuizamentoService ajuizamentoService;
    private MarketplaceGovernanceService governanceService;
    private MarketplaceDocumentoPersistenceService documentoPersistenceService;
    private DocumentoProcessualRepository documentoRepository;
    private ProcessoRepository processoRepository;
    private ApiMarketplaceService service;

    @BeforeEach
    void setUp() {
        ajuizamentoService = mock(AjuizamentoService.class);
        governanceService = mock(MarketplaceGovernanceService.class);
        documentoPersistenceService = mock(MarketplaceDocumentoPersistenceService.class);
        documentoRepository = mock(DocumentoProcessualRepository.class);
        processoRepository = mock(ProcessoRepository.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        when(comarcaResolutionService.resolver(any(), any())).thenReturn(Optional.empty());
        when(ajuizamentoService.ajuizar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(processoRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ApiMarketplaceService(ajuizamentoService, governanceService, new CompletudeDocumentalPolicyService(),
                new MarketplaceRepresentacaoResolver(new RepresentacaoProcessualPolicyService()), documentoPersistenceService,
                documentoRepository, processoRepository, comarcaResolutionService);
    }

    private void stubDocumentosPersistidos(TipoDocumento... tipos) {
        List<DocumentoProcessual> persistidos = Arrays.stream(tipos)
                .map(tipo -> DocumentoProcessual.builder().tipoDocumento(tipo).build())
                .toList();
        when(documentoRepository.findByProcessoId(any())).thenReturn(persistidos);
    }

    @Test
    void clienteQueNaoEnviaCampoDocumentosRecebeSinalizacaoPendenteEDocumentosFaltantesListados() {
        ApiMarketplaceService.MarketplaceProtocoloRequest request = baseRequest(null);

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(request, "client-teste");

        assertThat(result.documentacaoCompleta()).isFalse();
        assertThat(result.documentosFaltantes()).isNotEmpty();
        assertThat(result.status()).isEqualTo("PENDENTE_DOCUMENTACAO");

        ArgumentCaptor<Processo> ajuizarCaptor = ArgumentCaptor.forClass(Processo.class);
        org.mockito.Mockito.verify(ajuizamentoService).ajuizar(ajuizarCaptor.capture());
        assertThat(ajuizarCaptor.getValue().getStatusProcesso().name()).isEqualTo("DISTRIBUIDO");

        ArgumentCaptor<Processo> saveCaptor = ArgumentCaptor.forClass(Processo.class);
        org.mockito.Mockito.verify(processoRepository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getConnectorSubmissionStatus()).isEqualTo("PENDENTE_DOCUMENTACAO");

        verify(governanceService, times(1)).publicarEventoPendenciaDocumental(
                anyString(), any(), anyString(), anyString(), any());
        verify(governanceService, times(1)).publicarEventoProtocolo(anyString(), any(), anyString(), anyString());
    }

    @Test
    void clienteQueEnviaTodosDocumentosObrigatoriosDoCatalogoCivilRecebeStatusCompleto() {
        List<Attachment> completos = List.of(
                attachment(TipoDocumento.PETICAO_INICIAL),
                attachment(TipoDocumento.PROCURACAO),
                attachment(TipoDocumento.DOCUMENTO_IDENTIDADE),
                attachment(TipoDocumento.COMPROVANTE_ENDERECO),
                attachment(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
        );
        stubDocumentosPersistidos(TipoDocumento.PETICAO_INICIAL, TipoDocumento.PROCURACAO,
                TipoDocumento.DOCUMENTO_IDENTIDADE, TipoDocumento.COMPROVANTE_ENDERECO,
                TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS);

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(baseRequest(completos), "client-teste");

        assertThat(result.documentacaoCompleta()).isTrue();
        assertThat(result.documentosFaltantes()).isEmpty();
        assertThat(result.status()).isEqualTo("RECEBIDO_MARKETPLACE");

        verify(governanceService, never()).publicarEventoPendenciaDocumental(
                anyString(), any(), anyString(), anyString(), any());
        verify(governanceService, times(1)).publicarEventoProtocolo(anyString(), any(), anyString(), anyString());
    }

    @Test
    void clienteQueEnviaDocumentosParciaisListaExatamenteOsFaltantes() {
        List<Attachment> parciais = List.of(
                attachment(TipoDocumento.PETICAO_INICIAL),
                attachment(TipoDocumento.PROCURACAO)
        );
        stubDocumentosPersistidos(TipoDocumento.PETICAO_INICIAL, TipoDocumento.PROCURACAO);

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(baseRequest(parciais), "client-teste");

        assertThat(result.documentacaoCompleta()).isFalse();
        assertThat(result.documentosFaltantes())
                .containsExactlyInAnyOrder(
                        TipoDocumento.DOCUMENTO_IDENTIDADE.name(),
                        TipoDocumento.COMPROVANTE_ENDERECO.name(),
                        TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS.name());
    }

    @Test
    void documentoDeclaradoSemConteudoNaoPersistidoNaoContaComoCompleto() {
        List<Attachment> declarados = List.of(
                attachment(TipoDocumento.PETICAO_INICIAL),
                attachment(TipoDocumento.PROCURACAO),
                attachment(TipoDocumento.DOCUMENTO_IDENTIDADE),
                attachment(TipoDocumento.COMPROVANTE_ENDERECO),
                attachment(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
        );
        stubDocumentosPersistidos(TipoDocumento.PETICAO_INICIAL, TipoDocumento.PROCURACAO);

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(baseRequest(declarados), "client-teste");

        assertThat(result.documentacaoCompleta()).isFalse();
        assertThat(result.status()).isEqualTo("PENDENTE_DOCUMENTACAO");
        assertThat(result.documentosFaltantes())
                .containsExactlyInAnyOrder(
                        TipoDocumento.DOCUMENTO_IDENTIDADE.name(),
                        TipoDocumento.COMPROVANTE_ENDERECO.name(),
                        TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS.name());

        verify(governanceService, times(1)).publicarEventoPendenciaDocumental(
                anyString(), any(), anyString(), anyString(), any());
    }

    @Test
    void clienteComPerfilAtorCidadaoEmRitoJuizadoDispensaProcuracao() {
        List<Attachment> semProcuracao = List.of(
                attachment(TipoDocumento.PETICAO_INICIAL),
                attachment(TipoDocumento.DOCUMENTO_IDENTIDADE),
                attachment(TipoDocumento.COMPROVANTE_ENDERECO),
                attachment(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
        );

        stubDocumentosPersistidos(TipoDocumento.PETICAO_INICIAL, TipoDocumento.DOCUMENTO_IDENTIDADE,
                TipoDocumento.COMPROVANTE_ENDERECO, TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS);

        var request = baseRequestComPerfil(semProcuracao, "CIDADAO");
        var result = service.protocolar(request, "client-teste");

        assertThat(result.documentacaoCompleta()).isTrue();
        assertThat(result.documentosFaltantes()).isEmpty();
    }

    @Test
    void clienteSemPerfilAtorContinuaExigindoProcuracao() {
        List<Attachment> semProcuracao = List.of(
                attachment(TipoDocumento.PETICAO_INICIAL),
                attachment(TipoDocumento.DOCUMENTO_IDENTIDADE),
                attachment(TipoDocumento.COMPROVANTE_ENDERECO),
                attachment(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
        );

        stubDocumentosPersistidos(TipoDocumento.PETICAO_INICIAL, TipoDocumento.DOCUMENTO_IDENTIDADE,
                TipoDocumento.COMPROVANTE_ENDERECO, TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS);

        var request = baseRequestComPerfil(semProcuracao, null);
        var result = service.protocolar(request, "client-teste");

        assertThat(result.documentacaoCompleta()).isFalse();
        assertThat(result.documentosFaltantes()).containsExactly(TipoDocumento.PROCURACAO.name());
    }

    private ApiMarketplaceService.MarketplaceProtocoloRequest baseRequestComPerfil(List<Attachment> documentos, String perfilAtor) {
        return new ApiMarketplaceService.MarketplaceProtocoloRequest(
                "ref-0009999-30.2026.8.06.0001", "0009999-30.2026.8.06.0001", "ESTADUAL", "CIVIL", "CE", "Fortaleza",
                "JUIZADO_ESPECIAL_CIVEL", "Cobranca via marketplace", "Condenacao ao pagamento", null,
                "Cliente Marketplace Ltda", "12345678000199", "Fornecedor Reu Ltda", "98765432000188",
                BigDecimal.valueOf(5000), null, null, null, null, false, documentos, perfilAtor);
    }

    private Attachment attachment(TipoDocumento tipo) {
        return Attachment.builder().tipoDocumento(tipo).build();
    }

    private ApiMarketplaceService.MarketplaceProtocoloRequest baseRequest(List<Attachment> documentos) {
        return new ApiMarketplaceService.MarketplaceProtocoloRequest(
                "ref-0009999-20.2026.8.06.0001",
                "0009999-20.2026.8.06.0001",
                "ESTADUAL",
                "CIVIL",
                "CE",
                "Fortaleza",
                "ACAO_DE_COBRANCA",
                "Cobranca via marketplace",
                "Condenacao ao pagamento",
                null,
                "Cliente Marketplace Ltda",
                "12345678000199",
                "Fornecedor Reu Ltda",
                "98765432000188",
                BigDecimal.valueOf(5000),
                null,
                null,
                null,
                null,
                false,
                documentos,
                null
        );
    }
}
