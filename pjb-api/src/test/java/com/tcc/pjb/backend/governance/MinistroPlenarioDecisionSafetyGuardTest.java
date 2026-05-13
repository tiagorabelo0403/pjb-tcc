package com.tcc.pjb.backend.governance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class MinistroPlenarioDecisionSafetyGuardTest {

    @Test
    void plenario_service_keeps_decision_safety_calls_for_vote_and_proclamation() throws Exception {
        Path source = Path.of("src/main/java/com/tcc/pjb/backend/service/ministro/MinistroPlenarioAvancadoService.java");
        String java = Files.readString(source);
        assertTrue(java.contains("requireSafeDecisionContext(sessao.getProcesso(), ministro, \"VOTO_PLENARIO\""),
                "Blindagem do voto plenário não pode ser removida.");
        assertTrue(java.contains("requireSafeDecisionContext(sessao.getProcesso(), ministro, \"PROCLAMACAO_PLENARIA\""),
                "Blindagem da proclamação plenária não pode ser removida.");
    }
}
