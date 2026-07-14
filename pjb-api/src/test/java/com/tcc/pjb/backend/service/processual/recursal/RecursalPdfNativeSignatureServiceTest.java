package com.tcc.pjb.backend.service.processual.recursal;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.HsmTestFactory;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHardwareSecurityModule;
import com.tcc.pjb.backend.core.comunicacao.judicial.hsm.PjbHsmProperties;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.integration.judicial.security.JudicialKeyStoreLoader;
import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalTimestampAuthorityService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalTimestampAuthorityProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfLongTermValidationService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfLongTermValidationProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfExportService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfNativeSignatureService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalNativePdfSignatureProperties;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfProofEnvelopeService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfExportService;
import com.tcc.pjb.backend.service.processual.recursal.pdf.RecursalPdfNativeSignatureService;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RecursalPdfNativeSignatureServiceTest {

    private RecursalPdfExportService exportService;
    private RecursalPdfNativeSignatureService nativeSignatureService;

    @BeforeEach
    void setUp() {
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
        exportService = new RecursalPdfExportService();
        nativeSignatureService = new RecursalPdfNativeSignatureService(
                hsm,
                new AuditLedgerService(Mockito.mock(AuditLedgerRepository.class), Mockito.mock(CurrentUserService.class)),
                Mockito.mock(JudicialKeyStoreLoader.class),
                new RecursalNativePdfSignatureProperties(true, null, null, null, null, null, null)
        );
    }

    @Test
    void applyNativeSignatureShouldEmbedCmsSignatureInMockMode() throws Exception {
        Processo processo = Processo.builder().id(9L).numeroProcesso("0001").numeroUnificado("0001").tribunal("TJCE").build();
        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setNome("Ana Paula Lima");
        RecursalPdfArtifact artifact = exportService.export(
                processo,
                usuario,
                LegalAppealType.APELACAO,
                Map.of("titulo", "Apelação", "conteudoMinuta", "Razões recursais suficientes.", "revisionHash", "REV-1"),
                Map.of("signatureMode", "ASSINATURA_CONTROLADA"),
                Map.of("nivelRecomendado", "PUBLICO")
        );

        RecursalPdfArtifact signed = nativeSignatureService.applyNativeSignature(
                processo,
                usuario,
                LegalAppealType.APELACAO,
                artifact,
                Map.of("signatureMode", "ASSINATURA_CONTROLADA"),
                Map.of("nivelRecomendado", "PUBLICO")
        );

        assertThat(signed.available()).isTrue();
        assertThat(signed.sha256()).isNotEqualTo(artifact.sha256());
        assertThat(signed.metadata()).containsEntry("nativePdfSignatureEmbedded", true);
        assertThat(signed.metadata()).containsEntry("nativePdfSignatureMocked", true);
        assertThat(signed.metadata()).containsEntry("nativePdfSignatureStatus", "EMBEDDED_NATIVE_CMS_SIGNATURE_MOCK");
        assertThat(Hashes.sha256Hex(signed.bytes())).isEqualTo(signed.sha256());
        try (PDDocument document = Loader.loadPDF(signed.bytes())) {
            assertThat(document.getSignatureDictionaries()).hasSize(1);
        }
    }
}
