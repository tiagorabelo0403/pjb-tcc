package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingMessageCatalogBoundaryGovernanceTest {

    @Test
    void mustKeepReviewAndForumOperationalMessagesOutOfGeneralRoutingCatalog() throws Exception {
        String routingMessages = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingMessages.java"));
        String reviewMessages = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralReviewMessages.java"));
        String forumMessages = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationMessages.java"));

        assertFalse(routingMessages.contains("Pré-protocolo nacional exige saneamento adicional antes da distribuição assistida."));
        assertFalse(routingMessages.contains("Classe processual ausente para fechamento seguro da rota procedimental."));
        assertFalse(routingMessages.contains("Conector judicial ainda não homologado para protocolo completo no tribunal sugerido."));
        assertFalse(routingMessages.contains("Âncora territorial extraída de indicação expressa no pedido ou no payload."));
        assertTrue(reviewMessages.contains("Pré-protocolo nacional exige saneamento adicional antes da distribuição assistida."));
        assertTrue(reviewMessages.contains("Classe processual ausente para fechamento seguro da rota procedimental."));
        assertTrue(forumMessages.contains("Conector judicial ainda não homologado para protocolo completo no tribunal sugerido."));
        assertTrue(forumMessages.contains("Âncora territorial extraída de indicação expressa no pedido ou no payload."));
    }
}
