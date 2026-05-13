package com.tcc.pjb.backend.ai.core.model;

import java.util.Collections;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.core.pipeline.CognitivePhaseName;
import com.tcc.pjb.backend.ai.provenance.EvidenceItem;
import com.tcc.pjb.backend.platform.logging.MdcTraceScope;
import com.tcc.pjb.backend.platform.logging.TraceIds;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;

public final class AgentExecutionContext {

    private final IARequest request;
    private final ApiVersion version;
    private final String capability;
    private final Instant startedAt;
    private final Clock clock;

    
    private final String traceId;

    private final EnumMap<CognitivePhaseName, String> phaseSpanIds = new EnumMap<>(CognitivePhaseName.class);
    private final EnumMap<CognitivePhaseName, Duration> phaseDurations = new EnumMap<>(CognitivePhaseName.class);

    private final Map<String, Object> plan = new LinkedHashMap<>();
    private final Map<String, Object> facts = new LinkedHashMap<>();
    private final List<EvidenceItem> evidences = new ArrayList<>();

    
    private final Map<String, Object> memory = new LinkedHashMap<>();

    private boolean failFast;
    private String failFastReason;
    private String draft;

    public AgentExecutionContext(IARequest request,
                                ApiVersion version,
                                String capability,
                                Instant startedAt,
                                Clock clock) {
        this.request = Objects.requireNonNull(request, "request");
        this.version = Objects.requireNonNullElse(version, ApiVersion.latest());
        this.capability = (capability == null || capability.isBlank()) ? request.getAcao() : capability;
        this.startedAt = Objects.requireNonNullElseGet(startedAt, () -> Instant.now(clock != null ? clock : Clock.systemUTC()));
        this.clock = (clock != null) ? clock : Clock.systemUTC();

        String corr = request.getCorrelationId();
        String rid = request.getRequestId();
        String base = (corr != null && !corr.isBlank()) ? corr : rid;
        this.traceId = (base != null && !base.isBlank()) ? TraceIds.normalize(base) : TraceIds.newTraceId();
    }

    
    public AgentExecutionContext(IARequest request,
                                ApiVersion version,
                                String capability,
                                Instant startedAt) {
        this(request, version, capability, startedAt, Clock.systemUTC());
    }

    public IARequest request() { return request; }
    public ApiVersion version() { return version; }
    public String capability() { return capability; }
    public Instant startedAt() { return startedAt; }
    public Clock clock() { return clock; }

    public Instant now() { return Instant.now(clock); }

    public String traceId() { return traceId; }

    
    public MdcTraceScope enterPhase(CognitivePhaseName phase) {
        CognitivePhaseName p = Objects.requireNonNullElse(phase, CognitivePhaseName.TOTAL);
        String span = phaseSpanIds.computeIfAbsent(p, k -> TraceIds.newSpanId());
        return MdcTraceScope.open(traceId, p.name(), span);
    }

    public Map<String, Object> plan() { return Collections.unmodifiableMap(plan); }
    public Map<String, Object> facts() { return Collections.unmodifiableMap(facts); }
    public List<EvidenceItem> evidences() { return Collections.unmodifiableList(evidences); }

    public Map<String, Object> memory() { return Collections.unmodifiableMap(memory); }

    public void putMemory(String key, Object value) {
        if (key == null || key.isBlank()) return;
        if (value == null) return;
        memory.put(key, value);
    }

    public void putPlan(String key, Object value) {
        if (key == null || key.isBlank()) return;
        if (value == null) return;
        plan.put(key, value);
    }

    public void plan(Map<String, Object> nextPlan) {
        plan.clear();
        if (nextPlan == null || nextPlan.isEmpty()) return;
        for (Map.Entry<String, Object> e : nextPlan.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (k == null || k.isBlank()) continue;
            if (v == null) continue;
            plan.put(k, v);
        }
    }

    public void putFact(String key, Object value) {
        if (key == null || key.isBlank()) return;
        if (value == null) return;
        facts.put(key, value);
    }

    public void addEvidences(Collection<EvidenceItem> items) {
        if (items == null || items.isEmpty()) return;
        for (EvidenceItem it : items) {
            if (it != null) evidences.add(it);
        }
    }

    public void recordPhaseDuration(CognitivePhaseName phase, Duration duration) {
        if (phase == null || duration == null || duration.isNegative()) return;
        phaseDurations.put(phase, duration);
    }

    public Map<CognitivePhaseName, Duration> phaseDurations() {
        return Collections.unmodifiableMap(phaseDurations);
    }

    public Map<CognitivePhaseName, String> phaseSpanIds() {
        return Collections.unmodifiableMap(phaseSpanIds);
    }

    public boolean isFailFast() { return failFast; }

    public void failFast(String reason) {
        this.failFast = true;
        this.failFastReason = (reason == null || reason.isBlank()) ? "fail_fast" : reason.trim();
    }

    public String failFastReason() { return failFastReason; }

    public String draft() { return draft; }

    public void setDraft(String draft) {
        this.draft = draft;
    }

    
    public Map<String, Object> traceMeta() {
        Map<String, Object> spans = new LinkedHashMap<>();
        for (Map.Entry<CognitivePhaseName, String> e : phaseSpanIds.entrySet()) {
            spans.put(e.getKey().name(), e.getValue());
        }
        return Map.of(
                "traceId", traceId,
                "spans", spans
        );
    }
}
