package com.tcc.pjb.backend.controller.audiencia;

import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcBiometriaRequest;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcEncerrarRequest;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcOfferRequest;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcSessaoRequest;
import com.tcc.pjb.backend.model.dto.audiencia.AudienciaWebRtcTranscricaoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.audiencia.surface.AudienciaWebRtcSurfaceFacadeService;
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
@RequestMapping("/api/v1/audiencias/webrtc")
public class AudienciaWebRtcController {

    private final AudienciaWebRtcSurfaceFacadeService facadeService;
    private final CapabilityRateLimiter rateLimiter;

    public AudienciaWebRtcController(AudienciaWebRtcSurfaceFacadeService facadeService,
                                     CapabilityRateLimiter rateLimiter) {
        this.facadeService = facadeService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/sessoes")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','ADVOGADO','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<SurfaceCollectionResponse> listar(Authentication authentication) {
        enforce(authentication, "audiencia_webrtc_listar");
        return ResponseEntity.ok(facadeService.listar());
    }

    @GetMapping("/sessoes/{sessaoToken}")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','ADVOGADO','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<SurfaceSnapshotResponse> detalhar(@PathVariable String sessaoToken,
                                                            Authentication authentication) {
        enforce(authentication, "audiencia_webrtc_detalhar");
        return ResponseEntity.ok(facadeService.detalhar(sessaoToken));
    }

    @PostMapping("/sessao")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','ADVOGADO','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<SurfaceSnapshotResponse> abrir(@Valid @RequestBody AudienciaWebRtcSessaoRequest request,
                                                         Authentication authentication) {
        enforce(authentication, "audiencia_webrtc_sessao");
        return ResponseEntity.status(HttpStatus.CREATED).body(facadeService.abrir(request));
    }

    @PostMapping("/offer")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','ADVOGADO','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<SurfaceSnapshotResponse> offer(@Valid @RequestBody AudienciaWebRtcOfferRequest request,
                                                         Authentication authentication) {
        enforce(authentication, "audiencia_webrtc_offer");
        return ResponseEntity.ok(facadeService.offer(request));
    }

    @PostMapping("/transcricao")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','ADVOGADO','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<SurfaceSnapshotResponse> transcrever(@Valid @RequestBody AudienciaWebRtcTranscricaoRequest request,
                                                               Authentication authentication) {
        enforce(authentication, "audiencia_webrtc_transcricao");
        return ResponseEntity.ok(facadeService.transcrever(request));
    }

    @PostMapping("/biometria")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','ADVOGADO','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<SurfaceSnapshotResponse> biometria(@Valid @RequestBody AudienciaWebRtcBiometriaRequest request,
                                                             Authentication authentication) {
        enforce(authentication, "audiencia_webrtc_biometria");
        return ResponseEntity.ok(facadeService.biometria(request));
    }

    @PostMapping("/encerrar")
    @PreAuthorize("hasAnyRole('MAGISTRADO','JUIZ','SERVIDOR_FORUM','ADVOGADO','MEMBRO_MINISTERIO_PUBLICO','DEFENSOR_PUBLICO')")
    public ResponseEntity<SurfaceSnapshotResponse> encerrar(@Valid @RequestBody AudienciaWebRtcEncerrarRequest request,
                                                            Authentication authentication) {
        enforce(authentication, "audiencia_webrtc_encerrar");
        return ResponseEntity.ok(facadeService.encerrar(request));
    }

    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, capability, ApiVersion.V1);
    }
}
