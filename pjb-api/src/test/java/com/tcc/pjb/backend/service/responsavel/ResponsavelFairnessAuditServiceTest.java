package com.tcc.pjb.backend.service.responsavel;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ResponsavelFairnessAuditServiceTest {

    private final ResponsavelFairnessAuditService service = new ResponsavelFairnessAuditService();

    @Test
    void auditar_equitativo_quandoDistribuicaoUniforme() {
        var historico = List.of(
                new ResponsavelFairnessAuditService.DesignacaoHistorico("srv-001", "EXPEDIR", UUID.randomUUID()),
                new ResponsavelFairnessAuditService.DesignacaoHistorico("srv-002", "EXPEDIR", UUID.randomUUID()),
                new ResponsavelFairnessAuditService.DesignacaoHistorico("srv-001", "CERTIFICAR", UUID.randomUUID()),
                new ResponsavelFairnessAuditService.DesignacaoHistorico("srv-002", "CERTIFICAR", UUID.randomUUID()));
        var relatorio = service.auditar(historico);
        assertThat(relatorio.equitativo()).isTrue();
        assertThat(relatorio.desvioMediaPercent()).isLessThanOrEqualTo(20.0);
    }

    @Test
    void auditar_desequilibrado_quandoCargaConcentrada() {
        var historico = List.of(
                new ResponsavelFairnessAuditService.DesignacaoHistorico("srv-001", "EXPEDIR", UUID.randomUUID()),
                new ResponsavelFairnessAuditService.DesignacaoHistorico("srv-001", "EXPEDIR", UUID.randomUUID()),
                new ResponsavelFairnessAuditService.DesignacaoHistorico("srv-001", "EXPEDIR", UUID.randomUUID()),
                new ResponsavelFairnessAuditService.DesignacaoHistorico("srv-001", "EXPEDIR", UUID.randomUUID()),
                new ResponsavelFairnessAuditService.DesignacaoHistorico("srv-002", "EXPEDIR", UUID.randomUUID()));
        var relatorio = service.auditar(historico);
        assertThat(relatorio.equitativo()).isFalse();
        assertThat(relatorio.observacao()).contains("desequilibrada");
    }

    @Test
    void auditar_semHistorico_retornaEquitativo() {
        var relatorio = service.auditar(List.of());
        assertThat(relatorio.equitativo()).isTrue();
        assertThat(relatorio.observacao()).contains("Sem histórico");
    }
}
