package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryEntryRepository {

    MemoryEntry salvar(MemoryEntry entry);

    Optional<MemoryEntry> buscarPorId(UUID id);

    List<MemoryEntry> listarPorStore(MemoryStoreId storeId);

    void deletar(UUID id);
}
