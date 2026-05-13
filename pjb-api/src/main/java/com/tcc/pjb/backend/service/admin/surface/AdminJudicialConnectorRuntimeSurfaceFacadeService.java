package com.tcc.pjb.backend.service.admin.surface;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorControlPlaneService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCryptographyProfileService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorDataPlaneService;
import com.tcc.pjb.backend.integration.judicial.FederatedIntegrityReconciliationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorObservabilityService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRuntimePostureService;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCertificateInventoryReport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCertificateInventoryService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCryptoAdminOpsService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCryptoCommandCenterService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCryptoProbeRequest;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorSecurityPackService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorSecuritySessionService;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AdminJudicialConnectorRuntimeSurfaceFacadeService {

    private final JudicialConnectorObservabilityService observabilityService;
    private final JudicialConnectorRuntimePostureService runtimePostureService;
    private final FederatedIntegrityReconciliationService integrityReconciliationService;
    private final JudicialConnectorCommandCenterService commandCenterService;
    private final JudicialConnectorDataPlaneService dataPlaneService;
    private final JudicialConnectorControlPlaneService controlPlaneService;
    private final JudicialConnectorCryptographyProfileService cryptographyProfileService;
    private final JudicialConnectorCryptoCommandCenterService cryptoCommandCenterService;
    private final JudicialConnectorSecurityPackService securityPackService;
    private final JudicialConnectorSecuritySessionService securitySessionService;
    private final JudicialConnectorCertificateInventoryService certificateInventoryService;
    private final JudicialConnectorCryptoAdminOpsService cryptoAdminOpsService;
    private final SurfaceProjectionSupport surfaceProjectionSupport;

    public AdminJudicialConnectorRuntimeSurfaceFacadeService(JudicialConnectorObservabilityService observabilityService,
                                                             JudicialConnectorRuntimePostureService runtimePostureService,
                                                             FederatedIntegrityReconciliationService integrityReconciliationService,
                                                             JudicialConnectorCommandCenterService commandCenterService,
                                                             JudicialConnectorDataPlaneService dataPlaneService,
                                                             JudicialConnectorControlPlaneService controlPlaneService,
                                                             JudicialConnectorCryptographyProfileService cryptographyProfileService,
                                                             JudicialConnectorCryptoCommandCenterService cryptoCommandCenterService,
                                                             JudicialConnectorSecurityPackService securityPackService,
                                                             JudicialConnectorSecuritySessionService securitySessionService,
                                                             JudicialConnectorCertificateInventoryService certificateInventoryService,
                                                             JudicialConnectorCryptoAdminOpsService cryptoAdminOpsService,
                                                             SurfaceProjectionSupport surfaceProjectionSupport) {
        this.observabilityService = Objects.requireNonNull(observabilityService);
        this.runtimePostureService = Objects.requireNonNull(runtimePostureService);
        this.integrityReconciliationService = Objects.requireNonNull(integrityReconciliationService);
        this.commandCenterService = Objects.requireNonNull(commandCenterService);
        this.dataPlaneService = Objects.requireNonNull(dataPlaneService);
        this.controlPlaneService = Objects.requireNonNull(controlPlaneService);
        this.cryptographyProfileService = Objects.requireNonNull(cryptographyProfileService);
        this.cryptoCommandCenterService = Objects.requireNonNull(cryptoCommandCenterService);
        this.securityPackService = Objects.requireNonNull(securityPackService);
        this.securitySessionService = Objects.requireNonNull(securitySessionService);
        this.certificateInventoryService = Objects.requireNonNull(certificateInventoryService);
        this.cryptoAdminOpsService = Objects.requireNonNull(cryptoAdminOpsService);
        this.surfaceProjectionSupport = Objects.requireNonNull(surfaceProjectionSupport);
    }

    public SurfaceSnapshotResponse observabilityNational(long hours) {
        return surfaceProjectionSupport.snapshot("admin.judicial.observability.national", observabilityService.nationalReport(windowHours(hours)).toMap());
    }

    public SurfaceSnapshotResponse observabilityTribunal(String tribunalCodigo, long hours) {
        return surfaceProjectionSupport.snapshot("admin.judicial.observability.tribunal", observabilityService.tribunalReport(tribunalCodigo, windowHours(hours)).toMap());
    }

    public SurfaceSnapshotResponse commandCenterNational(long hours) {
        return surfaceProjectionSupport.snapshot("admin.judicial.command-center.national", commandCenterService.nationalReport(windowHours(hours)).toMap());
    }

    public SurfaceSnapshotResponse commandCenterTribunal(String tribunalCodigo, long hours) {
        return surfaceProjectionSupport.snapshot("admin.judicial.command-center.tribunal", commandCenterService.tribunalReport(tribunalCodigo, windowHours(hours)).toMap());
    }

    public SurfaceSnapshotResponse dataPlaneNational(long hours) {
        return surfaceProjectionSupport.snapshot("admin.judicial.data-plane.national", dataPlaneService.nationalReport(windowHours(hours)).toMap());
    }

    public SurfaceSnapshotResponse dataPlaneTribunal(String tribunalCodigo, long hours) {
        return surfaceProjectionSupport.snapshot("admin.judicial.data-plane.tribunal", dataPlaneService.tribunalReport(tribunalCodigo, windowHours(hours)).toMap());
    }

    public SurfaceSnapshotResponse controlPlaneNational() {
        return surfaceProjectionSupport.snapshot("admin.judicial.control-plane.national", controlPlaneService.nationalReport().toMap());
    }

    public SurfaceSnapshotResponse runtimeNational() {
        return surfaceProjectionSupport.snapshot("admin.judicial.runtime-posture.national", runtimePostureService.nationalReport().toMap());
    }

    public SurfaceSnapshotResponse runtimeTribunal(String tribunalCodigo) {
        return surfaceProjectionSupport.snapshot("admin.judicial.runtime-posture.tribunal", runtimePostureService.tribunalReport(tribunalCodigo).toMap());
    }

    public SurfaceSnapshotResponse integrityNational(long hours) {
        return surfaceProjectionSupport.snapshot("admin.judicial.integrity.national", integrityReconciliationService.captureNational(Duration.ofHours(Math.max(1L, hours))).toMap());
    }

    public SurfaceSnapshotResponse integrityTribunal(String tribunalCodigo, long hours) {
        return surfaceProjectionSupport.snapshot("admin.judicial.integrity.tribunal", integrityReconciliationService.captureTribunal(tribunalCodigo, Duration.ofHours(Math.max(1L, hours))).toMap());
    }

    public SurfaceSnapshotResponse controlPlaneTribunal(String tribunalCodigo) {
        return surfaceProjectionSupport.snapshot("admin.judicial.control-plane.tribunal", controlPlaneService.tribunalReport(tribunalCodigo).toMap());
    }

    public SurfaceCollectionResponse securitySessionsRecent(Long windowSeconds) {
        return surfaceProjectionSupport.collection(
                "admin.judicial.crypto-sessions.recent",
                securitySessionService.recentSessions(windowSeconds(windowSeconds), null).stream().map(report -> report.toMap()).toList()
        );
    }

    public SurfaceCollectionResponse securitySessionsRecentByTribunal(String tribunalCodigo, Long windowSeconds) {
        return surfaceProjectionSupport.collection(
                "admin.judicial.crypto-sessions.recent.tribunal",
                securitySessionService.recentSessions(windowSeconds(windowSeconds), tribunalCodigo).stream().map(report -> report.toMap()).toList()
        );
    }

    public SurfaceSnapshotResponse securitySessionsSummary(Long windowSeconds) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-sessions.summary", securitySessionService.summary(windowSeconds(windowSeconds), null).toMap());
    }

    public SurfaceSnapshotResponse securitySessionsSummaryByTribunal(String tribunalCodigo, Long windowSeconds) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-sessions.summary.tribunal", securitySessionService.summary(windowSeconds(windowSeconds), tribunalCodigo).toMap());
    }

    public SurfaceSnapshotResponse cryptographyNational() {
        return surfaceProjectionSupport.snapshot("admin.judicial.cryptography.national", cryptographyProfileService.nationalReport().toMap());
    }

    public SurfaceSnapshotResponse cryptographyTribunal(String tribunalCodigo) {
        return surfaceProjectionSupport.snapshot("admin.judicial.cryptography.tribunal", cryptographyProfileService.tribunalReport(tribunalCodigo).toMap());
    }

    public SurfaceSnapshotResponse cryptoCommandCenterNational(Long recentFailureWindowSeconds) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-command-center.national", cryptoCommandCenterService.nationalReport(windowSeconds(recentFailureWindowSeconds)).toMap());
    }

    public SurfaceSnapshotResponse cryptoCommandCenterTribunal(String tribunalCodigo, Long recentFailureWindowSeconds) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-command-center.tribunal", cryptoCommandCenterService.tribunalReport(tribunalCodigo, windowSeconds(recentFailureWindowSeconds)).toMap());
    }

    public SurfaceSnapshotResponse cryptoSessionSummary(Long recentFailureWindowSeconds) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-command-center.sessions.summary", securitySessionService.summary(windowSeconds(recentFailureWindowSeconds), null).toMap());
    }

    public SurfaceSnapshotResponse cryptoSessionSummaryByTribunal(String tribunalCodigo, Long recentFailureWindowSeconds) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-command-center.sessions.summary.tribunal", securitySessionService.summary(windowSeconds(recentFailureWindowSeconds), tribunalCodigo).toMap());
    }

    public SurfaceCollectionResponse cryptoPacks() {
        return surfaceProjectionSupport.collection("admin.judicial.crypto-command-center.packs", securityPackService.effectivePacks().stream().map(pack -> pack.toMap()).toList());
    }

    public SurfaceSnapshotResponse cryptoPack(JudicialSystem system, String tribunalCodigo) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-command-center.pack", securityPackService.effectivePack(system, tribunalCodigo).toMap());
    }

    public SurfaceSnapshotResponse cryptoPackSummary() {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-command-center.packs.summary", securityPackService.summary().toMap());
    }

    public SurfaceCollectionResponse cryptoPostureInventory() {
        List<Map<String, Object>> inventory = certificateInventoryService.latestInventory().stream().map(JudicialConnectorCertificateInventoryReport::toMap).toList();
        return surfaceProjectionSupport.collection("admin.judicial.crypto-posture.inventory", inventory);
    }

    public SurfaceSnapshotResponse cryptoPostureSummary(Long recentFailureWindowSeconds) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-posture.summary", certificateInventoryService.postureSummary(windowSeconds(recentFailureWindowSeconds)).toMap());
    }

    public SurfaceCollectionResponse cryptoPostureRefreshAll() {
        return surfaceProjectionSupport.collection(
                "admin.judicial.crypto-posture.refresh-all",
                certificateInventoryService.refreshConfiguredInventory().stream().map(JudicialConnectorCertificateInventoryReport::toMap).toList()
        );
    }

    public SurfaceSnapshotResponse cryptoPostureRefresh(JudicialSystem system, String tribunalCodigo) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-posture.refresh", certificateInventoryService.refresh(system, tribunalCodigo).toMap());
    }

    public SurfaceSnapshotResponse cryptoInspect(JudicialSystem system, String tribunalCodigo, String targetUrl, String requestedBy, String correlationId) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-ops.inspect", cryptoAdminOpsService.inspect(buildProbeRequest(system, tribunalCodigo, targetUrl, requestedBy, correlationId)).toMap());
    }

    public SurfaceSnapshotResponse cryptoProbe(JudicialSystem system, String tribunalCodigo, String targetUrl, String requestedBy, String correlationId) {
        return surfaceProjectionSupport.snapshot("admin.judicial.crypto-ops.probe", cryptoAdminOpsService.probeHandshake(buildProbeRequest(system, tribunalCodigo, targetUrl, requestedBy, correlationId)).toMap());
    }

    private JudicialConnectorCryptoProbeRequest buildProbeRequest(JudicialSystem system, String tribunalCodigo, String targetUrl, String requestedBy, String correlationId) {
        return new JudicialConnectorCryptoProbeRequest(system, tribunalCodigo, targetUrl, requestedBy, correlationId, Map.of());
    }

    private Duration windowHours(long hours) {
        long sanitized = hours <= 0L ? 24L : Math.min(hours, 24L * 30L);
        return Duration.ofHours(sanitized);
    }

    private Duration windowSeconds(Long seconds) {
        return seconds == null ? Duration.ofHours(24) : Duration.ofSeconds(Math.max(60L, seconds));
    }
}
