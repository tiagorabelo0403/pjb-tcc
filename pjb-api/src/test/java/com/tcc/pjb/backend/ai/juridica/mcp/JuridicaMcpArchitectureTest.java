package com.tcc.pjb.backend.ai.juridica.mcp;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;

class JuridicaMcpArchitectureTest {

@Test
    void mcpMustBeMaterializedByDedicatedCatalogAndDomainServers() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/JuridicaMcpServerCatalogService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalLegislationMcpServer.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalJurisprudenceMcpServer.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalProcessualMcpServer.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalDocumentalMcpServer.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalAgendaPrazosMcpServer.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalInteroperabilityMcpServer.java")));
        String mesh = Files.readString(root.resolve("ai/juridica/mesh/JuridicaUnifiedMeshProfileService.java"));
        String assembler = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationContextAssemblerService.java"));
        String composer = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationResponseComposerService.java"));
        assertTrue(mesh.contains("mcpServerCatalogService.resolvePlan"));
        assertTrue(mesh.contains("mcpPlan.asMap()"));
        assertTrue(assembler.contains("juridicaMcpPlan"));
        assertTrue(composer.contains("mcpSelectionMode"));
    }

@Test
    void mcpMustExposeContinuousEvaluationReplayAndPromotionPolicies() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/eval/LegalBenchmarkCatalog.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/eval/LegalEvalReplayRunner.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/eval/LegalMcpPlanScorer.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/eval/LegalMcpServerPromotionPolicy.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/eval/LegalMcpServerDemotionPolicy.java")));
        String catalog = Files.readString(root.resolve("ai/juridica/mcp/JuridicaMcpServerCatalogService.java"));
        String mesh = Files.readString(root.resolve("ai/juridica/mesh/JuridicaUnifiedMeshProfileService.java"));
        assertTrue(catalog.contains("evalReplayRunner.run"));
        assertTrue(mesh.contains("benchmarkSuiteId"));
        assertTrue(mesh.contains("qualityScore"));
        assertTrue(mesh.contains("promotionCandidates"));
        assertTrue(mesh.contains("demotionCandidates"));
    }

@Test
    void mcpMustExposeSkillsExamplesDeliberationAndContextCompaction() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalMcpSkillCatalogService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalMcpToolExampleRegistry.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalMcpDeliberationCheckpointService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalMcpContextCompactionService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalMcpExecutionTranscriptService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalMcpDoctorService.java")));
        String catalog = Files.readString(root.resolve("ai/juridica/mcp/JuridicaMcpServerCatalogService.java"));
        String mesh = Files.readString(root.resolve("ai/juridica/mesh/JuridicaUnifiedMeshProfileService.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        assertTrue(catalog.contains("pinnedSkills"));
        assertTrue(catalog.contains("pinnedToolExamples"));
        assertTrue(catalog.contains("deliberationCheckpointService"));
        assertTrue(catalog.contains("contextCompactionService"));
        assertTrue(catalog.contains("executionTranscriptService"));
        assertTrue(catalog.contains("doctorService"));
        assertTrue(mesh.contains("skillIds"));
        assertTrue(mesh.contains("toolExampleIds"));
        assertTrue(mesh.contains("contextCompaction"));
        assertTrue(mesh.contains("doctorStatus"));
        assertTrue(toolScope.contains("mcpSkillIds"));
        assertTrue(toolScope.contains("mcpDeliberationMode"));
        assertTrue(toolScope.contains("mcpTranscriptMode"));
        assertTrue(toolScope.contains("mcpDoctorStatus"));
    }

@Test
    void mcpMustExposeEvidencePromotionAndApprovalLaneByReplay() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/mcp/LegalMcpEvidencePromotionService.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/mcp/LegalMcpEvidencePromotionDecision.java")));
        String catalog = Files.readString(root.resolve("ai/juridica/mcp/JuridicaMcpServerCatalogService.java"));
        String toolScope = Files.readString(root.resolve("ai/juridica/conversation/security/LegalToolScopePolicy.java"));
        String approval = Files.readString(root.resolve("ai/juridica/conversation/security/LegalSensitiveActionApprovalService.java"));
        String mesh = Files.readString(root.resolve("ai/juridica/mesh/JuridicaUnifiedMeshProfileService.java"));
        assertTrue(catalog.contains("evidencePromotionService"));
        assertTrue(catalog.contains("evidencePromotion"));
        assertTrue(toolScope.contains("mcpEvidencePromotionStatus"));
        assertTrue(toolScope.contains("mcpEvidenceApprovalLane"));
        assertTrue(approval.contains("MCP_EVIDENCE_LANE_"));
        assertTrue(mesh.contains("evidencePromotionStatus"));
        assertTrue(mesh.contains("evidenceApprovalLane"));
    }
}
