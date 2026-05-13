package com.tcc.pjb.backend.controller.processual.surface.evolution;

import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceAggregateResponse;
import com.tcc.pjb.backend.service.processual.surface.ProcessoSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoEvolucaoOperacionalController {

    private final ProcessoSurfaceFacadeService facadeService;

    public ProcessoEvolucaoOperacionalController(ProcessoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/linha-do-tempo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> timeline(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.timeline(processoId));
    }

    @GetMapping("/{processoId}/integracoes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> integracoes(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.integracoes(processoId));
    }

    @GetMapping("/{processoId}/migracao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> migracao(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.migracao(processoId));
    }
}
