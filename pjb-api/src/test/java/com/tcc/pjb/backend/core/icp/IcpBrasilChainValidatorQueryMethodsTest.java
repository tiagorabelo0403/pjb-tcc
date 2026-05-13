package com.tcc.pjb.backend.core.icp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult;
import com.tcc.pjb.backend.core.icp.domain.IcpCertificateQueryCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpSignatureQueryCommand;
import com.tcc.pjb.backend.model.entity.icp.IcpCertificateCache;
import com.tcc.pjb.backend.model.entity.icp.IcpSignatureEvent;
import com.tcc.pjb.backend.model.repository.IcpCertificateCacheRepository;
import com.tcc.pjb.backend.model.repository.IcpSignatureEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IcpBrasilChainValidatorQueryMethodsTest {

    @Test
    void shouldExposeSignatureAndCertificateQueries() throws Exception {
        IcpCertificateCacheRepository cacheRepository = mock(IcpCertificateCacheRepository.class);
        IcpSignatureEventRepository eventRepository = mock(IcpSignatureEventRepository.class);
        IcpBrasilOcspVerifier ocspVerifier = mock(IcpBrasilOcspVerifier.class);
        IcpBrasilTrustAnchorLoader trustAnchorLoader = mock(IcpBrasilTrustAnchorLoader.class);
        when(trustAnchorLoader.countActiveAnchors()).thenReturn(1);
        IcpCertificateCache cache = IcpCertificateCache.builder()
                .issuerDn("issuer-dn")
                .serialHex("serial-1")
                .acSigla("SERASA")
                .revoked(false)
                .revocationChecked(Instant.parse("2026-04-11T12:00:00Z"))
                .validUntil(Instant.parse("2027-01-01T00:00:00Z"))
                .build();
        when(cacheRepository.findBySerialHex("serial-1")).thenReturn(Optional.of(cache));
        when(cacheRepository.findByIssuerDnAndSerialHex("issuer-dn", "serial-1")).thenReturn(Optional.of(cache));
        when(eventRepository.findTop20ByDocHashOrderBySignedAtAsc("doc-1")).thenReturn(List.of(
                IcpSignatureEvent.builder().docHash("doc-1").profileCandidate("LTA").profileAchieved("LTA").signedAt(Instant.parse("2026-04-11T12:30:00Z")).validationOk(true).build()
        ));
        when(ocspVerifier.check(org.mockito.ArgumentMatchers.any(IcpBrasilOcspCommand.class))).thenReturn(new IcpBrasilOcspResult(false, null));
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(
                cacheRepository,
                eventRepository,
                ocspVerifier,
                trustAnchorLoader,
                new IcpBrasilSignatureProperties(true, false, List.of("SERASA"), "icp:", 3600, 86400, null, null, null, "LTA", false),
                mock(AuditLedgerService.class));

        var assinatura = validator.consultarAssinatura(new IcpSignatureQueryCommand("doc-1"));
        var certificado = validator.consultarCertificado(new IcpCertificateQueryCommand("serial-1"));

        assertThat(assinatura.events()).hasSize(1);
        assertThat(certificado.cache().serialHex()).isEqualTo("serial-1");
    }
}
