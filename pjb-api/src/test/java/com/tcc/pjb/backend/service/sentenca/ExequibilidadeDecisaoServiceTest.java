package com.tcc.pjb.backend.service.sentenca;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ExequibilidadeDecisaoServiceTest {

    private final ExequibilidadeDecisaoService service = new ExequibilidadeDecisaoService();

    @Test
    void avaliar_exequivel_quandoSemLiquidacaoEDevedorCitado() {
        var input = new ExequibilidadeDecisaoService.ExequibilidadeInput(
                UUID.randomUUID(), SentencaTipo.MERITO_PROCEDENTE,
                true, false, false, true, true, false, true);
        var result = service.avaliar(input);
        assertThat(result.exequivel()).isTrue();
        assertThat(result.exigeLiquidacao()).isFalse();
    }

    @Test
    void avaliar_naoExequivel_semTituloExecutivo() {
        var input = new ExequibilidadeDecisaoService.ExequibilidadeInput(
                UUID.randomUUID(), SentencaTipo.DESPACHO_ORDINATORIO,
                false, false, false, false, false, false, false);
        var result = service.avaliar(input);
        assertThat(result.exequivel()).isFalse();
        assertThat(result.impedimentos()).anyMatch(i -> i.contains("título executivo"));
    }

    @Test
    void avaliar_bloqueado_porLiquidacaoPendente() {
        var input = new ExequibilidadeDecisaoService.ExequibilidadeInput(
                UUID.randomUUID(), SentencaTipo.MERITO_PROCEDENTE,
                true, true, false, true, true, false, false);
        var result = service.avaliar(input);
        assertThat(result.exequivel()).isFalse();
        assertThat(result.exigeLiquidacao()).isTrue();
        assertThat(result.impedimentos()).anyMatch(i -> i.contains("Liquidação"));
    }

    @Test
    void avaliar_pendencia_devedorNaoCitado() {
        var input = new ExequibilidadeDecisaoService.ExequibilidadeInput(
                UUID.randomUUID(), SentencaTipo.MERITO_PROCEDENTE,
                true, false, false, true, false, false, false);
        var result = service.avaliar(input);
        assertThat(result.impedimentos()).anyMatch(i -> i.contains("não citado"));
    }
}
