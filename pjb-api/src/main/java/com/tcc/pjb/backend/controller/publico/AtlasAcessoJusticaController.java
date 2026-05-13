package com.tcc.pjb.backend.controller.publico;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.publico.surface.PublicJusticeAnalyticsSurfaceFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/atlas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("permitAll()")
public class AtlasAcessoJusticaController {

    private final PublicJusticeAnalyticsSurfaceFacadeService facadeService;

    @GetMapping("/nacional/resumo")
    public ResponseEntity<SurfaceSnapshotResponse> resumoNacional() {
        return ResponseEntity.ok(facadeService.atlasResumoNacional());
    }

    @GetMapping("/heatmap")
    public ResponseEntity<SurfaceCollectionResponse> heatmap() {
        return ResponseEntity.ok(facadeService.atlasHeatmap());
    }

    @GetMapping("/uf/{uf}")
    public ResponseEntity<SurfaceSnapshotResponse> relatorioUf(@PathVariable String uf) {
        return ResponseEntity.ok(facadeService.atlasRelatorioUf(uf));
    }

    @GetMapping("/uf/{uf}/municipios")
    public ResponseEntity<SurfaceCollectionResponse> municipiosUf(@PathVariable String uf) {
        return ResponseEntity.ok(facadeService.atlasMunicipiosUf(uf));
    }

    @GetMapping("/municipio/{codigoIbge}")
    public ResponseEntity<SurfaceSnapshotResponse> municipio(@PathVariable String codigoIbge) {
        return facadeService.atlasMunicipio(codigoIbge)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/classificacao")
    public ResponseEntity<SurfaceCollectionResponse> porClassificacao(@RequestParam("valor") String classificacao) {
        return ResponseEntity.ok(facadeService.atlasPorClassificacao(classificacao));
    }
}
