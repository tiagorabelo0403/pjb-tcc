package com.tcc.pjb.backend.core.processual.routing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class NationalProcessRoutingMessagesCentralizationGovernanceTest {

    @Test
    void mensagensOperacionaisNaoDevemFicarEspalhadasNoServicoPrincipal() throws IOException {
        Path service = Path.of("src/main/java/com/tcc/pjb/backend/core/processual/routing/NationalProcessRoutingService.java");
        Path messages = Path.of("src/main/java/com/tcc/pjb/backend/core/processual/routing/NationalProcessRoutingMessages.java");
        String serviceSource = Files.readString(service);
        String messagesSource = Files.readString(messages);
        List<String> literals = List.of(
                "Valor da causa acima do teto parametrizado do juizado; revisar competência ou rito.",
                "Pedido liminar declarado; revisar competência funcional, urgência, sigilo e fila prioritária antes do sorteio.",
                "Contexto de resolução: ",
                "Perfil sugerido de mesa/unidade: "
        );

        for (String literal : literals) {
            assertFalse(serviceSource.contains(literal), "Literal operacional ainda espalhado no serviço principal: " + literal);
            assertTrue(messagesSource.contains(literal), "Literal operacional deve permanecer no catálogo canônico: " + literal);
        }
    }
}
