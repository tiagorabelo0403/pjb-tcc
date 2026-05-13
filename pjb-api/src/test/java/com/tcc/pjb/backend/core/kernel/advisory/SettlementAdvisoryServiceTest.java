package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettlementAdvisoryServiceTest {

    private final SettlementAdvisoryService service = new SettlementAdvisoryService(new SettlementIntelligenceService());

    @Test
    void shouldReduceExecutabilityWhenAmountExceedsSuggestedCeiling() {
        Processo processo = Processo.builder()
                .id(11L)
                .numeroUnificado("0003")
                .faseAtual(FaseProcessual.EXECUCAO)
                .valorCausa(BigDecimal.valueOf(1000))
                .build();

        SettlementAdvisoryReport report = service.analyze(processo, "JUIZADO_ESPECIAL_CIVEL", BigDecimal.valueOf(1500), java.util.List.of("Processo em execução"), null);

        assertFalse(report.executable());
        assertFalse(report.conditionalClauses().isEmpty());
        assertFalse(report.executionSafeguards().isEmpty());
    }
}
