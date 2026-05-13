package com.tcc.pjb.backend.core.explainability;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class DecisionTraceService {

    private static final int MAX_RECORDS = 10_000;

    private final ConcurrentLinkedDeque<DecisionTraceRecord> records = new ConcurrentLinkedDeque<>();
    private final AtomicInteger recordCount = new AtomicInteger();

    public DecisionTraceRecord recordSnapshot(String ruleCode,
                                              String resourceType,
                                              String resourceId,
                                              BigDecimal confidence,
                                              String reasonsJson,
                                              String evidenceJson,
                                              String inputDigest,
                                              String outputDigest,
                                              String modelCode,
                                              String metadataJson) {
        DecisionTraceRecord record = new DecisionTraceRecord(
                safe(ruleCode),
                safe(resourceType),
                safe(resourceId),
                confidence == null ? BigDecimal.ZERO : confidence,
                safe(reasonsJson),
                safe(evidenceJson),
                safe(inputDigest),
                safe(outputDigest),
                safe(modelCode),
                safe(metadataJson),
                Instant.now()
        );
        records.addLast(record);
        int size = recordCount.incrementAndGet();
        trimOverflow(size);
        return record;
    }

    public DecisionTrace record(String ruleCode,
                                String resourceType,
                                String resourceId,
                                BigDecimal confidence,
                                String reasonsJson,
                                String evidenceJson,
                                String inputDigest,
                                String outputDigest,
                                String modelCode,
                                String metadataJson) {
        DecisionTraceRecord snapshot = recordSnapshot(
                ruleCode,
                resourceType,
                resourceId,
                confidence,
                reasonsJson,
                evidenceJson,
                inputDigest,
                outputDigest,
                modelCode,
                metadataJson
        );
        DecisionTrace entity = new DecisionTrace();
        entity.setDecisionType(snapshot.ruleCode());
        entity.setSubjectType(snapshot.resourceType());
        entity.setSubjectId(snapshot.resourceId());
        entity.setConfidence(snapshot.confidence());
        entity.setReasonsJson(snapshot.reasonsJson());
        entity.setCitationsJson(snapshot.evidenceJson());
        entity.setInputDigest(snapshot.inputDigest());
        entity.setOutputDigest(snapshot.outputDigest());
        entity.setModelVersion(snapshot.modelCode());
        entity.setMetadataJson(snapshot.metadataJson());
        entity.setCreatedAt(java.time.LocalDateTime.ofInstant(snapshot.recordedAt(), java.time.ZoneOffset.UTC));
        return entity;
    }

    public List<DecisionTraceRecord> records() {
        return List.copyOf(new ArrayList<>(records));
    }

    public List<DecisionTraceRecord> recordsFor(String resourceType, String resourceId) {
        List<DecisionTraceRecord> out = new ArrayList<>();
        for (DecisionTraceRecord item : records) {
            if (Objects.equals(item.resourceType(), safe(resourceType)) && Objects.equals(item.resourceId(), safe(resourceId))) {
                out.add(item);
            }
        }
        return List.copyOf(out);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void trimOverflow(int size) {
        while (size > MAX_RECORDS) {
            DecisionTraceRecord removed = records.pollFirst();
            if (removed == null) {
                recordCount.compareAndSet(size, 0);
                return;
            }
            size = recordCount.decrementAndGet();
        }
    }

    public record DecisionTraceRecord(String ruleCode,
                                      String resourceType,
                                      String resourceId,
                                      BigDecimal confidence,
                                      String reasonsJson,
                                      String evidenceJson,
                                      String inputDigest,
                                      String outputDigest,
                                      String modelCode,
                                      String metadataJson,
                                      Instant recordedAt) {
    }
}
