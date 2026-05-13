package com.tcc.pjb.backend.service.processual.resilience;

import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorHomologationReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorReadinessReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import com.tcc.pjb.backend.model.dto.processual.resilience.NationalContingencyAssessmentRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class NationalContingencyOrchestratorServiceTest {

    @Test
    void choosesDryRunContingencyWhenSubmissionIsNotReady() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        WorkItemRepository workItemRepository = Mockito.mock(WorkItemRepository.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        TribunalProtocolRoutingService routingService = Mockito.mock(TribunalProtocolRoutingService.class);
        JudicialConnectorRegistry registry = Mockito.mock(JudicialConnectorRegistry.class);
        JudicialConnectorOperationalProfileService profileService = Mockito.mock(JudicialConnectorOperationalProfileService.class);
        NationalContingencyOrchestratorService service = new NationalContingencyOrchestratorService(processoRepository, workItemRepository, authorizationService, routingService, registry, profileService);
        Processo processo = new Processo();
        processo.setId(5L);
        processo.setNumeroProcesso("0005-88");
        when(processoRepository.findById(5L)).thenReturn(Optional.of(processo));
        when(routingService.resolve(Mockito.anyMap(), Mockito.<String>any(), Mockito.<String>any(), Mockito.<String>any(), Mockito.anyBoolean())).thenReturn(new TribunalProtocolRoutingService.RoutingDecision("TJCE", "Tribunal", JudicialSystem.PJE, null, null, false, false, java.util.List.of(), java.util.Map.of(), java.time.Instant.now()));
        JudicialProcessConnector connector = Mockito.mock(JudicialProcessConnector.class);
        JudicialSubmissionCapability capability = new JudicialSubmissionCapability(JudicialSystem.PJE, true, true, true, false, false, false, false, false, java.util.List.of(), java.util.List.of(), java.util.List.of(), "http://base");
        when(connector.system()).thenReturn(JudicialSystem.PJE);
        when(connector.capability()).thenReturn(capability);
        when(registry.find(JudicialSystem.PJE)).thenReturn(Optional.of(connector));
        when(workItemRepository.countOpenBlockingByProcessAndRole(5L, com.tcc.pjb.backend.model.entity.enums.TipoUsuario.OFICIAL_JUSTICA)).thenReturn(0L);
        JudicialConnectorHomologationReport homologation = new JudicialConnectorHomologationReport(java.time.Instant.now(), JudicialSystem.PJE, "TJCE", true, true, false, false, false, null, null, null, null, java.util.List.of(), java.util.List.of(), java.util.Map.of());
        JudicialConnectorReadinessReport readiness = new JudicialConnectorReadinessReport(java.time.Instant.now(), JudicialSystem.PJE, true, true, true, false, true, true, true, false, java.util.List.of(), java.util.List.of(), java.util.Map.of());
        JudicialConnectorOperationalProfileReport profile = new JudicialConnectorOperationalProfileReport(java.time.Instant.now(), JudicialSystem.PJE, "TJCE", true, true, true, false, false, com.tcc.pjb.backend.integration.judicial.JudicialConnectorAuthMode.NONE, homologation, readiness, java.util.List.of(), java.util.List.of(), java.util.Map.of());
        when(profileService.analyze(Mockito.eq(JudicialSystem.PJE), Mockito.any(), Mockito.any(ProtocolSubmissionRequest.class))).thenReturn(profile);

        var response = service.assess(new NationalContingencyAssessmentRequest(5L, JudicialSystem.PJE, true, false, false, "PROTOCOLO"));

        assertEquals("DRY_RUN_CONTINGENCY", response.contingencyMode());
        assertTrue(response.dryRunOnly());
    }
}
