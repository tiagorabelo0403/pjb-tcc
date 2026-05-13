package com.tcc.pjb.backend.service.casefile;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityConsistencyResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityDecisionGateResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityIntegrationResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityObservabilityResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityProductionSealResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityRemediationResponse;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class CaseContinuityObservabilityMetrics {

    private static final int MAX_COUNTERS_PER_BUCKET = 96;

    private final MeterRegistry registry;
    private final AtomicLong lastProceedingCount = new AtomicLong();
    private final AtomicLong lastEventCount = new AtomicLong();
    private final AtomicLong lastStaleProceedings = new AtomicLong();
    private final AtomicLong lastWarnings = new AtomicLong();
    private final AtomicLong lastGeneratedEpoch = new AtomicLong();
    private final AtomicLong lastConsistencyInconsistencies = new AtomicLong();
    private final AtomicLong lastReadinessBlockers = new AtomicLong();
    private final AtomicLong lastIntegrationBlockers = new AtomicLong();
    private final AtomicLong lastDecisionGateBlockers = new AtomicLong();
    private final AtomicLong lastRemediationIssues = new AtomicLong();
    private final AtomicLong lastProductionSealBlockers = new AtomicLong();
    private final Map<String, Counter> lifecycleCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> inspectionCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> mergeCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> consistencyCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> readinessCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> integrationCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> decisionGateCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> remediationCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> productionSealCounters = new ConcurrentHashMap<>();

    public CaseContinuityObservabilityMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry);
        Gauge.builder("pjb.case_continuity.last.proceedings", lastProceedingCount, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.events", lastEventCount, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.stale_proceedings", lastStaleProceedings, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.warnings", lastWarnings, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.generated_epoch", lastGeneratedEpoch, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.consistency_inconsistencies", lastConsistencyInconsistencies, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.readiness_blockers", lastReadinessBlockers, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.integration_blockers", lastIntegrationBlockers, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.decision_gate_blockers", lastDecisionGateBlockers, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.remediation_issues", lastRemediationIssues, AtomicLong::get).register(registry);
        Gauge.builder("pjb.case_continuity.last.production_seal_blockers", lastProductionSealBlockers, AtomicLong::get).register(registry);
    }

    public void recordLifecycleTransition(CaseContinuityTrack track, FaseProcessual fase, StatusProcesso status, boolean archivalMutation) {
        String trackTag = token(track);
        String faseTag = token(fase);
        String statusTag = token(status);
        String key = trackTag + '|' + faseTag + '|' + statusTag + '|' + archivalMutation;
        counter(lifecycleCounters, key, "pjb.case_continuity.transition.total",
                "track", trackTag,
                "fase", faseTag,
                "status", statusTag,
                "archival_mutation", String.valueOf(archivalMutation)).increment();
    }

    public void recordInspection(CaseContinuityObservabilityResponse response) {
        if (response == null) {
            return;
        }
        lastProceedingCount.set(response.proceedingCount());
        lastEventCount.set(response.eventCount());
        lastStaleProceedings.set(response.staleProceedings());
        lastWarnings.set(response.warnings().size());
        String trackTag = token(response.dominantTrack());
        String key = trackTag + '|' + response.attentionRequired();
        counter(inspectionCounters, key, "pjb.case_continuity.inspect.total",
                "track", trackTag,
                "attention", String.valueOf(response.attentionRequired())).increment();
    }

    public void recordMerge(String linkageType) {
        String tag = token(linkageType);
        counter(mergeCounters, tag, "pjb.case_continuity.merge.total",
                "linkage_type", tag).increment();
    }

    public void recordConsistency(CaseContinuityConsistencyResponse response) {
        if (response == null) {
            return;
        }
        lastConsistencyInconsistencies.set(response.inconsistencies().size());
        String trackTag = token(response.dominantTrack());
        String key = trackTag + '|' + response.healthy();
        counter(consistencyCounters, key, "pjb.case_continuity.consistency.total",
                "track", trackTag,
                "healthy", String.valueOf(response.healthy())).increment();
    }

    public void recordReadiness(CaseContinuityReadinessResponse response) {
        if (response == null) {
            return;
        }
        lastReadinessBlockers.set(response.blockers().size());
        String trackTag = token(response.dominantTrack());
        String readinessTag = token(response.readinessLevel());
        String key = trackTag + '|' + readinessTag + '|' + response.healthy();
        counter(readinessCounters, key, "pjb.case_continuity.readiness.total",
                "track", trackTag,
                "readiness", readinessTag,
                "healthy", String.valueOf(response.healthy())).increment();
    }

    public void recordIntegration(CaseContinuityIntegrationResponse response) {
        if (response == null) {
            return;
        }
        lastIntegrationBlockers.set(response.blockers().size());
        String trackTag = token(response.dominantTrack());
        String key = trackTag + '|' + response.healthy() + '|' + response.recursalMatrixReady();
        counter(integrationCounters, key, "pjb.case_continuity.integration.total",
                "track", trackTag,
                "healthy", String.valueOf(response.healthy()),
                "recursal_matrix_ready", String.valueOf(response.recursalMatrixReady())).increment();
    }

    public void recordDecisionGate(CaseContinuityDecisionGateResponse response) {
        if (response == null) {
            return;
        }
        lastDecisionGateBlockers.set(response.blockers().size());
        String trackTag = token(response.dominantTrack());
        String actionTag = token(response.action());
        String key = trackTag + '|' + actionTag + '|' + response.allowed();
        counter(decisionGateCounters, key, "pjb.case_continuity.decision_gate.total",
                "track", trackTag,
                "action", actionTag,
                "allowed", String.valueOf(response.allowed())).increment();
    }

    public void recordRemediation(CaseContinuityRemediationResponse response) {
        if (response == null) {
            return;
        }
        lastRemediationIssues.set(response.totalIssues());
        String trackTag = token(response.dominantTrack());
        String key = trackTag + '|' + response.autoRepairEligible() + '|' + response.healthy();
        counter(remediationCounters, key, "pjb.case_continuity.remediation.total",
                "track", trackTag,
                "auto_repair", String.valueOf(response.autoRepairEligible()),
                "healthy", String.valueOf(response.healthy())).increment();
    }

    public void recordProductionSeal(CaseContinuityProductionSealResponse response) {
        if (response == null) {
            return;
        }
        lastProductionSealBlockers.set(response.blockers().size());
        String trackTag = token(response.dominantTrack());
        String sealTag = token(response.sealLevel());
        String key = trackTag + '|' + sealTag + '|' + response.healthy();
        counter(productionSealCounters, key, "pjb.case_continuity.production_seal.total",
                "track", trackTag,
                "seal_level", sealTag,
                "healthy", String.valueOf(response.healthy())).increment();
    }

    public void touchObservation(Instant generatedAt) {
        if (generatedAt == null) {
            return;
        }
        lastGeneratedEpoch.set(generatedAt.getEpochSecond());
    }

    private Counter counter(Map<String, Counter> counters, String key, String metricName, String... tags) {
        Counter existing = counters.get(key);
        if (existing != null) {
            return existing;
        }
        if (counters.size() >= MAX_COUNTERS_PER_BUCKET) {
            String otherKey = metricName + "|OTHER";
            return counters.computeIfAbsent(otherKey, ignored -> buildCounter(metricName, otherTags(tags)));
        }
        return counters.computeIfAbsent(key, ignored -> buildCounter(metricName, tags));
    }

    private Counter buildCounter(String metricName, String... tags) {
        Counter.Builder builder = Counter.builder(metricName);
        for (int i = 0; i + 1 < tags.length; i += 2) {
            builder.tag(tags[i], tags[i + 1]);
        }
        return builder.register(registry);
    }

    private String[] otherTags(String... tags) {
        String[] other = new String[tags.length];
        for (int i = 0; i < tags.length; i += 2) {
            other[i] = tags[i];
            if (i + 1 < tags.length) {
                other[i + 1] = "OTHER";
            }
        }
        return other;
    }

    private static String token(Object value) {
        if (value == null) {
            return "NAO_CLASSIFICADO";
        }
        String normalized = value.toString().trim().toUpperCase().replaceAll("[^A-Z0-9_.:-]", "_");
        if (normalized.isBlank()) {
            return "NAO_CLASSIFICADO";
        }
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }
}
