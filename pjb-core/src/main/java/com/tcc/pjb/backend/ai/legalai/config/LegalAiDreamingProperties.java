package com.tcc.pjb.backend.ai.legalai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.legal-ai.dreaming")
public record LegalAiDreamingProperties(
        boolean enabled,
        String schedule,
        int maxSessionsPerDream,
        int pollingMaxAttempts,
        int pollingInitialDelaySeconds
) {}
