package com.tcc.pjb.backend.ai.jurimetria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsAggregationService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JurimetriaServiceTest {

    @Test
    void buildsRepositoryBackedIndicators() {
        ProcessoAnalyticsAggregationService analytics = mock(ProcessoAnalyticsAggregationService.class);
        when(analytics.agregadosPorRamo(eq("CIVIL")))
                .thenReturn(List.<Object[]>of(new Object[]{"CIVIL", 100L, 62L, 18L, 38L, 240.0}));
        when(analytics.agregadosPorRamoETribunal(eq("CIVIL"), eq("TJCE")))
                .thenReturn(List.<Object[]>of(new Object[]{80L, 30L, 12L, 20L}));

        JurimetriaService service = new JurimetriaService(analytics);
        var report = service.gerarRelatorio(
                "Obrigação de fazer com tutela de urgência",
                "TJCE",
                "PROCEDIMENTO_COMUM_CIVEL",
                "Saúde",
                Map.of("ramo", "CIVIL")
        );

        assertThat(report.getIndicadores()).isNotEmpty();
        assertThat(report.getIndicadores().stream().map(i -> i.getNome())).contains(
                "taxa_julgamento_ramo",
                "taxa_acordo_tribunal",
                "prob_tutela_urgencia"
        );
        assertThat(report.getExplicacao()).contains("CIVIL");
    }
}
