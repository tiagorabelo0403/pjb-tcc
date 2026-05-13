package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.workspace;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProcessDiagnosticReportResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.workspace.NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse;
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
public class NationalCommunicationInstitutionalProcessWorkspaceController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalProcessWorkspaceController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_PAPEIS_PROCESSUAIS)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<NationalCommunicationInstitutionalProcessWorkspaceSummaryResponse>> listar(@RequestParam(required = false) Long processoId,
                                                                                                           @RequestParam(required = false) String rito,
                                                                                                           @RequestParam(required = false) String fase,
                                                                                                           @RequestParam(required = false) String status,
                                                                                                           @RequestParam(required = false) String ramo) {
        return ResponseEntity.ok(facadeService.listarWorkspaces(processoId, rito, fase, status, ramo));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_PAPEIS_PROCESSUAIS_PROFILE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalProcessWorkspaceResponse> detalhar(@PathVariable String profileCode,
                                                                                                @RequestParam(required = false) Long processoId,
                                                                                                @RequestParam(required = false) String rito,
                                                                                                @RequestParam(required = false) String fase,
                                                                                                @RequestParam(required = false) String status,
                                                                                                @RequestParam(required = false) String ramo) {
        return ResponseEntity.ok(facadeService.detalharWorkspace(profileCode, processoId, rito, fase, status, ramo));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_PAPEIS_PROCESSUAIS_DIAGNOSTICO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalProcessDiagnosticReportResponse> diagnosticar(@RequestParam(required = false) Long processoId,
                                                                                                           @RequestParam(required = false) String rito,
                                                                                                           @RequestParam(required = false) String fase,
                                                                                                           @RequestParam(required = false) String status,
                                                                                                           @RequestParam(required = false) String ramo) {
        return ResponseEntity.ok(facadeService.diagnosticarWorkspace(processoId, rito, fase, status, ramo));
    }
}
