package com.tcc.pjb.backend.service.processual.integration.external;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorHomologationReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorHomologationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.model.dto.processual.integration.external.ExternalIntegrationDiagnosticRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalExternalIntegrationGatewayServiceTest {

    @Test
    void resolvesConnectorAndReadinessForProcess() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        TribunalProtocolRoutingService routingService = Mockito.mock(TribunalProtocolRoutingService.class);
        JudicialConnectorReadinessService readinessService = Mockito.mock(JudicialConnectorReadinessService.class);
        JudicialConnectorHomologationService homologationService = Mockito.mock(JudicialConnectorHomologationService.class);
        JudicialConnectorOperationalProfileService operationalProfileService = Mockito.mock(JudicialConnectorOperationalProfileService.class);
        JudicialProcessConnector connector = new JudicialProcessConnector() {
            @Override
            public JudicialSystem system() {
                return JudicialSystem.PJE;
            }

            @Override
            public Optional<com.tcc.pjb.backend.integration.judicial.ExternalProcessSnapshot> fetchSnapshotByNumero(String numeroUnificado) {
                return Optional.empty();
            }

            @Override
            public List<com.tcc.pjb.backend.integration.judicial.ExternalProcessEvent> fetchEvents(String numeroUnificado, Instant since) {
                return List.of();
            }

            @Override
            public JudicialSubmissionCapability capability() {
                return new JudicialSubmissionCapability(JudicialSystem.PJE, true, true, true, true, true, false, false, false, List.of("application/pdf"), List.of(), List.of(), "https://pje.test");
            }
        };
        JudicialConnectorRegistry registry = new JudicialConnectorRegistry(List.of(connector));
        NationalExternalIntegrationGatewayService service = new NationalExternalIntegrationGatewayService(
                processoRepository,
                authorizationService,
                routingService,
                registry,
                readinessService,
                homologationService,
                operationalProfileService
        );
        Processo processo = new Processo();
        processo.setId(10L);
        processo.setNumeroProcesso("0010-22");
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setRamoDireito(RamoDireito.CIVIL);
        when(processoRepository.findById(10L)).thenReturn(Optional.of(processo));
        when(routingService.resolve(Mockito.anyMap(), Mockito.any(Enum.class), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean()))
                .thenReturn(new TribunalProtocolRoutingService.RoutingDecision(
                        "TJCE",
                        "Tribunal de Justiça do Ceará",
                        JudicialSystem.PJE,
                        connector.capability(),
                        "ESTADUAL",
                        false,
                        false,
                        List.of("ok"),
                        Map.of("source", "routing"),
                        Instant.now()
                ));
        when(homologationService.analyze(Mockito.eq(JudicialSystem.PJE), Mockito.any(), Mockito.any()))
                .thenReturn(new JudicialConnectorHomologationReport(Instant.now(), JudicialSystem.PJE, "TJCE", true, true, false, true, true, "/submit", "/dry", "/snapshot", "/events", List.of(), List.of(), Map.of()));
        when(readinessService.analyze(Mockito.eq(JudicialSystem.PJE), Mockito.any(), Mockito.any()))
                .thenReturn(new JudicialConnectorReadinessReport(Instant.now(), JudicialSystem.PJE, true, true, true, true, true, true, true, true, List.of(), List.of(), Map.of()));
        when(operationalProfileService.analyze(Mockito.eq(JudicialSystem.PJE), Mockito.any(), Mockito.any()))
                .thenReturn(new JudicialConnectorOperationalProfileReport(Instant.now(), JudicialSystem.PJE, "TJCE", true, true, true, true, true, com.tcc.pjb.backend.integration.judicial.JudicialConnectorAuthMode.NONE, null, null, List.of(), List.of(), Map.of()));

        var response = service.diagnosticar(new ExternalIntegrationDiagnosticRequest(10L, true, true));

        assertEquals("PJE", response.selectedConnectorSystem());
        assertTrue(response.readyForSubmission());
        assertTrue(response.connectorLandscape().stream().anyMatch(item -> "PJE".equals(item.system())));
    }
}
