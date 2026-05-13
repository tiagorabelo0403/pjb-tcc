package com.tcc.pjb.backend.ai.legalai.dreaming.domain;

import java.util.UUID;

public record DreamId(UUID value) {

    public static DreamId gerar() {
        return new DreamId(UUID.randomUUID());
    }

    public static DreamId de(UUID value) {
        return new DreamId(value);
    }

    public static DreamId de(String value) {
        return new DreamId(UUID.fromString(value));
    }
}
