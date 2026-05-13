package com.tcc.pjb.backend.controller.magistratura;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.jurisprudencia.surface.RadarJurisprudenciaSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/magistratura/processos")
public class RadarJurisprudenciaController {

    private final RadarJurisprudenciaSurfaceFacadeService facadeService;

    public RadarJurisprudenciaController(RadarJurisprudenciaSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/{processoId}/radar-jurisprudencia")
    @PreAuthorize("hasAnyRole('JUIZ','JUIZ_ESTADUAL','JUIZ_FEDERAL','JUIZ_TRABALHISTA','DESEMBARGADOR','MINISTRO','ASSESSOR','ASSESSOR_MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> analisar(@PathVariable Long processoId) {
        return ResponseEntity.ok(facadeService.analisar(processoId));
    }
}
