package com.tcc.pjb.backend.service.processual.recursal.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalGuidedWorkspaceStructureRefinementArchitectureTest {

    @Test
    void deveManterBlueprintsEFactoriesGuiadasNoEixoRecursal() {
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalGuidedPieceBlueprint.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalGuidedPieceTrackFactory.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalTribunalTrackFactory.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalJurisdictionPanelBlueprint.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/workspace/RecursalPanelHandoffTrackFactory.java"))).isTrue();
    }
}
