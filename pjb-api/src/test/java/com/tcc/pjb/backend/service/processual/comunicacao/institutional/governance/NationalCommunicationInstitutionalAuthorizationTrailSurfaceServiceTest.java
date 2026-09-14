package com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationApprovalTrailApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationValidationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRemoteCertificateAuthorizationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSensitiveActAuthorizationApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSessionRiskApplicationService;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationValidationReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalApprovalTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSimpleFundamentosRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.InstitutionalRemoteCertificateAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSessionRiskAssessmentResponse;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSensitiveAct;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NationalCommunicationInstitutionalAuthorizationTrailSurfaceServiceTest {

    private final InstitutionalAffiliationValidationApplicationService validationApplicationService = mock(InstitutionalAffiliationValidationApplicationService.class);
    private final InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService = mock(InstitutionalAffiliationApprovalTrailApplicationService.class);
    private final InstitutionalRemoteCertificateAuthorizationApplicationService remoteCertificateAuthorizationApplicationService = mock(InstitutionalRemoteCertificateAuthorizationApplicationService.class);
    private final InstitutionalSessionRiskApplicationService sessionRiskApplicationService = mock(InstitutionalSessionRiskApplicationService.class);
    private final InstitutionalSensitiveActAuthorizationApplicationService sensitiveActAuthorizationApplicationService = mock(InstitutionalSensitiveActAuthorizationApplicationService.class);
    private final NationalCommunicationInstitutionalGovernanceAssemblerSupport governanceAssemblerSupport = mock(NationalCommunicationInstitutionalGovernanceAssemblerSupport.class);
    private final NationalCommunicationInstitutionalAuthorizationTrailSurfaceService service = new NationalCommunicationInstitutionalAuthorizationTrailSurfaceService(
            validationApplicationService, approvalTrailApplicationService, remoteCertificateAuthorizationApplicationService,
            sessionRiskApplicationService, sensitiveActAuthorizationApplicationService, governanceAssemblerSupport);

    @Test
    void validacaoAdesaoDelegaEMapeiaQuandoPresente() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationReport.class);
        var response = mock(NationalCommunicationInstitutionalAffiliationValidationReportResponse.class);
        when(validationApplicationService.buscarUltimo("req-1")).thenReturn(Optional.of(domain));
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.validacaoAdesao("req-1")).contains(response);
    }

    @Test
    void validacaoAdesaoRetornaVazioQuandoAusente() {
        when(validationApplicationService.buscarUltimo("req-2")).thenReturn(Optional.empty());

        assertThat(service.validacaoAdesao("req-2")).isEmpty();
    }

    @Test
    void trilhaAprovacaoDelegaEMapeiaQuandoPresente() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationApprovalTrail.class);
        var response = mock(NationalCommunicationInstitutionalApprovalTrailResponse.class);
        when(approvalTrailApplicationService.buscarUltima("req-3")).thenReturn(Optional.of(domain));
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.trilhaAprovacao("req-3")).contains(response);
    }

    @Test
    void emitirAutorizacaoRemotaPassaTodos7CamposDoRequest() {
        var request = new InstitutionalRemoteCertificateAuthorizationRequest("aff-1", 42L, "razao", List.of("10.0.0.0/8"), List.of("dev-1"), 24, List.of("fund"));
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRemoteCertificateAuthorization.class);
        var response = mock(NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse.class);
        when(remoteCertificateAuthorizationApplicationService.emitir("aff-1", 42L, "razao", List.of("10.0.0.0/8"), List.of("dev-1"), 24, List.of("fund"))).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.emitirAutorizacaoRemota(request)).isSameAs(response);
    }

    @Test
    void riscoSessaoDelegaComOs4Campos() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskAssessment.class);
        var response = mock(NationalCommunicationInstitutionalSessionRiskAssessmentResponse.class);
        when(sessionRiskApplicationService.avaliarAtual("aff-2", "nom-1", "UNI-1", "CX-1")).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.riscoSessao("aff-2", "nom-1", "UNI-1", "CX-1")).isSameAs(response);
    }

    @Test
    void autorizarAtoSensivelResolveEnumEDelega() {
        var request = new NationalCommunicationInstitutionalSensitiveActAuthorizationRequest("PETICIONAR_EM_NOME_DO_ORGAO", "aff-3", "nom-2");
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSensitiveActAuthorization.class);
        var response = mock(NationalCommunicationInstitutionalSensitiveActAuthorizationResponse.class);
        when(sensitiveActAuthorizationApplicationService.autorizar(InstitutionalSensitiveAct.PETICIONAR_EM_NOME_DO_ORGAO, "aff-3", "nom-2")).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.autorizarAtoSensivel(request)).isSameAs(response);
    }

    @Test
    void autorizarAtoSensivelRejeitaEnumInvalido() {
        var request = new NationalCommunicationInstitutionalSensitiveActAuthorizationRequest("ATO_INEXISTENTE", "aff-x", "nom-x");

        assertThatThrownBy(() -> service.autorizarAtoSensivel(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não reconhecido");
    }

    @Test
    void revogarAutorizacaoRemotaRequestNuloUsaListaVazia() {
        var domain = mock(com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRemoteCertificateAuthorization.class);
        var response = mock(NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse.class);
        when(remoteCertificateAuthorizationApplicationService.revogar("auth-1", List.of())).thenReturn(domain);
        when(governanceAssemblerSupport.toResponse(domain)).thenReturn(response);

        assertThat(service.revogarAutorizacaoRemota("auth-1", null)).isSameAs(response);
    }
}
