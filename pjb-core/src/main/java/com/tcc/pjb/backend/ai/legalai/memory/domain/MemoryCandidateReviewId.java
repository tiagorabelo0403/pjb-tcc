package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.util.UUID;

public record MemoryCandidateReviewId(UUID value) {

    public static MemoryCandidateReviewId gerar() {
        return new MemoryCandidateReviewId(UUID.randomUUID());
    }

    public static MemoryCandidateReviewId de(UUID uuid) {
        return new MemoryCandidateReviewId(uuid);
    }

    public static MemoryCandidateReviewId de(String uuid) {
        return new MemoryCandidateReviewId(UUID.fromString(uuid));
    }
}
