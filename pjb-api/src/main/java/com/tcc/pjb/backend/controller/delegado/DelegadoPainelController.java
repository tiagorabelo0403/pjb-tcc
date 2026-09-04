package com.tcc.pjb.backend.controller.delegado;

import java.time.Duration;
import java.util.Map;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceRouteOptimizationResponse;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryBatchSyncRequest;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryBatchSyncResponse;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetrySnapshotResponse;
import com.tcc.pjb.backend.model.dto.profile.RouteTelemetryUpsertRequest;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.delegado.DelegadoPainelService;
import com.tcc.pjb.backend.service.profile.DiligenceRouteOptimizationService;
import com.tcc.pjb.backend.service.profile.DiligenceTelemetryService;

@RestController
@RequestMapping("/api/v1/delegado")
public class DelegadoPainelController {

    private final DelegadoPainelService service;
    private final CapabilityRateLimiter rateLimiter;
    private final PainelSharedExperienceService sharedExperienceService;
    private final DiligenceRouteOptimizationService diligenceRouteOptimizationService;
    private final DiligenceTelemetryService diligenceTelemetryService;

    public DelegadoPainelController(DelegadoPainelService service,
                                    CapabilityRateLimiter rateLimiter,
                                    DiligenceRouteOptimizationService diligenceRouteOptimizationService,
                                    DiligenceTelemetryService diligenceTelemetryService,
                                    PainelSharedExperienceService sharedExperienceService) {
        this.service = service;
        this.rateLimiter = rateLimiter;
        this.sharedExperienceService = sharedExperienceService;
        this.diligenceRouteOptimizationService = diligenceRouteOptimizationService;
        this.diligenceTelemetryService = diligenceTelemetryService;
    }

    @GetMapping("/painel")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<PerfilDashboardPayload.DelegadoPayload> painel(Authentication authentication, @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_painel", ApiVersion.V1);
        PerfilDashboardPayload.DelegadoPayload payload = service.bootstrapPainel();
        if (payload.etag() != null && payload.etag().equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(payload.etag()).build();
        }
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate()).header(HttpHeaders.VARY, "Authorization").eTag(payload.etag()).body(payload);
    }

    @GetMapping("/processos/{processoId}/malha")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<Object> malhaProcesso(@PathVariable Long processoId, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha", ApiVersion.V1);
        return ResponseEntity.ok(service.malhaProcesso(processoId));
    }

    @PostMapping("/diligencias/telemetria")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<RouteTelemetrySnapshotResponse> registrarTelemetria(@Valid @RequestBody RouteTelemetryUpsertRequest request,
                                                                              @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencias_telemetria", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.register(TelemetriaOperacionalCanal.DELEGADO, request, deviceId));
    }

    @GetMapping("/diligencias/telemetria/ultima")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<RouteTelemetrySnapshotResponse> ultimaTelemetria(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencias_telemetria_ultima", ApiVersion.V1);
        return diligenceTelemetryService.latest(TelemetriaOperacionalCanal.DELEGADO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/diligencias/telemetria/sync")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<RouteTelemetryBatchSyncResponse> sincronizarTelemetria(@Valid @RequestBody RouteTelemetryBatchSyncRequest request,
                                                                                 @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencias_telemetria_sync", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.registerBatch(TelemetriaOperacionalCanal.DELEGADO, request, deviceId));
    }

    @GetMapping("/diligencias/telemetria/historico")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<RouteTelemetrySnapshotResponse>> historicoTelemetria(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_diligencias_telemetria_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.history(TelemetriaOperacionalCanal.DELEGADO, 20));
    }

    @PostMapping("/diligencias/rota-otimizada")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceRouteOptimizationResponse> rotaOtimizada(@Valid @RequestBody DiligenceRouteOptimizationRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_rota_otimizada", ApiVersion.V1);
        return ResponseEntity.ok(diligenceRouteOptimizationService.optimize(request));
    }

    @GetMapping("/painel/experiencia-compartilhada")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL')")
    public ResponseEntity<Map<String, Object>> experienciaCompartilhada(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_painel_experiencia_compartilhada", ApiVersion.V1);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePrivate())
                .header(HttpHeaders.VARY, "Authorization")
                .body(sharedExperienceService.snapshot("DELEGADO"));
    }

}
