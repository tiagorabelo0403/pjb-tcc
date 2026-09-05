package com.tcc.pjb.backend.controller.oficial_justica;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
import com.tcc.pjb.backend.service.profile.DiligenceRouteOptimizationService;
import com.tcc.pjb.backend.service.profile.DiligenceTelemetryService;

@RestController
@RequestMapping("/api/v1/oficial-justica")
public class OficialJusticaCampoController {

    private final CapabilityRateLimiter rateLimiter;
    private final DiligenceRouteOptimizationService diligenceRouteOptimizationService;
    private final DiligenceTelemetryService diligenceTelemetryService;

    public OficialJusticaCampoController(
            CapabilityRateLimiter rateLimiter,
            DiligenceRouteOptimizationService diligenceRouteOptimizationService,
            DiligenceTelemetryService diligenceTelemetryService
    ) {
        this.rateLimiter = rateLimiter;
        this.diligenceRouteOptimizationService = diligenceRouteOptimizationService;
        this.diligenceTelemetryService = diligenceTelemetryService;
    }

    @PostMapping("/rota/telemetria")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<RouteTelemetrySnapshotResponse> registrarTelemetria(@Valid @RequestBody RouteTelemetryUpsertRequest request,
                                                                              @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_telemetria", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.register(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, request, deviceId));
    }

    @GetMapping("/rota/telemetria/ultima")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<RouteTelemetrySnapshotResponse> ultimaTelemetria(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_telemetria_ultima", ApiVersion.V1);
        return diligenceTelemetryService.latest(TelemetriaOperacionalCanal.OFICIAL_JUSTICA)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/rota/telemetria/sync")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<RouteTelemetryBatchSyncResponse> sincronizarTelemetria(@Valid @RequestBody RouteTelemetryBatchSyncRequest request,
                                                                                 @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_telemetria_sync", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.registerBatch(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, request, deviceId));
    }

    @GetMapping("/rota/telemetria/historico")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<RouteTelemetrySnapshotResponse>> historicoTelemetria(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_telemetria_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceTelemetryService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, 20));
    }

    @PostMapping("/rota/otimizada")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceRouteOptimizationResponse> rotaOtimizada(@Valid @RequestBody DiligenceRouteOptimizationRequest request, Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_rota_otimizada", ApiVersion.V1);
        return ResponseEntity.ok(diligenceRouteOptimizationService.optimize(request));
    }

}
