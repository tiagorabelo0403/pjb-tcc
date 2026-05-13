package com.tcc.pjb.backend.architecture;

import static com.tcc.pjb.backend.architecture.ArquiteturaSourceScanSupport.scanJavaSourcesUnder;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbInstitutionalSupportPackageOrganizationArchTest {

    private static final Path MAIN_JAVA = ArquiteturaSourceScanSupport.moduleSourceRoot("src", "main", "java", "com", "tcc", "pjb", "backend");

    @Test
    void institutionalSupportMustBePartitionedByPanelOperationsAndLane() throws Exception {
        List<Path> files = scanJavaSourcesUnder(MAIN_JAVA);

        assertThat(files)
                .anyMatch(path -> path.endsWith(Path.of("controller", "institutional", "support", "panel", "InstitutionalSupportPanelController.java")))
                .anyMatch(path -> path.endsWith(Path.of("service", "institutional", "support", "panel", "InstitutionalSupportPanelService.java")))
                .anyMatch(path -> path.endsWith(Path.of("service", "institutional", "support", "operations", "InstitutionalSupportOperationsService.java")))
                .anyMatch(path -> path.endsWith(Path.of("service", "institutional", "support", "lane", "InstitutionalSupportLaneResolver.java")))
                .anyMatch(path -> path.endsWith(Path.of("model", "dto", "institutional", "support", "panel", "InstitutionalSupportPanelSnapshotResponse.java")))
                .anyMatch(path -> path.endsWith(Path.of("model", "dto", "institutional", "support", "operations", "InstitutionalSupportCompetenceSnapshotResponse.java")));
    }

    @Test
    void legacyInstitutionalSupportFlatPackagesMustBeEmpty() throws Exception {
        List<Path> files = scanJavaSourcesUnder(MAIN_JAVA);

        assertThat(files)
                .noneMatch(path -> path.endsWith(Path.of("controller", "institutional", "support", "InstitutionalSupportPanelController.java")))
                .noneMatch(path -> path.endsWith(Path.of("service", "institutional", "support", "InstitutionalSupportPanelService.java")))
                .noneMatch(path -> path.endsWith(Path.of("service", "institutional", "support", "InstitutionalSupportOperationsService.java")))
                .noneMatch(path -> path.endsWith(Path.of("service", "institutional", "support", "InstitutionalSupportLaneResolver.java")))
                .noneMatch(path -> path.endsWith(Path.of("model", "dto", "institutional", "support", "InstitutionalSupportPanelSnapshotResponse.java")))
                .noneMatch(path -> path.endsWith(Path.of("model", "dto", "institutional", "support", "InstitutionalSupportCompetenceSnapshotResponse.java")));
    }
}
