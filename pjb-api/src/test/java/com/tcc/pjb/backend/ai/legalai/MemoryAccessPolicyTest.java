package com.tcc.pjb.backend.ai.legalai;

import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryAccessPolicy.AccessType;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemorySigiloNivel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryAccessPolicyTest {

    @Test
    void publicDevePermitirReadWrite() {
        assertThat(MemoryAccessPolicy.computarAccessType(MemorySigiloNivel.PUBLIC))
                .isEqualTo(AccessType.READ_WRITE);
    }

    @Test
    void institucionalDevePermitirReadWrite() {
        assertThat(MemoryAccessPolicy.computarAccessType(MemorySigiloNivel.INSTITUCIONAL))
                .isEqualTo(AccessType.READ_WRITE);
    }

    @Test
    void sigilosoDevePermitirApenasReadOnly() {
        assertThat(MemoryAccessPolicy.computarAccessType(MemorySigiloNivel.SIGILOSO))
                .isEqualTo(AccessType.READ_ONLY);
    }

    @Test
    void criticoDeveNegarAcesso() {
        assertThat(MemoryAccessPolicy.computarAccessType(MemorySigiloNivel.CRITICO))
                .isEqualTo(AccessType.DENIED);
    }

    @Test
    void publicPodeEnviarParaAnthropicApi() {
        assertThat(MemoryAccessPolicy.podeEnviarParaAnthropicApi(MemorySigiloNivel.PUBLIC))
                .isTrue();
    }

    @Test
    void institucionalPodeEnviarParaAnthropicApi() {
        assertThat(MemoryAccessPolicy.podeEnviarParaAnthropicApi(MemorySigiloNivel.INSTITUCIONAL))
                .isTrue();
    }

    @Test
    void sigilosoNaoPodeEnviarParaAnthropicApi() {
        assertThat(MemoryAccessPolicy.podeEnviarParaAnthropicApi(MemorySigiloNivel.SIGILOSO))
                .isFalse();
    }

    @Test
    void criticoNaoPodeEnviarParaAnthropicApi() {
        assertThat(MemoryAccessPolicy.podeEnviarParaAnthropicApi(MemorySigiloNivel.CRITICO))
                .isFalse();
    }

    @Test
    void sigilosoRequerRevisaoHumana() {
        assertThat(MemoryAccessPolicy.requerRevisaoHumana(MemorySigiloNivel.SIGILOSO))
                .isTrue();
    }

    @Test
    void criticoRequerRevisaoHumana() {
        assertThat(MemoryAccessPolicy.requerRevisaoHumana(MemorySigiloNivel.CRITICO))
                .isTrue();
    }

    @Test
    void publicNaoRequerRevisaoHumana() {
        assertThat(MemoryAccessPolicy.requerRevisaoHumana(MemorySigiloNivel.PUBLIC))
                .isFalse();
    }

    @Test
    void institucionalNaoRequerRevisaoHumana() {
        assertThat(MemoryAccessPolicy.requerRevisaoHumana(MemorySigiloNivel.INSTITUCIONAL))
                .isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = MemorySigiloNivel.class, names = {"PUBLIC", "INSTITUCIONAL"})
    void niveisPermitidosParaAnthropicNaoRequeremRevisaoHumana(MemorySigiloNivel nivel) {
        assertThat(MemoryAccessPolicy.podeEnviarParaAnthropicApi(nivel)).isTrue();
        assertThat(MemoryAccessPolicy.requerRevisaoHumana(nivel)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = MemorySigiloNivel.class, names = {"SIGILOSO", "CRITICO"})
    void niveisRestritosNaoPodemIrParaAnthropicERequeremRevisao(MemorySigiloNivel nivel) {
        assertThat(MemoryAccessPolicy.podeEnviarParaAnthropicApi(nivel)).isFalse();
        assertThat(MemoryAccessPolicy.requerRevisaoHumana(nivel)).isTrue();
    }
}
