package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalBindingApprovalApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalContextActivationGuardApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalIdentityGuardApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalRepresentativeVerificationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalStepUpAuthenticationPolicyApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.application.InstitutionalTextClosureApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalBindingApproval;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalContextActivationDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalIdentityGuardDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalRepresentativeVerification;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalStepUpAuthenticationPolicy;
import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalTextClosureAudit;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalIdentityGuardResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalRepresentativeVerificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.entry.NationalCommunicationInstitutionalTextClosureAuditResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalBindingApprovalResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalStepUpPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalContextActivationResponse;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalAccessGuardSurfaceServiceTest {

    private final InstitutionalRepresentativeVerificationApplicationService representativeVerificationApplicationService = mock(InstitutionalRepresentativeVerificationApplicationService.class);
    private final InstitutionalBindingApprovalApplicationService bindingApprovalApplicationService = mock(InstitutionalBindingApprovalApplicationService.class);
    private final InstitutionalIdentityGuardApplicationService identityGuardApplicationService = mock(InstitutionalIdentityGuardApplicationService.class);
    private final InstitutionalStepUpAuthenticationPolicyApplicationService stepUpAuthenticationPolicyApplicationService = mock(InstitutionalStepUpAuthenticationPolicyApplicationService.class);
    private final InstitutionalContextActivationGuardApplicationService contextActivationGuardApplicationService = mock(InstitutionalContextActivationGuardApplicationService.class);
    private final InstitutionalTextClosureApplicationService textClosureApplicationService = mock(InstitutionalTextClosureApplicationService.class);
    private final NationalCommunicationInstitutionalSurfaceAssemblerSupport surfaceAssemblerSupport = mock(NationalCommunicationInstitutionalSurfaceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalAccessGuardSurfaceService service = new NationalCommunicationInstitutionalAccessGuardSurfaceService(
            representativeVerificationApplicationService, bindingApprovalApplicationService, identityGuardApplicationService,
            stepUpAuthenticationPolicyApplicationService, contextActivationGuardApplicationService, textClosureApplicationService, surfaceAssemblerSupport);

    @Test
    void verificarRepresentanteDelegaEMapeiaQuandoExiste() {
        var domain = mock(InstitutionalRepresentativeVerification.class);
        var response = mock(NationalCommunicationInstitutionalRepresentativeVerificationResponse.class);
        when(representativeVerificationApplicationService.buscarSeExistir("req-1")).thenReturn(Optional.of(domain));
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.verificarRepresentante("req-1")).contains(response);
    }

    @Test
    void verificarRepresentanteRetornaVazioQuandoNaoExiste() {
        when(representativeVerificationApplicationService.buscarSeExistir("req-2")).thenReturn(Optional.empty());

        assertThat(service.verificarRepresentante("req-2")).isEmpty();
    }

    @Test
    void aprovacaoVinculoDelegaEMapeia() {
        var domain = mock(InstitutionalBindingApproval.class);
        var response = mock(NationalCommunicationInstitutionalBindingApprovalResponse.class);
        when(bindingApprovalApplicationService.avaliarAtual("aff-1", "nom-1")).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.aprovacaoVinculo("aff-1", "nom-1")).isSameAs(response);
    }

    @Test
    void guardaIdentidadeDelegaEMapeia() {
        var domain = mock(InstitutionalIdentityGuardDecision.class);
        var response = mock(NationalCommunicationInstitutionalIdentityGuardResponse.class);
        when(identityGuardApplicationService.avaliarAtual()).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.guardaIdentidade()).isSameAs(response);
    }

    @Test
    void politicaStepUpDelegaEMapeia() {
        var domain = mock(InstitutionalStepUpAuthenticationPolicy.class);
        var response = mock(NationalCommunicationInstitutionalStepUpPolicyResponse.class);
        when(stepUpAuthenticationPolicyApplicationService.avaliarAtual("aff-1", "nom-1", "SENSITIVE")).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.politicaStepUp("aff-1", "nom-1", "SENSITIVE")).isSameAs(response);
    }

    @Test
    void ativacaoContextoDelegaEMapeia() {
        var domain = mock(InstitutionalContextActivationDecision.class);
        var response = mock(NationalCommunicationInstitutionalContextActivationResponse.class);
        when(contextActivationGuardApplicationService.avaliarAtual("aff-1", "nom-1", "UNI-1", "CX-1", "SENSITIVE")).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.ativacaoContexto("aff-1", "nom-1", "UNI-1", "CX-1", "SENSITIVE")).isSameAs(response);
    }

    @Test
    void fechamentoTextoDelegaEMapeia() {
        var domain = mock(InstitutionalTextClosureAudit.class);
        var response = mock(NationalCommunicationInstitutionalTextClosureAuditResponse.class);
        when(textClosureApplicationService.auditar()).thenReturn(domain);
        when(surfaceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.fechamentoTexto()).isSameAs(response);
    }
}
