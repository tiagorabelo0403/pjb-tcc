package com.tcc.pjb.backend.controller.ministro;

import com.tcc.pjb.backend.model.dto.ministro.RepercussaoGeralJulgamentoRequest;
import com.tcc.pjb.backend.model.dto.ministro.RepercussaoGeralReconhecimentoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.ministro.surface.MinistroCourtSurfaceFacadeService;
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
@RequestMapping("/api/v1/ministro/repercussao-geral")
public class MinistroRepercussaoGeralController {

    private final MinistroCourtSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public MinistroRepercussaoGeralController(MinistroCourtSurfaceFacadeService facadeService,
                                              CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceCollectionResponse> listar(Authentication authentication) {
        enforce(authentication, "ministro_repercussao_listar");
        return ResponseEntity.ok(facadeService.listarTemasRepercussao());
    }

    @PostMapping("/processos/{processoId}/reconhecer")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> reconhecer(@PathVariable Long processoId,
                                                              @Valid @RequestBody RepercussaoGeralReconhecimentoRequest request,
                                                              Authentication authentication) {
        enforce(authentication, "ministro_repercussao_reconhecer");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.reconhecerTema(processoId, request));
    }

    @PostMapping("/{codigo}/aplicar")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> aplicar(@PathVariable String codigo,
                                                           @Valid @RequestBody RepercussaoGeralJulgamentoRequest request,
                                                           Authentication authentication) {
        enforce(authentication, "ministro_repercussao_aplicar");
        return ResponseEntity.ok(facadeService.aplicarTema(codigo, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
