package com.tcc.pjb.backend.ai.juridica.spine;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;

class JuridicaLegalAiSpineArchitectureTest {

@Test
    void spineFilesMustExistAndConnectVersions() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaLegalAiSpineService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/api/JuridicaLegalAiSpineController.java")));
        String v1 = Files.readString(root.resolve("ai/juridica/v1/IAJuridicaV1.java"));
        String v2 = Files.readString(root.resolve("ai/juridica/v2/IAJuridicaV2.java"));
        String v3 = Files.readString(root.resolve("ai/juridica/v3/IAJuridicaV3.java"));
        assertTrue(v1.contains("juridicaLegalAiSpineService"));
        assertTrue(v2.contains("juridicaLegalAiSpineService"));
        assertTrue(v3.contains("juridicaLegalAiSpineService"));
    }

@Test
    void spineMustExposeRetrievalMemoryAndValidationProfiles() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaHybridRetrievalProfileService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaMemoryIsolationProfileService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaSymbolicValidationProfileService.java")));
        String v1 = Files.readString(root.resolve("ai/juridica/v1/IAJuridicaV1.java"));
        String surface = Files.readString(root.resolve("service/intelligence/surface/LegalAiSurfaceFacadeService.java"));
        assertTrue(v1.contains("juridica_spine_profile"));
        assertTrue(surface.contains("juridicaSpineProfile"));
    }

@Test
    void spineMustExposeGraphMultimodalAndEvaluationProfiles() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaGraphProfileService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaMultimodalProfileService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaEvaluationProfileService.java")));
        String v3 = Files.readString(root.resolve("ai/juridica/v3/IAJuridicaV3.java"));
        String surface = Files.readString(root.resolve("service/intelligence/surface/LegalAiSurfaceFacadeService.java"));
        assertTrue(v3.contains("juridica_graph_enabled"));
        assertTrue(v3.contains("juridica_multimodal_modalities"));
        assertTrue(surface.contains("juridicaGraphTraversalModes"));
        assertTrue(surface.contains("juridicaEvalSuites"));
    }

@Test
    void spineMustExposeResearchAndValidationPipelinesInsideExistingIa() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaResearchDossierService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaValidationEnvelopeService.java")));
        String controller = Files.readString(root.resolve("ai/juridica/api/LegalAiController.java"));
        String facade = Files.readString(root.resolve("service/intelligence/surface/LegalAiSurfaceFacadeService.java"));
        String v3 = Files.readString(root.resolve("ai/juridica/v3/IAJuridicaV3.java"));
        assertTrue(controller.contains("/research/dossier"));
        assertTrue(controller.contains("/validate"));
        assertTrue(facade.contains("researchDossier"));
        assertTrue(facade.contains("validate"));
        assertTrue(v3.contains("juridica_validation_capability"));
    }

@Test
    void spineMustExposeAntiHallucinationGuardInsideExistingIa() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaAntiHallucinationProfileService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/spine/JuridicaHallucinationGuardService.java")));
        String controller = Files.readString(root.resolve("ai/juridica/api/LegalAiController.java"));
        String facade = Files.readString(root.resolve("service/intelligence/surface/LegalAiSurfaceFacadeService.java"));
        String v3 = Files.readString(root.resolve("ai/juridica/v3/IAJuridicaV3.java"));
        assertTrue(controller.contains("/grounding/check"));
        assertTrue(facade.contains("hallucinationGuard"));
        assertTrue(v3.contains("juridica_hallucination_guard"));
    }

@Test
    void symbolicValidationMustBeMaterializedByDedicatedExecutionEngines() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/symbolic/JuridicaSymbolicValidationExecutionService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/symbolic/LegalDeterministicRuleEngine.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/symbolic/LegalPrazoRuleEngine.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/symbolic/LegalCompetenciaRuleEngine.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/symbolic/LegalCabimentoRuleEngine.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/symbolic/LegalSigiloRuleEngine.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/symbolic/LegalProceduralCompatibilityEngine.java")));
        String validationEnvelope = Files.readString(root.resolve("ai/juridica/spine/JuridicaValidationEnvelopeService.java"));
        String profile = Files.readString(root.resolve("ai/juridica/spine/JuridicaSymbolicValidationProfileService.java"));
        assertTrue(validationEnvelope.contains("juridicaSymbolicValidationExecutionService.execute("));
        assertTrue(validationEnvelope.contains("symbolicExecutionStatus"));
        assertTrue(profile.contains("LegalSymbolicValidationCatalog.standardV3Engines()"));
    }

@Test
    void structuredSchemasMustBeMaterializedByDedicatedCatalogAndConcreteDefinitions() throws Exception {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/schema/LegalAiStructuredSchemaCatalog.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/schema/LegalAiDespachoSchema.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/schema/LegalAiDecisaoSchema.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/schema/LegalAiParecerSchema.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/schema/LegalAiChecklistSchema.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/schema/LegalAiProceduralPlanSchema.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/schema/LegalAiDraftEnvelopeSchema.java")));
        assertTrue(Files.exists(root.resolve("model/dto/ai/legal/schema/LegalAiRiskReportSchema.java")));
        String structuredOutputs = Files.readString(root.resolve("ai/juridica/spine/JuridicaStructuredOutputProfileService.java"));
        String assembler = Files.readString(root.resolve("ai/juridica/conversation/LegalAiConversationContextAssemblerService.java"));
        assertTrue(structuredOutputs.contains("structuredSchemaCatalog.resolve(effectiveVersion)"));
        assertTrue(assembler.contains("juridicaStructuredSchemaCatalog"));
        assertTrue(assembler.contains("juridicaRecommendedSchema"));
    }
}
