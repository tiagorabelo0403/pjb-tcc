package com.tcc.pjb.backend.core.icp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult;
import com.tcc.pjb.backend.model.repository.IcpCertificateCacheRepository;
import com.tcc.pjb.backend.model.repository.IcpSignatureEventRepository;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IcpBrasilChainValidatorSnapshotHelpersTest {

    @Test
    void shouldExposeHelperSnapshotsWithoutValidationFlow() {
        IcpSignatureEventRepository signatureEventRepository = mock(IcpSignatureEventRepository.class);
        when(signatureEventRepository.findTop20ByDocHashOrderBySignedAtAsc(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        IcpBrasilTrustAnchorLoader trustAnchorLoader = mock(IcpBrasilTrustAnchorLoader.class);
        when(trustAnchorLoader.exists("AC-TESTE")).thenReturn(true);
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(
                mock(IcpCertificateCacheRepository.class),
                signatureEventRepository,
                command -> new IcpBrasilOcspResult(false, Instant.parse("2026-04-11T12:00:00Z")),
                trustAnchorLoader,
                new IcpBrasilSignatureProperties(false, false, List.of(), null, 1440, 1440, null, null, null, null, false),
                mock(AuditLedgerService.class));

        var details = validator.detailsFor("AC-TESTE", "sem-anchor");
        var trustAnchor = validator.trustAnchorSnapshot("alias-1", "AC-TESTE", true);
        var policy = validator.policySnapshot();
        var crl = validator.crlSnapshot("http://ac.example/crl");
        var signer = validator.signerSelectionSnapshot("pkcs12", "alias-a", true, false);
        var signerHealth = validator.signerHealthSnapshot("pkcs11", "alias-b", false, true);
        var ocspHealth = validator.ocspHealthSnapshot();
        var trustAnchorResult = validator.trustAnchorResult(new com.tcc.pjb.backend.core.icp.domain.IcpBrasilTrustAnchorQuery("AC-TESTE"));

        assertThat(details.acSigla()).isEqualTo("AC-TESTE");
        assertThat(trustAnchor.alias()).isEqualTo("alias-1");
        assertThat(policy.enforceChainValidation()).isFalse();
        assertThat(crl.distributionPoint()).contains("crl");
        assertThat(signer.keyAlias()).isEqualTo("alias-a");
        assertThat(signerHealth.pkcs11()).isTrue();
        assertThat(ocspHealth.enabled()).isTrue();
        assertThat(trustAnchorResult.acSigla()).isEqualTo("AC-TESTE");
    }

    @Test
    void shouldExposeCertificateViewsForNullAndNonNullCertificates() {
        IcpSignatureEventRepository signatureEventRepository = mock(IcpSignatureEventRepository.class);
        when(signatureEventRepository.findTop20ByDocHashOrderBySignedAtAsc(org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(
                mock(IcpCertificateCacheRepository.class),
                signatureEventRepository,
                command -> new IcpBrasilOcspResult(false, Instant.now()),
                mock(IcpBrasilTrustAnchorLoader.class),
                new IcpBrasilSignatureProperties(false, false, List.of(), null, 1440, 1440, null, null, null, null, false),
                mock(AuditLedgerService.class));

        var audit = validator.audit((X509Certificate) null);
        var projection = validator.projection((X509Certificate) null);
        var chainAudit = validator.chainAudit((X509Certificate) null);
        var revocationAudit = validator.revocationAudit((X509Certificate) null);
        var timeline = validator.certificateTimeline((X509Certificate) null);

        assertThat(audit.validationOk()).isFalse();
        assertThat(projection.failureReason()).contains("certificado ausente");
        assertThat(chainAudit.failureReason()).contains("certificado ausente");
        assertThat(revocationAudit.revoked()).isFalse();
        assertThat(timeline).hasSize(1);
    }
}
