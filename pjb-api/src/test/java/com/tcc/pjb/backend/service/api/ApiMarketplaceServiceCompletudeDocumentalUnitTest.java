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
import com.tcc.pjb.backend.model.entity.enums.processual.TipoDocumento;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.completude.CompletudeDocumentalPolicyService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Cobertura sem Testcontainers/Spring context: valida a decisão de completude documental
 * de {@link ApiMarketplaceService#protocolar} isoladamente, sem depender de Docker.
 */
class ApiMarketplaceServiceCompletudeDocumentalUnitTest {

    private AjuizamentoService ajuizamentoService;
    private MarketplaceGovernanceService governanceService;
    private ApiMarketplaceService service;

    @BeforeEach
    void setUp() {
        ajuizamentoService = mock(AjuizamentoService.class);
        governanceService = mock(MarketplaceGovernanceService.class);
        when(ajuizamentoService.ajuizar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service = new ApiMarketplaceService(ajuizamentoService, governanceService, new CompletudeDocumentalPolicyService());
    }

    @Test
    void clienteQueNaoEnviaCampoDocumentosRecebeSinalizacaoPendenteEDocumentosFaltantesListados() {
        ApiMarketplaceService.MarketplaceProtocoloRequest request = baseRequest(null);

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(request, "client-teste");

        assertThat(result.documentacaoCompleta()).isFalse();
        assertThat(result.documentosFaltantes()).isNotEmpty();
        assertThat(result.status()).isEqualTo("PENDENTE_DOCUMENTACAO");

        ArgumentCaptor<Processo> captor = ArgumentCaptor.forClass(Processo.class);
        org.mockito.Mockito.verify(ajuizamentoService).ajuizar(captor.capture());
        assertThat(captor.getValue().getConnectorSubmissionStatus()).isEqualTo("PENDENTE_DOCUMENTACAO");
        assertThat(captor.getValue().getStatusProcesso().name()).isEqualTo("DISTRIBUIDO");

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

        ApiMarketplaceService.MarketplaceProtocoloResponse result = service.protocolar(baseRequest(parciais), "client-teste");

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
                documentos
        );
    }
}
