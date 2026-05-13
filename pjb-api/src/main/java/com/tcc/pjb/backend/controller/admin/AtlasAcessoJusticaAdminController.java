package com.tcc.pjb.backend.controller.admin;

import com.tcc.pjb.backend.model.dto.atlas.AtlasCelulaUpsertRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.admin.surface.AdminAtlasAcessoJusticaFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/atlas")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
public class AtlasAcessoJusticaAdminController {

    private final AdminAtlasAcessoJusticaFacadeService facadeService;

    @PostMapping("/celulas")
    public ResponseEntity<SurfaceSnapshotResponse> upsert(@Valid @RequestBody AtlasCelulaUpsertRequest request) {
        return ResponseEntity.ok(facadeService.upsert(request));
    }

    @PostMapping("/sync/ibge")
    public ResponseEntity<SurfaceSnapshotResponse> syncIbge() {
        return ResponseEntity.ok(facadeService.syncIbge());
    }
}
