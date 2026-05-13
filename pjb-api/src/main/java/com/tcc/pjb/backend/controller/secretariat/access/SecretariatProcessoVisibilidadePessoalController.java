package com.tcc.pjb.backend.controller.secretariat.access;


import com.tcc.pjb.backend.model.dto.profile.operational.SecretariaVisibilidadePainelPessoalRequest;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatProcessoVisibilidadePessoalService;
import jakarta.validation.Valid;
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
@RequestMapping(OperationalApiRoutes.SECRETARIAT_OPERATIONAL_BASE)
@Validated
public class SecretariatProcessoVisibilidadePessoalController {

    private static final String ROLES = "hasAnyRole('SERVIDOR','SERVIDOR_FORUM')";

    private final SecretariatProcessoVisibilidadePessoalService service;
    private final CapabilityRateLimiter rateLimiter;

    public SecretariatProcessoVisibilidadePessoalController(SecretariatProcessoVisibilidadePessoalService service,
                                                            CapabilityRateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_VISIBILIDADE_PESSOAL)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceSnapshotResponse> snapshot(@PathVariable Long processoId,
                                                            Authentication authentication) {
        enforce(authentication, "secretaria_visibilidade_pessoal_snapshot");
        return ResponseEntity.ok(service.snapshot(processoId));
    }

    @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_OPERATIONAL_VISIBILIDADE_PESSOAL)
    @PreAuthorize(ROLES)
    public ResponseEntity<SurfaceActionResponse> definir(@PathVariable Long processoId,
                                                         @Valid @RequestBody SecretariaVisibilidadePainelPessoalRequest request,
                                                         Authentication authentication) {
        enforce(authentication, "secretaria_visibilidade_pessoal_definir");
        return ResponseEntity.ok(service.definir(processoId, Boolean.TRUE.equals(request.visivel()), request.fundamento(), request.diasValidade()));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, capability, ApiVersion.V1);
    }
}
