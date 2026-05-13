package com.tcc.pjb.backend.controller.processual.surface.unificado;

import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceValueItemResponse;
import com.tcc.pjb.backend.service.processual.surface.ProcessoSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoOrquestracaoUnificadaController {

    private final ProcessoSurfaceFacadeService facadeService;

    public ProcessoOrquestracaoUnificadaController(ProcessoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/prazos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> prazos(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.prazos(processoId));
    }

    @GetMapping("/{processoId}/prazos/{tipoPrazo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceValueItemResponse> prazoEspecifico(@PathVariable Long processoId,
                                                                            @PathVariable String tipoPrazo) {
        return ResponseEntity.ok(facadeService.prazoEspecifico(processoId, tipoPrazo));
    }

    @GetMapping("/{processoId}/workstream")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> workstream(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.workstream(processoId));
    }

    @GetMapping("/{processoId}/documentos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> documentos(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.documentos(processoId));
    }
}
