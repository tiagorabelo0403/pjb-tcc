package com.tcc.pjb.backend.service.processual.acceleration.civil;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CivilSaneamentoChecklistServiceTest {

    private final CivilSaneamentoChecklistService service = new CivilSaneamentoChecklistService();

    @Test
    void avaliar_aptoParaInstrucao_quandoPreliminaresESaneamento() {
        var input = new CivilSaneamentoChecklistService.SaneamentoChecklistInput(
                UUID.randomUUID(), true, true, true, true, false, false, false);
        var snapshot = service.avaliar(input);
        assertThat(snapshot.aptoParaInstrucao()).isTrue();
        assertThat(snapshot.sugestaoJulgamentoAntecipado()).isFalse();
    }

    @Test
    void avaliar_julgamentoAntecipado_quandoCabe() {
        var input = new CivilSaneamentoChecklistService.SaneamentoChecklistInput(
                UUID.randomUUID(), true, true, true, true, true, false, false);
        var snapshot = service.avaliar(input);
        assertThat(snapshot.sugestaoJulgamentoAntecipado()).isTrue();
        assertThat(snapshot.proximaAcao()).contains("julgamento antecipado");
    }

    @Test
    void avaliar_naoApto_quandoPreliminaresNaoApreciadas() {
        var input = new CivilSaneamentoChecklistService.SaneamentoChecklistInput(
                UUID.randomUUID(), false, true, true, true, false, false, false);
        var snapshot = service.avaliar(input);
        assertThat(snapshot.aptoParaInstrucao()).isFalse();
        assertThat(snapshot.proximaAcao()).contains("Regularizar");
    }

    @Test
    void avaliar_adicionaItemPericia_quandoRequerida() {
        var input = new CivilSaneamentoChecklistService.SaneamentoChecklistInput(
                UUID.randomUUID(), true, true, true, true, false, false, true);
        var snapshot = service.avaliar(input);
        assertThat(snapshot.itens()).anyMatch(i -> i.descricao().contains("Perícia"));
    }
}
