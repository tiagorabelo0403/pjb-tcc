package com.tcc.pjb.backend.service.communication.domicilio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DomicilioJudicialFailureDashboardServiceTest {

    private final DomicilioJudicialFailureDashboardService service =
            new DomicilioJudicialFailureDashboardService();

    @Test
    void construir_totalCorreto_comFalhasAtivas() {
        var falhas = List.of(
                falha(DomicilioJudicialFailureClassifier.FalhaClassificacao.TRANSITORIA, 2, false),
                falha(DomicilioJudicialFailureClassifier.FalhaClassificacao.INDISPONIBILIDADE_SERVICO, 4, false),
                falha(DomicilioJudicialFailureClassifier.FalhaClassificacao.TRANSITORIA, 1, true));
        var dashboard = service.construir(falhas);
        assertThat(dashboard.totalFalhas()).isEqualTo(3);
        assertThat(dashboard.falhasAtivas()).isEqualTo(2);
    }

    @Test
    void construir_maisCriticas_quandoTresTentativas() {
        var falhas = List.of(
                falha(DomicilioJudicialFailureClassifier.FalhaClassificacao.TRANSITORIA, 5, false),
                falha(DomicilioJudicialFailureClassifier.FalhaClassificacao.TRANSITORIA, 1, false));
        var dashboard = service.construir(falhas);
        assertThat(dashboard.maisCriticas()).hasSize(1);
        assertThat(dashboard.maisCriticas().getFirst().tentativas()).isEqualTo(5);
    }

    @Test
    void construir_porClassificacao_agrupado() {
        var falhas = List.of(
                falha(DomicilioJudicialFailureClassifier.FalhaClassificacao.TRANSITORIA, 2, false),
                falha(DomicilioJudicialFailureClassifier.FalhaClassificacao.TRANSITORIA, 3, false),
                falha(DomicilioJudicialFailureClassifier.FalhaClassificacao.CREDENCIAL_INVALIDA, 1, false));
        var dashboard = service.construir(falhas);
        assertThat(dashboard.porClassificacao())
                .containsEntry(DomicilioJudicialFailureClassifier.FalhaClassificacao.TRANSITORIA, 2L);
    }

    private DomicilioJudicialFailureDashboardService.FalhaRegistrada falha(
            DomicilioJudicialFailureClassifier.FalhaClassificacao classificacao,
            int tentativas, boolean resolvida) {
        return new DomicilioJudicialFailureDashboardService.FalhaRegistrada(
                UUID.randomUUID(), "destinatario-001",
                classificacao, tentativas, Instant.now(), resolvida);
    }
}
