package com.tcc.pjb.backend.controller.extrajudicial;

import com.tcc.pjb.backend.model.dto.extrajudicial.EscrituraLavraturaRequest;
import com.tcc.pjb.backend.model.dto.extrajudicial.EscrituraVinculacaoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.extrajudicial.surface.CartorioExtrajudicialSurfaceFacadeService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/extrajudicial/escrituras")
public class CartorioExtrajudicialEscrituraController {

    private final CartorioExtrajudicialSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public CartorioExtrajudicialEscrituraController(CartorioExtrajudicialSurfaceFacadeService facadeService,
                                                    CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TABELIAO','REGISTRADOR_IMOVEIS','ESCREVENTE_CARTORIO')")
    public ResponseEntity<SurfaceCollectionResponse> listar(Authentication authentication,
                                                            @RequestParam(name = "processoId", required = false) Long processoId) {
        enforce(authentication, "extrajudicial_escrituras_listar");
        return ResponseEntity.ok(facadeService.listar(processoId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TABELIAO','REGISTRADOR_IMOVEIS','ESCREVENTE_CARTORIO')")
    public ResponseEntity<SurfaceSnapshotResponse> lavrar(@Valid @RequestBody EscrituraLavraturaRequest request,
                                                          Authentication authentication) {
        enforce(authentication, "extrajudicial_escrituras_lavrar");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.lavrar(request));
    }

    @PostMapping("/{escrituraId}/vincular-processo/{processoId}")
    @PreAuthorize("hasAnyRole('TABELIAO','REGISTRADOR_IMOVEIS','ESCREVENTE_CARTORIO')")
    public ResponseEntity<SurfaceSnapshotResponse> vincular(@PathVariable Long escrituraId,
                                                            @PathVariable Long processoId,
                                                            @Valid @RequestBody EscrituraVinculacaoRequest request,
                                                            Authentication authentication) {
        enforce(authentication, "extrajudicial_escrituras_vincular");
        return ResponseEntity.ok(facadeService.vincular(escrituraId, processoId, request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
