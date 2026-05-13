package com.tcc.pjb.backend.controller.processual.surface.hardening;

import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceAggregateResponse;
import com.tcc.pjb.backend.service.processual.surface.ProcessoSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoFatiasSensivelController {

    private final ProcessoSurfaceFacadeService facadeService;

    public ProcessoFatiasSensivelController(ProcessoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/pre-gravacao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> preGravacao(@PathVariable Long processoId,
                                                                        @RequestParam String profileCode,
                                                                        @RequestParam String actionCode) {
        return ResponseEntity.ok(facadeService.preGravacao(processoId, profileCode, actionCode));
    }

    @GetMapping("/{processoId}/fatias/civel-primeiro-grau")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> fatiaCivel(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.fatiaCivel(processoId));
    }

    @GetMapping("/{processoId}/fatias/penal-custodia")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> fatiaPenalCustodia(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.fatiaPenalCustodia(processoId));
    }
}
