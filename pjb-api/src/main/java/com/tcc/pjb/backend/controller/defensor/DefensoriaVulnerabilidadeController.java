package com.tcc.pjb.backend.controller.defensor;

import com.tcc.pjb.backend.model.dto.defensor.surface.DefensoriaVulnerabilidadePriorizarRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.defensor.surface.DefensoriaOperationalSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/defensor/vulnerabilidade")
public class DefensoriaVulnerabilidadeController {

    private final DefensoriaOperationalSurfaceFacadeService facadeService;

    public DefensoriaVulnerabilidadeController(DefensoriaOperationalSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @PostMapping("/priorizar")
    @PreAuthorize("hasAnyRole('DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> priorizar(@Valid @RequestBody DefensoriaVulnerabilidadePriorizarRequest request) {
        return ResponseEntity.ok(facadeService.priorizar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEFENSOR_PUBLICO','DEFENSOR_PUBLICO_FEDERAL')")
    public ResponseEntity<SurfaceCollectionResponse> listar(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(facadeService.listarCasos(status));
    }
}
