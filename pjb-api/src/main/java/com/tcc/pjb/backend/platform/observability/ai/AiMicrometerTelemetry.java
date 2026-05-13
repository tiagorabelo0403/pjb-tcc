package com.tcc.pjb.backend.platform.observability.ai;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public final class AiMicrometerTelemetry {

    private static final int MAX_CAPABILITY_TAGS = 128;
    private static final int MAX_OUTCOME_TAGS = 16;
    private static final int MAX_PHASE_TAGS = 16;
    private static final int MAX_RESOLUTION_TAGS = 8;

    public static final String METRIC_REQUESTS = "pjb_ai_requests_total";
    public static final String METRIC_DURATION = "pjb_ai_request_duration";
    public static final String METRIC_PIPELINE_DURATION = "pjb_ai_pipeline_duration";
    public static final String METRIC_PIPELINE_FAILFAST = "pjb_ai_pipeline_failfast_total";

    public static final String METRIC_EVIDENCE_SUFFICIENCY = "pjb_ai_evidence_sufficiency";
    public static final String METRIC_EVIDENCE_CONFLICT = "pjb_ai_evidence_conflict_total";
    public static final String METRIC_EVIDENCE_CONTRADICTION = "pjb_ai_evidence_contradiction_total";
    public static final String METRIC_EVIDENCE_INCONSISTENCY = "pjb_ai_evidence_inconsistency";
    public static final String METRIC_TRENDS_DISAGREEMENT = "pjb_ai_trends_disagreement";

    public static final String METRIC_CONTRADICTION_RESOLUTION = "pjb_ai_contradiction_resolution_total";
    public static final String METRIC_MIN_QUESTIONS = "pjb_ai_min_questions";

    private final MeterRegistry registry;

    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> timers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> pipelineTimers = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> failFastCounters = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, DistributionSummary> sufficiencySummaries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> conflictCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> contradictionCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DistributionSummary> inconsistencySummaries = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DistributionSummary> trendsDisagreementSummaries = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Counter> contradictionResolutionCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DistributionSummary> minQuestionsSummaries = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Boolean> capabilityTags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> outcomeTags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> phaseTags = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> resolutionTags = new ConcurrentHashMap<>();

    public AiMicrometerTelemetry(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void record(AiTelemetryDomain domain,
                       String capabilityRaw,
                       ApiVersion version,
                       String outcomeRaw,
                       Duration duration) {
        try {
            AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
            String cap = boundedTag(capabilityTags, CapabilityStrings.canonical(capabilityRaw), MAX_CAPABILITY_TAGS);
            ApiVersion v = (version != null) ? version : ApiVersion.latest();
            String outcome = boundedTag(outcomeTags, AiOutcomeTag.normalize(outcomeRaw), MAX_OUTCOME_TAGS);

            Tags tags = Tags.of(
                    Tag.of("domain", d.tag()),
                    Tag.of("capability", cap),
                    Tag.of("version", v.name()),
                    Tag.of("outcome", outcome)
            );

            String key = d.tag() + "|" + cap + "|" + v.name() + "|" + outcome;

            Counter c = counters.computeIfAbsent(key, k -> Counter.builder(METRIC_REQUESTS)
                    .description("Total de requisições de IA por domínio/capability/versão/outcome")
                    .tags(tags)
                    .register(registry));
            c.increment();

            Timer t = timers.computeIfAbsent(key, k -> Timer.builder(METRIC_DURATION)
                    .description("Duração de requisições de IA (Timer)")
                    .tags(tags)
                    .publishPercentileHistogram()
                    .register(registry));
            if (duration != null && !duration.isNegative()) {
                t.record(duration);
            }
        } catch (Throwable t) {
            log.warn("[AI][METRICS] telemetry failed: {}", t.toString());
        }
    }

    
    public void recordPipelinePhase(AiTelemetryDomain domain,
                                    String capabilityRaw,
                                    ApiVersion version,
                                    String outcomeRaw,
                                    String phaseRaw,
                                    Duration duration) {
        try {
            AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
            String cap = boundedTag(capabilityTags, CapabilityStrings.canonical(capabilityRaw), MAX_CAPABILITY_TAGS);
            ApiVersion v = (version != null) ? version : ApiVersion.latest();
            String outcome = boundedTag(outcomeTags, AiOutcomeTag.normalize(outcomeRaw), MAX_OUTCOME_TAGS);
            String phase = boundedTag(phaseTags, (phaseRaw == null || phaseRaw.isBlank()) ? "TOTAL" : phaseRaw.trim().toUpperCase(), MAX_PHASE_TAGS);

            Tags tags = Tags.of(
                    Tag.of("domain", d.tag()),
                    Tag.of("capability", cap),
                    Tag.of("version", v.name()),
                    Tag.of("outcome", outcome),
                    Tag.of("phase", phase)
            );

            String key = d.tag() + "|" + cap + "|" + v.name() + "|" + outcome + "|" + phase;

            Timer t = pipelineTimers.computeIfAbsent(key, k -> Timer.builder(METRIC_PIPELINE_DURATION)
                    .description("Duração do pipeline cognitivo por fase (THINK/RAG/RESPOND/TOTAL)")
                    .tags(tags)
                    .publishPercentileHistogram()
                    .register(registry));
            if (duration != null && !duration.isNegative()) {
                t.record(duration);
            }
        } catch (Throwable t) {
            log.warn("[AI][METRICS] pipeline telemetry failed: {}", t.toString());
        }
    }

    public void recordPipelineFailFast(AiTelemetryDomain domain,
                                       String capabilityRaw,
                                       ApiVersion version,
                                       String outcomeRaw) {
        try {
            AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
            String cap = boundedTag(capabilityTags, CapabilityStrings.canonical(capabilityRaw), MAX_CAPABILITY_TAGS);
            ApiVersion v = (version != null) ? version : ApiVersion.latest();
            String outcome = boundedTag(outcomeTags, AiOutcomeTag.normalize(outcomeRaw), MAX_OUTCOME_TAGS);

            Tags tags = Tags.of(
                    Tag.of("domain", d.tag()),
                    Tag.of("capability", cap),
                    Tag.of("version", v.name()),
                    Tag.of("outcome", outcome)
            );
            String key = d.tag() + "|" + cap + "|" + v.name() + "|" + outcome;

            Counter c = failFastCounters.computeIfAbsent(key, k -> Counter.builder(METRIC_PIPELINE_FAILFAST)
                    .description("Total de execuções do pipeline com fail-fast (short-circuit)")
                    .tags(tags)
                    .register(registry));
            c.increment();
        } catch (Throwable t) {
            log.warn("[AI][METRICS] failfast telemetry failed: {}", t.toString());
        }
    }

    public void recordEvidenceQuality(AiTelemetryDomain domain,
                                      String capabilityRaw,
                                      ApiVersion version,
                                      String outcomeRaw,
                                      double sufficiencyScore,
                                      boolean conflictRisk) {
        try {
            AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
            String cap = boundedTag(capabilityTags, CapabilityStrings.canonical(capabilityRaw), MAX_CAPABILITY_TAGS);
            ApiVersion v = (version != null) ? version : ApiVersion.latest();
            String outcome = boundedTag(outcomeTags, AiOutcomeTag.normalize(outcomeRaw), MAX_OUTCOME_TAGS);

            Tags tags = Tags.of(
                    Tag.of("domain", d.tag()),
                    Tag.of("capability", cap),
                    Tag.of("version", v.name()),
                    Tag.of("outcome", outcome)
            );

            String key = d.tag() + "|" + cap + "|" + v.name() + "|" + outcome;

            DistributionSummary s = sufficiencySummaries.computeIfAbsent(key, k -> DistributionSummary.builder(METRIC_EVIDENCE_SUFFICIENCY)
                    .description("Escore [0..1] de suficiência de evidências (RAG)")
                    .tags(tags)
                    .publishPercentileHistogram()
                    .register(registry));
            s.record(Math.max(0.0, Math.min(1.0, sufficiencyScore)));

            if (conflictRisk) {
                Counter c = conflictCounters.computeIfAbsent(key, k -> Counter.builder(METRIC_EVIDENCE_CONFLICT)
                        .description("Total de execuções com risco de conflito/ambiguidade entre evidências")
                        .tags(tags)
                        .register(registry));
                c.increment();
            }
        } catch (Throwable t) {
            log.warn("[AI][METRICS] evidence telemetry failed: {}", t.toString());
        }
    }

    public void recordEvidenceSignals(AiTelemetryDomain domain,
                                      String capabilityRaw,
                                      ApiVersion version,
                                      String outcomeRaw,
                                      double contradictionScore,
                                      double inconsistencyScore) {
        try {
            AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
            String cap = boundedTag(capabilityTags, CapabilityStrings.canonical(capabilityRaw), MAX_CAPABILITY_TAGS);
            ApiVersion v = (version != null) ? version : ApiVersion.latest();
            String outcome = boundedTag(outcomeTags, AiOutcomeTag.normalize(outcomeRaw), MAX_OUTCOME_TAGS);

            Tags tags = Tags.of(
                    Tag.of("domain", d.tag()),
                    Tag.of("capability", cap),
                    Tag.of("version", v.name()),
                    Tag.of("outcome", outcome)
            );
            String key = d.tag() + "|" + cap + "|" + v.name() + "|" + outcome;

            DistributionSummary s = inconsistencySummaries.computeIfAbsent(key, k -> DistributionSummary.builder(METRIC_EVIDENCE_INCONSISTENCY)
                    .description("Escore [0..1] de inconsistência (contradição/temporal/jurisdição) entre evidências")
                    .tags(tags)
                    .publishPercentileHistogram()
                    .register(registry));
            s.record(Math.max(0.0, Math.min(1.0, inconsistencyScore)));

            if (contradictionScore >= 0.25 || inconsistencyScore >= 0.55) {
                Counter c = contradictionCounters.computeIfAbsent(key, k -> Counter.builder(METRIC_EVIDENCE_CONTRADICTION)
                        .description("Total de execuções com contradição/inconsistência relevante entre evidências")
                        .tags(tags)
                        .register(registry));
                c.increment();
            }
        } catch (Throwable t) {
            log.warn("[AI][METRICS] contradiction telemetry failed: {}", t.toString());
        }
    }

    public void recordTrendsDisagreement(AiTelemetryDomain domain,
                                        String capabilityRaw,
                                        ApiVersion version,
                                        String outcomeRaw,
                                        double disagreement) {
        try {
            AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
            String cap = boundedTag(capabilityTags, CapabilityStrings.canonical(capabilityRaw), MAX_CAPABILITY_TAGS);
            ApiVersion v = (version != null) ? version : ApiVersion.latest();
            String outcome = boundedTag(outcomeTags, AiOutcomeTag.normalize(outcomeRaw), MAX_OUTCOME_TAGS);

            Tags tags = Tags.of(
                    Tag.of("domain", d.tag()),
                    Tag.of("capability", cap),
                    Tag.of("version", v.name()),
                    Tag.of("outcome", outcome)
            );

            String key = d.tag() + "|" + cap + "|" + v.name() + "|" + outcome;

            DistributionSummary s = trendsDisagreementSummaries.computeIfAbsent(key, k -> DistributionSummary.builder(METRIC_TRENDS_DISAGREEMENT)
                    .description("Divergência agregada do conselho Trends [0..1]")
                    .tags(tags)
                    .publishPercentileHistogram()
                    .register(registry));
            s.record(Math.max(0.0, Math.min(1.0, disagreement)));
        } catch (Throwable t) {
            log.warn("[AI][METRICS] trends telemetry failed: {}", t.toString());
        }
    }

    
    public void recordContradictionResolution(AiTelemetryDomain domain,
                                              String capabilityRaw,
                                              ApiVersion version,
                                              String outcomeRaw,
                                              boolean resolved) {
        try {
            AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
            String cap = boundedTag(capabilityTags, CapabilityStrings.canonical(capabilityRaw), MAX_CAPABILITY_TAGS);
            ApiVersion v = (version != null) ? version : ApiVersion.latest();
            String outcome = boundedTag(outcomeTags, AiOutcomeTag.normalize(outcomeRaw), MAX_OUTCOME_TAGS);
            String res = boundedTag(resolutionTags, resolved ? "resolved" : "unresolved", MAX_RESOLUTION_TAGS);

            Tags tags = Tags.of(
                    Tag.of("domain", d.tag()),
                    Tag.of("capability", cap),
                    Tag.of("version", v.name()),
                    Tag.of("outcome", outcome),
                    Tag.of("resolution", res)
            );

            String key = d.tag() + "|" + cap + "|" + v.name() + "|" + outcome + "|" + res;

            Counter c = contradictionResolutionCounters.computeIfAbsent(key, k -> Counter.builder(METRIC_CONTRADICTION_RESOLUTION)
                    .description("Total de execuções onde o conflito/inconsistência foi resolvido (ou não) de forma segura")
                    .tags(tags)
                    .register(registry));
            c.increment();
        } catch (Throwable t) {
            log.warn("[AI][METRICS] contradiction resolution telemetry failed: {}", t.toString());
        }
    }

    
    public void recordMinQuestions(AiTelemetryDomain domain,
                                   String capabilityRaw,
                                   ApiVersion version,
                                   String outcomeRaw,
                                   int count) {
        try {
            AiTelemetryDomain d = (domain != null) ? domain : AiTelemetryDomain.LEGAL;
            String cap = boundedTag(capabilityTags, CapabilityStrings.canonical(capabilityRaw), MAX_CAPABILITY_TAGS);
            ApiVersion v = (version != null) ? version : ApiVersion.latest();
            String outcome = boundedTag(outcomeTags, AiOutcomeTag.normalize(outcomeRaw), MAX_OUTCOME_TAGS);

            Tags tags = Tags.of(
                    Tag.of("domain", d.tag()),
                    Tag.of("capability", cap),
                    Tag.of("version", v.name()),
                    Tag.of("outcome", outcome)
            );

            String key = d.tag() + "|" + cap + "|" + v.name() + "|" + outcome;

            DistributionSummary s = minQuestionsSummaries.computeIfAbsent(key, k -> DistributionSummary.builder(METRIC_MIN_QUESTIONS)
                    .description("Quantidade de perguntas mínimas (top-N) sugeridas")
                    .tags(tags)
                    .publishPercentileHistogram()
                    .register(registry));
            s.record(Math.max(0, count));
        } catch (Throwable t) {
            log.warn("[AI][METRICS] min questions telemetry failed: {}", t.toString());
        }
    }
    private static String boundedTag(ConcurrentHashMap<String, Boolean> seen, String value, int max) {
        String normalized = normalizeTag(value);
        if (normalized == null) {
            return "UNKNOWN";
        }
        if (seen.containsKey(normalized)) {
            return normalized;
        }
        if (seen.size() >= max) {
            return "OTHER";
        }
        seen.putIfAbsent(normalized, Boolean.TRUE);
        return normalized;
    }

    private static String normalizeTag(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

}
