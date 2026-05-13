package com.tcc.pjb.backend.ai.core.model;


public record CognitiveOutput(
        String domain,
        String role,
        String content
) {
}
