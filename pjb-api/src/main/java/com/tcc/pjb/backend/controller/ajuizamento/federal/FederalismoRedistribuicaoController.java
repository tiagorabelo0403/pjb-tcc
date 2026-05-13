package com.tcc.pjb.backend.controller.ajuizamento.federal;

import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.ajuizamento.federal.surface.FederalismoRedistribuicaoSurfaceFacadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN','ROLE_ADMINISTRADOR')")
@RequestMapping("/api/v1/admin/federalismo/redistribuicao")
public class FederalismoRedistribuicaoController {

    private final FederalismoRedistribuicaoSurfaceFacadeService facadeService;

    public FederalismoRedistribuicaoController(FederalismoRedistribuicaoSurfaceFacadeService facadeService) {
        this.facadeService = facadeService;
    }

    @GetMapping("/sugestoes")
    public ResponseEntity<SurfaceSnapshotResponse> sugestoes(@RequestParam(name = "indiceCritico", defaultValue = "0.85") double indiceCritico) {
        return ResponseEntity.ok(facadeService.sugestoes(indiceCritico));
    }
}
