package com.tcc.pjb.backend.service.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@Tag("integration")
@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false",
        "pjb.outbox.ingress.enabled=false"
})
class ApiMarketplaceServiceCompletudeDocumentalTest extends PjbIntegrationTestBase {

    @Autowired
    private ApiMarketplaceService service;

    @Autowired
    private ProcessoRepository processoRepository;

    @MockitoBean
    private MarketplaceGovernanceService governanceService;

    @MockitoBean
    private TriagemNacionalIAEngine triagemNacionalIAEngine;

    @Test
    void clienteQueNaoEnviaCampoDocumentosRecebeSinalizacaoPendenteEDocumentosFaltantesListados() {
        stubGovernance();

        ApiMarketplaceService.MarketplaceProtocoloRequest request = baseRequest("0009999-21.2026.8.06.0001", null);

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(request, "client-teste");

        assertThat(result.documentacaoCompleta()).isFalse();
        assertThat(result.documentosFaltantes()).isNotEmpty();

        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getConnectorSubmissionStatus()).isEqualTo("PENDENTE_DOCUMENTACAO");

        verify(governanceService, times(1)).publicarEventoPendenciaDocumental(
                anyString(), any(), anyString(), anyString(), any());
    }

    @Test
    void clienteQueEnviaTodosDocumentosObrigatoriosDoCatalogoCivilRecebeStatusCompleto() {
        stubGovernance();

        List<Attachment> completos = List.of(
                attachment(TipoDocumento.PETICAO_INICIAL),
                attachment(TipoDocumento.PROCURACAO),
                attachment(TipoDocumento.DOCUMENTO_IDENTIDADE),
                attachment(TipoDocumento.COMPROVANTE_ENDERECO),
                attachment(TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS)
        );

        ApiMarketplaceService.MarketplaceProtocoloRequest request = baseRequest("0009999-22.2026.8.06.0001", completos);

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(request, "client-teste");

        assertThat(result.documentacaoCompleta()).isTrue();
        assertThat(result.documentosFaltantes()).isEmpty();

        Processo processo = processoRepository.findById(result.processoId()).orElseThrow();
        assertThat(processo.getConnectorSubmissionStatus()).isEqualTo("RECEBIDO_MARKETPLACE");

        verify(governanceService, never()).publicarEventoPendenciaDocumental(
                anyString(), any(), anyString(), anyString(), any());
    }

    @Test
    void clienteQueEnviaDocumentosParciaisListaExatamenteOsFaltantes() {
        stubGovernance();

        List<Attachment> parciais = List.of(
                attachment(TipoDocumento.PETICAO_INICIAL),
                attachment(TipoDocumento.PROCURACAO)
        );

        ApiMarketplaceService.MarketplaceProtocoloRequest request = baseRequest("0009999-23.2026.8.06.0001", parciais);

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(request, "client-teste");

        assertThat(result.documentacaoCompleta()).isFalse();
        assertThat(result.documentosFaltantes())
                .containsExactlyInAnyOrder(
                        TipoDocumento.DOCUMENTO_IDENTIDADE.name(),
                        TipoDocumento.COMPROVANTE_ENDERECO.name(),
                        TipoDocumento.PROVAS_DOCUMENTAIS_BASICAS.name());
    }

    private Attachment attachment(TipoDocumento tipo) {
        return Attachment.builder().tipoDocumento(tipo).build();
    }

    private void stubGovernance() {
        doNothing().when(governanceService).assertClientCanProtocol(anyString());
        doNothing().when(governanceService).registrarConsumoProtocolo(anyString());
        when(governanceService.publicarEventoProtocolo(anyString(), any(), anyString(), anyString()))
                .thenReturn(List.of());
        when(governanceService.publicarEventoPendenciaDocumental(anyString(), any(), anyString(), anyString(), any()))
                .thenReturn(List.of());
    }

    private ApiMarketplaceService.MarketplaceProtocoloRequest baseRequest(String numeroExterno, List<Attachment> documentos) {
        return new ApiMarketplaceService.MarketplaceProtocoloRequest(
                "ref-" + numeroExterno,
                numeroExterno,
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
