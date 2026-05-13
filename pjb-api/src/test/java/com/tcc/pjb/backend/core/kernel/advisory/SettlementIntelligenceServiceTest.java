package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.*;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SettlementIntelligenceServiceTest {

    private final SettlementIntelligenceService service = new SettlementIntelligenceService();

    @Test
    void shouldReturnFavorableWindowWhenProcessHasEconomicBase() {
        Processo processo = Processo.builder()
                .valorCausa(new BigDecimal("10000.00"))
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build();

        NegotiationWindowReport report = service.analyze(processo, new BigDecimal("7000.00"), java.util.List.of("Parte contrária sinalizou abertura para composição"));

        assertNotNull(report);
        assertTrue(report.alvoSugerido().compareTo(BigDecimal.ZERO) > 0);
        assertFalse(report.recommendations().isEmpty());
    }
}
