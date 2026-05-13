package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.governance;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalFourLevelAccessResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationalCaseResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalStructuralDiagnosticResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalClosureController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalClosureController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_FOUR_LEVELS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalFourLevelAccessResponse> quatroNiveis(@RequestParam(required = false) String affiliationId) {
        return ResponseEntity.ok(facadeService.quatroNiveis(affiliationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_AFFILIATION_OPERATIONAL_CASES)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalOperationalCaseResponse>> casosOperacionais(@PathVariable String affiliationId) {
        return ResponseEntity.ok(facadeService.casosOperacionais(affiliationId));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_STRUCTURAL_DIAGNOSTIC)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalStructuralDiagnosticResponse> diagnosticoEstrutural(@RequestParam(required = false) String affiliationId) {
        return ResponseEntity.ok(facadeService.diagnosticoEstrutural(affiliationId));
    }
}
