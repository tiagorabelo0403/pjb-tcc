package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JudicialProtocolSubmissionServiceTest {

    @Test
    void submitsAndAppliesAcceptedProtocolResult() {
        JudicialConnectorRegistry registry = Mockito.mock(JudicialConnectorRegistry.class);
        JudicialProcessConnector connector = Mockito.mock(JudicialProcessConnector.class);
        when(registry.find(JudicialSystem.PJE)).thenReturn(Optional.of(connector));
        when(connector.capability()).thenReturn(new JudicialSubmissionCapability(JudicialSystem.PJE, true, true, true, true, true, false, false, true, List.of("application/pdf"), List.of("CIVIL"), List.of("PETICAO_INICIAL"), "https://pje.test.local"));
        when(connector.submit(any())).thenReturn(new ProtocolSubmissionResult(true, JudicialSystem.PJE, "PJE-2026-1", "SUBMITTED", "ok", Instant.parse("2026-03-09T12:00:00Z"), Map.of()));
        JudicialConnectorTelemetryService telemetryService = Mockito.mock(JudicialConnectorTelemetryService.class);
        JudicialIntegrationProperties properties = new JudicialIntegrationProperties();
        JudicialIntegrationProperties.Connector cfg = new JudicialIntegrationProperties.Connector();
        cfg.setEnabled(true);
        cfg.setBaseUrl("https://pje.test.local");
        cfg.setProductionReady(true);
        cfg.setHomologatedTribunals(java.util.List.of("TJCE"));
        properties.setPje(cfg);
        JudicialConnectorHomologationService homologationService = JudicialConnectorHomologationService.withoutPolicy(properties);
        JudicialConnectorReadinessService readinessService = new JudicialConnectorReadinessService(properties, homologationService, new JudicialOAuthTokenService(new org.springframework.boot.web.client.RestTemplateBuilder(), new ObjectMapper()));
        JudicialConnectorOperationalProfileService operationalProfileService = new JudicialConnectorOperationalProfileService(registry, homologationService, readinessService);
        JudicialProtocolSubmissionService service = new JudicialProtocolSubmissionService(registry, new ObjectMapper(), telemetryService, readinessService, homologationService, operationalProfileService);

        Processo processo = new Processo();
        processo.setId(77L);
        processo.setNumeroUnificado("0000001-00.2026.8.06.0001");
        processo.setPedidoPrincipal("Obrigação de fazer");
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        processo.setUsuario(usuario);
        processo.setMaterialProbatorioHash("HASH-777");

        ProceduralSubmissionBlueprintReport blueprint = new ProceduralSubmissionBlueprintReport(
                Instant.now(),
                "REQ-77",
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
                List.of("Checklist"),
                List.of(),
                Map.of("payload", "ok"),
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
                "IDEMPOTENCY-1",
                "PASSWORD",
                "FAST_RETRY",
                true,
                false,
                false,
                false,
                List.of("PAYLOAD_SEAL_AND_IDEMPOTENCY"),
                List.of("CHECK_1"),
                List.of(),
                List.of(),
                Map.of()
        );

        var result = service.submitIfEligible(processo, blueprint, execution, true);
        assertThat(result).isPresent();
        assertThat(result.orElseThrow().accepted()).isTrue();

        service.applySubmissionResult(processo, result.orElseThrow());
        assertThat(processo.getConnectorSubmissionStatus()).isEqualTo("SUBMITTED");
        assertThat(processo.getConnectorProtocolReference()).isEqualTo("PJE-2026-1");
        assertThat(processo.getConnectorSubmissionProcessedAt()).isEqualTo(LocalDateTime.of(2026, 3, 9, 12, 0));
        assertThat(processo.getConnectorSubmissionAttempts()).isEqualTo(1);
        assertThat(processo.getConnectorLastSubmissionAttemptAt()).isNotNull();
        assertThat(processo.getPreProtocoloStatus()).isEqualTo("PROTOCOLO_REAL_ENVIADO");
    }
}
