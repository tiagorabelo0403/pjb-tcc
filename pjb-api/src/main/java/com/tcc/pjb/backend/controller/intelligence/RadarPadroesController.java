package com.tcc.pjb.backend.controller.intelligence;

import com.tcc.pjb.backend.model.dto.radar.RadarPadroesRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.intelligence.surface.IntelligenceOperationalSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/intelligence/radar")
@Validated
@PreAuthorize("isAuthenticated()")
public class RadarPadroesController {

    private final IntelligenceOperationalSurfaceFacadeService facadeService;

    public RadarPadroesController(IntelligenceOperationalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping("/analisar")
    public ResponseEntity<SurfaceSnapshotResponse> analisar(@RequestBody RadarPadroesRequest request) {
        return ResponseEntity.ok(facadeService.analisarRadar(request));
    }

    @GetMapping("/processo/{processoId}/analisar")
    public ResponseEntity<SurfaceSnapshotResponse> analisarProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.analisarRadarProcesso(processoId));
    }

    @GetMapping("/processo/{processoId}/latest")
    public ResponseEntity<SurfaceSnapshotResponse> ultimoPorProcesso(@PathVariable Long processoId) {
        SurfaceSnapshotResponse response = facadeService.ultimoRadarPorProcesso(processoId);
        return response == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(response);
    }

    @GetMapping("/processo/{processoId}/alertas")
    public ResponseEntity<SurfaceCollectionResponse> alertasDoProcesso(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.alertasRadarPorProcesso(processoId));
    }

    @GetMapping("/nupn/{nupn}/alertas")
    public ResponseEntity<SurfaceCollectionResponse> alertasPorNupn(@PathVariable String nupn) {
        return ResponseEntity.ok(facadeService.alertasRadarPorNupn(nupn));
    }
}
