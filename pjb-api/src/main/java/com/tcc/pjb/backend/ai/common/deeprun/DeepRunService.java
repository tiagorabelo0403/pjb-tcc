package com.tcc.pjb.backend.ai.common.deeprun;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DeepRunService {

    private final DeepRunJobStore store;

    public DeepRunService(DeepRunJobStore store) {
        this.store = store;
    }

    public DeepRunJob create(DeepRunJobType type, DeepRunBudget budget) {
        return store.create(type, budget);
    }

    public Optional<DeepRunJob> get(UUID id) {
        return store.get(id);
    }

    public Optional<DeepRunJob> checkpoint(UUID id, String summary, Map<String, Object> metrics) {
        Optional<DeepRunJob> opt = store.get(id);
        opt.ifPresent(job -> job.addCheckpoint(new DeepRunCheckpoint(Instant.now(), summary, metrics)));
        return opt;
    }

    public Optional<DeepRunJob> complete(UUID id, String resultRef) {
        Optional<DeepRunJob> opt = store.get(id);
        opt.ifPresent(job -> job.complete(resultRef));
        return opt;
    }

    public Optional<DeepRunJob> fail(UUID id, String error) {
        Optional<DeepRunJob> opt = store.get(id);
        opt.ifPresent(job -> job.fail(error));
        return opt;
    }
}
