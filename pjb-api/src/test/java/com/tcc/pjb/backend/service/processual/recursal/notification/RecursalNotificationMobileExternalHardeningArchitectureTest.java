package com.tcc.pjb.backend.service.processual.recursal.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalNotificationMobileExternalHardeningArchitectureTest {

    @Test
    void deveManterHardeningMobileNosPacotesCorretos() {
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationMobileHardeningRequest.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationMobilePostureResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationMobileExternalDeliveryResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/notification/RecursalNotificationMobileExternalHardeningService.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/notification/RecursalNotificationMobileExternalHardeningController.java"))).isTrue();
    }
}
