package com.tcc.pjb.backend.ai.juridica.pipeline;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;
import com.tcc.pjb.backend.ai.core.model.AgentExecutionContext;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhase;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhaseName;
import com.tcc.pjb.backend.core.util.SafeMaps;
import com.tcc.pjb.backend.platform.logging.MdcTraceScope;
import com.tcc.pjb.backend.platform.observability.ai.AiMicrometerTelemetry;
import com.tcc.pjb.backend.platform.observability.ai.AiTelemetryDomain;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JuridicaCognitivePipelineOrchestrator {

    private final JuridicaStrategistPhase strategist;
    private final JuridicaResearcherPhase researcher;
    private final JuridicaRelatorPhase relator;
    private final AiMicrometerTelemetry telemetry;
    private final Clock pjbClock;

    private final Executor aiTelemetryExecutor;

    public JuridicaCognitivePipelineOrchestrator(
            JuridicaStrategistPhase strategist,
            JuridicaResearcherPhase researcher,
            JuridicaRelatorPhase relator,
            AiMicrometerTelemetry telemetry,
            Clock pjbClock,
            @Qualifier("aiTelemetryExecutor") Executor aiTelemetryExecutor) {
        this.strategist = strategist;
        this.researcher = researcher;
        this.relator = relator;
        this.telemetry = telemetry;
        this.pjbClock = pjbClock;
        this.aiTelemetryExecutor = aiTelemetryExecutor;
    }

    public IAResponse run(IARequest request, String capability, ApiVersion version) {
        Objects.requireNonNull(request, "request");
        ApiVersion v = (version != null) ? version : ApiVersion.latest();
        String cap = (capability != null && !capability.isBlank()) ? capability : request.getAcao();

        Instant t0 = Instant.now(pjbClock);
        AgentExecutionContext ctx = new AgentExecutionContext(request, v, cap, t0, pjbClock);

        try {
            Duration think = timePhase(strategist, ctx);
            if (ctx.isFailFast()) {
                return failFastResponse(ctx, think, Duration.ZERO, Duration.ZERO, Duration.between(t0, Instant.now(pjbClock)));
            }
            Duration rag = timePhase(researcher, ctx);
            if (ctx.isFailFast()) {
                Duration total = Duration.between(t0, Instant.now(pjbClock));
                return failFastResponse(ctx, think, rag, Duration.ZERO, total);
            }
            Duration respond = timePhase(relator, ctx);

            IAResponse out = (IAResponse) ctx.memory().get("finalResponse");
            Duration total = Duration.between(t0, Instant.now(pjbClock));
            out = attachTimings(out, ctx, think, rag, respond, total, false);
            recordTelemetryAsync(ctx, "200_OK", think, rag, respond, total, false);
            return out;
        } catch (RuntimeException e) {
            Duration total = Duration.between(t0, Instant.now(pjbClock));
            recordTelemetryAsync(ctx, "500_ERROR", null, null, null, total, false);
            throw e;
        }
    }

    private Duration timePhase(CognitivePhase phase, AgentExecutionContext ctx) {
        Instant s = Instant.now(pjbClock);
        try (MdcTraceScope ignored = ctx.enterPhase(phase.name())) {
            phase.execute(ctx);
        }
        Duration d = Duration.between(s, Instant.now(pjbClock));
        ctx.recordPhaseDuration(phase.name(), d);
        return d;
    }

    private IAResponse failFastResponse(AgentExecutionContext ctx,
                                       Duration think,
                                       Duration rag,
                                       Duration respond,
                                       Duration total) {
        IAResponse out = IAResponse.builder()
                .origem("JURIDICA_PIPELINE_" + ctx.version().name())
                .status(IAResponse.StatusIA.INDETERMINADO)
                .confianca(0.0)
                .texto(ctx.draft() != null ? ctx.draft() : "Fatos insuficientes encontrados para uma conclusão segura.")
                .metadados(SafeMaps.of(
                        "pipeline", SafeMaps.of(
                                "failFast", true,
                                "reason", ctx.failFastReason(),
                                "capability", ctx.capability(),
                                "version", ctx.version().name(),
                                "trace", SafeMaps.ofNullable(ctx.traceMeta()),
                                "plan", SafeMaps.ofNullable(ctx.plan()),
                                "facts", SafeMaps.ofNullable(ctx.facts())
                        )
                ))
                .evidencias(ctx.evidences())
                .dataGeracao(Instant.now(pjbClock))
                .build();

        IAResponse enriched = attachTimings(out, ctx, think, rag, respond, total, true);
        recordTelemetryAsync(ctx, "200_OK", think, rag, respond, total, true);
        return enriched;
    }

    private IAResponse attachTimings(IAResponse out,
                                    AgentExecutionContext ctx,
                                    Duration think,
                                    Duration rag,
                                    Duration respond,
                                    Duration total,
                                    boolean failFast) {
        Map<String, Object> timings = new LinkedHashMap<>();
        if (think != null) timings.put("think_ms", think.toMillis());
        if (rag != null) timings.put("rag_ms", rag.toMillis());
        if (respond != null) timings.put("respond_ms", respond.toMillis());
        if (total != null) timings.put("total_ms", total.toMillis());

        Map<String, Object> pipeline = new LinkedHashMap<>();
        pipeline.put("capability", ctx.capability());
        pipeline.put("version", ctx.version().name());
        pipeline.put("failFast", failFast);
        pipeline.put("trace", SafeMaps.ofNullable(ctx.traceMeta()));
        pipeline.put("timings", timings);
        Object governance = ctx.request().getPayload().get("meshGovernance");
        if (governance instanceof Map<?, ?> governanceMap) {
            pipeline.put("governance", governanceMap);
        }
        Object cadence = ctx.request().getPayload().get("knowledgeCadence");
        if (cadence instanceof Map<?, ?> cadenceMap) {
            pipeline.put("knowledgeCadence", cadenceMap);
        }
        Object fusion = ctx.request().getPayload().get("mcpRagFusion");
        if (fusion instanceof Map<?, ?> fusionMap) {
            pipeline.put("mcpRagFusion", fusionMap);
        }
        Object strategy = ctx.request().getPayload().get("strategicExecution");
        if (strategy instanceof Map<?, ?> strategyMap) {
            pipeline.put("strategicExecution", strategyMap);
        }
        Object ssd = ctx.facts().get("semanticSourceDistillation");
        if (ssd instanceof Map<?, ?> ssdMap) {
            pipeline.put("semanticSourceDistillation", ssdMap);
        }
        Object strategicApplied = ctx.facts().get("strategicExecutionApplied");
        if (strategicApplied instanceof Map<?, ?> strategicMap) {
            pipeline.put("strategicExecutionApplied", strategicMap);
        }

        return out.adicionarMetadados(SafeMaps.of("pipeline_timings", pipeline));
    }

    private void recordTelemetryAsync(AgentExecutionContext ctx,
                                     String outcome,
                                     Duration think,
                                     Duration rag,
                                     Duration respond,
                                     Duration total,
                                     boolean failFast) {
        aiTelemetryExecutor.execute(() -> {
            try {
                if (think != null) telemetry.recordPipelinePhase(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), outcome, CognitivePhaseName.THINK.name(), think);
                if (rag != null) telemetry.recordPipelinePhase(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), outcome, CognitivePhaseName.RAG.name(), rag);
                if (respond != null) telemetry.recordPipelinePhase(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), outcome, CognitivePhaseName.RESPOND.name(), respond);
                if (total != null) telemetry.recordPipelinePhase(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), outcome, CognitivePhaseName.TOTAL.name(), total);
                if (failFast) telemetry.recordPipelineFailFast(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), outcome);

                Object qObj = ctx.facts().get("evidenceQuality");
                if (qObj instanceof Map<?, ?> qm) {
                    Object sObj = qm.get("sufficiencyScore");
                    double score = (sObj instanceof Number n) ? n.doubleValue() : 0.0;
                    boolean conflict = Boolean.TRUE.equals(qm.get("conflictRisk"));
                    telemetry.recordEvidenceQuality(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), outcome, score, conflict);
                }

                Object cObj = ctx.facts().get("evidenceContradiction");
                if (cObj instanceof Map<?, ?> cm) {
                    double contradiction = (cm.get("contradictionScore") instanceof Number n) ? n.doubleValue() : 0.0;
                    double inconsistency = (cm.get("inconsistencyScore") instanceof Number n) ? n.doubleValue() : 0.0;
                    telemetry.recordEvidenceSignals(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), outcome, contradiction, inconsistency);
                }

                Object rObj = ctx.facts().get("contradictionResolution");
                if (rObj instanceof Map<?, ?> rm) {
                    boolean resolved = Boolean.TRUE.equals(rm.get("resolved"));
                    telemetry.recordContradictionResolution(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), outcome, resolved);
                }

                Object pObj = ctx.facts().get("sufficiencyPlan");
                if (pObj instanceof Map<?, ?> pm) {
                    Object minQs = pm.get("minQuestions");
                    int count = 0;
                    if (minQs instanceof Iterable<?> it) {
                        for (Object ignored : it) count++;
                    }
                    telemetry.recordMinQuestions(AiTelemetryDomain.LEGAL, ctx.capability(), ctx.version(), outcome, count);
                }

            } catch (Throwable t) {
                log.warn("[AI][JURIDICA][PIPELINE] telemetry error traceId={} err={}", ctx.traceId(), t.toString());
            }
        });
    }
}
