package com.tcc.pjb.backend.core.processo.juizado.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralJuizadoDecisionMessageCentralizationGovernanceTest {

    @Test
    void mustKeepJuizadoDecisionOperationalMessagesOutOfServiceAndResolver() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String resolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoDecisionResolver.java"));
        String exclusionResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoExclusionResolver.java"));
        String trackResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoTrackResolver.java"));
        String messages = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoDecisionMessages.java"));

        assertFalse(service.contains("A natureza da ação aponta trilha especial ou excluída do regime dos juizados."));
        assertFalse(service.contains("Valor da causa ausente para fechar aderência ao JEF."));
        assertFalse(service.contains("Marcadores de menor potencial ofensivo sugerem trilha do Juizado Especial Criminal, sujeita à conferência da capitulação final."));
        assertFalse(resolver.contains("A natureza da ação aponta trilha especial ou excluída do regime dos juizados."));
        assertFalse(resolver.contains("Valor da causa ausente para fechar aderência ao JEF."));
        assertFalse(resolver.contains("Marcadores de menor potencial ofensivo sugerem trilha do Juizado Especial Criminal, sujeita à conferência da capitulação final."));
        assertFalse(exclusionResolver.contains("A natureza da ação aponta trilha especial ou excluída do regime dos juizados."));
        assertFalse(trackResolver.contains("Valor da causa ausente para fechar aderência ao JEF."));
        assertFalse(trackResolver.contains("Marcadores de menor potencial ofensivo sugerem trilha do Juizado Especial Criminal, sujeita à conferência da capitulação final."));

        assertTrue(messages.contains("A natureza da ação aponta trilha especial ou excluída do regime dos juizados."));
        assertTrue(messages.contains("Valor da causa ausente para fechar aderência ao JEF."));
        assertTrue(messages.contains("Marcadores de menor potencial ofensivo sugerem trilha do Juizado Especial Criminal, sujeita à conferência da capitulação final."));
    }
}
