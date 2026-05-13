package com.tcc.pjb.backend.controller.intelligence;

import com.tcc.pjb.backend.model.dto.batna.BatnaGenerateRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.intelligence.surface.BatnaSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/intelligence/batna")
@RequiredArgsConstructor
@Validated
@PreAuthorize("isAuthenticated()")
public class BatnaController {

    private final BatnaSurfaceFacadeService facadeService;

    @GetMapping("/processo/{processoId}")
    public ResponseEntity<SurfaceSnapshotResponse> gerarPorProcesso(
            @PathVariable Long processoId,
            @RequestParam(value = "valorAcordo", required = false) java.math.BigDecimal valorAcordo,
            @RequestParam(value = "estritoTeto", defaultValue = "false") boolean estritoTeto
    ) {
        return ResponseEntity.ok(facadeService.gerarPorProcesso(processoId, valorAcordo, estritoTeto));
    }

    @GetMapping("/processo/{processoId}/latest")
    public ResponseEntity<SurfaceSnapshotResponse> ultimoPorProcesso(@PathVariable Long processoId) {
        return facadeService.ultimoPorProcesso(processoId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/proposta/{propostaId}/latest")
    public ResponseEntity<SurfaceSnapshotResponse> ultimoPorProposta(@PathVariable Long propostaId) {
        return facadeService.ultimoPorProposta(propostaId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/gerar")
    public ResponseEntity<SurfaceSnapshotResponse> gerar(@RequestBody BatnaGenerateRequest request) {
        return ResponseEntity.ok(facadeService.gerar(request));
    }
}
