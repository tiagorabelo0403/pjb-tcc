package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorAuthMode;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileReport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfValidationResult;
import com.tcc.pjb.backend.service.processual.recursal.protocolo.RecursalProtocolArtifactReadinessService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RecursalProtocolArtifactReadinessServiceTest {

    private final RecursalProtocolArtifactReadinessService service = new RecursalProtocolArtifactReadinessService();

    @Test
    void shouldDowngradeProductionReadinessWhenOnlyPadesTIsAvailableAndCertificateIsRequired() {
        RecursalPdfArtifact artifact = new RecursalPdfArtifact(
                new byte[]{1, 2, 3},
                "recurso.pdf",
                "application/pdf",
                "abc",
                1,
                Map.of(
                        "padesProfileCandidate", "PADES_T_EVIDENCE_CANDIDATE",
                        "nativePdfSignatureEmbedded", true,
                        "documentTimestampEmbedded", true,
                        "dssMaterialized", false,
                        "vriMaterialized", false,
                        "revocationMaterialized", false
                )
        );
        RecursalPdfValidationResult validation = RecursalPdfValidationResult.valid("VALID_PROTOCOL_ARTIFACT", List.of(), Map.of());
        JudicialConnectorOperationalProfileReport operationalProfile = operationalProfile(true, true);

        var readiness = service.assess(artifact, validation, operationalProfile, true);

        assertThat(readiness.status()).isEqualTo("REVIEW_LONG_TERM_EVIDENCE_BEFORE_PRODUCTION");
        assertThat(readiness.connectorSubmissionReady()).isTrue();
        assertThat(readiness.productionEvidenceReady()).isFalse();
        assertThat(readiness.readyForProduction()).isFalse();
        assertThat(readiness.reasons()).contains("LONG_TERM_PADES_PROFILE_REQUIRED", "DSS_MATERIALIZATION_REQUIRED", "VRI_MATERIALIZATION_REQUIRED", "CERTIFICATE_VALIDATION_REQUIRED_FOR_PRODUCTION");
    }

    @Test
    void shouldAllowProductionWhenLtEvidenceIsMaterialized() {
        RecursalPdfArtifact artifact = new RecursalPdfArtifact(
                new byte[]{1, 2, 3},
                "recurso.pdf",
                "application/pdf",
                "abc",
                1,
                Map.of(
                        "padesProfileCandidate", "PADES_LT_EVIDENCE_CANDIDATE",
                        "nativePdfSignatureEmbedded", true,
                        "documentTimestampEmbedded", true,
                        "dssMaterialized", true,
                        "vriMaterialized", true,
                        "revocationMaterialized", false,
                        "longTermValidationBundle", Map.of(
                                "profileAchieved", "PADES_LT_EVIDENCE_CANDIDATE",
                                "certificateValidationAvailable", true,
                                "certificateValidationPassed", true
                        )
                )
        );
        RecursalPdfValidationResult validation = RecursalPdfValidationResult.valid("VALID_PROTOCOL_ARTIFACT", List.of(), Map.of());
        JudicialConnectorOperationalProfileReport operationalProfile = operationalProfile(true, true);

        var readiness = service.assess(artifact, validation, operationalProfile, true);

        assertThat(readiness.status()).isEqualTo("READY_FOR_REAL_CONNECTOR_SUBMISSION");
        assertThat(readiness.connectorSubmissionReady()).isTrue();
        assertThat(readiness.productionEvidenceReady()).isTrue();
        assertThat(readiness.readyForProduction()).isTrue();
    }

    @Test
    void shouldRequireCertificateValidationForProductionWhenCertificateIsRequired() {
        RecursalPdfArtifact artifact = new RecursalPdfArtifact(
                new byte[]{1, 2, 3},
                "recurso.pdf",
                "application/pdf",
                "abc",
                1,
                Map.of(
                        "padesProfileCandidate", "PADES_LT_EVIDENCE_CANDIDATE",
                        "nativePdfSignatureEmbedded", true,
                        "documentTimestampEmbedded", true,
                        "dssMaterialized", true,
                        "vriMaterialized", true,
                        "revocationMaterialized", false,
                        "longTermValidationBundle", Map.of(
                                "profileAchieved", "PADES_LT_EVIDENCE_CANDIDATE",
                                "certificateValidationAvailable", false,
                                "certificateValidationPassed", false
                        )
                )
        );
        RecursalPdfValidationResult validation = RecursalPdfValidationResult.valid("VALID_PROTOCOL_ARTIFACT", List.of(), Map.of());
        JudicialConnectorOperationalProfileReport operationalProfile = operationalProfile(true, true);

        var readiness = service.assess(artifact, validation, operationalProfile, true);

        assertThat(readiness.productionEvidenceReady()).isFalse();
        assertThat(readiness.readyForProduction()).isFalse();
        assertThat(readiness.reasons()).contains("CERTIFICATE_VALIDATION_REQUIRED_FOR_PRODUCTION");
    }

    @Test
    void shouldRequireRevocationEvidenceWhenProfileIsLta() {
        RecursalPdfArtifact artifact = new RecursalPdfArtifact(
                new byte[]{1, 2, 3},
                "recurso.pdf",
                "application/pdf",
                "abc",
                1,
                Map.of(
                        "padesProfileCandidate", "PADES_LTA_EVIDENCE_CANDIDATE",
                        "nativePdfSignatureEmbedded", true,
                        "documentTimestampEmbedded", true,
                        "dssMaterialized", true,
                        "vriMaterialized", true,
                        "revocationMaterialized", false,
                        "longTermValidationBundle", Map.of(
                                "profileAchieved", "PADES_LTA_EVIDENCE_CANDIDATE",
                                "certificateValidationAvailable", true,
                                "certificateValidationPassed", true
                        )
                )
        );
        RecursalPdfValidationResult validation = RecursalPdfValidationResult.valid("VALID_PROTOCOL_ARTIFACT", List.of(), Map.of());
        JudicialConnectorOperationalProfileReport operationalProfile = operationalProfile(true, true);

        var readiness = service.assess(artifact, validation, operationalProfile, true);

        assertThat(readiness.productionEvidenceReady()).isFalse();
        assertThat(readiness.readyForProduction()).isFalse();
        assertThat(readiness.reasons()).contains("REVOCATION_EVIDENCE_REQUIRED_FOR_LTA");
    }

    private JudicialConnectorOperationalProfileReport operationalProfile(boolean readyForSubmission, boolean readyForProduction) {
        return new JudicialConnectorOperationalProfileReport(
                Instant.now(),
                JudicialSystem.PJE,
                "TJCE",
                readyForSubmission,
                readyForProduction,
                true,
                true,
                true,
                JudicialConnectorAuthMode.NONE,
                null,
                null,
                List.of(),
                List.of(),
                Map.of()
        );
    }
}
