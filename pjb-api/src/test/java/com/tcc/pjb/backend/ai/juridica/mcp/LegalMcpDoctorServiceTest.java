package com.tcc.pjb.backend.ai.juridica.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.ai.juridica.eval.LegalBenchmarkCatalog;
import com.tcc.pjb.backend.ai.juridica.eval.LegalEvalReplayRunner;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpPlanScorer;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerDemotionPolicy;
import com.tcc.pjb.backend.ai.juridica.eval.LegalMcpServerPromotionPolicy;
import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LegalMcpDoctorServiceTest {

    @Test
    void mustReportReadyWhenPlanIsBenchmarkedAndGoverned() {
        var service = com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.mcpCatalog();

        var plan = service.resolvePlan(ApiVersion.V3, "LEGAL_GENERAL_ASSIST_V3", Map.of("message", "Quero um parecer citation-first", "userProfile", "ADVOGADO"), List.of());

        assertEquals("READY", plan.doctor().status());
        assertTrue(plan.doctor().ready());
        assertTrue(plan.doctor().checks().stream().anyMatch(check -> check.checkId().equals("MCP_EVAL_SCORE")));
    }
}
