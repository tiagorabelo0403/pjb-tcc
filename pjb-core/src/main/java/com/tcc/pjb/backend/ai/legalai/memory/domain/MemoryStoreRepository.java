package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.util.List;
import java.util.Optional;

public interface MemoryStoreRepository {

    MemoryStore salvar(MemoryStore store);

    Optional<MemoryStore> buscarPorId(MemoryStoreId id);

    Optional<MemoryStore> buscarPorAnthropicStoreId(String anthropicStoreId);

    List<MemoryStore> listarAtivos();

    List<MemoryStore> listarPorModulo(String moduloOrigem);
}
