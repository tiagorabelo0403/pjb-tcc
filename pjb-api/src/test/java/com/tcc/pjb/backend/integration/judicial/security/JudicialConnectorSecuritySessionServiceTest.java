package com.tcc.pjb.backend.integration.judicial.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorSecuritySession;
import com.tcc.pjb.backend.model.repository.JudicialConnectorSecuritySessionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JudicialConnectorSecuritySessionServiceTest {

    @Mock
    private JudicialConnectorSecuritySessionRepository repository;

    private JudicialConnectorSecuritySessionService service;

    @BeforeEach
    void setUp() {
        service = new JudicialConnectorSecuritySessionService(repository, new ObjectMapper());
    }

    @Test
    void shouldSummarizeRecentSessions() {
        JudicialConnectorSecuritySession success = session("TJCE", "SUCCESS", true, 120L, true, true, true, Instant.now().minusSeconds(30));
        JudicialConnectorSecuritySession remoteFailure = session("TJCE", "REMOTE_FAILURE", false, 240L, true, false, true, Instant.now().minusSeconds(20));
        JudicialConnectorSecuritySession handshakeFailure = session("TJCE", "HANDSHAKE_FAILURE", false, 360L, false, true, false, Instant.now().minusSeconds(10));
        when(repository.findTop300ByTribunalCodigoIgnoreCaseAndCreatedAtAfterOrderByCreatedAtDesc(org.mockito.ArgumentMatchers.eq("TJCE"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(success, remoteFailure, handshakeFailure));

        JudicialConnectorSecuritySessionSummary summary = service.summary(Duration.ofHours(1), "tjce");

        assertThat(summary.sessionCount()).isEqualTo(3);
        assertThat(summary.successCount()).isEqualTo(1);
        assertThat(summary.remoteFailureCount()).isEqualTo(1);
        assertThat(summary.transportFailureCount()).isEqualTo(1);
        assertThat(summary.mutualTlsCount()).isEqualTo(2);
        assertThat(summary.hardwareBackedCount()).isEqualTo(2);
        assertThat(summary.hostnameVerifiedCount()).isEqualTo(2);
        assertThat(summary.averageDurationMillis()).isEqualTo(240L);
        assertThat(summary.maxDurationMillis()).isEqualTo(360L);
        assertThat(summary.outcomeBreakdown()).hasSize(3);
    }

    private JudicialConnectorSecuritySession session(String tribunalCodigo,
                                                     String outcomeStatus,
                                                     boolean success,
                                                     long durationMillis,
                                                     boolean mutualTls,
                                                     boolean hardwareBacked,
                                                     boolean hostnameVerification,
                                                     Instant createdAt) {
        JudicialConnectorSecuritySession session = new JudicialConnectorSecuritySession();
        session.setConnectorSystem(JudicialSystem.PJE);
        session.setTribunalCodigo(tribunalCodigo);
        session.setEnvironmentName("PROD");
        session.setOperationName("CRYPTO_HANDSHAKE_PROBE");
        session.setOutcomeStatus(outcomeStatus);
        session.setSuccess(success);
        session.setDurationMillis(durationMillis);
        session.setMutualTls(mutualTls);
        session.setHardwareBacked(hardwareBacked);
        session.setHostnameVerification(hostnameVerification);
        session.setCreatedAt(createdAt);
        return session;
    }
}
