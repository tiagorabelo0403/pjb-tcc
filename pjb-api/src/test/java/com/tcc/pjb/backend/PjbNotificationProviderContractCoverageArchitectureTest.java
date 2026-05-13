package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbNotificationProviderContractCoverageArchitectureTest {

    @Test
    void pactDeNotificacaoDeveCobrirPreferenciasEDispatchMulticanal() throws IOException {
        String pact = Files.readString(
                Path.of("src/test/resources/pacts/provider/PjbNotificationConsumer-PjbNotificationProvider.json"),
                StandardCharsets.UTF_8
        );

        assertThat(pact)
                .contains("/api/v1/notificacoes/preferencias/usuarios/77")
                .contains("/api/v1/notificacoes/multicanal/processos/501/usuarios/77");
    }
}
