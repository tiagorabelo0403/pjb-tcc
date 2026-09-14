package com.tcc.pjb.backend.controller.oficial_justica;

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
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalAnalyticsResponse;
import com.tcc.pjb.backend.model.dto.profile.DiligenceOperationalTimelineEntryResponse;
import com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.profile.DiligenceCheckpointService;
import com.tcc.pjb.backend.service.profile.DiligenceInstitutionalMeshDispatchService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalAnalyticsService;
import com.tcc.pjb.backend.service.profile.DiligenceOperationalTimelineService;

/**
 * Acompanhamento operacional em tempo real do cumprimento de mandado: checkpoints de chegada,
 * timeline, analytics e expedição/replay na malha institucional.
 * Extraído de {@link OficialJusticaCampoController} (recorte de F6).
 */
@RestController
@RequestMapping("/api/v1/oficial-justica")
public class OficialJusticaDiligenciaOperacionalController {

    private final CapabilityRateLimiter rateLimiter;
    private final DiligenceCheckpointService diligenceCheckpointService;
    private final DiligenceOperationalTimelineService diligenceOperationalTimelineService;
    private final DiligenceOperationalAnalyticsService diligenceOperationalAnalyticsService;
    private final DiligenceInstitutionalMeshDispatchService diligenceInstitutionalMeshDispatchService;

    public OficialJusticaDiligenciaOperacionalController(CapabilityRateLimiter rateLimiter,
                                                          DiligenceCheckpointService diligenceCheckpointService,
                                                          DiligenceOperationalTimelineService diligenceOperationalTimelineService,
                                                          DiligenceOperationalAnalyticsService diligenceOperationalAnalyticsService,
                                                          DiligenceInstitutionalMeshDispatchService diligenceInstitutionalMeshDispatchService) {
        this.rateLimiter = rateLimiter;
        this.diligenceCheckpointService = diligenceCheckpointService;
        this.diligenceOperationalTimelineService = diligenceOperationalTimelineService;
        this.diligenceOperationalAnalyticsService = diligenceOperationalAnalyticsService;
        this.diligenceInstitutionalMeshDispatchService = diligenceInstitutionalMeshDispatchService;
    }

    @PostMapping("/mandados/{mandadoId}/checkpoints/chegada")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceCheckpointResponse> confirmarChegada(@PathVariable String mandadoId,
                                                                        @Valid @RequestBody DiligenceArrivalCheckpointRequest request,
                                                                        @RequestHeader(name = "X-Device-ID", required = false) String deviceId,
                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_checkpoint_chegada", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCheckpointService.registerArrival(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request, deviceId));
    }

    @GetMapping("/mandados/{mandadoId}/checkpoints")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceCheckpointResponse>> historicoCheckpoints(@PathVariable String mandadoId,
                                                                                  Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_checkpoint_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceCheckpointService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @GetMapping("/mandados/{mandadoId}/timeline-operacional")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceOperationalTimelineEntryResponse>> timelineOperacional(@PathVariable String mandadoId,
                                                                                                Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_timeline_operacional", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalTimelineService.timeline(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 50));
    }

    @GetMapping("/mandados/{mandadoId}/analytics-operacionais")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceOperationalAnalyticsResponse> analyticsOperacionais(@PathVariable String mandadoId,
                                                                                        Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_analytics_operacionais", ApiVersion.V1);
        return ResponseEntity.ok(diligenceOperationalAnalyticsService.analytics(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId));
    }

    @PostMapping("/mandados/{mandadoId}/malha-institucional/expedicoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceInstitutionalMeshDispatchResponse> expedirParaMalhaInstitucional(@PathVariable String mandadoId,
                                                                                                     @Valid @RequestBody(required = false) DiligenceInstitutionalMeshDispatchRequest request,
                                                                                                     Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha_expedicao", ApiVersion.V1);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(diligenceInstitutionalMeshDispatchService.dispatch(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }

    @GetMapping("/mandados/{mandadoId}/malha-institucional/expedicoes")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<List<DiligenceInstitutionalMeshDispatchResponse>> historicoExpedicoesMalhaInstitucional(@PathVariable String mandadoId,
                                                                                                                    Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha_expedicao_historico", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.history(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, 20));
    }

    @PostMapping("/mandados/{mandadoId}/malha-institucional/expedicoes/{dispatchId}/ack")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceInstitutionalMeshAckResponse> confirmarAckMalhaInstitucional(@PathVariable String mandadoId,
                                                                                                 @PathVariable Long dispatchId,
                                                                                                 @Valid @RequestBody(required = false) DiligenceInstitutionalMeshAckRequest request,
                                                                                                 Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha_ack", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.acknowledge(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, dispatchId, request));
    }

    @PostMapping("/mandados/{mandadoId}/malha-institucional/replay")
    @PreAuthorize("hasAnyRole('OFICIAL_JUSTICA','OFICIAL_JUSTICA_AVALIADOR')")
    public ResponseEntity<DiligenceInstitutionalMeshReplayResponse> replayMalhaInstitucional(@PathVariable String mandadoId,
                                                                                              @Valid @RequestBody(required = false) DiligenceInstitutionalMeshReplayRequest request,
                                                                                              Authentication authentication) {
        rateLimiter.enforce(CapabilityRateLimitDomain.SERVIDOR, authentication, "oficial_malha_replay", ApiVersion.V1);
        return ResponseEntity.ok(diligenceInstitutionalMeshDispatchService.replay(TelemetriaOperacionalCanal.OFICIAL_JUSTICA, mandadoId, request));
    }
}
