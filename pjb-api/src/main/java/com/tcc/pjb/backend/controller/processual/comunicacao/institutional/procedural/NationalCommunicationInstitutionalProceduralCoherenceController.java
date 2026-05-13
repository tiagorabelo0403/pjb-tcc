package com.tcc.pjb.backend.controller.processual.comunicacao.institutional.procedural;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalHttpRoutes.InstitutionalApiRoutes;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralActEvaluationResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.procedural.InstitutionalProceduralCoherenceReportResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InstitutionalApiRoutes.CANONICAL_BASE)
public class NationalCommunicationInstitutionalProceduralCoherenceController {

    private final NationalCommunicationInstitutionalSurfaceFacadeService facadeService;

    public NationalCommunicationInstitutionalProceduralCoherenceController(NationalCommunicationInstitutionalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping(InstitutionalApiRoutes.PATH_COERENCIA_PROCESSUAL_DIAGNOSTICO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<InstitutionalProceduralCoherenceReportResponse> diagnosticar(@RequestParam(required = false) Long processoId,
                                                                                                                       @RequestParam(required = false) String rito,
                                                                                                                       @RequestParam(required = false) String fase,
                                                                                                                       @RequestParam(required = false) String status,
                                                                                                                       @RequestParam(required = false) String ramo) {
        return ResponseEntity.ok(facadeService.diagnosticarCoerencia(processoId, rito, fase, status, ramo));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_COERENCIA_PROCESSUAL_PROFILE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalProceduralCoherenceAggregateResponse> detalhar(@PathVariable String profileCode,
                                                                                                            @RequestParam(required = false) Long processoId,
                                                                                                            @RequestParam(required = false) String rito,
                                                                                                            @RequestParam(required = false) String fase,
                                                                                                            @RequestParam(required = false) String status,
                                                                                                            @RequestParam(required = false) String ramo) {
        return ResponseEntity.ok(facadeService.detalharCoerencia(profileCode, processoId, rito, fase, status, ramo));
    }

    @GetMapping(InstitutionalApiRoutes.PATH_COERENCIA_PROCESSUAL_ATO)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NationalCommunicationInstitutionalProceduralActEvaluationResponse> avaliarAto(@PathVariable String profileCode,
                                                                                                          @PathVariable String actionCode,
                                                                                                          @RequestParam(required = false) Long processoId,
                                                                                                          @RequestParam(required = false) String rito,
                                                                                                          @RequestParam(required = false) String fase,
                                                                                                          @RequestParam(required = false) String status,
                                                                                                          @RequestParam(required = false) String ramo) {
        return ResponseEntity.ok(facadeService.avaliarAtoCoerencia(profileCode, actionCode, processoId, rito, fase, status, ramo));
    }
}
