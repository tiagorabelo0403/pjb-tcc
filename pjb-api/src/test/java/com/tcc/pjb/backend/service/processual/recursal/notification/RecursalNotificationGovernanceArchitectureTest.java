package com.tcc.pjb.backend.service.processual.recursal.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalNotificationGovernanceArchitectureTest {

    @Test
    void deveManterSuiteNotificacionalRecursalNosPacotesCorretos() {
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalNotificationLabels.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationGovernanceRequest.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationMobilePreviewResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationGovernanceResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/notification/RecursalNotificationScienceResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/notification/RecursalNotificationGovernanceService.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/notification/RecursalNotificationGovernanceController.java"))).isTrue();
    }
}
