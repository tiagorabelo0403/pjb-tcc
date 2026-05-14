package com.tcc.pjb.backend.service.analytics.celerity;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RelatorioTempoPorRitoServiceTest {

    private final RelatorioTempoPorRitoService service = new RelatorioTempoPorRitoService();

    @Test
    void gerar_relatorio_comTemposDentroDoSla() {
        var tempos = Map.of(RitoProcessual.PROCEDIMENTO_COMUM, Duration.ofDays(60));
        var slas = Map.of(RitoProcessual.PROCEDIMENTO_COMUM, Duration.ofDays(90));
        var contagens = Map.of(RitoProcessual.PROCEDIMENTO_COMUM, 10L);
        var relatorio = service.gerar(UUID.randomUUID(), "2025-05", tempos, slas, contagens);
        assertThat(relatorio.tempos()).hasSize(1);
        assertThat(relatorio.tempos().getFirst().dentroDoSla()).isTrue();
    }

    @Test
    void gerar_relatorio_comTemposForaDoSla() {
        var tempos = Map.of(RitoProcessual.TRABALHISTA_ORDINARIO, Duration.ofDays(200));
        var slas = Map.of(RitoProcessual.TRABALHISTA_ORDINARIO, Duration.ofDays(90));
        var contagens = Map.of(RitoProcessual.TRABALHISTA_ORDINARIO, 5L);
        var relatorio = service.gerar(UUID.randomUUID(), "2025-05", tempos, slas, contagens);
        assertThat(relatorio.tempos().getFirst().dentroDoSla()).isFalse();
    }

    @Test
    void gerar_observacaoSemRankingIndividual() {
        var relatorio = service.gerar(UUID.randomUUID(), "2025-05", Map.of(), Map.of(), Map.of());
        assertThat(relatorio.observacao()).contains("individuais");
    }
}
