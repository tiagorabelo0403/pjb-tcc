package com.tcc.pjb.backend.service.gigs;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GigsMinutaReminderBridgeTest {

    private final GigsMinutaReminderBridge bridge = new GigsMinutaReminderBridge();

    @Test
    void construir_lembreteMinuta_quandoTipoMinutar() {
        var atividade = atividade(GigsActivityType.MINUTAR, null);
        var lembretes = bridge.construir(UUID.randomUUID(), List.of(atividade));
        assertThat(lembretes).anyMatch(l -> l.tipo().equals("MINUTA_PENDENTE"));
    }

    @Test
    void construir_lembretePrazoVencendo_quandoDueAtProximo() {
        var atividade = atividade(GigsActivityType.CONFERIR,
                Instant.now().plusSeconds(3600));
        var lembretes = bridge.construir(UUID.randomUUID(), List.of(atividade));
        assertThat(lembretes).anyMatch(l -> l.tipo().equals("PRAZO_VENCENDO") && l.urgente());
    }

    @Test
    void construir_semLembretes_quandoSemPendencias() {
        var atividade = atividade(GigsActivityType.EXPEDIR, null);
        var lembretes = bridge.construir(UUID.randomUUID(), List.of(atividade));
        assertThat(lembretes).isEmpty();
    }

    @Test
    void construir_listaVazia_quandoSemAtividades() {
        assertThat(bridge.construir(UUID.randomUUID(), List.of())).isEmpty();
    }

    private GigsActivityService.AtividadeGigs atividade(GigsActivityType tipo, Instant dueAt) {
        return new GigsActivityService.AtividadeGigs(
                UUID.randomUUID(), UUID.randomUUID(), tipo, "descricao",
                "servidor-001", Instant.now(), dueAt, false, null);
    }
}
