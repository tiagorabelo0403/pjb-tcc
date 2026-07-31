package com.tcc.pjb.backend.core.icp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerEntry;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.repository.IcpCertificateCacheRepository;
import com.tcc.pjb.backend.model.repository.IcpSignatureEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigInteger;
import java.security.Principal;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.security.auth.x500.X500Principal;
import org.junit.jupiter.api.Test;

class IcpBrasilChainValidatorTest {

    @Test
    void deveValidarCertificadoAceito() {
        IcpCertificateCacheRepository cacheRepository = mock(IcpCertificateCacheRepository.class);
        IcpSignatureEventRepository eventRepository = mock(IcpSignatureEventRepository.class);
        when(cacheRepository.findByIssuerDnAndSerialHex(any(), any())).thenReturn(java.util.Optional.empty());
        when(cacheRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(
                cacheRepository,
                eventRepository,
                certificate -> com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult.good(),
                new IcpBrasilSignatureProperties(true, true, List.of("AC-JUS"), "pjb:icp:ocsp:", 3600, 86400, null, null, null, "LTA", false),
                mock(AuditLedgerService.class)
        );
        var result = validator.validate(new FakeCertificate());
        assertThat(result.valid()).isTrue();
        assertThat(result.profile()).isNotNull();
        assertThat(result.profile().acSigla()).containsIgnoringCase("AC-JUS");
    }

    @Test
    void eventoIcpChainOkNaoContemCpfNoPayload() {
        IcpCertificateCacheRepository cacheRepository = mock(IcpCertificateCacheRepository.class);
        IcpSignatureEventRepository eventRepository = mock(IcpSignatureEventRepository.class);
        when(cacheRepository.findByIssuerDnAndSerialHex(any(), any())).thenReturn(java.util.Optional.empty());
        when(cacheRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        AuditLedgerService auditLedger = new AuditLedgerService(mock(AuditLedgerRepository.class), mock(CurrentUserService.class), new SimpleMeterRegistry());
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(
                cacheRepository,
                eventRepository,
                certificate -> com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult.good(),
                new IcpBrasilSignatureProperties(true, true, List.of("AC-JUS"), "pjb:icp:ocsp:", 3600, 86400, null, null, null, "LTA", false),
                auditLedger
        );
        validator.validate(new FakeCertificate());
        java.util.List<AuditLedgerEntry> icpOkEntries = auditLedger.entries().stream()
                .filter(e -> "ICP_CHAIN_OK".equals(e.getAction()))
                .toList();
        assertThat(icpOkEntries).hasSize(1);
        AuditLedgerEntry entry = icpOkEntries.get(0);
        assertThat(entry.getPayloadHash()).doesNotContain("cpf=");
        assertThat(entry.getPayloadHash()).doesNotContain("12345678901");
        assertThat(entry.getPayloadHash()).contains("tipo=A1");
        assertThat(entry.getResourceId()).isEqualTo("75BCD15");
    }

    static final class FakeCertificate extends X509Certificate {
        @Override public void checkValidity() {}
        @Override public void checkValidity(Date date) {}
        @Override public int getVersion() { return 3; }
        @Override public BigInteger getSerialNumber() { return BigInteger.valueOf(123456789L); }
        @Override public Principal getIssuerDN() { return getIssuerX500Principal(); }
        @Override public Principal getSubjectDN() { return getSubjectX500Principal(); }
        @Override public Date getNotBefore() { return new Date(System.currentTimeMillis() - 1000L); }
        @Override public Date getNotAfter() { return new Date(System.currentTimeMillis() + 86400000L); }
        @Override public byte[] getTBSCertificate() { return new byte[0]; }
        @Override public byte[] getSignature() { return new byte[0]; }
        @Override public String getSigAlgName() { return "SHA256withRSA"; }
        @Override public String getSigAlgOID() { return "1.2.840.113549.1.1.11"; }
        @Override public byte[] getSigAlgParams() { return new byte[0]; }
        @Override public boolean[] getIssuerUniqueID() { return null; }
        @Override public boolean[] getSubjectUniqueID() { return null; }
        @Override public boolean[] getKeyUsage() { return null; }
        @Override public int getBasicConstraints() { return -1; }
        @Override public byte[] getEncoded() { return new byte[] {1,2,3}; }
        @Override public void verify(PublicKey key) {}
        @Override public void verify(PublicKey key, String sigProvider) {}
        @Override public String toString() { return "fake"; }
        @Override public PublicKey getPublicKey() { return null; }
        @Override public boolean hasUnsupportedCriticalExtension() { return false; }
        @Override public Set<String> getCriticalExtensionOIDs() { return Set.of(); }
        @Override public Set<String> getNonCriticalExtensionOIDs() { return Set.of(); }
        @Override public byte[] getExtensionValue(String oid) { return null; }
        @Override public X500Principal getSubjectX500Principal() { return new X500Principal("CN=Titular A1 CPF=12345678901"); }
        @Override public X500Principal getIssuerX500Principal() { return new X500Principal("CN=AC-JUS"); }
    }
}
