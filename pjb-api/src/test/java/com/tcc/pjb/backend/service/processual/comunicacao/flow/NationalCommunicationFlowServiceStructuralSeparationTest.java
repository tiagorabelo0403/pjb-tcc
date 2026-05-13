package com.tcc.pjb.backend.service.processual.comunicacao.flow;

import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.access.NationalCommunicationInstitutionalMembershipResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.governance.NationalCommunicationInstitutionalActionResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalResolveResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalInboxItemResponse;
import com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel.NationalCommunicationInstitutionalObservabilityDashboardResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.operations.NationalCommunicationInstitutionalOperationsFacade;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NationalCommunicationFlowServiceStructuralSeparationTest {

    private static final Path SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/flow/NationalCommunicationFlowService.java");
    private static final Path FACADE = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/flow/NationalCommunicationFlowFacade.java");
    private static final Path INSTITUTIONAL_OPERATIONS = Path.of("src/main/java/com/tcc/pjb/backend/service/processual/comunicacao/institutional/operations/NationalCommunicationInstitutionalOperationsFacade.java");

    @Test
    void mustKeepFlowServiceAsShortOrchestrator() throws Exception {
        String source = Files.readString(SERVICE, StandardCharsets.UTF_8);
        assertTrue(source.contains("private final NationalCommunicationFlowFacade facade;"));
        assertFalse(source.contains("private WorkItem criarWorkItemSeguimento("));
        assertFalse(source.contains("private NationalCommunicationInstitutionalResolveResponse toInstitutionalResponse("));
        assertFalse(source.contains("private java.util.Optional<ResolucaoDestinoInstitucionalResult> preResolverInstitucional("));
    }

    @Test
    void mustKeepDispatchAndRoutingMassInsideMainFacadeButMoveInstitutionalOperationsOut() throws Exception {
        String source = Files.readString(FACADE, StandardCharsets.UTF_8);
        assertTrue(source.contains("private final NationalCommunicationInstitutionalOperationsFacade institutionalOperationsFacade;"));
        assertTrue(source.contains("private WorkItem criarWorkItemSeguimento("));
        assertTrue(source.contains("private NationalCommunicationInstitutionalResolveResponse toInstitutionalResponse("));
        assertTrue(source.contains("private java.util.Optional<ResolucaoDestinoInstitucionalResult> preResolverInstitucional("));
        assertFalse(source.contains("private NationalCommunicationInstitutionalMembershipResponse toMembershipResponse("));
        assertFalse(source.contains("private NationalCommunicationInstitutionalInboxItemResponse toInboxResponse("));
        assertFalse(source.contains("private NationalCommunicationInstitutionalObservabilityDashboardResponse toObservabilityResponse("));
    }

    @Test
    void mustKeepInstitutionalOperationsInDedicatedFacade() throws Exception {
        String source = Files.readString(INSTITUTIONAL_OPERATIONS, StandardCharsets.UTF_8);
        assertTrue(source.contains("List<NationalCommunicationInstitutionalMembershipResponse> minhasCaixasInstitucionais("));
        assertTrue(source.contains("NationalCommunicationInstitutionalActionResponse receberInboxInstitucional("));
        assertTrue(source.contains("private NationalCommunicationInstitutionalMembershipResponse toMembershipResponse("));
        assertTrue(source.contains("private NationalCommunicationInstitutionalObservabilityDashboardResponse toObservabilityResponse("));
    }
}