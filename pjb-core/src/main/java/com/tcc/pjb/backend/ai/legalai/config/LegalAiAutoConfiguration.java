package com.tcc.pjb.backend.ai.legalai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AnthropicProperties.class, LegalAiDreamingProperties.class})
public class LegalAiAutoConfiguration {}
