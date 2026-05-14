package com.tcc.pjb.backend.service.sentenca;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SentencaReadinessServiceTest {

    private final SentencaReadinessService service = new SentencaReadinessService();

    @Test
    void avaliar_apta_quandoTudoPronto() {
        var input = input(true, true, true, false, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isTrue();
        assertThat(result.pendencias()).isEmpty();
    }

    @Test
    void avaliar_inapta_quandoInstrucaoAberta() {
        var input = input(false, true, true, false, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Instrução"));
    }

    @Test
    void avaliar_inapta_quandoLaudoPericialPendente() {
        var input = input(true, true, false, true, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Laudo pericial"));
    }

    @Test
    void avaliar_apta_quandoPericiaDeNecessariaENaoExigida() {
        var input = input(true, true, false, false, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isTrue();
    }

    @Test
    void avaliar_inapta_quandoSemMinuta() {
        var input = input(true, true, true, false, true, false, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Minuta"));
    }

    @Test
    void avaliar_inapta_quandoAlegacoesFinaisPendentes() {
        var input = input(true, false, true, false, true, true, true, true);
        var result = service.avaliar(input);
        assertThat(result.apta()).isFalse();
        assertThat(result.pendencias()).anyMatch(p -> p.contains("Alegações finais"));
    }

    private SentencaReadinessService.SentencaReadinessInput input(
            boolean instrucao, boolean alegacoes, boolean peritoLaudo,
            boolean periciaNecessaria, boolean pendencias,
            boolean minuta, boolean revisao, boolean publicacao) {
        return new SentencaReadinessService.SentencaReadinessInput(
                UUID.randomUUID(), SentencaTipo.MERITO_PROCEDENTE,
                instrucao, alegacoes, peritoLaudo, periciaNecessaria,
                pendencias, minuta, revisao, publicacao);
    }
}
