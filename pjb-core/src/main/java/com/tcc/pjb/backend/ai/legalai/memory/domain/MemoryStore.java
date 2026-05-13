package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.time.Instant;
import java.util.List;

public record MemoryStore(
        MemoryStoreId id,
        String nome,
        String descricao,
        String anthropicStoreId,
        String moduloOrigem,
        MemorySigiloNivel sigiloNivel,
        MemoryAccessPolicy.AccessType accessType,
        Instant criadoEm,
        Instant arquivadoEm,
        List<MemoryEntry> entries
) {

    public static MemoryStore criar(
            MemoryStoreId id,
            String nome,
            String descricao,
            String moduloOrigem,
            MemorySigiloNivel sigiloNivel,
            Instant agora) {
        return new MemoryStore(id, nome, descricao, null, moduloOrigem, sigiloNivel,
                MemoryAccessPolicy.computarAccessType(sigiloNivel), agora, null, List.of());
    }

    public MemoryStore comAnthropicStoreId(String anthropicStoreId) {
        return new MemoryStore(id, nome, descricao, anthropicStoreId, moduloOrigem,
                sigiloNivel, accessType, criadoEm, arquivadoEm, entries);
    }

    public MemoryStore arquivado(Instant agora) {
        return new MemoryStore(id, nome, descricao, anthropicStoreId, moduloOrigem,
                sigiloNivel, accessType, criadoEm, agora, entries);
    }

    public boolean isArquivado() {
        return arquivadoEm != null;
    }

    public boolean podeEnviarParaAnthropicApi() {
        return MemoryAccessPolicy.podeEnviarParaAnthropicApi(sigiloNivel);
    }
}
