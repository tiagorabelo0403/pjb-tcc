package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRecertificationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRevocationRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.security.NationalCommunicationInstitutionalRevocationResponse;
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
public class NationalCommunicationInstitutionalGovernanceHardeningController {

    private final NationalCommunicationInstitutionalGovernanceSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalGovernanceHardeningController(NationalCommunicationInstitutionalGovernanceSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_RECERTIFICATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalRecertificationResponse>> recertificacoes(@RequestParam(required = false) String scope) {
        return ResponseEntity.ok(facadeService.recertificacoes(scope));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_RECERTIFY)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalRecertificationResponse> recertificar(@PathVariable String affiliationId,
                                                                                                   @RequestBody(required = false) NationalCommunicationInstitutionalRecertificationRequest request) {
        return ResponseEntity.ok(facadeService.recertificar(affiliationId, request));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_AFFILIATION_REVOKE_ACCESS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalRevocationResponse> revogarAcessos(@PathVariable String affiliationId,
                                                                                                @RequestBody(required = false) NationalCommunicationInstitutionalRevocationRequest request) {
        return ResponseEntity.ok(facadeService.revogarAcessos(affiliationId, request));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_GOVERNANCE_INTEGRATIONS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalIntegrationSecurityPolicyResponse>> integracoesGovernanca(@RequestParam(required = false) String scope,
                                                                                                                           @RequestParam(required = false) String affiliationId) {
        return ResponseEntity.ok(facadeService.integracoesGovernanca(scope, affiliationId));
    }
}
