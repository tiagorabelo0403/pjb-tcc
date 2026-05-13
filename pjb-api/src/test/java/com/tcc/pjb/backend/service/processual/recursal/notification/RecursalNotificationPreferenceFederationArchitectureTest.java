package com.tcc.pjb.backend.service.processual.recursal.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalNotificationPreferenceFederationArchitectureTest {

    @Test
    void deveManterPreferenciasFederadasNosPacotesCorretos() {
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationPreferencePolicyRequest.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationPreferencePolicyResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationFederatedDeliveryResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/notification/RecursalNotificationPreferenceFederationService.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/notification/RecursalNotificationPreferenceFederationController.java"))).isTrue();
    }
}
