package com.tcc.pjb.backend.controller.processual.surface.hardening;

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
public class ProcessoFechamentoAvancadoController {

    private final ProcessoSurfaceFacadeService facadeService;

    public ProcessoFechamentoAvancadoController(ProcessoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/fatias/execucao-fiscal-fazendaria")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> fatiaExecucaoFiscal(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.fatiaExecucaoFiscal(processoId));
    }

    @GetMapping("/{processoId}/sigilo")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> sigilo(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.sigilo(processoId));
    }

    @GetMapping("/{processoId}/hardening-final")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> hardening(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.hardening(processoId));
    }
}
