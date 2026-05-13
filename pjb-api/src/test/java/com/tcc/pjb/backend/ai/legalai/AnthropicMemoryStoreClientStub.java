package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicMemoryStoreClient;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AnthropicMemoryStoreClientStub implements AnthropicMemoryStoreClient {

    private final ConcurrentMap<String, AnthropicStoreRef> stores = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AnthropicMemoryEntryRef> memories = new ConcurrentHashMap<>();

    @Override
    public AnthropicStoreRef criarStore(String nome, String descricao, MemoryAccessPolicy.AccessType access) {
        String id = "stub_store_" + UUID.randomUUID();
        AnthropicStoreRef ref = new AnthropicStoreRef(id, nome, "active");
        stores.put(id, ref);
        return ref;
    }

    @Override
    public Optional<AnthropicStoreRef> buscarStore(String anthropicStoreId) {
        return Optional.ofNullable(stores.get(anthropicStoreId));
    }

    @Override
    public void arquivarStore(String anthropicStoreId) {
        stores.computeIfPresent(anthropicStoreId, (k, v) -> new AnthropicStoreRef(v.id(), v.nome(), "archived"));
    }

    @Override
    public AnthropicMemoryEntryRef criarMemory(String anthropicStoreId, String chave, String conteudo) {
        String id = "stub_mem_" + UUID.randomUUID();
        AnthropicMemoryEntryRef ref = new AnthropicMemoryEntryRef(id, chave, conteudo, 1);
        memories.put(id, ref);
        return ref;
    }

    @Override
    public AnthropicMemoryEntryRef atualizarMemory(String anthropicStoreId, String memoryId, String conteudo) {
        AnthropicMemoryEntryRef existing = memories.getOrDefault(memoryId,
                new AnthropicMemoryEntryRef(memoryId, "unknown", conteudo, 1));
        AnthropicMemoryEntryRef updated = new AnthropicMemoryEntryRef(existing.id(), existing.chave(), conteudo, existing.versao() + 1);
        memories.put(memoryId, updated);
        return updated;
    }

    @Override
    public void deletarMemory(String anthropicStoreId, String memoryId) {
        memories.remove(memoryId);
    }

    public int totalStores() {
        return stores.size();
    }

    public int totalMemories() {
        return memories.size();
    }
}
