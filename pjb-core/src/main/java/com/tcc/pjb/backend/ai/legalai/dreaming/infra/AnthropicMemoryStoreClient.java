package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;

import java.util.Optional;

public interface AnthropicMemoryStoreClient {

    AnthropicStoreRef criarStore(String nome, String descricao, MemoryAccessPolicy.AccessType access);

    Optional<AnthropicStoreRef> buscarStore(String anthropicStoreId);

    void arquivarStore(String anthropicStoreId);

    AnthropicMemoryEntryRef criarMemory(String anthropicStoreId, String chave, String conteudo);

    AnthropicMemoryEntryRef atualizarMemory(String anthropicStoreId, String memoryId, String conteudo);

    void deletarMemory(String anthropicStoreId, String memoryId);

    record AnthropicStoreRef(String id, String nome, String status) {}

    record AnthropicMemoryEntryRef(String id, String chave, String conteudo, int versao) {}
}
