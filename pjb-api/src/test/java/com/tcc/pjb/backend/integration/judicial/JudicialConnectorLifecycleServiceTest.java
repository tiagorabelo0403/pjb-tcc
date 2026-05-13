package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JudicialConnectorLifecycleServiceTest {

    @Test
    void submitsAndSynchronizesExternalState() {
        JudicialConnectorRegistry registry = Mockito.mock(JudicialConnectorRegistry.class);
        JudicialProcessConnector connector = Mockito.mock(JudicialProcessConnector.class);
        when(registry.find(JudicialSystem.PJE)).thenReturn(Optional.of(connector));
        when(connector.capability()).thenReturn(new JudicialSubmissionCapability(JudicialSystem.PJE, true, true, true, true, true, false, false, true, List.of("application/pdf"), List.of("CIVIL"), List.of("PETICAO_INICIAL"), "https://pje.test.local"));
        when(connector.submit(any())).thenReturn(new ProtocolSubmissionResult(true, JudicialSystem.PJE, "PJE-RECIBO-1", "SUBMITTED", "ok", Instant.parse("2026-03-09T13:00:00Z"), Map.of()));

        ProcessEventNormalizer normalizer = new ProcessEventNormalizer();
        JudicialConnectorTelemetryService telemetryService = Mockito.mock(JudicialConnectorTelemetryService.class);
        com.tcc.pjb.backend.service.outbox.OutboxPublisher outbox = Mockito.mock(com.tcc.pjb.backend.service.outbox.OutboxPublisher.class);
        JudicialProcessConnector syncConnector = new JudicialProcessConnector() {
            @Override
            public JudicialSystem system() {
                return JudicialSystem.PJE;
            }

            @Override
            public Optional<ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
                return Optional.of(new ExternalProcessSnapshot(JudicialSystem.PJE, numeroUnificado, "PROCEDIMENTO_COMUM_CIVEL", "Obrigação de fazer", NivelSigilo.PUBLICO, Instant.parse("2026-03-09T13:01:00Z"), Map.of()));
            }

            @Override
            public List<ExternalProcessEvent> fetchEvents(String numeroUnificado, Instant since) {
                return List.of(new ExternalProcessEvent(JudicialSystem.PJE, numeroUnificado, "EV-1", "JUNTADA", "Juntada", Instant.parse("2026-03-09T13:02:00Z"), Map.of()));
            }
        };
        when(registry.get(JudicialSystem.PJE)).thenReturn(syncConnector);

        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialConnectorHomologationService homologationService = JudicialConnectorHomologationService.withoutPolicy(properties);
        JudicialConnectorReadinessService readinessService = new JudicialConnectorReadinessService(properties, homologationService, new JudicialOAuthTokenService(new org.springframework.boot.web.client.RestTemplateBuilder(), new ObjectMapper()));
        JudicialConnectorOperationalProfileService operationalProfileService = new JudicialConnectorOperationalProfileService(registry, homologationService, readinessService);
        JudicialProtocolSubmissionService submissionService = new JudicialProtocolSubmissionService(registry, new ObjectMapper(), telemetryService, readinessService, homologationService, operationalProfileService);
        ProcessSyncService processSyncService = new ProcessSyncService(registry, normalizer, outbox, telemetryService);
        JudicialConnectorLifecycleService lifecycleService = new JudicialConnectorLifecycleService(submissionService, processSyncService);

        Processo processo = new Processo();
        processo.setId(88L);
        processo.setNumeroUnificado("0000001-00.2026.8.06.0001");
        processo.setMaterialProbatorioHash("HASH-88");
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        processo.setUsuario(usuario);

        ProceduralSubmissionBlueprintReport blueprint = new ProceduralSubmissionBlueprintReport(
                Instant.now(),
                "REQ-88",
                "READY_REAL_CONNECTOR",
                true,
                true,
                true,
                JudicialSystem.PJE,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "PROCEDIMENTO_COMUM_CIVEL",
                "Procedimento Comum Cível",
                "TJCE-CIVEL-CE-CAP",
                "1ª Vara Cível",
                "COMUM_ORDINARIO",
                "CIVIL",
                "DOMICILIO_REU",
                "NONE",
                "NONE",
                "LOCAL_HASH",
                List.of(),
                true,
                false,
                false,
                "DRY_RUN_OK",
                "DRY-PJE",
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Map.of()
        );
        ProceduralConnectorExecutionReport execution = new ProceduralConnectorExecutionReport(
                Instant.now(),
                "REAL_CONNECTOR",
                "DIRECT_PROTOCOL",
                "PJE:TJCE:TJCE-CIVEL-CE-CAP:PROCEDIMENTO_COMUM_CIVEL",
                JudicialSystem.PJE,
                "TJCE",
                "TJCE-CIVEL-CE-CAP",
                "PROCEDIMENTO_COMUM_CIVEL",
                "IDEMPOTENCY-88",
                "PASSWORD",
                "FAST_RETRY",
                true,
                false,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        var result = lifecycleService.submitAndSynchronize(processo, blueprint, execution, true);

        assertThat(result).isPresent();
        assertThat(processo.getConnectorSubmissionAttempts()).isEqualTo(1);
        assertThat(processo.getConnectorSyncAttempts()).isEqualTo(1);
        assertThat(processo.getConnectorProtocolReference()).isEqualTo("PJE-RECIBO-1");
        assertThat(processo.getConnectorSyncStatus()).isEqualTo("SNAPSHOT_AND_EVENTS_SYNCED");
        assertThat(processo.getClasseProcessual()).isEqualTo("PROCEDIMENTO_COMUM_CIVEL");
        assertThat(processo.getAssunto()).isEqualTo("Obrigação de fazer");
        assertThat(processo.getNivelSigilo()).isEqualTo(NivelSigilo.PUBLICO);
    }
}
