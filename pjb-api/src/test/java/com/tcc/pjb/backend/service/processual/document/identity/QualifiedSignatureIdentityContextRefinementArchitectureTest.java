package com.tcc.pjb.backend.service.processual.document.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class QualifiedSignatureIdentityContextRefinementArchitectureTest {

    private static final Path SERVICE_PATH = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/document/identity/QualifiedSignatureIdentityContextService.java");

    @Test
    void qualifiedSignatureIdentityContextServiceMustStayBelowServiceHotspotThreshold() throws IOException {
        long lineCount = Files.lines(SERVICE_PATH, StandardCharsets.UTF_8).count();

        assertThat(lineCount).isLessThan(900);
    }

    @Test
    void identityContextMustKeepInstitutionalCertificateAndPersonSupportsExtracted() throws IOException {
        String source = Files.readString(SERVICE_PATH, StandardCharsets.UTF_8);

        assertThat(source).contains("QualifiedSignatureInstitutionalAssignmentSupport");
        assertThat(source).contains("QualifiedSignatureCertificateContextSupport");
        assertThat(source).contains("QualifiedSignaturePersonIdentitySupport");
        assertThat(source).doesNotContain("private SpecializedSecretariatProfile resolveSpecializedSecretariatProfile(");
        assertThat(source).doesNotContain("private void inferRoleHints(");
        assertThat(source).doesNotContain("private boolean isNameCoherent(");
    }
}
