package com.tcc.pjb.backend.controller.publico;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.publico.surface.PublicJusticeAnalyticsSurfaceFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/painel/nacional")
@RequiredArgsConstructor
@Validated
@PreAuthorize("permitAll()")
public class PainelNacionalJusticaController {

    private final PublicJusticeAnalyticsSurfaceFacadeService facadeService;

    @GetMapping("/snapshot")
    public ResponseEntity<SurfaceSnapshotResponse> snapshot() {
        return ResponseEntity.ok(facadeService.painelSnapshot());
    }

    @GetMapping("/tribunal")
    public ResponseEntity<SurfaceSnapshotResponse> tribunal(@RequestParam("codigo") String codigo) {
        return facadeService.painelTribunal(codigo)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/serie-temporal")
    public ResponseEntity<SurfaceCollectionResponse> serieTemporal(
            @RequestParam(value = "dias", defaultValue = "30") int dias,
            @RequestParam(value = "tribunal", required = false) String tribunalCodigo
    ) {
        return ResponseEntity.ok(facadeService.painelSerieTemporal(dias, tribunalCodigo));
    }

    @GetMapping("/alertas")
    public ResponseEntity<SurfaceCollectionResponse> alertas(
            @RequestParam(value = "nivel", defaultValue = "BAIXO") String nivel,
            @RequestParam(value = "tribunal", required = false) String tribunalCodigo
    ) {
        return ResponseEntity.ok(facadeService.painelAlertas(nivel, tribunalCodigo));
    }
}
