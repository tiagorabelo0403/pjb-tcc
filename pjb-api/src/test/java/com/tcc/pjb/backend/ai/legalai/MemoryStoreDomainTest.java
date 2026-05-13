package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryEntry;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStore;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryVersion;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryStoreDomainTest {

    private static final Instant AGORA = Instant.parse("2026-05-13T02:00:00Z");

    @Test
    void criarDeveGerarAccessTypeCorretoParaPublic() {
        MemoryStore store = MemoryStore.criar(
                MemoryStoreId.gerar(), "store-pub", "desc", "modulo", MemorySigiloNivel.PUBLIC, AGORA);
        assertThat(store.accessType()).isEqualTo(MemoryAccessPolicy.AccessType.READ_WRITE);
    }

    @Test
    void criarDeveGerarAccessTypeCorretoParaCritico() {
        MemoryStore store = MemoryStore.criar(
                MemoryStoreId.gerar(), "store-crit", "desc", "modulo", MemorySigiloNivel.CRITICO, AGORA);
        assertThat(store.accessType()).isEqualTo(MemoryAccessPolicy.AccessType.DENIED);
    }

    @Test
    void criarDeveIniciarSemAnthropicStoreId() {
        MemoryStore store = MemoryStore.criar(
                MemoryStoreId.gerar(), "store", "desc", "modulo", MemorySigiloNivel.PUBLIC, AGORA);
        assertThat(store.anthropicStoreId()).isNull();
    }

    @Test
    void criarDeveIniciarNaoArquivado() {
        MemoryStore store = MemoryStore.criar(
                MemoryStoreId.gerar(), "store", "desc", "modulo", MemorySigiloNivel.PUBLIC, AGORA);
        assertThat(store.isArquivado()).isFalse();
    }

    @Test
    void comAnthropicStoreIdDevePreservarDemaisAtributos() {
        MemoryStoreId id = MemoryStoreId.gerar();
        MemoryStore store = MemoryStore.criar(id, "nome", "desc", "modulo", MemorySigiloNivel.INSTITUCIONAL, AGORA)
                .comAnthropicStoreId("anthr_store_abc123");

        assertThat(store.id()).isEqualTo(id);
        assertThat(store.anthropicStoreId()).isEqualTo("anthr_store_abc123");
        assertThat(store.nome()).isEqualTo("nome");
        assertThat(store.sigiloNivel()).isEqualTo(MemorySigiloNivel.INSTITUCIONAL);
    }

    @Test
    void arquivadoDeveDefinirArquivadoEmEIsArquivadoTrue() {
        MemoryStore store = MemoryStore.criar(
                MemoryStoreId.gerar(), "store", "desc", "modulo", MemorySigiloNivel.PUBLIC, AGORA)
                .arquivado(AGORA);

        assertThat(store.isArquivado()).isTrue();
        assertThat(store.arquivadoEm()).isEqualTo(AGORA);
    }

    @Test
    void publicPermitePodeEnviarParaAnthropicApi() {
        MemoryStore store = MemoryStore.criar(
                MemoryStoreId.gerar(), "store", "desc", "modulo", MemorySigiloNivel.PUBLIC, AGORA);
        assertThat(store.podeEnviarParaAnthropicApi()).isTrue();
    }

    @Test
    void sigilosoNaoPermitePodeEnviarParaAnthropicApi() {
        MemoryStore store = MemoryStore.criar(
                MemoryStoreId.gerar(), "store", "desc", "modulo", MemorySigiloNivel.SIGILOSO, AGORA);
        assertThat(store.podeEnviarParaAnthropicApi()).isFalse();
    }

    @Test
    void memoryEntryCriarDeveGerarUUID() {
        MemoryEntry entry = MemoryEntry.criar(MemoryStoreId.gerar(), "chave-juridica", "conteudo", AGORA);
        assertThat(entry.id()).isNotNull();
        assertThat(entry.ativo()).isTrue();
    }

    @Test
    void memoryEntryComConteudoDeveAtualizarConteudoMantendoId() {
        MemoryEntry original = MemoryEntry.criar(MemoryStoreId.gerar(), "chave", "original", AGORA);
        MemoryEntry atualizado = original.comConteudo("novo conteudo", AGORA.plusSeconds(60));

        assertThat(atualizado.id()).isEqualTo(original.id());
        assertThat(atualizado.conteudo()).isEqualTo("novo conteudo");
        assertThat(atualizado.ativo()).isTrue();
    }

    @Test
    void memoryEntryInativadoDeveDesativar() {
        MemoryEntry entry = MemoryEntry.criar(MemoryStoreId.gerar(), "chave", "conteudo", AGORA);
        MemoryEntry inativado = entry.inativado(AGORA.plusSeconds(30));

        assertThat(inativado.ativo()).isFalse();
        assertThat(inativado.id()).isEqualTo(entry.id());
    }

    @Test
    void memoryVersionRedactDeveMarcarRedacted() {
        MemoryVersion versao = new MemoryVersion(
                UUID.randomUUID(), UUID.randomUUID(), "conteudo original", 1, AGORA, false);

        assertThat(versao.redacted()).isFalse();

        MemoryVersion redacted = new MemoryVersion(
                versao.id(), versao.memoryEntryId(), "[REDACTED]", versao.versao(), versao.criadoEm(), true);

        assertThat(redacted.redacted()).isTrue();
        assertThat(redacted.conteudo()).isEqualTo("[REDACTED]");
    }

    @Test
    void memoryStoreIdGerarDeveProduzirsIdUnicosCadaVez() {
        MemoryStoreId id1 = MemoryStoreId.gerar();
        MemoryStoreId id2 = MemoryStoreId.gerar();
        assertThat(id1).isNotEqualTo(id2);
    }
}
