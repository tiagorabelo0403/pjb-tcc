package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.security;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.affiliation.NationalCommunicationInstitutionalAffiliationValidationReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalApprovalTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCallTrailCreateRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCallTrailResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialIssueRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationCredentialResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.InstitutionalRemoteCertificateAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSensitiveActAuthorizationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalSessionRiskAssessmentResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalSimpleFundamentosRequest;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalGovernanceSurfaceFacadeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalSecurityGovernanceController {

    private final NationalCommunicationInstitutionalGovernanceSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalSecurityGovernanceController(NationalCommunicationInstitutionalGovernanceSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_VALIDATION)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalAffiliationValidationReportResponse> validacaoAdesao(@PathVariable String requestId) {
        return ResponseEntity.of(facadeService.validacaoAdesao(requestId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DELEGATED_AFFILIATION_APPROVAL_TRAIL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalApprovalTrailResponse> trilhaAprovacao(@PathVariable String requestId) {
        return ResponseEntity.of(facadeService.trilhaAprovacao(requestId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_REMOTE_CERTIFICATE_AUTHORIZATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse> emitirAutorizacaoRemota(@RequestBody InstitutionalRemoteCertificateAuthorizationRequest request) {
        return ResponseEntity.ok(facadeService.emitirAutorizacaoRemota(request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_REMOTE_CERTIFICATE_AUTHORIZATION_REVOKE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse> revogarAutorizacaoRemota(@PathVariable String authorizationId,
                                                                                                                                 @RequestBody(required = false) NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return ResponseEntity.ok(facadeService.revogarAutorizacaoRemota(authorizationId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_REMOTE_CERTIFICATE_AUTHORIZATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalRemoteCertificateAuthorizationResponse>> listarAutorizacoesRemotas(@RequestParam(required = false) String affiliationId,
                                                                                                                                      @RequestParam(required = false) Long userId) {
        return ResponseEntity.ok(facadeService.listarAutorizacoesRemotas(affiliationId, userId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_SESSION_RISK)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalSessionRiskAssessmentResponse> riscoSessao(@RequestParam(required = false) String affiliationId,
                                                                                                        @RequestParam(required = false) String nominationId,
                                                                                                        @RequestParam(required = false) String unidadeCodigo,
                                                                                                        @RequestParam(required = false) String caixaCodigo) {
        return ResponseEntity.ok(facadeService.riscoSessao(affiliationId, nominationId, unidadeCodigo, caixaCodigo));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_SENSITIVE_ACT_AUTHORIZE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalSensitiveActAuthorizationResponse> autorizarAtoSensivel(@RequestBody NationalCommunicationInstitutionalSensitiveActAuthorizationRequest request) {
        return ResponseEntity.ok(facadeService.autorizarAtoSensivel(request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIALS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalIntegrationCredentialResponse> emitirCredencial(@RequestBody NationalCommunicationInstitutionalIntegrationCredentialIssueRequest request) {
        return ResponseEntity.ok(facadeService.emitirCredencial(request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIAL_ROTATE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalIntegrationCredentialResponse> rotacionarCredencial(@PathVariable String credentialId,
                                                                                                                   @RequestBody(required = false) NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return ResponseEntity.ok(facadeService.rotacionarCredencial(credentialId, request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIAL_REVOKE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalIntegrationCredentialResponse> revogarCredencial(@PathVariable String credentialId,
                                                                                                                @RequestBody(required = false) NationalCommunicationInstitutionalSimpleFundamentosRequest request) {
        return ResponseEntity.ok(facadeService.revogarCredencial(credentialId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIALS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalIntegrationCredentialResponse>> listarCredenciais(@RequestParam(required = false) String affiliationId) {
        return ResponseEntity.ok(facadeService.listarCredenciais(affiliationId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIAL_REGISTER_CALL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalIntegrationCallTrailResponse> registrarChamada(@PathVariable String credentialId,
                                                                                                              @RequestBody NationalCommunicationInstitutionalIntegrationCallTrailCreateRequest request) {
        return ResponseEntity.ok(facadeService.registrarChamada(credentialId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_INTEGRATION_CREDENTIAL_TRAIL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalIntegrationCallTrailResponse>> trilhaChamadas(@PathVariable String credentialId) {
        return ResponseEntity.ok(facadeService.trilhaChamadas(credentialId));
    }
}
