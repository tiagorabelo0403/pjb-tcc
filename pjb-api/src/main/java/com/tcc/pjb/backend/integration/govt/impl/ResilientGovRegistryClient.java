package com.tcc.pjb.backend.integration.govt.impl;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import com.tcc.pjb.backend.core.infra.cache.MortalityCache;
import com.tcc.pjb.backend.integration.govt.GovRegistryClient;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "pjb.gov.registry.enabled", havingValue = "true")
public class ResilientGovRegistryClient implements GovRegistryClient {

    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration ASYNC_TIMEOUT = Duration.ofSeconds(7);

    private final PjbExecutionOrchestrator executionOrchestrator;
    private final String crcUrl;
    private final String receitaUrl;
    private final RestClient restClient;

    private final SimpleCircuitBreaker cb = new SimpleCircuitBreaker(3, Duration.ofSeconds(20));

    public ResilientGovRegistryClient(PjbExecutionOrchestrator executionOrchestrator,
                                     @Value("${pjb.gov.registry.crc-url:}") String crcUrl,
                                     @Value("${pjb.gov.registry.receita-url:}") String receitaUrl,
                                     HttpClient pjbSharedHttpClient) {
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
        this.crcUrl = crcUrl;
        this.receitaUrl = receitaUrl;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(Objects.requireNonNull(pjbSharedHttpClient, "pjbSharedHttpClient"));
        requestFactory.setReadTimeout(READ_TIMEOUT);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public CompletableFuture<MortalityCache.VitalStatus> checkVitalStatus(String cpf) {
        final String trace = UUID.randomUUID().toString();

        return executionOrchestrator.supply(PjbExecutionDescriptor.externalIo("gov-registry-vital-status", ASYNC_TIMEOUT), () -> {
            if (cpf == null || cpf.isBlank()) {
                return unknown("invalid_cpf", trace);
            }
            if (!cb.allowRequest()) {
                log.warn("[{}] GovRegistry circuit OPEN -> UNKNOWN", trace);
                return unknown("circuit_open", trace);
            }

            try {
                MortalityCache.VitalStatus crc = callIfConfigured(restClient, crcUrl, cpf, "crc", trace);
                MortalityCache.VitalStatus rfb = callIfConfigured(restClient, receitaUrl, cpf, "receita", trace);
                MortalityCache.VitalStatus merged = mergeConservative(crc, rfb, trace);
                cb.onSuccess();
                return merged;
            } catch (Exception ex) {
                cb.onFailure();
                log.warn("[{}] GovRegistry failure -> UNKNOWN: {}", trace, ex.getMessage());
                return unknown("exception", trace);
            }
        }).exceptionally(ex -> {
            cb.onFailure();
            log.warn("[{}] GovRegistry async timeout/failure -> UNKNOWN: {}", trace, ex == null ? "unknown" : ex.getMessage());
            return unknown("async_failure", trace);
        });
    }

    
    private MortalityCache.VitalStatus callIfConfigured(RestClient client, String baseUrl, String cpf, String source, String trace) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return MortalityCache.VitalStatus.builder()
                    .state(MortalityCache.VitalStatus.State.UNKNOWN)
                    .source(source + ":not_configured")
                    .reference(trace)
                    .checkedAt(Instant.now())
                    .deathDate(null)
                    .build();
        }

        try {
            RegistryResponse resp = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(baseUrl)
                            .queryParam("cpf", cpf)
                            .build())
                    .retrieve()
                    .body(RegistryResponse.class);

            MortalityCache.VitalStatus.State st = MortalityCache.VitalStatus.State.UNKNOWN;
            Instant death = null;

            if (resp != null && resp.state != null) {
                st = MortalityCache.VitalStatus.State.valueOf(resp.state.trim().toUpperCase());
                if (resp.deathDateEpochMs != null) {
                    death = Instant.ofEpochMilli(resp.deathDateEpochMs);
                }
            }

            return MortalityCache.VitalStatus.builder()
                    .state(st)
                    .source(source)
                    .reference(Objects.requireNonNullElse(resp != null ? resp.reference : null, trace))
                    .checkedAt(Instant.now())
                    .deathDate(death)
                    .build();

        } catch (IllegalArgumentException | RestClientException ex) {
            return unknown(source + ":error", trace);
        }
    }

    private MortalityCache.VitalStatus mergeConservative(MortalityCache.VitalStatus a, MortalityCache.VitalStatus b, String trace) {
        
        
        
        

        if ((a != null && a.isDeceased()) || (b != null && b.isDeceased())) {
            Instant d = null;
            if (a != null && a.getDeathDate() != null) d = a.getDeathDate();
            if (d == null && b != null && b.getDeathDate() != null) d = b.getDeathDate();

            return MortalityCache.VitalStatus.builder()
                    .state(MortalityCache.VitalStatus.State.DECEASED)
                    .source("merged")
                    .reference(trace)
                    .checkedAt(Instant.now())
                    .deathDate(d)
                    .build();
        }

        if (a != null && a.isAlive() && b != null && b.isAlive()) {
            return MortalityCache.VitalStatus.builder()
                    .state(MortalityCache.VitalStatus.State.ALIVE)
                    .source("merged")
                    .reference(trace)
                    .checkedAt(Instant.now())
                    .deathDate(null)
                    .build();
        }

        return unknown("merged_unknown", trace);
    }

    private MortalityCache.VitalStatus unknown(String source, String trace) {
        return MortalityCache.VitalStatus.builder()
                .state(MortalityCache.VitalStatus.State.UNKNOWN)
                .source(source)
                .reference(trace)
                .checkedAt(Instant.now())
                .deathDate(null)
                .build();
    }

    
    static class SimpleCircuitBreaker {
        private final int failureThreshold;
        private final Duration openDuration;
        private int consecutiveFailures = 0;
        private Instant openUntil = null;

        SimpleCircuitBreaker(int failureThreshold, Duration openDuration) {
            this.failureThreshold = Math.max(1, failureThreshold);
            this.openDuration = openDuration == null ? Duration.ofSeconds(30) : openDuration;
        }

        synchronized boolean allowRequest() {
            if (openUntil == null) return true;
            if (Instant.now().isAfter(openUntil)) {
                openUntil = null;
                consecutiveFailures = 0;
                return true;
            }
            return false;
        }

        synchronized void onSuccess() {
            consecutiveFailures = 0;
            openUntil = null;
        }

        synchronized void onFailure() {
            consecutiveFailures++;
            if (consecutiveFailures >= failureThreshold) {
                openUntil = Instant.now().plus(openDuration);
            }
        }
    }

    
    static class RegistryResponse {
        public String state;              
        public Long deathDateEpochMs;     
        public String reference;          
    }
}
