package com.tcc.pjb.backend.integration.judicial.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorCertificateInventory;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorCryptographicFailureEvent;
import com.tcc.pjb.backend.model.repository.JudicialConnectorCertificateInventoryRepository;
import com.tcc.pjb.backend.model.repository.JudicialConnectorCryptographicFailureEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class JudicialConnectorCertificateInventoryServiceTest {

    @Test
    void summarizesInventoryAndRecentFailures() {
        JudicialConnectorCertificateInventoryRepository inventoryRepository = mock(JudicialConnectorCertificateInventoryRepository.class);
        JudicialConnectorCryptographicFailureEventRepository failureRepository = mock(JudicialConnectorCryptographicFailureEventRepository.class);
        JudicialConnectorCertificateInventory valid = new JudicialConnectorCertificateInventory();
        valid.setConnectorSystem(JudicialSystem.PJE);
        valid.setTribunalCodigo("TJCE");
        valid.setEnvironmentName("prod");
        valid.setBindingId("PJE-TJCE");
        valid.setValidationStatus("VALID");
        valid.setCertificatePresent(true);
        valid.setValidNow(true);
        valid.setLastValidatedAt(Instant.now());
        valid.setMetadataJson("{}");
        JudicialConnectorCertificateInventory blocked = new JudicialConnectorCertificateInventory();
        blocked.setConnectorSystem(JudicialSystem.EPROC);
        blocked.setTribunalCodigo("TRF5");
        blocked.setEnvironmentName("prod");
        blocked.setBindingId("EPROC-TRF5");
        blocked.setValidationStatus("BLOCKED");
        blocked.setCertificatePresent(true);
        blocked.setExpired(true);
        blocked.setExpiresSoon(true);
        blocked.setHardwareBacked(true);
        blocked.setLastValidatedAt(Instant.now());
        blocked.setMetadataJson("{}");
        blocked.setBlockersJson("[\"CERTIFICATE_EXPIRED\"]");
        when(inventoryRepository.findAllByOrderByConnectorSystemAscTribunalCodigoAscEnvironmentNameAscBindingIdAsc()).thenReturn(List.of(blocked, valid));
        JudicialConnectorCryptographicFailureEvent failureEvent = new JudicialConnectorCryptographicFailureEvent();
        failureEvent.setConnectorSystem(JudicialSystem.EPROC);
        failureEvent.setTribunalCodigo("TRF5");
        when(failureRepository.findTop200ByCreatedAtAfterOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(failureEvent));
        JudicialConnectorCertificateInventoryService service = new JudicialConnectorCertificateInventoryService(
                new JudicialIntegrationProperties(),
                new JudicialConnectorSecurityProperties(),
                mock(JudicialConnectorCertificateValidationService.class),
                mock(JudicialConnectorCryptographicContextService.class),
                inventoryRepository,
                failureRepository,
                new JudicialConnectorSecurityPostureMetricsService(new SimpleMeterRegistry()),
                new ObjectMapper()
        );

        JudicialConnectorCryptoPostureSummary summary = service.postureSummary(Duration.ofHours(24));

        assertThat(summary.total()).isEqualTo(2);
        assertThat(summary.valid()).isEqualTo(1);
        assertThat(summary.blocked()).isEqualTo(1);
        assertThat(summary.expired()).isEqualTo(1);
        assertThat(summary.expiringSoon()).isEqualTo(1);
        assertThat(summary.hardwareBacked()).isEqualTo(1);
        assertThat(summary.withRecentFailures()).isEqualTo(1);
        assertThat(summary.blockedTargets()).hasSize(1);
    }
}
