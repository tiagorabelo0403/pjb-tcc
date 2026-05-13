package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoStudioProtocolChecklistAndRiskEngineTest {

    @Test
    void deveBloquearEmbargosSemVicioEDecisaoEmbargada() {
        PeticionamentoStudioProtocolChecklistService checklistService = new PeticionamentoStudioProtocolChecklistService();
        PeticionamentoStudioRiskEngineService riskEngineService = new PeticionamentoStudioRiskEngineService();

        var checklist = checklistService.build(new PeticionamentoStudioProtocolChecklistService.ResolveRequest(
                "EMBARGOS",
                "EMBARGOS_DECLARACAO",
                false,
                List.of(),
                "Maria",
                "Empresa X",
                1,
                1,
                1,
                false,
                false,
                0,
                true,
                "PROCURAÇÃO",
                false,
                false,
                null,
                null
        ));

        var risk = riskEngineService.build(new PeticionamentoStudioRiskEngineService.ResolveRequest(
                "EMBARGOS",
                "EMBARGOS_DECLARACAO",
                false,
                List.of(),
                1,
                1,
                1,
                0,
                false,
                false,
                false,
                false,
                List.of(),
                List.of(),
                checklist.summary(),
                "CRITICO",
                (List<Map<String, Object>>) checklist.workspace().get("items"),
                null,
                null
        ));

        assertTrue(Boolean.TRUE.equals(risk.get("blocking")));
        assertFalse(((List<?>) risk.get("blockingIssues")).isEmpty());
        assertTrue(((List<?>) risk.get("blockingIssues")).stream().anyMatch(item -> String.valueOf(item).contains("Embargos") || String.valueOf(item).contains("decisão")));
    }
}
