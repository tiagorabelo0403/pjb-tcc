package com.tcc.pjb.backend.ai.legalai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.anthropic")
public record AnthropicProperties(
        String apiKey,
        String baseUrl,
        String managedAgentsVersion,
        String dreamingVersion,
        String dreamingModel
) {}
