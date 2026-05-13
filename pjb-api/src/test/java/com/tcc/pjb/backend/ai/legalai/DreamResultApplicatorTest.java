package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.audit.application.LegalAiAuditService;
import com.tcc.pjb.backend.ai.legalai.dreaming.application.DreamResultApplicator;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.Dream;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamId;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamInput;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamStatus;
import com.tcc.pjb.backend.ai.legalai.dreaming.infra.AnthropicDreamingClient.AnthropicDreamStatusRef;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreRepository;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DreamResultApplicatorTest {

    private final MemoryStoreRepository memoryStoreRepository = mock(MemoryStoreRepository.class);
    private final DreamRepository dreamRepository = mock(DreamRepository.class);
    private final LegalAiAuditService auditService = mock(LegalAiAuditService.class);
    private final Clock clock = Clock.systemUTC();

    private DreamResultApplicator applicator;

    @BeforeEach
    void setUp() {
        applicator = new DreamResultApplicator(memoryStoreRepository, dreamRepository, auditService, clock);
        when(dreamRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(memoryStoreRepository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(auditService).registrar(any());
    }

    private Dream dreamEmExecucao() {
        DreamId id = DreamId.gerar();
        MemoryStoreId storeId = MemoryStoreId.gerar();
        return Dream.criar(id, new DreamInput.MemoryStoreInput(storeId), "instrucoes", "claude-sonnet-4-6", Instant.now(clock))
                .comAnthropicId("anthr_dream_test_123");
    }

    @Test
    void deveMarcardreamComoCompletadoQuandoApiRetornaCompleted() {
        Dream dream = dreamEmExecucao();
        AnthropicDreamStatusRef status = new AnthropicDreamStatusRef(
                "anthr_dream_test_123", "completed", "anthr_out_store_xyz", 1500L, 700L, null);

        Dream resultado = applicator.aplicarResultado(dream, status);

        assertThat(resultado.status()).isEqualTo(DreamStatus.COMPLETED);
        assertThat(resultado.result()).isNotNull();
        assertThat(resultado.result().anthropicOutputStoreId()).isEqualTo("anthr_out_store_xyz");
        assertThat(resultado.result().inputTokens()).isEqualTo(1500L);
        assertThat(resultado.result().outputTokens()).isEqualTo(700L);
    }

    @Test
    void deveMarcardreamComoFalhadoQuandoApiRetornaFailed() {
        Dream dream = dreamEmExecucao();
        AnthropicDreamStatusRef status = new AnthropicDreamStatusRef(
                "anthr_dream_test_123", "failed", null, 0L, 0L, "Erro interno na consolidação");

        Dream resultado = applicator.aplicarResultado(dream, status);

        assertThat(resultado.status()).isEqualTo(DreamStatus.FAILED);
        assertThat(resultado.erro()).isEqualTo("Erro interno na consolidação");
    }

    @Test
    void deveMarcardreamComoCanceladoQuandoStatusDesconhecido() {
        Dream dream = dreamEmExecucao();
        AnthropicDreamStatusRef status = new AnthropicDreamStatusRef(
                "anthr_dream_test_123", "canceled", null, 0L, 0L, null);

        Dream resultado = applicator.aplicarResultado(dream, status);

        assertThat(resultado.status()).isEqualTo(DreamStatus.CANCELED);
    }

    @Test
    void marcarFalhaDeveRegistrarMotivoNoErro() {
        Dream dream = dreamEmExecucao();
        Dream resultado = applicator.marcarFalha(dream, "Timeout após 30 tentativas de polling");

        assertThat(resultado.status()).isEqualTo(DreamStatus.FAILED);
        assertThat(resultado.erro()).contains("Timeout");
    }

    @Test
    void deveCriarOutputStoreQuandoCompletadoComOutputStoreId() {
        Dream dream = dreamEmExecucao();
        AnthropicDreamStatusRef status = new AnthropicDreamStatusRef(
                "anthr_dream_test_123", "completed", "anthr_output_store_abc", 500L, 200L, null);

        Dream resultado = applicator.aplicarResultado(dream, status);

        assertThat(resultado.result().outputStoreId()).isNotNull();
        assertThat(resultado.result().anthropicOutputStoreId()).isEqualTo("anthr_output_store_abc");
    }

    @Test
    void dreamSemOutputStoreIdDeveSerTratadoComoNaoConcluido() {
        Dream dream = dreamEmExecucao();
        AnthropicDreamStatusRef status = new AnthropicDreamStatusRef(
                "anthr_dream_test_123", "completed", null, 100L, 50L, null);

        Dream resultado = applicator.aplicarResultado(dream, status);

        assertThat(resultado.status()).isEqualTo(DreamStatus.CANCELED);
    }
}
