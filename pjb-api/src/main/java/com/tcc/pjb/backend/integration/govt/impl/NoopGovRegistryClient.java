package com.tcc.pjb.backend.integration.govt.impl;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.infra.cache.MortalityCache;
import com.tcc.pjb.backend.integration.govt.GovRegistryClient;

@Component
@ConditionalOnMissingBean(GovRegistryClient.class)
public class NoopGovRegistryClient implements GovRegistryClient {

    @Override
    public CompletableFuture<MortalityCache.VitalStatus> checkVitalStatus(String cpf) {
        return CompletableFuture.completedFuture(
                MortalityCache.VitalStatus.builder()
                        .state(MortalityCache.VitalStatus.State.UNKNOWN)
                        .source("noop")
                        .reference(UUID.randomUUID().toString())
                        .checkedAt(Instant.now())
                        .deathDate(null)
                        .build()
        );
    }
}
