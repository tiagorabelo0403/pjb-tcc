package com.tcc.pjb.backend.core.icp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult;
import com.tcc.pjb.backend.core.icp.domain.IcpTrustAnchorHealthQuery;
import com.tcc.pjb.backend.core.icp.domain.IcpCertificateHealthQuery;
import com.tcc.pjb.backend.core.icp.domain.IcpValidationHealthQuery;
import com.tcc.pjb.backend.model.entity.icp.IcpCertificateCache;
import com.tcc.pjb.backend.model.entity.icp.IcpSignatureEvent;
import com.tcc.pjb.backend.model.repository.IcpCertificateCacheRepository;
import com.tcc.pjb.backend.model.repository.IcpSignatureEventRepository;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IcpBrasilChainValidatorHealthAndTimelineTest {

    @Test
    void shouldExposeHealthViewsAndSignatureTimeline() throws Exception {
        IcpCertificateCacheRepository certificateCacheRepository = mock(IcpCertificateCacheRepository.class);
        IcpSignatureEventRepository signatureEventRepository = mock(IcpSignatureEventRepository.class);
        IcpBrasilOcspVerifier ocspVerifier = mock(IcpBrasilOcspVerifier.class);
        IcpBrasilTrustAnchorLoader trustAnchorLoader = mock(IcpBrasilTrustAnchorLoader.class);
        when(trustAnchorLoader.countActiveAnchors()).thenReturn(1);
        IcpBrasilSignatureProperties properties = new IcpBrasilSignatureProperties(true, false, List.of("SERASA"), "icp:", 3600, 86400, null, null, null, "LTA", false);
        AuditLedgerService auditLedger = mock(AuditLedgerService.class);
        X509Certificate cert = SelfSignedCertificateFactory.generate("CN=Valid User, SERIALNUMBER=12345678901", "CN=Valid User, SERIALNUMBER=12345678901", Instant.now().minusSeconds(60), Instant.now().plusSeconds(3600));
        when(ocspVerifier.check(new IcpBrasilOcspCommand(cert))).thenReturn(new IcpBrasilOcspResult(false, null));
        IcpCertificateCache entity = IcpCertificateCache.builder()
                .issuerDn("issuer")
                .serialHex("serial")
                .acSigla("SERASA")
                .revoked(false)
                .revocationChecked(Instant.parse("2026-04-11T11:00:00Z"))
                .validUntil(Instant.parse("2026-12-31T00:00:00Z"))
                .build();
        when(certificateCacheRepository.findByIssuerDnAndSerialHex("issuer", "serial")).thenReturn(Optional.of(entity));
        when(signatureEventRepository.findTop20ByDocHashOrderBySignedAtAsc("doc-hash")).thenReturn(List.of(
                IcpSignatureEvent.builder().docHash("doc-hash").profileCandidate("LTA").profileAchieved("LTA").signedAt(Instant.parse("2026-04-11T11:10:00Z")).validationOk(true).build()
        ));
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(certificateCacheRepository, signatureEventRepository, ocspVerifier, trustAnchorLoader, properties, auditLedger);

        var validationHealth = validator.validationHealth(new IcpValidationHealthQuery(cert));
        var certificateHealth = validator.certificateHealthResult(new IcpCertificateHealthQuery("issuer", "serial"));
        var trustAnchorHealth = validator.trustAnchorHealth(new IcpTrustAnchorHealthQuery("anchor", "SERASA"));
        var timeline = validator.timeline(new com.tcc.pjb.backend.core.icp.domain.IcpBrasilTimelineQuery("doc-hash"));
        var signatureTimeline = validator.signatureTimeline(new com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignatureTimelineQuery("doc-hash"));
        var revocationAudit = validator.revocationAudit(cert);

        assertThat(validationHealth.healthy()).isTrue();
        assertThat(certificateHealth.health().acSigla()).isEqualTo("SERASA");
        assertThat(trustAnchorHealth.healthy()).isTrue();
        assertThat(timeline.entries()).hasSize(1);
        assertThat(signatureTimeline.entries()).isNotEmpty();
        assertThat(revocationAudit.source()).isEqualTo("OCSP");
    }
}
