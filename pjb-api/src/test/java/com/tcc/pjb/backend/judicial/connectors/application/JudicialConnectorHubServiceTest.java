package com.tcc.pjb.backend.judicial.connectors.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorCommandCenterService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCryptoCommandCenterReport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorCryptoCommandCenterService;
import com.tcc.pjb.backend.judicial.connectors.domain.JudicialConnectorHubReport;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JudicialConnectorHubServiceTest {

    @Mock
    private JudicialConnectorCommandCenterService commandCenterService;

    @Mock
    private JudicialConnectorCryptoCommandCenterService cryptoCommandCenterService;

    private JudicialConnectorHubService service;

    @BeforeEach
    void setUp() {
        service = new JudicialConnectorHubService(new JudicialConnectorStructureService(), commandCenterService, cryptoCommandCenterService);
    }

    @Test
    void shouldAggregateNationalOperationalAndCryptographicAlerts() {
        when(commandCenterService.nationalReport(Duration.ofHours(24))).thenReturn(new JudicialConnectorCommandCenterReport(
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of("OPS_ALERT"),
                Map.of("mode", "NATIONAL")
        ));
        when(cryptoCommandCenterService.nationalReport(Duration.ofHours(24))).thenReturn(new JudicialConnectorCryptoCommandCenterReport(
                Instant.now(),
                null,
                null,
                null,
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of("CRYPTO_ALERT"),
                Map.of("mode", "NATIONAL")
        ));

        JudicialConnectorHubReport report = service.nationalReport(Duration.ofHours(24));

        assertThat(report.structure().nodes()).isNotEmpty();
        assertThat(report.alerts()).containsExactlyInAnyOrder("OPS_ALERT", "CRYPTO_ALERT");
        assertThat(report.metadata()).containsEntry("entrypoint", "NATIONAL");
    }
}
