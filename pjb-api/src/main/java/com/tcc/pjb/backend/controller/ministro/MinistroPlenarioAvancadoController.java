package com.tcc.pjb.backend.controller.ministro;

import com.tcc.pjb.backend.model.dto.ministro.PlenarioAvancadoAbrirSessaoRequest;
import com.tcc.pjb.backend.model.dto.ministro.PlenarioAvancadoProclamarSessaoRequest;
import com.tcc.pjb.backend.model.dto.ministro.PlenarioAvancadoRegistrarVotoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
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
@RequestMapping("/api/v1/ministro/plenario-avancado")
public class MinistroPlenarioAvancadoController {

    private final MinistroPlenarioAvancadoSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public MinistroPlenarioAvancadoController(MinistroPlenarioAvancadoSurfaceFacadeService facadeService,
                                              CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/sessoes")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceCollectionResponse> listar(Authentication authentication) {
        enforce(authentication, "ministro_plenario_avancado_listar");
        return ResponseEntity.ok(facadeService.listarSessoes());
    }

    @PostMapping("/processos/{processoId}/abrir")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> abrir(@PathVariable Long processoId,
                                                         @Valid @RequestBody PlenarioAvancadoAbrirSessaoRequest request,
                                                         Authentication authentication) {
        enforce(authentication, "ministro_plenario_avancado_abrir");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.abrirSessao(processoId, request));
    }

    @PostMapping("/sessoes/{codigoSessao}/votar")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceActionResponse> votar(@PathVariable String codigoSessao,
                                                       @Valid @RequestBody PlenarioAvancadoRegistrarVotoRequest request,
                                                       Authentication authentication) {
        enforce(authentication, "ministro_plenario_avancado_votar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.votar(codigoSessao, request));
    }

    @GetMapping("/sessoes/{codigoSessao}")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> detalhar(@PathVariable String codigoSessao,
                                                            Authentication authentication) {
        enforce(authentication, "ministro_plenario_avancado_detalhar");
        return ResponseEntity.ok(facadeService.detalharSessao(codigoSessao));
    }

    @GetMapping("/sessoes/{codigoSessao}/integridade")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> integridade(@PathVariable String codigoSessao,
                                                               Authentication authentication) {
        enforce(authentication, "ministro_plenario_avancado_integridade");
        return ResponseEntity.ok(facadeService.integridadeSessao(codigoSessao));
    }

    @PostMapping("/sessoes/{codigoSessao}/proclamar")
    @PreAuthorize("hasRole('MINISTRO')")
    public ResponseEntity<SurfaceSnapshotResponse> proclamar(@PathVariable String codigoSessao,
                                                             @Valid @RequestBody PlenarioAvancadoProclamarSessaoRequest request,
                                                             Authentication authentication) {
        enforce(authentication, "ministro_plenario_avancado_proclamar");
        return ResponseEntity.ok(facadeService.proclamarSessao(codigoSessao, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
