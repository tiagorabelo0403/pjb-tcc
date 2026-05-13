package com.tcc.pjb.backend.query.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "pjb.search", name = "enabled", havingValue = "true")
@EnableElasticsearchRepositories(basePackages = "com.tcc.pjb.backend.query")
public class QueryInfrastructureConfig {
}
