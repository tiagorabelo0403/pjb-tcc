package com.tcc.pjb.backend.core.icp;

import com.tcc.pjb.backend.model.dto.processual.recursal.pdf.RecursalPdfArtifact;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.IcpCertificateCacheRepository;
import com.tcc.pjb.backend.model.repository.IcpSignatureEventRepository;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecursalIcpBrasilIntegrationServiceTest {

    @Test
    void deveEnriquecerArtifactQuandoCertificadoValido() throws Exception {
        X509Certificate certificate = SelfSignedCertificateFactory.generate("CN=Pessoa A3,SERIALNUMBER=CPF12345678901", "CN=AC-JUS", Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));
        IcpCertificateCacheRepository cacheRepository = mock(IcpCertificateCacheRepository.class);
        IcpSignatureEventRepository eventRepository = mock(IcpSignatureEventRepository.class);
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(
                cacheRepository,
                eventRepository,
                cert -> com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult.good(),
                new IcpBrasilSignatureProperties(true, true, List.of("AC-JUS"), "pjb:icp:ocsp:", 3600, 86400, null, null, null, "LTA", false),
                mock(com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService.class)
        );
        IcpBrasilSignerCertificateResolver resolver = mock(IcpBrasilSignerCertificateResolver.class);
        when(resolver.resolveRecursalCertificate()).thenReturn(certificate);
        RecursalIcpBrasilIntegrationService service = new RecursalIcpBrasilIntegrationService(
                new IcpBrasilSignatureProperties(true, true, List.of("AC-JUS"), "pjb:icp:ocsp:", 3600, 86400, null, null, null, "LTA", false),
                resolver,
                validator,
                new RecursalIcpBrasilMetadataAssembler()
        );
        RecursalPdfArtifact artifact = new RecursalPdfArtifact(new byte[]{1,2,3}, "peca.pdf", "application/pdf", "abc", 1, Map.of("dss", Map.of("present", true)));
        RecursalPdfArtifact out = service.apply(new Processo(), new Usuario(), artifact, Map.of(), Map.of());
        assertThat(out.metadata()).containsKey("icpBrasil");
        @SuppressWarnings("unchecked") Map<String, Object> icp = (Map<String, Object>) out.metadata().get("icpBrasil");
        assertThat(icp).containsEntry("validationOk", true);
        verify(eventRepository).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveMarcarSkipQuandoNaoHaCertificadoEFluxoNaoExigeBloqueio() {
        RecursalIcpBrasilIntegrationService service = new RecursalIcpBrasilIntegrationService(
                new IcpBrasilSignatureProperties(true, false, List.of("AC-JUS"), "pjb:icp:ocsp:", 3600, 86400, null, null, null, "LTA", false),
                certificateResolverNull(),
                mock(IcpBrasilChainValidator.class),
                new RecursalIcpBrasilMetadataAssembler()
        );
        RecursalPdfArtifact artifact = new RecursalPdfArtifact("pdf".getBytes(StandardCharsets.UTF_8), "peca.pdf", "application/pdf", "abc", 1, Map.of());
        RecursalPdfArtifact out = service.apply(null, null, artifact, Map.of(), Map.of());
        @SuppressWarnings("unchecked") Map<String, Object> icp = (Map<String, Object>) out.metadata().get("icpBrasil");
        assertThat(icp).containsEntry("mode", "SKIPPED_NO_CERTIFICATE");
    }

    private static IcpBrasilSignerCertificateResolver certificateResolverNull() {
        IcpBrasilSignerCertificateResolver resolver = mock(IcpBrasilSignerCertificateResolver.class);
        when(resolver.resolveRecursalCertificate()).thenReturn(null);
        return resolver;
    }
}
