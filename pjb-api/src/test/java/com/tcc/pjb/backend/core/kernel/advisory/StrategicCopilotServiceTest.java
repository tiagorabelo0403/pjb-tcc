package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StrategicCopilotServiceTest {

    private final StrategicCopilotService service = new StrategicCopilotService();

    @Test
    void shouldCreateRecursalGuidanceForTwin() {
        Processo processo = Processo.builder()
                .id(10L)
                .numeroUnificado("0002")
                .faseAtual(FaseProcessual.RECURSAL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .build();

        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport(
                "EXECUTABLE_WITH_GUARDS",
                0.71d,
                true,
                new NegotiationWindowReport(
                        "ESTAVEL",
                        0.71d,
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        BigDecimal.valueOf(20),
                        List.of(),
                        List.of(),
                        List.of("Avancar")
                ),
                List.of("Clausula de vencimento antecipado"),
                List.of("Quitacao delimitada"),
                List.of("Negociar desistência recursal"),
                Map.of("phase", "RECURSAL")
        );

        StrategicCopilotReport report = service.analyzeProcess(processo, "COMUM_ORDINARIO", null, null, null, null, settlement);

        assertEquals("PROCESS_TWIN", report.lane());
        assertTrue(report.immediateActions().stream().anyMatch(a -> "RECURSAL_FOCUS".equals(a.code())));
        assertTrue(report.negotiationActions().stream().anyMatch(a -> "SETTLEMENT_LANE".equals(a.code())));
    }

    @Test
    void shouldCreateUrgentAndJuizadoGuidanceForPetitionAssist() {
        LaianePeticaoAssistRequest request = new LaianePeticaoAssistRequest();
        request.setRequerLiminar(true);
        request.setRequerJuizadoEspecial(true);
        request.setTextoFatosResumido("Corte abrupto de tratamento e risco imediato ao autor.");
        request.setCpfCnpjAutor("123");
        request.setCpfCnpjReu("456");

        StrategicCopilotReport report = service.analyzeRequest(request, null, "JEC", null, null, null, null);

        assertEquals("PETITION_ASSIST", report.lane());
        assertTrue(report.immediateActions().stream().anyMatch(a -> "URGENT_RELIEF_STRATEGY".equals(a.code())));
        assertTrue(report.negotiationActions().stream().anyMatch(a -> "JUÍZADO_SETTLEMENT_LANE".equals(a.code())));
        assertTrue(report.watchpoints().stream().anyMatch(item -> item.contains("JEC")));
    }
}
