package com.tcc.pjb.backend.service.recursal;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.audit.cross.service.CrossAuditPublisher;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyBeginResult;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyStatus;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalHash;
import com.tcc.pjb.backend.core.kernel.recursal.governance.RecursalFactIdempotencyHasher;
import com.tcc.pjb.backend.core.kernel.recursal.governance.RecursalIdempotencyProperties;
import com.tcc.pjb.backend.core.kernel.recursal.model.CanonicalFact;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.RecursalFactIngestResponse;

@Service
public class RecursalFactIdempotentIngestService {

    private static final String IDEMP_ACTION = "recursal.kernel.fact.ingest";
    private static final String IDEMP_RESOURCE = "recursal_fact";

    private final RecursalIntelligenceFacadeService facade;
    private final RequestIdempotencyService idempotency;
    private final RecursalFactIdempotencyHasher hasher;
    private final RecursalIdempotencyProperties props;
    private final ObjectMapper mapper;
    private final CrossAuditPublisher crossAudit;

    public RecursalFactIdempotentIngestService(RecursalIntelligenceFacadeService facade,
                                              RequestIdempotencyService idempotency,
                                              RecursalFactIdempotencyHasher hasher,
                                              RecursalIdempotencyProperties props,
                                              ObjectMapper mapper,
                                              CrossAuditPublisher crossAudit) {
        this.facade = Objects.requireNonNull(facade, "facade");
        this.idempotency = Objects.requireNonNull(idempotency, "idempotency");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.props = Objects.requireNonNull(props, "props");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.crossAudit = Objects.requireNonNull(crossAudit, "crossAudit");
    }

    public RecursalFactIngestResponse ingest(Long processoId,
                                             CanonicalFact fact,
                                             RecursalFactIngestRequest req,
                                             String normalizedProceedingNumber) {
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(fact, "fact");
        Objects.requireNonNull(req, "req");

        String requestHash = hasher.requestHash(processoId, req, normalizedProceedingNumber);
        Duration ttl = props.getInProgressTtl();
        RequestIdempotencyBeginResult begin = idempotency.begin(IDEMP_ACTION, requestHash, ttl);

        if (begin.status() == RequestIdempotencyStatus.COMPLETED || begin.status() == RequestIdempotencyStatus.LOCKED) {
            String json = begin.responseJson();
            if (json == null || json.isBlank()) {
                throw new IllegalStateException("Idempotency replay sem responseJson: requestHash=" + requestHash);
            }
            try {
                return mapper.readValue(json, RecursalFactIngestResponse.class);
            } catch (Exception e) {
                throw new IllegalStateException("Idempotency replay com responseJson inválido: requestHash=" + requestHash, e);
            }
        }

        int maxAttempts = Math.max(1, props.getOptimisticRetryMaxAttempts());
        Duration baseBackoff = props.getOptimisticRetryBackoff();

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                RecursalFactIngestResponse response = facade.ingest(processoId, fact);

                String responseJson = mapper.writeValueAsString(response);
                String responseHash = RecursalHash.sha256Hex(responseJson);

                String factId = response.factId() == null
                        ? (fact.factId() == null ? null : fact.factId().toString())
                        : response.factId().toString();

                idempotency.complete(requestHash, IDEMP_RESOURCE, factId, responseHash, responseJson);
                publishCrossAudit(processoId, response, factId, requestHash, responseHash);

                return response;

            } catch (ObjectOptimisticLockingFailureException | OptimisticLockException optimistic) {
                if (attempt >= maxAttempts) {
                    idempotency.fail(requestHash);
                    throw optimistic;
                }
                sleepBackoff(baseBackoff, attempt);
                

            } catch (RuntimeException e) {
                idempotency.fail(requestHash);
                throw e;

            } catch (Exception e) {
                idempotency.fail(requestHash);
                throw new IllegalStateException("Falha ao serializar resposta idempotente", e);
            }
        }
    }

    private static void sleepBackoff(Duration base, int attempt) {
        long baseMs = base == null ? 40L : Math.max(0L, base.toMillis());
        long ms = Math.min(750L, baseMs * attempt);
        long jitter = ThreadLocalRandom.current().nextLong(0, 25);
        try {
            Thread.sleep(ms + jitter);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private void publishCrossAudit(Long processoId,
                                  RecursalFactIngestResponse response,
                                  String factId,
                                  String requestHash,
                                  String responseHash) {
        if (processoId == null) return;

        String pid = processoId.toString();
        String rid = (factId == null || factId.isBlank())
                ? (response == null ? null : Objects.toString(response.dedupKey(), null))
                : factId;

        if (rid != null && !rid.isBlank()) {
            crossAudit.publish("recursal:process:" + pid, "RECURSAL_FACT", rid, requestHash);
            crossAudit.publish("recursal:fact:" + rid, "PROCESSO", pid, responseHash);
        }
        if (response != null && response.dedupKey() != null && !response.dedupKey().isBlank()) {
            crossAudit.publish("recursal:dedup:" + response.dedupKey(), "RECURSAL_FACT", rid, requestHash);
        }
    }
}
