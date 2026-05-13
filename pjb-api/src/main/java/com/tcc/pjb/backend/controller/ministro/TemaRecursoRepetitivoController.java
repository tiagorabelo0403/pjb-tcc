package com.tcc.pjb.backend.controller.ministro;

import com.tcc.pjb.backend.model.dto.ministro.TemaRecursoRepetitivoAfetarRequest;
import com.tcc.pjb.backend.model.dto.ministro.TemaRecursoRepetitivoJulgarRequest;
import com.tcc.pjb.backend.model.dto.ministro.TemaRecursoRepetitivoRelacionarRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.ministro.surface.TemaRecursoRepetitivoSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ministro/temas-repetitivos")
public class TemaRecursoRepetitivoController {

    private final TemaRecursoRepetitivoSurfaceFacadeService facadeService;

    public TemaRecursoRepetitivoController(TemaRecursoRepetitivoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MINISTRO','DESEMBARGADOR','DESEMBARGADOR_FEDERAL','ASSESSOR_MINISTRO','ASSESSOR_DESEMBARGADOR')")
    public ResponseEntity<SurfaceCollectionResponse> listar(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(facadeService.listar(status));
    }

    @PostMapping("/processos/{processoId}/afetar")
    @PreAuthorize("hasAnyRole('MINISTRO','DESEMBARGADOR','DESEMBARGADOR_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> afetar(@PathVariable Long processoId,
                                                          @Valid @RequestBody TemaRecursoRepetitivoAfetarRequest request) {
        return ResponseEntity.ok(facadeService.afetar(processoId, request));
    }

    @PostMapping("/{temaId}/sobrestar")
    @PreAuthorize("hasAnyRole('MINISTRO','DESEMBARGADOR','DESEMBARGADOR_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> sobrestar(@PathVariable Long temaId,
                                                             @Valid @RequestBody TemaRecursoRepetitivoRelacionarRequest request) {
        return ResponseEntity.ok(facadeService.sobrestar(temaId, request));
    }

    @PostMapping("/{temaId}/julgar")
    @PreAuthorize("hasAnyRole('MINISTRO','DESEMBARGADOR','DESEMBARGADOR_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> julgar(@PathVariable Long temaId,
                                                          @Valid @RequestBody TemaRecursoRepetitivoJulgarRequest request) {
        return ResponseEntity.ok(facadeService.julgar(temaId, request));
    }

    @PostMapping("/{temaId}/aplicar")
    @PreAuthorize("hasAnyRole('MINISTRO','DESEMBARGADOR','DESEMBARGADOR_FEDERAL')")
    public ResponseEntity<SurfaceSnapshotResponse> aplicar(@PathVariable Long temaId,
                                                           @Valid @RequestBody TemaRecursoRepetitivoRelacionarRequest request) {
        return ResponseEntity.ok(facadeService.aplicar(temaId, request));
    }
}
