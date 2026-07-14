package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.HsmTestFactory;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.icp.RecursalIcpBrasilIntegrationService;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.security.JudicialCertificateValidationReport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCertificateValidationService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreLoader;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfExportService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfLongTermValidationProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfLongTermValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfNativeSignatureService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfProofEnvelopeService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalNativePdfSignatureProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalTimestampAuthorityProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalTimestampAuthorityService;

class RecursalPdfLongTermValidationServiceTest {

    @Test
    void finalizeEvidenceShouldPromoteArtifactToLtaCandidateWhenCertificateValidationPasses() {
        AuditLedgerService auditLedgerService = new AuditLedgerService(mock(AuditLedgerRepository.class), mock(CurrentUserService.class));
        PjbHardwareSecurityModule hsm = HsmTestFactory.forTest(new PjbHsmProperties(
                false,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                0,
                false
        ));
        JudicialConnectorCertificateValidationService certificateValidationService = mock(JudicialConnectorCertificateValidationService.class);
        when(certificateValidationService.validate(any(), any(), any(), anyMap()))
                .thenReturn(new JudicialCertificateValidationReport(
                        Instant.now(),
                        JudicialSystem.PJE,
                        "TJCE",
                        "recursal-pdf-signature",
                        "recursal-pdf",
                        "connector-keystore",
                        "connector-truststore",
                        "recursal",
                        true,
                        false,
                        true,
                        false,
                        false,
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        "VALID",
                        Instant.now().minusSeconds(60),
                        Instant.now().plusSeconds(86400),
                        Duration.ofHours(24),
                        2,
                        "CN=Ana Paula Lima",
                        "CN=PJB CA",
                        "01",
                        "ABCD",
                        List.of(),
                        List.of(),
                        Map.of()
                ));
        JudicialKeyStoreLoader keyStoreLoader = mock(JudicialKeyStoreLoader.class);
        RecursalTimestampAuthorityService timestampAuthorityService = new RecursalTimestampAuthorityService(
                hsm,
                auditLedgerService,
                keyStoreLoader,
                new RecursalTimestampAuthorityProperties(true, true, null, null, null, null, null, null)
        );
        RecursalPdfLongTermValidationService longTermValidationService = new RecursalPdfLongTermValidationService(
                auditLedgerService,
                new RecursalPdfLongTermValidationProperties(true, true, true, null, null, true, true, true, false),
                timestampAuthorityService,
                certificateValidationService,
                keyStoreLoader
        );
        RecursalPdfExportService exportService = new RecursalPdfExportService();
        RecursalPdfNativeSignatureService nativeSignatureService = new RecursalPdfNativeSignatureService(
                hsm,
                auditLedgerService,
                keyStoreLoader,
                new RecursalNativePdfSignatureProperties(true, null, null, null, null, null, null)
        );
        RecursalPdfProofEnvelopeService proofEnvelopeService = new RecursalPdfProofEnvelopeService(hsm, auditLedgerService, timestampAuthorityService, org.mockito.Mockito.mock(RecursalIcpBrasilIntegrationService.class));

        Processo processo = Processo.builder().id(91L).numeroProcesso("0001").numeroUnificado("0001").tribunal("TJCE").build();
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setNome("Ana Paula Lima");
        RecursalPdfArtifact artifact = exportService.export(
                processo,
                usuario,
                LegalAppealType.APELACAO,
                Map.of("titulo", "Apelação", "conteudoMinuta", "Razões recursais consistentes.", "revisionHash", "REV-1"),
                Map.of("signatureMode", "CERTIFICADO_OU_CREDENCIAL_REFORCADA", "keyStoreRef", "connector-keystore", "trustStoreRef", "connector-truststore", "keyAlias", "recursal"),
                Map.of("nivelRecomendado", "SIGILO_N2", "timestampExternalAuthority", true)
        );
        artifact = nativeSignatureService.applyNativeSignature(
                processo,
                usuario,
                LegalAppealType.APELACAO,
                artifact,
                Map.of("signatureMode", "CERTIFICADO_OU_CREDENCIAL_REFORCADA"),
                Map.of("nivelRecomendado", "SIGILO_N2", "timestampExternalAuthority", true)
        );
        artifact = longTermValidationService.prepare(
                processo,
                LegalAppealType.APELACAO,
                artifact,
                Map.of("keyStoreRef", "connector-keystore", "trustStoreRef", "connector-truststore", "keyAlias", "recursal"),
                Map.of("nivelRecomendado", "SIGILO_N2", "timestampExternalAuthority", true)
        );
        artifact = proofEnvelopeService.seal(
                processo,
                usuario,
                LegalAppealType.APELACAO,
                artifact,
                Map.of("signatureMode", "CERTIFICADO_OU_CREDENCIAL_REFORCADA"),
                Map.of("nivelRecomendado", "SIGILO_N2", "timestampExternalAuthority", true)
        );
        artifact = longTermValidationService.finalizeEvidence(
                processo,
                LegalAppealType.APELACAO,
                artifact,
                Map.of("keyStoreRef", "connector-keystore", "trustStoreRef", "connector-truststore", "keyAlias", "recursal"),
                Map.of("nivelRecomendado", "SIGILO_N2", "timestampExternalAuthority", true)
        );

        assertThat(artifact.metadata()).containsEntry("documentTimestampEmbedded", true);
        assertThat(artifact.metadata()).containsKeys("padesProfileCandidate", "longTermValidationBundle", "archiveTimestampTokenSha256", "certificateValidationStatus", "dssMaterializationStatus");
        assertThat(String.valueOf(artifact.metadata().get("padesProfileCandidate"))).isNotBlank();
    }
}
