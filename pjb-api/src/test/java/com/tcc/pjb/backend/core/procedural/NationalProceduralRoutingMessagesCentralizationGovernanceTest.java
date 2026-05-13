package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingMessagesCentralizationGovernanceTest {

    @Test
    void mustKeepOperationalRoutingMessagesOutOfMainService() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String reviewMessages = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralReviewMessages.java"));
        String forumMessages = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralForumAllocationMessages.java"));

        assertFalse(service.contains("Pré-protocolo nacional exige saneamento adicional antes da distribuição assistida."));
        assertFalse(service.contains("Malha territorial e unidade julgadora demandam validação humana complementar."));
        assertFalse(service.contains("Conector judicial ainda não homologado para protocolo completo no tribunal sugerido."));
        assertFalse(service.contains("Valor da causa próximo ao teto operacional do rito sugerido."));
        assertFalse(service.contains("Conferir distribuição por dependência, prevenção, conexão ou continência antes do protocolo final."));
        assertFalse(service.contains("Âncora territorial extraída de indicação expressa no pedido ou no payload."));
        assertFalse(service.contains("Foram identificados processos relacionados no payload/corpus."));

        assertTrue(reviewMessages.contains("Pré-protocolo nacional exige saneamento adicional antes da distribuição assistida."));
        assertTrue(reviewMessages.contains("Malha territorial e unidade julgadora demandam validação humana complementar."));
        assertTrue(reviewMessages.contains("Valor da causa próximo ao teto operacional do rito sugerido."));
        assertTrue(reviewMessages.contains("Conferir distribuição por dependência, prevenção, conexão ou continência antes do protocolo final."));
        assertTrue(forumMessages.contains("Conector judicial ainda não homologado para protocolo completo no tribunal sugerido."));
        assertTrue(forumMessages.contains("Âncora territorial extraída de indicação expressa no pedido ou no payload."));
        assertTrue(forumMessages.contains("Foram identificados processos relacionados no payload/corpus."));
    }
}
