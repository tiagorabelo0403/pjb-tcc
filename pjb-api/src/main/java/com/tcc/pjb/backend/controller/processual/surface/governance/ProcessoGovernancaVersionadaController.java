package com.tcc.pjb.backend.controller.processual.surface.governance;

import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceAggregateResponse;
import com.tcc.pjb.backend.service.processual.surface.ProcessoSurfaceFacadeService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoGovernancaVersionadaController {

    private final ProcessoSurfaceFacadeService facadeService;

    public ProcessoGovernancaVersionadaController(ProcessoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/dsl")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> dsl(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.dsl(processoId));
    }

    @GetMapping("/{processoId}/policy-vigencia")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> policy(@PathVariable Long processoId,
                                                                   @RequestParam(name = "em", required = false)
                                                                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate em) {
        return ResponseEntity.ok(facadeService.policy(processoId, em));
    }

    @GetMapping("/{processoId}/posse-trabalho")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> posse(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.posse(processoId));
    }
}
