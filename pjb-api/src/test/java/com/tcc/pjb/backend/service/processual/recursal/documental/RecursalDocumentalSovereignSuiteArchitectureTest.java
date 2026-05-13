package com.tcc.pjb.backend.service.processual.recursal.documental;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RecursalDocumentalSovereignSuiteArchitectureTest {

    @Test
    void deveManterSuiteDocumentalSoberanaNosPacotesCorretos() {
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/core/processo/recursal/domain/foundation/RecursalDocumentalLabels.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/documental/RecursalDocumentalArtifactRequest.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/documental/RecursalDocumentViewerResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/documental/RecursalDocumentAuthenticityResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/model/dto/processual/recursal/documental/RecursalDocumentSignatureEvidenceResponse.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/service/processual/recursal/documental/RecursalDocumentalSovereignSuiteService.java"))).isTrue();
        assertThat(Files.exists(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/recursal/documental/RecursalDocumentalSovereignSuiteController.java"))).isTrue();
    }
}
