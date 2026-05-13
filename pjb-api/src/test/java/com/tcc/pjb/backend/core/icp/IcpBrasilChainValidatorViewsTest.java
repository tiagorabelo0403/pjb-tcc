package com.tcc.pjb.backend.core.icp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspResult;
import com.tcc.pjb.backend.model.repository.IcpBrasilTrustAnchorRepository;
import com.tcc.pjb.backend.model.repository.IcpCertificateCacheRepository;
import com.tcc.pjb.backend.model.repository.IcpSignatureEventRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IcpBrasilChainValidatorViewsTest {

    @Test
    void shouldExposePolicyDetailsAndOcspEvidence() {
        IcpBrasilTrustAnchorRepository trustAnchorRepository = mock(IcpBrasilTrustAnchorRepository.class);
        when(trustAnchorRepository.findByAtivoTrueOrderByAcSiglaAsc()).thenReturn(List.of());
        IcpBrasilChainValidator validator = new IcpBrasilChainValidator(
                mock(IcpCertificateCacheRepository.class),
                mock(IcpSignatureEventRepository.class),
                command -> new IcpBrasilOcspResult(false, null),
                new IcpBrasilTrustAnchorLoader(trustAnchorRepository),
                new IcpBrasilSignatureProperties(true, false, List.of("AC-JUS"), "pref", 3600, 86400, null, null, null, "LTA", false),
                mock(AuditLedgerService.class));

        var policy = validator.policySnapshot();
        var details = validator.detailsFor("AC-JUS", null);
        var evidence = validator.ocspEvidence(new IcpBrasilOcspResult(false, Instant.parse("2026-04-11T12:00:00Z")));
        var snapshot = validator.validationSnapshot(null);

        assertThat(policy.enabled()).isTrue();
        assertThat(details.acceptedAc()).isTrue();
        assertThat(evidence.checked()).isTrue();
        assertThat(snapshot.valid()).isFalse();
    }
}
