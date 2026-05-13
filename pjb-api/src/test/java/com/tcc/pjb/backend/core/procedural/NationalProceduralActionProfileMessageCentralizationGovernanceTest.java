package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class NationalProceduralActionProfileMessageCentralizationGovernanceTest {

    @Test
    void mustKeepActionProfileOperationalMessagesOutOfServiceAndResolvers() throws Exception {
        String service = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingService.java"));
        String resolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileResolver.java"));
        String publicLawResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfilePublicLawResolver.java"));
        String privateRightsResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfilePrivateRightsResolver.java"));
        String familyResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileFamilyResolver.java"));
        String businessResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileBusinessResolver.java"));
        String consumerResolver = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileConsumerResolver.java"));
        String messages = Files.readString(Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfileMessages.java"));

        assertFalse(service.contains("Rito sumaríssimo trabalhista depende de liquidez, individualização do pedido e identificação precisa da parte reclamada."));
        assertFalse(service.contains("Improbidade não tramita em juizado especial."));
        assertFalse(service.contains("Desapropriação é matéria típica fora do sistema dos juizados especiais."));
        assertFalse(resolver.contains("Rito sumaríssimo trabalhista depende de liquidez, individualização do pedido e identificação precisa da parte reclamada."));
        assertFalse(resolver.contains("Improbidade não tramita em juizado especial."));
        assertFalse(resolver.contains("Desapropriação é matéria típica fora do sistema dos juizados especiais."));
        assertFalse(publicLawResolver.contains("Rito sumaríssimo trabalhista depende de liquidez, individualização do pedido e identificação precisa da parte reclamada."));
        assertFalse(publicLawResolver.contains("Improbidade não tramita em juizado especial."));
        assertFalse(publicLawResolver.contains("Desapropriação é matéria típica fora do sistema dos juizados especiais."));
        assertFalse(privateRightsResolver.contains("Rito sumaríssimo trabalhista depende de liquidez, individualização do pedido e identificação precisa da parte reclamada."));
        assertFalse(familyResolver.contains("Rito sumaríssimo trabalhista depende de liquidez, individualização do pedido e identificação precisa da parte reclamada."));
        assertFalse(businessResolver.contains("Improbidade não tramita em juizado especial."));
        assertFalse(consumerResolver.contains("Desapropriação é matéria típica fora do sistema dos juizados especiais."));

        assertTrue(messages.contains("Rito sumaríssimo trabalhista depende de liquidez, individualização do pedido e identificação precisa da parte reclamada."));
        assertTrue(messages.contains("Improbidade não tramita em juizado especial."));
        assertTrue(messages.contains("Desapropriação é matéria típica fora do sistema dos juizados especiais."));
    }
}
