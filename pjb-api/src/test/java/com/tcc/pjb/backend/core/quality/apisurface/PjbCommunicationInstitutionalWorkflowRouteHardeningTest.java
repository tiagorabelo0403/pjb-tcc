package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.service.processual.comunicacao.flow.NationalCommunicationFlowRoutes;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbCommunicationInstitutionalWorkflowRouteHardeningTest {

    @Test
    void nationalCommunicationFlowControllerMustUseCanonicalRouteRegistryConstants() {
        String controller = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/flow/NationalCommunicationFlowController.java"));
        assertTrue(controller.contains("@RequestMapping(NationalCommunicationFlowRoutes.CANONICAL_BASE)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_DISPATCH)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_ACKNOWLEDGE)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_RESOLVE_CANONICAL_ACT)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_RESOLVE_PROCESSUAL_RECIPIENT)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_RESOLVE_INSTITUTIONAL)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_RESOLVE_INSTITUTIONAL_ROUTING)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_BOXES)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_AUTHORIZE_BOX)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_INBOX)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_RECEIVE)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_REDISTRIBUTE)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_CERTIFY_SCIENCE)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_FULFILL)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_TIMELINE)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_SEMANTIC_TIMELINE)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_PROOFS)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_GATES)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DELIVERIES)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DLQ)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_REPROCESS_DELIVERY)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_EXTERNAL_INTEGRATIONS)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_OBSERVABILITY)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_ANALYTICS)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_HARDENING)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DELEGATE)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_SUBSTITUTE)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DELEGATIONS)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFT_CREATE)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFT_SUBMIT)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFT_APPROVE)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFT_REJECT)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_INSTITUTIONAL_DRAFTS)"));
        assertTrue(controller.contains("@PostMapping(NationalCommunicationFlowRoutes.PATH_FALLBACK)"));
        assertTrue(controller.contains("@GetMapping(NationalCommunicationFlowRoutes.PATH_DASHBOARD)"));
        assertFalse(controller.contains("@RequestMapping(\"/api/v1/processual/comunicacoes\")"));
        assertFalse(controller.contains("@PostMapping(\"/institucional/delegar\")"));
        assertFalse(controller.contains("@PostMapping(\"/institucional/minutas/criar\")"));
        assertFalse(controller.contains("@GetMapping(\"/institucional/analytics\")"));
    }
}