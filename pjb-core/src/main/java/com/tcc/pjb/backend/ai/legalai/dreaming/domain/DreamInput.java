package com.tcc.pjb.backend.ai.legalai.dreaming.domain;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;

import java.util.List;

public sealed interface DreamInput permits DreamInput.MemoryStoreInput, DreamInput.SessionsInput {

    record MemoryStoreInput(MemoryStoreId storeId) implements DreamInput {}

    record SessionsInput(
            MemoryStoreId inputStoreId,
            List<String> sessionIds
    ) implements DreamInput {
        public SessionsInput {
            if (sessionIds == null || sessionIds.isEmpty()) {
                throw new IllegalArgumentException("sessionIds não pode ser vazio");
            }
            if (sessionIds.size() > 100) {
                throw new IllegalArgumentException("Máximo de 100 sessions por dream");
            }
            sessionIds = List.copyOf(sessionIds);
        }
    }

    default MemoryStoreId inputStoreId() {
        return switch (this) {
            case MemoryStoreInput m -> m.storeId();
            case SessionsInput s -> s.inputStoreId();
        };
    }
}
