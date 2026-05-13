package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.dreaming.domain.Dream;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamId;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamInput;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamResult;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamStatus;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DreamDomainTest {

    private static final Instant AGORA = Instant.parse("2026-05-13T02:00:00Z");
    private static final String MODELO = "claude-sonnet-4-6";

    private Dream dreamPadrao() {
        return Dream.criar(
                DreamId.gerar(),
                new DreamInput.MemoryStoreInput(MemoryStoreId.gerar()),
                "Consolidar aprendizados jurídicos",
                MODELO,
                AGORA);
    }

    @Test
    void criarDeveIniciarComStatusPending() {
        Dream dream = dreamPadrao();
        assertThat(dream.status()).isEqualTo(DreamStatus.PENDING);
    }

    @Test
    void criarDevePreservarIdInformado() {
        DreamId id = DreamId.gerar();
        Dream dream = Dream.criar(id, new DreamInput.MemoryStoreInput(MemoryStoreId.gerar()), null, MODELO, AGORA);
        assertThat(dream.id()).isEqualTo(id);
    }

    @Test
    void criarDeveRejeitarInstrucoesAcimaDoLimite() {
        String instrucoesLongas = "x".repeat(4097);
        assertThatThrownBy(() -> Dream.criar(DreamId.gerar(),
                new DreamInput.MemoryStoreInput(MemoryStoreId.gerar()),
                instrucoesLongas, MODELO, AGORA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("4096");
    }

    @Test
    void criarDeveAceitarInstrucoesNoLimiteExato() {
        String instrucoes4096 = "x".repeat(4096);
        Dream dream = Dream.criar(DreamId.gerar(),
                new DreamInput.MemoryStoreInput(MemoryStoreId.gerar()),
                instrucoes4096, MODELO, AGORA);
        assertThat(dream.instrucoes()).hasSize(4096);
    }

    @Test
    void comAnthropicIdDeveTransicionarParaRunning() {
        Dream dream = dreamPadrao().comAnthropicId("anthr_dream_xyz123");
        assertThat(dream.status()).isEqualTo(DreamStatus.RUNNING);
        assertThat(dream.anthropicDreamId()).isEqualTo("anthr_dream_xyz123");
    }

    @Test
    void completadoDeveTransicionarParaCompleted() {
        MemoryStoreId outputId = MemoryStoreId.gerar();
        DreamResult result = new DreamResult(outputId, "anthr_out_store_abc", 2000L, 800L);
        Dream dream = dreamPadrao().comAnthropicId("anthr_dream_abc").completado(result, AGORA);

        assertThat(dream.status()).isEqualTo(DreamStatus.COMPLETED);
        assertThat(dream.result()).isNotNull();
        assertThat(dream.result().outputStoreId()).isEqualTo(outputId);
        assertThat(dream.encerradoEm()).isEqualTo(AGORA);
        assertThat(dream.erro()).isNull();
    }

    @Test
    void falhouDeveTransicionarParaFailed() {
        Dream dream = dreamPadrao().falhou("timeout na API Anthropic", AGORA);
        assertThat(dream.status()).isEqualTo(DreamStatus.FAILED);
        assertThat(dream.erro()).isEqualTo("timeout na API Anthropic");
        assertThat(dream.encerradoEm()).isEqualTo(AGORA);
    }

    @Test
    void canceladoDeveTransicionarParaCanceled() {
        Dream dream = dreamPadrao().cancelado(AGORA);
        assertThat(dream.status()).isEqualTo(DreamStatus.CANCELED);
        assertThat(dream.encerradoEm()).isEqualTo(AGORA);
        assertThat(dream.erro()).isNull();
    }

    @Test
    void statusTerminalDeveCobrir3EstadosFinais() {
        assertThat(DreamStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(DreamStatus.FAILED.isTerminal()).isTrue();
        assertThat(DreamStatus.CANCELED.isTerminal()).isTrue();
        assertThat(DreamStatus.PENDING.isTerminal()).isFalse();
        assertThat(DreamStatus.RUNNING.isTerminal()).isFalse();
    }

    @Test
    void sessionsInputDeveRejeitarListaVazia() {
        assertThatThrownBy(() -> new DreamInput.SessionsInput(MemoryStoreId.gerar(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sessionsInputDeveRejeitarMaisde100Sessions() {
        List<String> sessions = java.util.stream.IntStream.range(0, 101)
                .mapToObj(i -> "sess-" + i)
                .toList();
        assertThatThrownBy(() -> new DreamInput.SessionsInput(MemoryStoreId.gerar(), sessions))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("100");
    }

    @Test
    void sessionsInputDeveAceitarExatamente100Sessions() {
        List<String> sessions = java.util.stream.IntStream.range(0, 100)
                .mapToObj(i -> "sess-" + i)
                .toList();
        DreamInput.SessionsInput input = new DreamInput.SessionsInput(MemoryStoreId.gerar(), sessions);
        assertThat(input.sessionIds()).hasSize(100);
    }

    @Test
    void dreamPendenteNaoDeveSerAtivo() {
        Dream dream = dreamPadrao();
        assertThat(dream.status().isTerminal()).isFalse();
    }

    @Test
    void sonnetModeloDeveSerPreservado() {
        Dream dream = Dream.criar(DreamId.gerar(),
                new DreamInput.MemoryStoreInput(MemoryStoreId.gerar()),
                null, "claude-opus-4-7", AGORA);
        assertThat(dream.modelo()).isEqualTo("claude-opus-4-7");
    }
}
