package com.tcc.pjb.backend.controller.delegado;

import jakarta.validation.Valid;
import java.util.List;
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
import com.tcc.pjb.backend.model.dto.profile.DiligenceArrivalCheckpointRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceCheckpointResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshAckRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshAckResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshDispatchRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshDispatchResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshReplayRequest;
import com.tcc.pjb.backend.model.dto.profile.DiligenceInstitutionalMeshReplayResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalCommandCenterResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalAnalyticsResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalTimelineEntryResponse;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.profile.DiligenceCheckpointService;
import com.tcc.pjb.backend.service.profile.DiligenceInstitutionalMeshDispatchService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalAnalyticsService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalCommandCenterService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalTimelineService;

/**
 * Acompanhamento operacional em tempo real de uma diligência: checkpoints de chegada, timeline,
 * analytics, painel de comando e expedição/replay na malha institucional.
 * Extraído de {@link DelegadoPainelController} (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/delegado")
public class DelegadoDiligenciaOperacionalController {

    private final CapabilityRateLimiter rateLimiter;
    private final DiligenceCheckpointService diligenceCheckpointService;
    private final DiligenceOperationalTimelineService diligenceOperationalTimelineService;
    private final DiligenceOperationalAnalyticsService diligenceOperationalAnalyticsService;
    private final DiligenceInstitutionalMeshDispatchService diligenceInstitutionalMeshDispatchService;
    private final DiligenceOperationalCommandCenterService diligenceOperationalCommandCenterService;

    public DelegadoDiligenciaOperacionalController(CapabilityRateLimiter rateLimiter,
                                                    DiligenceCheckpointService diligenceCheckpointService,
                                                    DiligenceOperationalTimelineService diligenceOperationalTimelineService,
                                                    DiligenceOperationalAnalyticsService diligenceOperationalAnalyticsService,
                                                    DiligenceInstitutionalMeshDispatchService diligenceInstitutionalMeshDispatchService,
                                                    DiligenceOperationalCommandCenterService diligenceOperationalCommandCenterService) {
        this.rateLimiter = rateLimiter;
        this.diligenceCheckpointService = diligenceCheckpointService;
        this.diligenceOperationalTimelineService = diligenceOperationalTimelineService;
        this.diligenceOperationalAnalyticsService = diligenceOperationalAnalyticsService;
        this.diligenceInstitutionalMeshDispatchService = diligenceInstitutionalMeshDispatchService;
        this.diligenceOperationalCommandCenterService = diligenceOperationalCommandCenterService;
    }

    @PostMapping("/diligencias/{diligenciaId}/checkpoints/chegada")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceCheckpointResponse> confirmarChegada(@PathVariable String diligenciaId,
                                                                        @Valid @RequestBody DiligenceArrivalCheckpointRequest request,
                                                                        @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_checkpoint_chegada", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCheckpointService.registerArrival(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request, deviceId));
    }

    @GetMapping("/diligencias/{diligenciaId}/checkpoints")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceCheckpointResponse>> historicoCheckpoints(@PathVariable String diligenciaId,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_checkpoint_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCheckpointService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @GetMapping("/diligencias/{diligenciaId}/timeline-operacional")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceOperationalTimelineEntryResponse>> timelineOperacional(@PathVariable String diligenciaId,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_timeline_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalTimelineService.timeline(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 50));
    }

    @GetMapping("/diligencias/{diligenciaId}/analytics-operacionais")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceOperationalAnalyticsResponse> analyticsOperacionais(@PathVariable String diligenciaId,
                                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_analytics_operacionais", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalAnalyticsService.analytics(TelemetriaOperacionalCanal.DELEGADO, diligenciaId));
    }

    @GetMapping("/painel/comando-operacional")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceOperationalCommandCenterResponse> comandoOperacional(Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_comando_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalCommandCenterService.snapshot(TelemetriaOperacionalCanal.DELEGADO, 30, 10));
    }

    @PostMapping("/diligencias/{diligenciaId}/malha-institucional/expedicoes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceInstitutionalMeshDispatchResponse> expedirParaMalhaInstitucional(@PathVariable String diligenciaId,
                                                                                                     @Valid @RequestBody(required = false) DiligenceInstitutionalMeshDispatchRequest request,
                                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha_expedicao", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceInstitutionalMeshDispatchService.dispatch(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }

    @GetMapping("/diligencias/{diligenciaId}/malha-institucional/expedicoes")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<List<DiligenceInstitutionalMeshDispatchResponse>> historicoExpedicoesMalhaInstitucional(@PathVariable String diligenciaId,
                                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha_expedicao_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.history(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, 20));
    }

    @PostMapping("/diligencias/{diligenciaId}/malha-institucional/expedicoes/{dispatchId}/ack")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceInstitutionalMeshAckResponse> confirmarAckMalhaInstitucional(@PathVariable String diligenciaId,
                                                                                                 @PathVariable Long dispatchId,
                                                                                                 @Valid @RequestBody(required = false) DiligenceInstitutionalMeshAckRequest request,
                                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha_ack", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.acknowledge(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, dispatchId, request));
    }

    @PostMapping("/diligencias/{diligenciaId}/malha-institucional/replay")
    @PreAuthorize("hasAnyRole('DELEGADO_POLICIA','DELEGADO_POLICIA_FEDERAL','AGENTE_POLICIAL')")
    public ResponseEntity<DiligenceInstitutionalMeshReplayResponse> replayMalhaInstitucional(@PathVariable String diligenciaId,
                                                                                              @Valid @RequestBody(required = false) DiligenceInstitutionalMeshReplayRequest request,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.INSTITUCIONAL, authentication, "delegado_malha_replay", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.replay(TelemetriaOperacionalCanal.DELEGADO, diligenciaId, request));
    }
}
