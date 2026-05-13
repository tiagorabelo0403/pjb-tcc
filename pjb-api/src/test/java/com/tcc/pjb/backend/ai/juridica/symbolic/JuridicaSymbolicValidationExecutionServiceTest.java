package com.tcc.pjb.backend.ai.juridica.symbolic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JuridicaSymbolicValidationExecutionServiceTest {

    private final JuridicaSymbolicValidationExecutionService service = new JuridicaSymbolicValidationExecutionService(List.of(
            new LegalPrazoRuleEngine(),
            new LegalCompetenciaRuleEngine(),
            new LegalCabimentoRuleEngine(),
            new LegalSigiloRuleEngine(),
            new LegalProceduralCompatibilityEngine()
    ));

    @Test
    void mustBlockIncompatibleJuizadoEscalationAndExposeEngineDiagnostics() {
        var execution = service.execute(
                LegalSymbolicValidationContext.from(new LegalValidationRequest(
                        "Pretendo interpor recurso especial no juizado especial e sustentar que ele eh tempestivo.",
                        "civel",
                        "juizado especial",
                        "recurso inominado",
                        "validar cabimento recursal e prazo",
                        "publico",
                        Map.of()
                )),
                LegalSymbolicValidationCatalog.standardV3Engines()
        );

        assertEquals(LegalSymbolicValidationExecution.STATUS_BLOCK, execution.status());
        assertTrue(execution.contradictions().stream().anyMatch(item -> item.contains("juizado")));
        assertTrue(execution.diagnostics().containsKey("executedEngineCodes"));
        assertFalse(execution.outcomes().isEmpty());
    }

    @Test
    void mustWarnWhenSensitiveDataLacksMaskingPolicy() {
        var execution = service.execute(
                LegalSymbolicValidationContext.from(new LegalValidationRequest(
                        "CPF 123.456.789-10 juntado para consulta publica irrestrita.",
                        "civel",
                        "comum",
                        "peticao inicial",
                        "validar sigilo",
                        "restrito",
                        Map.of()
                )),
                List.of(LegalSymbolicValidationCatalog.ENGINE_SIGILO)
        );

        assertFalse(execution.missingEvidence().isEmpty());
        assertTrue(execution.outcomes().stream().anyMatch(outcome -> LegalSymbolicValidationCatalog.ENGINE_SIGILO.equals(outcome.engineCode())));
    }
}
