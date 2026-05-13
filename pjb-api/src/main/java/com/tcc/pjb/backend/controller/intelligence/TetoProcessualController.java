package com.tcc.pjb.backend.controller.intelligence;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.teto.SalarioMinimoUpsertRequest;
import com.tcc.pjb.backend.model.dto.teto.TetoProcessualDiagnosticoRequest;
import com.tcc.pjb.backend.service.intelligence.surface.IntelligenceOperationalSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intelligence/teto")
@PreAuthorize("isAuthenticated()")
public class TetoProcessualController {

    private final IntelligenceOperationalSurfaceFacadeService facadeService;

    public TetoProcessualController(IntelligenceOperationalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/processo/{processoId}")
    public ResponseEntity<SurfaceSnapshotResponse> diagnosticarProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.diagnosticarTetoProcesso(processoId));
    }

    @PostMapping("/diagnosticar")
    public ResponseEntity<SurfaceSnapshotResponse> diagnosticar(@RequestBody TetoProcessualDiagnosticoRequest request) {
        return ResponseEntity.ok(facadeService.diagnosticarTeto(request));
    }

    @GetMapping("/salario-minimo")
    public ResponseEntity<SurfaceCollectionResponse> listarSalariosMinimos() {
        return ResponseEntity.ok(facadeService.listarSalariosMinimos());
    }

    @PostMapping("/salario-minimo")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
    public ResponseEntity<SurfaceSnapshotResponse> salvarSalarioMinimo(@RequestBody SalarioMinimoUpsertRequest request) {
        return ResponseEntity.ok(facadeService.salvarSalarioMinimo(request));
    }
}
