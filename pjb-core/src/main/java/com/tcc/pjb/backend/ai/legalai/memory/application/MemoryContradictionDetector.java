package com.tcc.pjb.backend.ai.legalai.memory.application;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntryRepository;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.events.MemoryContradictionDetectedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class MemoryContradictionDetector {

    private final MemoryEntryRepository memoryEntryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public MemoryContradictionDetector(
            MemoryEntryRepository memoryEntryRepository,
            ApplicationEventPublisher eventPublisher,
            Clock clock) {
        this.memoryEntryRepository = memoryEntryRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public boolean detectarEPublicar(MemoryStoreId storeId, String chave, UUID candidateId) {
        boolean contradita = memoryEntryRepository.listarPorStore(storeId).stream()
                .filter(e -> e.ativo())
                .anyMatch(e -> chavesConflitam(e.chave(), chave));

        if (contradita) {
            eventPublisher.publishEvent(new MemoryContradictionDetectedEvent(
                    candidateId, storeId.value(), chave, Instant.now(clock)));
        }
        return contradita;
    }

    private boolean chavesConflitam(String existente, String nova) {
        if (existente.equalsIgnoreCase(nova)) {
            return true;
        }
        String prefixoExistente = prefixo(existente);
        String prefixoNova = prefixo(nova);
        return prefixoExistente.equalsIgnoreCase(prefixoNova);
    }

    private String prefixo(String chave) {
        int separador = chave.indexOf(':');
        return separador > 0 ? chave.substring(0, separador) : chave;
    }
}
