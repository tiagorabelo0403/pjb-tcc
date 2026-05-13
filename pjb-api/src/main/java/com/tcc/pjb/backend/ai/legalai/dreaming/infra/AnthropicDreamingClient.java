package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import java.util.List;
import java.util.Optional;

public interface AnthropicDreamingClient {

    AnthropicDreamRef criarDream(
            String inputStoreId,
            List<String> sessionIds,
            String instrucoes,
            String modelo
    );

    Optional<AnthropicDreamStatusRef> consultarDream(String anthropicDreamId);

    void cancelarDream(String anthropicDreamId);

    record AnthropicDreamRef(String id, String status) {}

    record AnthropicDreamStatusRef(
            String id,
            String status,
            String outputMemoryStoreId,
            long inputTokens,
            long outputTokens,
            String erro
    ) {
        public boolean isTerminal() {
            return "completed".equalsIgnoreCase(status)
                    || "failed".equalsIgnoreCase(status)
                    || "canceled".equalsIgnoreCase(status);
        }

        public boolean isCompleted() {
            return "completed".equalsIgnoreCase(status);
        }
    }
}
