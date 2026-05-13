package com.tcc.pjb.backend.controller.processual.surface.unificado;

import com.tcc.pjb.backend.model.dto.processual.surface.aggregate.ProcessoSurfaceAggregateResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfaceAtoResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfaceCompetenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfaceDiagnosticoResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoSurfacePerfilResponse;
import com.tcc.pjb.backend.model.dto.processual.surface.unificado.ProcessoUnificadoSurfaceResponse;
import com.tcc.pjb.backend.service.processual.surface.ProcessoSurfaceFacadeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/processual/unificado")
public class ProcessoUnificadoNacionalController {

    private final ProcessoSurfaceFacadeService facadeService;

    public ProcessoUnificadoNacionalController(ProcessoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoUnificadoSurfaceResponse> detalhar(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.detalhar(processoId));
    }

    @GetMapping("/{processoId}/competencia")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceCompetenciaResponse> competencia(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.competencia(processoId));
    }

    @GetMapping("/{processoId}/atos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProcessoSurfaceAtoResponse>> atos(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.atos(processoId));
    }

    @GetMapping("/{processoId}/diagnostico")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceDiagnosticoResponse> diagnostico(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.diagnostico(processoId));
    }

    @GetMapping("/{processoId}/recursal")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> recursal(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.recursal(processoId));
    }

    @GetMapping("/{processoId}/execucao")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> execucao(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.execucao(processoId));
    }

    @GetMapping("/{processoId}/papeis")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfaceAggregateResponse> papeis(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.papeis(processoId));
    }

    @GetMapping("/{processoId}/papeis/{profileCode}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProcessoSurfacePerfilResponse> papel(@PathVariable Long processoId, @PathVariable String profileCode) {
        return ResponseEntity.ok(facadeService.papel(processoId, profileCode));
    }
}
