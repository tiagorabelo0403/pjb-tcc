package com.tcc.pjb.backend.controller.publico;

import org.springframework.security.access.prepost.PreAuthorize;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.publico.surface.PublicJusticeAnalyticsSurfaceFacadeService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tribunal/perfil")
@Validated
@PreAuthorize("permitAll()")
public class TribunalPerfilController {

    private final PublicJusticeAnalyticsSurfaceFacadeService facadeService;

    public TribunalPerfilController(PublicJusticeAnalyticsSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping
    public ResponseEntity<SurfaceSnapshotResponse> perfilAtivo(@RequestParam(value = "codigo", required = false) String codigo) {
        return ResponseEntity.ok(facadeService.tribunalPerfilAtivo(codigo));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<SurfaceSnapshotResponse> porCodigo(@PathVariable @NotBlank String codigo) {
        return ResponseEntity.ok(facadeService.tribunalPerfilPorCodigo(codigo));
    }

    @GetMapping("/{codigo}/resumo")
    public ResponseEntity<SurfaceSnapshotResponse> resumo(@PathVariable @NotBlank String codigo) {
        return ResponseEntity.ok(facadeService.tribunalResumo(codigo));
    }

    @GetMapping("/{codigo}/bindings")
    public ResponseEntity<SurfaceSnapshotResponse> bindings(@PathVariable @NotBlank String codigo) {
        return ResponseEntity.ok(facadeService.tribunalBindings(codigo));
    }

    @GetMapping("/comparar")
    public ResponseEntity<SurfaceCollectionResponse> comparar(@RequestParam("a") String codigoA,
                                                              @RequestParam("b") String codigoB) {
        return ResponseEntity.ok(facadeService.tribunalComparar(codigoA, codigoB));
    }

    @GetMapping("/ranking")
    public ResponseEntity<SurfaceCollectionResponse> ranking() {
        return ResponseEntity.ok(facadeService.tribunalRanking());
    }
}
