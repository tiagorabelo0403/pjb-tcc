package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.access;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalAccessContextResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology.NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalProfileResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustGovernanceDecisionRequest;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalTrustGovernanceProfileResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalGovernanceSurfaceFacadeService;
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
public class NationalCommunicationInstitutionalAccessOrchestrationController {

    private final NationalCommunicationInstitutionalGovernanceSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalAccessOrchestrationController(NationalCommunicationInstitutionalGovernanceSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_DIMENSIONAMENTO_USUARIOS_INTERNOS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse> dimensionamentoUsuariosInternos() {
        return ResponseEntity.ok(facadeService.dimensionamentoUsuariosInternos());
    }

    @GetMapping(InstitutionalApiRoutes.PATH_GOVERNANCA_CONFIANCA)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalTrustGovernanceProfileResponse> governancaConfianca(@PathVariable String nominationId,
                                                                                                                   @RequestParam(required = false) String affiliationId) {
        return ResponseEntity.ok(facadeService.governancaConfianca(affiliationId, nominationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_ACCESS_CONTEXT)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalAccessContextResponse> contextoAcesso(@PathVariable String affiliationId,
                                                                                                   @RequestParam(required = false) String nominationId) {
        return ResponseEntity.ok(facadeService.contextoAcesso(affiliationId, nominationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_PLANO_DADOS_HORIZONTAL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalHorizontalDataPlanePlanResponse> planoDadosHorizontal(@PathVariable String nominationId,
                                                                                                                     @RequestParam(required = false) String affiliationId) {
        return ResponseEntity.ok(facadeService.planoDadosHorizontal(affiliationId, nominationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_PERFIL_OPERACIONAL)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalOperationalProfileResponse> perfilOperacional(@PathVariable String nominationId,
                                                                                                            @RequestParam(required = false) String affiliationId) {
        return ResponseEntity.ok(facadeService.perfilOperacional(affiliationId, nominationId));
    }

    @PostMapping(InstitutionalApiRoutes.PATH_GOVERNANCA_CONFIANCA_DECISOES)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalTrustGovernanceProfileResponse> decidirGovernancaConfianca(@PathVariable String nominationId,
                                                                                                                          @RequestParam(required = false) String affiliationId,
                                                                                                                          @RequestBody NationalCommunicationInstitutionalTrustGovernanceDecisionRequest request) {
        return ResponseEntity.ok(facadeService.decidirGovernancaConfianca(affiliationId, nominationId, request));
    }
}
