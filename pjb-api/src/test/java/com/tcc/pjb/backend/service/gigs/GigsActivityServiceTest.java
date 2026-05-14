package com.tcc.pjb.backend.service.gigs;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GigsActivityServiceTest {

    private final GigsActivityService service = new GigsActivityService();

    @Test
    void agrupar_separaPendentesEConcluidas() {
        var processoId = UUID.randomUUID();
        var atividades = List.of(
                atividade(GigsActivityType.ANALISAR, false, null),
                atividade(GigsActivityType.EXPEDIR, true, null));
        var resultado = service.agrupar(processoId, atividades);
        assertThat(resultado.pendentes()).hasSize(1);
        assertThat(resultado.concluidas()).hasSize(1);
    }

    @Test
    void agrupar_contaAtrasadas_quandoDueAtNoPassado() {
        var processoId = UUID.randomUUID();
        var atrasada = atividade(GigsActivityType.MINUTAR, false, Instant.now().minusSeconds(3600));
        var resultado = service.agrupar(processoId, List.of(atrasada));
        assertThat(resultado.totalAtrasadas()).isEqualTo(1);
    }

    @Test
    void agrupar_naoContaAtrasadas_quandoDueAtFuturo() {
        var processoId = UUID.randomUUID();
        var futura = atividade(GigsActivityType.CONFERIR, false, Instant.now().plusSeconds(86400));
        var resultado = service.agrupar(processoId, List.of(futura));
        assertThat(resultado.totalAtrasadas()).isEqualTo(0);
    }

    @Test
    void agrupar_listaVazia_quandoSemAtividades() {
        var resultado = service.agrupar(UUID.randomUUID(), List.of());
        assertThat(resultado.pendentes()).isEmpty();
        assertThat(resultado.concluidas()).isEmpty();
        assertThat(resultado.totalAtrasadas()).isEqualTo(0);
    }

    private GigsActivityService.AtividadeGigs atividade(GigsActivityType tipo, boolean concluida,
            Instant dueAt) {
        return new GigsActivityService.AtividadeGigs(
                UUID.randomUUID(), UUID.randomUUID(), tipo, "descricao",
                "servidor-001", Instant.now(), dueAt, concluida,
                concluida ? Instant.now() : null);
    }
}
