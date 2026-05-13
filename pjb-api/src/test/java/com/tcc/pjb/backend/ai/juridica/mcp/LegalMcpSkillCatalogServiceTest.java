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

class LegalMcpSkillCatalogServiceTest {

    @Test
    void mustAttachSkillsExamplesDeliberationAndCompactionToSensitivePetitionPlan() {
        var service = new JuridicaMcpServerCatalogService(List.of(
                new LegalLegislationMcpServer(),
                new LegalJurisprudenceMcpServer(),
                new LegalProcessualMcpServer(),
                new LegalDocumentalMcpServer(),
                new LegalAgendaPrazosMcpServer(),
                new LegalInteroperabilityMcpServer()
        ), new LegalAiStructuredSchemaCatalog(), new LegalEvalReplayRunner(new LegalBenchmarkCatalog(), new LegalMcpPlanScorer(), new LegalMcpServerPromotionPolicy(), new LegalMcpServerDemotionPolicy()), new LegalMcpSkillCatalogService(), com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.toolExampleRegistry(), com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.deliberationCheckpointService(), new LegalMcpContextCompactionService(), new LegalMcpExecutionTranscriptService(), new LegalMcpDoctorService(), new LegalMcpEvidencePromotionService(), com.tcc.pjb.backend.ai.juridica.testsupport.LegalAiTestFixtures.mcpTextCatalogService());

        var plan = service.resolvePlan(
                ApiVersion.V3,
                "PETICAO_ASSISTIDA",
                Map.of(
                        "message", "Quero minuta citation-first com conferência de assinatura e prazo.",
                        "ramo", "civel",
                        "rito", "comum",
                        "userProfile", "ADVOGADO",
                        "sigilo", true,
                        "attachments", List.of("peticao.pdf", "laudo.pdf"),
                        "history", List.of("preciso consolidar precedentes", "há anexos assinados", "calcular prazo recursal")
                ),
                List.of()
        );

        assertTrue(plan.pinnedSkills().stream().anyMatch(skill -> skill.skillId().equals("LEGAL_SKILL_CITATION_FIRST_DRAFTING")));
        assertTrue(plan.pinnedSkills().stream().anyMatch(skill -> skill.skillId().equals("LEGAL_SKILL_DOCUMENT_PROVENANCE_AND_SIGNATURE_CHAIN")));
        assertTrue(plan.pinnedToolExamples().stream().anyMatch(example -> example.toolId().equals("document.signature.verify")));
        assertTrue(plan.deliberation().required());
        assertEquals("INLINE_ONLY", plan.contextCompaction().externalizationMode());
    }
}
