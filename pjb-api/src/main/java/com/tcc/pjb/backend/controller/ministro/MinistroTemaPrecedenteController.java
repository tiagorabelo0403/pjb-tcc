package com.tcc.pjb.backend.controller.ministro;

import com.tcc.pjb.backend.model.dto.ministro.TemaPrecedenteAplicacaoSurfaceRequest;
import com.tcc.pjb.backend.model.dto.ministro.TemaPrecedenteReconhecimentoSurfaceRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.ministro.surface.MinistroPlenarioAvancadoSurfaceFacadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ministro/temas")
public class MinistroTemaPrecedenteController {

    private final MinistroPlenarioAvancadoSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public MinistroTemaPrecedenteController(MinistroPlenarioAvancadoSurfaceFacadeService facadeService,
                                            CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceCollectionResponse> listar(Authentication authentication) {
        enforce(authentication, "ministro_temas_listar");
        return ResponseEntity.ok(facadeService.listarTemas());
    }

    @PostMapping("/processos/{processoId}/reconhecer")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> reconhecer(@PathVariable Long processoId,
                                                              @Valid @RequestBody TemaPrecedenteReconhecimentoSurfaceRequest request,
                                                              Authentication authentication) {
        enforce(authentication, "ministro_temas_reconhecer");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.reconhecerTema(processoId, request));
    }

    @PostMapping("/{codigo}/aplicar")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> aplicar(@PathVariable String codigo,
                                                           @Valid @RequestBody TemaPrecedenteAplicacaoSurfaceRequest request,
                                                           Authentication authentication) {
        enforce(authentication, "ministro_temas_aplicar");
        return ResponseEntity.ok(facadeService.aplicarTema(codigo, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
