package com.tcc.pjb.backend.modules.advocacia.office.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeGovernedPetitionRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class OfficeGovernedPetitionServiceTest {

    @Test
    void deveRetornarViewComSignatarioPatronalQuandoFilaForExigida() {
        OfficeProcessWorkspaceScopeService scopeService = mock(OfficeProcessWorkspaceScopeService.class);
        OfficeGovernedProcessOperationService operationService = mock(OfficeGovernedProcessOperationService.class);
        OfficeGovernedPetitionService service = new OfficeGovernedPetitionService(scopeService, operationService);

        when(scopeService.access(
                org.mockito.ArgumentMatchers.eq(901L),
                org.mockito.ArgumentMatchers.eq(com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType.PETICIONAR),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(
                new PjbFrontendOfficeProcessAccessView(
                        901L,
                        "0001",
                        44L,
                        "OFFICE",
                        "PETICIONAR",
                        true,
                        true,
                        true,
                        77L,
                        "Dr. Patrono",
                        List.of(),
                        List.of("ASSINATURA_PATRONAL_OBRIGATORIA")
                )
        );
        when(operationService.protocolizarPeticao(901L, "PETICAO_INTERMEDIARIA", "Conteudo", "Fundamentacao")).thenReturn(
                Map.of(
                        "status", "PENDING_SIGNER",
                        "operationId", 5001L,
                        "queueItemId", 7001L,
                        "signerUserId", 77L,
                        "signerNome", "Dr. Patrono",
                        "signerRegistration", "12345/CE",
                        "signatureMode", "PATRONO_CERTIFICATE",
                        "signatureEnvelopeReady", false
                )
        );

        var result = service.submit(901L, new FrontendOfficeGovernedPetitionRequest("PETICAO_INTERMEDIARIA", "Conteudo", "Fundamentacao"), new MockHttpServletRequest());

        assertThat(result.processoId()).isEqualTo(901L);
        assertThat(result.status()).isEqualTo("PENDING_SIGNER");
        assertThat(result.queueRequired()).isTrue();
        assertThat(result.patronCertificateRequired()).isTrue();
        assertThat(result.effectiveSignerUserId()).isEqualTo(77L);
        assertThat(result.effectiveSignerNome()).isEqualTo("Dr. Patrono");
        assertThat(result.effectiveSignerRegistration()).isEqualTo("12345/CE");
        assertThat(result.signatureMode()).isEqualTo("PATRONO_CERTIFICATE");
    }
}
