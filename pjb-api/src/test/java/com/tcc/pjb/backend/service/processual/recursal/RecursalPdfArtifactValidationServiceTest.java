package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.HsmTestFactory;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.icp.RecursalIcpBrasilIntegrationService;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreLoader;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfValidationResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfArtifactValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfProofEnvelopeService;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalTimestampAuthorityProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalTimestampAuthorityService;

class RecursalPdfArtifactValidationServiceTest {

    private RecursalPdfProofEnvelopeService proofEnvelopeService;
    private RecursalPdfArtifactValidationService validationService;

    @BeforeEach
    void setUp() {
        AuditLedgerService auditLedgerService = new AuditLedgerService();
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
        RecursalTimestampAuthorityService timestampAuthorityService = new RecursalTimestampAuthorityService(
                hsm,
                auditLedgerService,
                org.mockito.Mockito.mock(JudicialKeyStoreLoader.class),
                new RecursalTimestampAuthorityProperties(true, true, null, null, null, null, null, null)
        );
        proofEnvelopeService = new RecursalPdfProofEnvelopeService(hsm, auditLedgerService, timestampAuthorityService, org.mockito.Mockito.mock(RecursalIcpBrasilIntegrationService.class));
        validationService = new RecursalPdfArtifactValidationService(auditLedgerService);
    }

    @Test
    void validateShouldAcceptDetachedEnvelopeWithWarningsWhenCertificateIsNotRequired() {
        RecursalPdfArtifact artifact = new RecursalPdfArtifact(
                "pdf-teste".getBytes(),
                "peca.pdf",
                "application/pdf",
                com.tcc.pjb.backend.core.util.Hashes.sha256Hex("pdf-teste".getBytes()),
                1,
                Map.of()
        );
        Processo processo = Processo.builder().id(10L).numeroProcesso("0001").build();
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setCpf("12345678900");
        RecursalPdfArtifact sealed = proofEnvelopeService.seal(processo, usuario, LegalAppealType.APELACAO, artifact, Map.of("signatureMode", "ASSINATURA_CONTROLADA"), Map.of());

        RecursalPdfValidationResult validation = validationService.validate(sealed, false);

        assertThat(validation.valid()).isTrue();
        assertThat(validation.warnings()).contains("MOCK_HSM_SIGNATURE_ACTIVE", "TIMESTAMP_WITHOUT_EXTERNAL_ACT");
    }

    @Test
    void validateShouldBlockProtocolWhenRealCertificateIsRequiredAndSignatureIsMocked() {
        RecursalPdfArtifact artifact = new RecursalPdfArtifact(
                "pdf-teste".getBytes(),
                "peca.pdf",
                "application/pdf",
                com.tcc.pjb.backend.core.util.Hashes.sha256Hex("pdf-teste".getBytes()),
                1,
                Map.of()
        );
        Processo processo = Processo.builder().id(10L).numeroProcesso("0001").build();
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setCpf("12345678900");
        RecursalPdfArtifact sealed = proofEnvelopeService.seal(processo, usuario, LegalAppealType.RESP, artifact, Map.of("signatureMode", "CERTIFICADO_OU_CREDENCIAL_REFORCADA"), Map.of());

        RecursalPdfValidationResult validation = validationService.validate(sealed, true);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.errors()).contains("REAL_CERTIFICATE_SIGNATURE_REQUIRED");
    }

    @Test
    void validateShouldWarnWhenDssCertificateChainIsEmpty() throws Exception {
        byte[] pdf = pdfWithEmptyDssCertificateChain();
        String sha256 = com.tcc.pjb.backend.core.util.Hashes.sha256Hex(pdf);
        LinkedHashMap<String, Object> proofEnvelope = new LinkedHashMap<>();
        proofEnvelope.put("envelopeId", "env-1");
        proofEnvelope.put("proofMode", "DETACHED");
        proofEnvelope.put("documentSha256", sha256);
        proofEnvelope.put("signatureDigestSha256", "sig-digest");
        proofEnvelope.put("signatureBase64", "c2ln");
        proofEnvelope.put("timestampTokenSha256", "ts-digest");
        proofEnvelope.put("timestampTokenBase64", "dHM=");
        proofEnvelope.put("signatureMocked", false);
        proofEnvelope.put("timestampExternalAuthority", true);

        LinkedHashMap<String, Object> longTermValidationBundle = new LinkedHashMap<>();
        longTermValidationBundle.put("profileAchieved", "PADES_BASIC");
        longTermValidationBundle.put("profileRequested", "PADES_LT");
        longTermValidationBundle.put("certificateValidationAvailable", false);
        longTermValidationBundle.put("certificateValidationPassed", false);
        longTermValidationBundle.put("documentTimestampStatus", "EMBEDDED");
        longTermValidationBundle.put("documentTimestampEmbedded", true);
        longTermValidationBundle.put("archiveTimestampTokenSha256", "archive-ts");

        RecursalPdfArtifact artifact = new RecursalPdfArtifact(
                pdf,
                "peca-dss.pdf",
                "application/pdf",
                sha256,
                1,
                Map.of(
                        "proofEnvelope", proofEnvelope,
                        "longTermValidationBundle", longTermValidationBundle
                )
        );

        RecursalPdfValidationResult validation = validationService.validate(artifact, false);

        assertThat(validation.warnings()).contains("PDF_DSS_CERTIFICATE_CHAIN_EMPTY");
        assertThat(validation.details()).containsEntry("pdfDssPresent", true);
        assertThat(validation.details()).containsEntry("pdfDssCertCount", 0);
    }

    private byte[] pdfWithEmptyDssCertificateChain() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            COSDictionary dss = new COSDictionary();
            dss.setItem(COSName.getPDFName("Certs"), new COSArray());
            document.getDocumentCatalog().getCOSObject().setItem(COSName.getPDFName("DSS"), dss);
            document.save(output);
            return output.toByteArray();
        }
    }

}
