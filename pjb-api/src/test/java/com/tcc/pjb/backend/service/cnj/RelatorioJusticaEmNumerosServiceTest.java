package com.tcc.pjb.backend.service.cnj;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RelatorioJusticaEmNumerosServiceTest {

    private final RelatorioJusticaEmNumerosService service =
            new RelatorioJusticaEmNumerosService();

    private RelatorioJusticaEmNumerosService.PeriodoRelatorio periodo() {
        return new RelatorioJusticaEmNumerosService.PeriodoRelatorio(
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), 2025);
    }

    @Test
    void montar_calculaTaxaSolucao() {
        var r = service.montar("TJCE", periodo(), 1000, 800, 500,
                Map.of(RitoProcessual.PROCEDIMENTO_COMUM, 400L));
        assertThat(r.taxaSolucao()).isEqualTo(0.8);
    }

    @Test
    void montar_semNovos_taxaZero() {
        var r = service.montar("TJCE", periodo(), 0, 0, 100, Map.of());
        assertThat(r.taxaSolucao()).isEqualTo(0.0);
    }

    @Test
    void montar_contémDoisIndicadores() {
        var r = service.montar("TJSP", periodo(), 500, 400, 200,
                Map.of(RitoProcessual.PROCEDIMENTO_COMUM, 100L));
        assertThat(r.indicadores()).hasSize(2);
    }

    @Test
    void montar_observacaoSemRankingPunitivo() {
        var r = service.montar("TRT2", periodo(), 100, 80, 50, Map.of());
        assertThat(r.observacoes()).contains("ranking punitivo");
    }

    @Test
    void montar_processosPorRitoMapeados() {
        var r = service.montar("TJRS", periodo(), 200, 150, 80,
                Map.of(RitoProcessual.COMUM_ORDINARIO, 120L,
                       RitoProcessual.SUMARIO, 80L));
        assertThat(r.processosPorRito()).hasSize(2);
    }
}
