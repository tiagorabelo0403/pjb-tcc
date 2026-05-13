package com.tcc.pjb.backend.controller.ministro;

import com.tcc.pjb.backend.model.dto.ministro.MinistroDecisaoMonocraticaRequest;
import com.tcc.pjb.backend.model.dto.ministro.MinistroDecisaoPlenariaRequest;
import com.tcc.pjb.backend.model.dto.ministro.MinistroPautaRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ministro/plenario")
@Validated
public class MinistroPlenarioController {

    private final MinistroCourtSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public MinistroPlenarioController(MinistroCourtSurfaceFacadeService facadeService,
                                      CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/snapshot")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(Authentication authentication) {
        enforce(authentication, "ministro_snapshot");
        return ResponseEntity.ok(facadeService.snapshotPlenario());
    }

    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> malhaProcesso(@PathVariable Long processoId, Authentication authentication) {
        enforce(authentication, "ministro_malha");
        return ResponseEntity.ok(facadeService.malhaProcesso(processoId));
    }

    @PostMapping("/processos/{processoId}/decisao-monocratica")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceActionResponse> proferirDecisaoMonocratica(@PathVariable Long processoId,
                                                                            @Valid @RequestBody MinistroDecisaoMonocraticaRequest request,
                                                                            Authentication authentication) {
        enforce(authentication, "ministro_decisao_monocratica");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.proferirDecisaoMonocratica(processoId, request));
    }

    @PostMapping("/processos/{processoId}/pauta")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceActionResponse> incluirPauta(@PathVariable Long processoId,
                                                              @Valid @RequestBody MinistroPautaRequest request,
                                                              Authentication authentication) {
        enforce(authentication, "ministro_pauta");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.incluirPauta(processoId, request));
    }

    @PostMapping("/processos/{processoId}/decisao-plenaria")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceActionResponse> registrarDecisaoPlenaria(@PathVariable Long processoId,
                                                                          @Valid @RequestBody MinistroDecisaoPlenariaRequest request,
                                                                          Authentication authentication) {
        enforce(authentication, "ministro_decisao_plenaria");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.registrarDecisaoPlenaria(processoId, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
