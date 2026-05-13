package com.tcc.pjb.backend.integration.govt;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.engine.lifecycle.ProcessLifeCycleOrchestrator;
import com.tcc.pjb.backend.core.infra.cache.MortalityCache;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "pjb.gov.vital-monitor.enabled", havingValue = "true")
public class CrcIntegrationService {

    private static final long VITAL_STATUS_TIMEOUT_SECONDS = 8L;

    private final GovRegistryClient govRegistryClient;
    private final ProcessoRepository processoRepository;
    private final MortalityCache mortalityCache;
    private final ProcessLifeCycleOrchestrator orchestrator;
    private final AuditoriaService auditoriaService;

    
    public CompletableFuture<Void> verificarCpfEmProcessoAtivo(Long processoId, String cpf, String operador) {
        if (processoId == null || cpf == null || cpf.isBlank()) {
            return CompletableFuture.completedFuture(null);
        }

        
        var cached = mortalityCache.get(cpf).orElse(null);
        if (cached != null && cached.isDeceased()) {
            orchestrator.onDeathDetected(processoId, cpf, cached, operador);
            return CompletableFuture.completedFuture(null);
        }

        return govRegistryClient.checkVitalStatus(cpf)
                .completeOnTimeout(null, VITAL_STATUS_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .thenAccept(status -> {
                    if (status != null) {
                        mortalityCache.put(cpf, status);
                    }
                    if (status != null && status.isDeceased()) {
                        orchestrator.onDeathDetected(processoId, cpf, status, operador);
                    } else {
                        auditoriaService.registrar(operador == null ? "system" : operador,
                                "VITAL_CHECK",
                                String.valueOf(processoId));
                    }
                })
                .exceptionally(ex -> {
                    log.warn("CRC check failed: {}", ex == null ? "unknown" : ex.getMessage());
                    return null;
                });
    }

    
    public CompletableFuture<Void> verificarProcesso(Processo processo, String operador) {
        if (processo == null || processo.getId() == null) {
            return CompletableFuture.completedFuture(null);
        }
        if (processo.getUsuario() == null || processo.getUsuario().getCpf() == null) {
            return CompletableFuture.completedFuture(null);
        }
        return verificarCpfEmProcessoAtivo(processo.getId(), processo.getUsuario().getCpf(), operador);
    }
}
