package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.util.UUID;

public record MemoryStoreId(UUID value) {

    public static MemoryStoreId gerar() {
        return new MemoryStoreId(UUID.randomUUID());
    }

    public static MemoryStoreId de(UUID value) {
        return new MemoryStoreId(value);
    }

    public static MemoryStoreId de(String value) {
        return new MemoryStoreId(UUID.fromString(value));
    }
}
