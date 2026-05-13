package com.tcc.pjb.backend.integration.govt;

import java.util.concurrent.CompletableFuture;
import com.tcc.pjb.backend.core.infra.cache.MortalityCache;

public interface GovRegistryClient {

    
    CompletableFuture<MortalityCache.VitalStatus> checkVitalStatus(String cpf);
}
