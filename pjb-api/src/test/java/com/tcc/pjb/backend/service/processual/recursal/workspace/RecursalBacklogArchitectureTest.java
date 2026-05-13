package com.tcc.pjb.backend.service.processual.recursal.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalBacklogArchitectureTest {

    @Test
    void deveManterNovasTrilhasRecursaisDoBacklogMaterializadasNosPacotesCorretos() {
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalDigitalCasefileBlueprint.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalAttorneyAssociationBlueprint.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalOfficeCollaborationBlueprint.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalMediaCollaborationBlueprint.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalAnalyticsIntelligenceBlueprint.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalDigitalCasefileTrackFactory.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalAttorneyAssociationTrackFactory.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalOfficeCollaborationTrackFactory.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalMediaCollaborationTrackFactory.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalAnalyticsIntelligenceTrackFactory.java"))).isTrue();
    }
}
