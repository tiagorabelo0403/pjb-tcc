package com.tcc.pjb.backend.core.icp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspCommand;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult;
import com.tcc.pjb.backend.model.repository.IcpCertificateCacheRepository;
import com.tcc.pjb.backend.model.repository.IcpSignatureEventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class IcpBrasilChainValidatorFailureModesTest {

    @Test
    void shouldFailWhenNoTrustAnchorsLoadedAndValidationIsEnforced() {
        IcpBrasilTrustAnchorLoader loader = mock(IcpBrasilTrustAnchorLoader.class);
        when(loader.countActiveAnchors()).thenReturn(0);
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(
                mock(IcpCertificateCacheRepository.class),
                mock(IcpSignatureEventRepository.class),
                command -> IcpBrasilOcspResult.good(),
                loader,
                new IcpBrasilSignatureProperties(true, true, List.of("AC-JUS"), "pjb:icp:ocsp:", 3600, 86400, null, null, null, "LTA", false),
                mock(AuditLedgerService.class)
        );
        var result = validator.validate(new IcpBrasilChainValidatorTest.FakeCertificate());
        assertThat(result.valid()).isFalse();
        assertThat(result.failureReason()).contains("trust anchors");
    }

    @Test
    void shouldFailWhenOcspMarksCertificateAsRevoked() {
        IcpBrasilTrustAnchorLoader loader = mock(IcpBrasilTrustAnchorLoader.class);
        when(loader.countActiveAnchors()).thenReturn(1);
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(
                mock(IcpCertificateCacheRepository.class),
                mock(IcpSignatureEventRepository.class),
                command -> new IcpBrasilOcspResult(true, java.time.Instant.parse("2026-04-10T10:00:00Z")),
                loader,
                new IcpBrasilSignatureProperties(true, true, List.of("AC-JUS"), "pjb:icp:ocsp:", 3600, 86400, null, null, null, "LTA", false),
                mock(AuditLedgerService.class)
        );
        var result = validator.validate(new IcpBrasilChainValidatorTest.FakeCertificate());
        assertThat(result.valid()).isFalse();
        assertThat(result.failureReason()).contains("revogado");
    }
}
