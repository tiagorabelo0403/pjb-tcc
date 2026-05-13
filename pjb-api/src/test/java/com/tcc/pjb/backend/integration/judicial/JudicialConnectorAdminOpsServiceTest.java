package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.judicial.JudicialConnectorAdminOperation;
import com.tcc.pjb.backend.model.repository.JudicialConnectorAdminOperationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JudicialConnectorAdminOpsServiceTest {

    @Test
    void executesQuarantineAndReturnsOperationalView() {
        JudicialConnectorPolicyService policyService = Mockito.mock(JudicialConnectorPolicyService.class);
        JudicialConnectorAdminOperationRepository repository = Mockito.mock(JudicialConnectorAdminOperationRepository.class);
        JudicialConnectorControlPlaneService controlPlaneService = Mockito.mock(JudicialConnectorControlPlaneService.class);
        JudicialConnectorDataPlaneService dataPlaneService = Mockito.mock(JudicialConnectorDataPlaneService.class);
        when(policyService.save(any())).thenReturn(new JudicialConnectorPolicyOverlay(null, JudicialSystem.PJE, "PROD", "TJCE", true, true, true, false, true, false, null, null, "/submit", null, null, null, null, null, null, null, null, null, List.of("CONNECTOR_POLICY_QUARANTINED"), List.of(), Map.of()));
        when(repository.findTop100ByOrderByCreatedAtDesc()).thenReturn(List.of(savedOperation()));
        when(controlPlaneService.tribunalReport("TJCE")).thenReturn(new JudicialConnectorControlPlaneReport(Instant.now(), "TJCE", 1, 1, 0, List.of(), List.of("PJE"), List.of(), List.of("CONNECTOR_POLICY_QUARANTINED"), List.of(), Map.of()));
        when(dataPlaneService.tribunalReport(any(), any())).thenReturn(new JudicialConnectorDataPlaneReport(Instant.now(), "TJCE", Instant.now(), 0L, List.of(), List.of(), List.of("DATA_PLANE_NO_SUBMISSION_READY_CONNECTOR_FOR_TRIBUNAL"), Map.of()));
        JudicialConnectorAdminOpsService service = new JudicialConnectorAdminOpsService(policyService, repository, controlPlaneService, dataPlaneService, new ObjectMapper());

        JudicialConnectorAdminOperationReport report = service.execute(new JudicialConnectorAdminOperationRequest("QUARANTINE", JudicialSystem.PJE, "TJCE", "prod", "admin", "maintenance", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, Map.of()));

        assertThat(report.outcomeStatus()).isEqualTo("APPLIED_WITH_BLOCKERS");
        assertThat(report.policy().blockers()).contains("CONNECTOR_POLICY_QUARANTINED");
        assertThat(report.recentOperations()).hasSize(1);
    }

    private JudicialConnectorAdminOperation savedOperation() {
        JudicialConnectorAdminOperation operation = new JudicialConnectorAdminOperation();
        operation.setOperationType("QUARANTINE");
        operation.setConnectorSystem(JudicialSystem.PJE);
        operation.setTribunalCodigo("TJCE");
        operation.setEnvironmentName("PROD");
        operation.setRequestedBy("admin");
        operation.setOutcomeStatus("APPLIED_WITH_BLOCKERS");
        operation.setOutcomeMessage("Connector quarantine updated");
        operation.setCreatedAt(Instant.now());
        return operation;
    }
}
