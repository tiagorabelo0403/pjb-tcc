package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.integration.judicial.impl.NoopJudicialConnector;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JudicialConnectorRegistryTest {

    @Test
    void rejectsDuplicateConnectorRegistrationsForSameSystem() {
        assertThatThrownBy(() -> new JudicialConnectorRegistry(List.of(
                connector(JudicialSystem.PJE),
                connector(JudicialSystem.PJE),
                new NoopJudicialConnector()
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Conector judicial duplicado");
    }

    private JudicialProcessConnector connector(JudicialSystem system) {
        return new JudicialProcessConnector() {
            @Override
            public JudicialSystem system() {
                return system;
            }

            @Override
            public java.util.Optional<ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.List<ExternalProcessEvent> fetchEvents(String numeroUnificado, java.time.Instant since) {
                return java.util.List.of();
            }

            @Override
            public JudicialSubmissionCapability capability() {
                return new JudicialSubmissionCapability(
                        system,
                        true,
                        true,
                        true,
                        true,
                        true,
                        false,
                        false,
                        true,
                        List.of("application/pdf"),
                        List.of("CIVIL"),
                        List.of("PETICAO_INICIAL"),
                        "https://connector.test.local"
                );
            }
        };
    }
}
