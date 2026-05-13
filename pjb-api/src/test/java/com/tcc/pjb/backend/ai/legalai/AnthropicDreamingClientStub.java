package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicDreamingClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AnthropicDreamingClientStub implements AnthropicDreamingClient {

    private final ConcurrentMap<String, DreamState> dreams = new ConcurrentHashMap<>();

    @Override
    public AnthropicDreamRef criarDream(String inputStoreId, List<String> sessionIds, String instrucoes, String modelo) {
        String id = "stub_dream_" + UUID.randomUUID();
        dreams.put(id, new DreamState(id, inputStoreId));
        return new AnthropicDreamRef(id, "pending");
    }

    @Override
    public Optional<AnthropicDreamStatusRef> consultarDream(String anthropicDreamId) {
        DreamState state = dreams.get(anthropicDreamId);
        if (state == null) {
            return Optional.empty();
        }
        String status = state.proximoStatus();
        String outputStoreId = "completed".equals(status) ? "stub_out_store_" + UUID.randomUUID() : null;
        return Optional.of(new AnthropicDreamStatusRef(
                anthropicDreamId,
                status,
                outputStoreId,
                status.equals("completed") ? 1000L : 0L,
                status.equals("completed") ? 500L : 0L,
                null
        ));
    }

    @Override
    public void cancelarDream(String anthropicDreamId) {
        dreams.computeIfPresent(anthropicDreamId, (k, v) -> new DreamState(k, v.inputStoreId, "canceled"));
    }

    public int totalDreamsCriados() {
        return dreams.size();
    }

    private static final class DreamState {
        final String id;
        final String inputStoreId;
        final AtomicInteger consultaCount = new AtomicInteger(0);
        final String forcedStatus;

        DreamState(String id, String inputStoreId) {
            this.id = id;
            this.inputStoreId = inputStoreId;
            this.forcedStatus = null;
        }

        DreamState(String id, String inputStoreId, String forcedStatus) {
            this.id = id;
            this.inputStoreId = inputStoreId;
            this.forcedStatus = forcedStatus;
        }

        String proximoStatus() {
            if (forcedStatus != null) {
                return forcedStatus;
            }
            int count = consultaCount.incrementAndGet();
            if (count == 1) return "running";
            return "completed";
        }
    }
}
