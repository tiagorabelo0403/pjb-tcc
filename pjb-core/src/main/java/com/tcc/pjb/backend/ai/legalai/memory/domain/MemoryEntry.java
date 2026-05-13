package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.time.Instant;
import java.util.UUID;

public record MemoryEntry(
        UUID id,
        MemoryStoreId storeId,
        String chave,
        String conteudo,
        Instant criadoEm,
        Instant atualizadoEm,
        boolean ativo
) {

    public static MemoryEntry criar(MemoryStoreId storeId, String chave, String conteudo, Instant agora) {
        return new MemoryEntry(UUID.randomUUID(), storeId, chave, conteudo, agora, agora, true);
    }

    public MemoryEntry comConteudo(String novoConteudo, Instant agora) {
        return new MemoryEntry(id, storeId, chave, novoConteudo, criadoEm, agora, ativo);
    }

    public MemoryEntry inativado(Instant agora) {
        return new MemoryEntry(id, storeId, chave, conteudo, criadoEm, agora, false);
    }
}
