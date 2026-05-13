package com.tcc.pjb.backend.ai.juridica.policy.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeJsonResourceLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LegalAiPolicyTextCatalogService {

    private final JsonNode root;
    private final Set<String> highStakesCapabilities;
    private final Set<String> readHeavyCapabilities;
    private final Set<String> strategicProtocolCapabilities;
    private final Set<String> strategicDocumentHeavyCapabilities;
    private final Set<String> strategicStrictProcedures;

    public LegalAiPolicyTextCatalogService(LegalKnowledgeJsonResourceLoader resourceLoader) {
        Objects.requireNonNull(resourceLoader, "resourceLoader");
        this.root = resourceLoader.readTree(LegalAiPolicyResourcePaths.TEXT_CATALOG);
        this.highStakesCapabilities = loadSet("capabilities", "highStakes", List.of(
                "PETICAO_INICIAL_ASSISTIDA",
                "PETICAO_ASSISTIDA",
                "DRAFT_PETITION",
                "ANALISE_COMPETENCIA",
                "ANALISE_PREVIDENCIARIA",
                "SENTENCA_ASSISTIDA",
                "PARECER_JURIDICO",
                "PROTOCOLO_ASSISTIDO"));
        this.readHeavyCapabilities = loadSet("capabilities", "readHeavy", List.of(
                "CONSULTA_JURIDICA_V1",
                "CONSULTA_JURIDICA_V2",
                "CONSULTA_JURIDICA_V3",
                "LEGAL_GENERAL_ASSIST_V3",
                "PETICAO_ASSISTIDA",
                "DRAFT_PETITION"));
        this.strategicProtocolCapabilities = loadSet("strategicExecution", "capabilities", "protocol", List.of(
                "PROTOCOLO_ASSISTIDO",
                "PETICAO_ASSISTIDA",
                "PETICAO_INICIAL_ASSISTIDA",
                "DRAFT_PETITION"));
        this.strategicDocumentHeavyCapabilities = loadSet("strategicExecution", "capabilities", "documentHeavy", List.of(
                "PETICAO_ASSISTIDA",
                "PETICAO_INICIAL_ASSISTIDA",
                "DRAFT_PETITION",
                "PROTOCOLO_ASSISTIDO",
                "ANALISE_COMPETENCIA",
                "ANALISE_PREVIDENCIARIA",
                "LEGAL_GENERAL_ASSIST_V3"));
        this.strategicStrictProcedures = loadSet("strategicExecution", "capabilities", "strictProcedures", List.of(
                "MANDADO_DE_SEGURANCA",
                "CUSTODIA",
                "TRIBUNAL_DO_JURI",
                "EXECUCAO_FISCAL",
                "BENEFICIO_PREVIDENCIARIO",
                "HABEAS_CORPUS",
                "AIJE",
                "AIRC",
                "AIME"));
    }

    public boolean isHighStakesCapability(String capability) {
        String normalized = normalize(capability);
        return !normalized.isEmpty() && highStakesCapabilities.contains(normalized);
    }

    public boolean isReadHeavyCapability(String capability) {
        String normalized = normalize(capability);
        return !normalized.isEmpty() && readHeavyCapabilities.contains(normalized);
    }

    public boolean isStrategicProtocolCapability(String capability) {
        String normalized = normalize(capability);
        return !normalized.isEmpty() && strategicProtocolCapabilities.contains(normalized);
    }

    public boolean isStrategicDocumentHeavyCapability(String capability) {
        String normalized = normalize(capability);
        return !normalized.isEmpty() && strategicDocumentHeavyCapabilities.contains(normalized);
    }

    public boolean isStrategicStrictProcedure(String procedureFamily) {
        String normalized = normalize(procedureFamily);
        return !normalized.isEmpty() && strategicStrictProcedures.contains(normalized);
    }

    public String ragProfileStrictNoMcp() { return text("ragFusion", "profiles", "strictNoMcp", "JURIDICA_RAG_STRICT_NO_MCP_V3"); }
    public String ragProfileLocalOnlySuffix() { return text("ragFusion", "profiles", "localOnlySuffix", "_RAG_LOCAL_ONLY"); }
    public String ragProfileStagedStrict() { return text("ragFusion", "profiles", "stagedStrict", "JURIDICA_STAGED_RAG_MCP_STRICT_V3"); }
    public String ragProfileHybridComplex() { return text("ragFusion", "profiles", "hybridComplex", "JURIDICA_HYBRID_RAG_MCP_COMPLEX_V3"); }
    public String ragProfileHybridBalanced() { return text("ragFusion", "profiles", "hybridBalanced", "JURIDICA_HYBRID_RAG_MCP_BALANCED_V2"); }

    public String executionModeLocked() { return text("ragFusion", "executionMode", "locked", "RAG_ONLY_LOCKED"); }
    public String executionModeLocal() { return text("ragFusion", "executionMode", "local", "RAG_ONLY_LOCAL"); }
    public String executionModeStagedReadOnly() { return text("ragFusion", "executionMode", "stagedReadOnly", "STAGED_RAG_THEN_MCP_READONLY"); }
    public String executionModeHybridReadOnly() { return text("ragFusion", "executionMode", "hybridReadOnly", "HYBRID_PARALLEL_RAG_MCP_READONLY"); }
    public String executionModeDeferredDiscovery() { return text("ragFusion", "executionMode", "deferredDiscovery", "RAG_WITH_DEFERRED_MCP_DISCOVERY"); }

    public String reasoningEffortXhigh() { return text("ragFusion", "reasoningEffort", "xhigh", "xhigh"); }
    public String reasoningEffortHigh() { return text("ragFusion", "reasoningEffort", "high", "high"); }
    public String reasoningEffortMedium() { return text("ragFusion", "reasoningEffort", "medium", "medium"); }
    public String reasoningEffortLow() { return text("ragFusion", "reasoningEffort", "low", "low"); }

    public String promptCacheRetentionLong() { return text("ragFusion", "promptCacheRetention", "longWindow", "24h"); }
    public String promptCacheRetentionSession() { return text("ragFusion", "promptCacheRetention", "session", "session"); }

    public List<String> connectorBaseFamilies() { return list("ragFusion", "connectorFamilies", "base", List.of("PJB_INTERNAL_JURISPRUDENCE", "PJB_CURRICULUM", "PJB_PETITION_BLUEPRINTS", "PJB_COMPETENCE_MATRIX")); }
    public List<String> connectorRemoteFamilies() { return list("ragFusion", "connectorFamilies", "remote", List.of("MCP_REMOTE_STORAGE_READONLY", "MCP_REMOTE_COLLAB_READONLY", "MCP_REMOTE_OFFICE_KNOWLEDGE_READONLY")); }
    public String connectorProtocolGuard() { return text("ragFusion", "connectorFamilies", "protocolGuard", "PJB_PROTOCOL_GUARD"); }
    public String connectorCalculator() { return text("ragFusion", "connectorFamilies", "calculator", "PJB_CALCULATOR"); }
    public String connectorFederalRecords() { return text("ragFusion", "connectorFamilies", "federalRecords", "PJB_FEDERAL_RECORDS"); }
    public String connectorCriminalLane() { return text("ragFusion", "connectorFamilies", "criminalLane", "PJB_CRIMINAL_LANE"); }
    public String connectorElectoralLane() { return text("ragFusion", "connectorFamilies", "electoralLane", "PJB_ELECTORAL_LANE"); }
    public String connectorLaborLane() { return text("ragFusion", "connectorFamilies", "laborLane", "PJB_LABOR_LANE"); }
    public String connectorEnvironmentalLane() { return text("ragFusion", "connectorFamilies", "environmentalLane", "PJB_ENVIRONMENTAL_LANE"); }
    public String connectorBusinessLane() { return text("ragFusion", "connectorFamilies", "businessLane", "PJB_BUSINESS_LANE"); }

    public List<String> allowedToolClassBase() { return list("ragFusion", "allowedToolClasses", "base", List.of("READ_ONLY_SEARCH", "READ_ONLY_RETRIEVAL", "READ_ONLY_LEGAL_LOOKUP")); }
    public String allowedToolClassProtocolPrecheck() { return text("ragFusion", "allowedToolClasses", "protocolPrecheck", "READ_ONLY_PROTOCOL_PRECHECK"); }
    public String allowedToolClassCompetenceRouting() { return text("ragFusion", "allowedToolClasses", "competenceRouting", "READ_ONLY_COMPETENCE_ROUTING"); }
    public String allowedToolClassCalculatorLookup() { return text("ragFusion", "allowedToolClasses", "calculatorLookup", "READ_ONLY_CALCULATOR_LOOKUP"); }

    public List<String> evidenceLaneBase() { return list("ragFusion", "evidenceLanes", "base", List.of("VECTOR_JURISPRUDENCE", "CURRICULUM", "LEGAL_KNOWLEDGE_BASE")); }
    public String evidenceLanePetitionBlueprints() { return text("ragFusion", "evidenceLanes", "petitionBlueprints", "PETITION_BLUEPRINTS"); }
    public String evidenceLaneToolSearch() { return text("ragFusion", "evidenceLanes", "toolSearch", "MCP_TOOL_SEARCH"); }
    public String evidenceLaneRemoteReadonly() { return text("ragFusion", "evidenceLanes", "remoteReadonly", "MCP_REMOTE_READONLY"); }

    public List<String> verifierCheckBase() { return list("ragFusion", "verifierChecks", "base", List.of("SOURCE_AUTHORITY_ALIGNMENT", "PROCEDURE_FAMILY_ALIGNMENT", "CITATION_GROUNDING")); }
    public String verifierCheckContradictionResolution() { return text("ragFusion", "verifierChecks", "contradictionResolution", "CONTRADICTION_RESOLUTION"); }
    public String verifierCheckPromptInjectionFence() { return text("ragFusion", "verifierChecks", "promptInjectionFence", "PROMPT_INJECTION_FENCE"); }
    public String verifierCheckPetitionStructure() { return text("ragFusion", "verifierChecks", "petitionStructure", "PETITION_STRUCTURE_COMPLETENESS"); }
    public String verifierCheckProtocolPrecheckAlignment() { return text("ragFusion", "verifierChecks", "protocolPrecheckAlignment", "PROTOCOL_PRECHECK_ALIGNMENT"); }
    public String verifierCheckCompetenceRoutingAlignment() { return text("ragFusion", "verifierChecks", "competenceRoutingAlignment", "COMPETENCE_ROUTING_ALIGNMENT"); }

    public String approvalModeOff() { return text("ragFusion", "approvalModes", "off", "OFF"); }
    public String approvalModeAlways() { return text("ragFusion", "approvalModes", "always", "ALWAYS"); }
    public String approvalModeReadOnlyAuto() { return text("ragFusion", "approvalModes", "readOnlyAuto", "READ_ONLY_AUTO"); }

    public String sessionModeLocalState() { return text("ragFusion", "sessionModes", "localState", "LOCAL_STATE"); }
    public String sessionModePinnedServer() { return text("ragFusion", "sessionModes", "pinnedServer", "PINNED_SERVER_SESSION"); }
    public String sessionModeDeferredServer() { return text("ragFusion", "sessionModes", "deferredServer", "DEFERRED_SERVER_SESSION"); }

    public List<String> precedentWindowBase() { return list("ragFusion", "precedentWindows", "base", List.of("STF_RECENT_BINDING", "STJ_REPETITIVOS")); }
    public String precedentWindowLabor() { return text("ragFusion", "precedentWindows", "labor", "TST_IRR_E_SUMULAS"); }
    public String precedentWindowElectoral() { return text("ragFusion", "precedentWindows", "electoral", "TSE_PRECEDENTS"); }
    public String precedentWindowMilitary() { return text("ragFusion", "precedentWindows", "military", "STM_PRECEDENTS"); }
    public String precedentWindowCriminal() { return text("ragFusion", "precedentWindows", "criminal", "STJ_PENAL_AND_STF_CRIMINAL"); }
    public String precedentWindowFederal() { return text("ragFusion", "precedentWindows", "federal", "TRF_AND_TNU_WINDOWS"); }

    public String generalPromptCacheProfile() { return text("ragFusion", "defaults", "generalPromptCacheProfile", "LEGAL_GENERAL"); }
    public String decisionReasonCapabilityPrefix() { return text("ragFusion", "decisionReasons", "capabilityPrefix", "capability="); }
    public String decisionReasonStrictGuardrails() { return text("ragFusion", "decisionReasons", "strictGuardrails", "strict_guardrails_enabled"); }
    public String decisionReasonPetitionGroundedPipeline() { return text("ragFusion", "decisionReasons", "petitionGroundedPipeline", "petition_flow_requires_grounded_pipeline"); }
    public String decisionReasonComplexMultilane() { return text("ragFusion", "decisionReasons", "complexMultilane", "complex_case_requires_multilane_retrieval"); }
    public String decisionReasonReadonlyMcpAllowed() { return text("ragFusion", "decisionReasons", "readonlyMcpAllowed", "readonly_mcp_allowed"); }
    public String decisionReasonToolSearchEnabled() { return text("ragFusion", "decisionReasons", "toolSearchEnabled", "tool_search_enabled_for_connector_discovery"); }
    public String decisionReasonLocalOnlyFallback() { return text("ragFusion", "decisionReasons", "localOnlyFallback", "local_only_fallback"); }

    public String governancePolicyVersionV3() { return text("adaptiveGovernance", "governance", "policyVersionV3", "JURIDICA_ADAPTIVE_MESH_GOVERNANCE_V3"); }
    public String governancePolicyVersionV4() { return text("adaptiveGovernance", "governance", "policyVersionV4", "JURIDICA_ADAPTIVE_MESH_GOVERNANCE_V4"); }
    public String knowledgeCadenceProfileV2() { return text("adaptiveGovernance", "knowledgeCadence", "profileV2", "LEGAL_KNOWLEDGE_CADENCE_V2"); }
    public String activationModeEditDraftProtocol() { return text("adaptiveGovernance", "knowledgeCadence", "activationModeEditDraftProtocol", "OPEN_EDIT_DRAFT_PROTOCOL"); }
    public String activationModeQueryDraft() { return text("adaptiveGovernance", "knowledgeCadence", "activationModeQueryDraft", "OPEN_QUERY_DRAFT"); }

    public List<String> sourceScopePetitionSources() { return list("adaptiveGovernance", "sourceScope", "petitionSources", List.of("VECTOR_JURISPRUDENCE", "CURRICULUM", "BRAZILIAN_LEGAL_KNOWLEDGE_BASE", "PETITION_BLUEPRINT_CATALOG")); }
    public List<String> sourceScopeQuerySources() { return list("adaptiveGovernance", "sourceScope", "querySources", List.of("VECTOR_JURISPRUDENCE", "CURRICULUM", "BRAZILIAN_LEGAL_KNOWLEDGE_BASE")); }

    public String adaptiveDecisionReasonCapabilityPrefix() { return text("adaptiveGovernance", "decisionReasons", "capabilityPrefix", "capability="); }
    public String adaptiveDecisionReasonEffectiveVersionPrefix() { return text("adaptiveGovernance", "decisionReasons", "effectiveVersionPrefix", "effectiveVersion="); }
    public String adaptiveDecisionReasonComplexityPrefix() { return text("adaptiveGovernance", "decisionReasons", "complexityPrefix", "complexity="); }
    public String adaptiveDecisionReasonInjectionRiskPrefix() { return text("adaptiveGovernance", "decisionReasons", "injectionRiskPrefix", "injectionRisk="); }
    public String adaptiveDecisionReasonPetitionDetected() { return text("adaptiveGovernance", "decisionReasons", "petitionDetected", "petition_detected"); }
    public String adaptiveDecisionReasonSigiloSensitive() { return text("adaptiveGovernance", "decisionReasons", "sigiloSensitive", "sigilo_sensitive_context"); }
    public String adaptiveDecisionReasonComplexCaseStrictRag() { return text("adaptiveGovernance", "decisionReasons", "complexCaseStrictRag", "complex_case_requires_strict_rag"); }
    public String adaptiveDecisionReasonExternalToolingLocked() { return text("adaptiveGovernance", "decisionReasons", "externalToolingLocked", "external_tooling_locked_by_risk"); }

    public String ragProfileStrictMultistage() { return text("adaptiveGovernance", "ragPolicy", "strictProfile", "RAG_STRICT_MULTISTAGE_V3"); }
    public String ragProfileBalancedSuffix() { return text("adaptiveGovernance", "ragPolicy", "balancedSuffix", "_BALANCED_RAG"); }
    public String distillationStrictProfile() { return text("adaptiveGovernance", "ragPolicy", "strictDistillation", "SEMANTIC_SOURCE_DISTILLATION_STRICT_V3"); }
    public String distillationBalancedProfile() { return text("adaptiveGovernance", "ragPolicy", "balancedDistillation", "SEMANTIC_SOURCE_DISTILLATION_BALANCED_V2"); }

    public String toolModeDisabledByRisk() { return text("adaptiveGovernance", "toolPolicy", "disabledByRisk", "DISABLED_BY_RISK"); }
    public String toolModeReadOnlyGuarded() { return text("adaptiveGovernance", "toolPolicy", "readOnlyGuarded", "READ_ONLY_GUARDED"); }
    public String toolModeLocalOnly() { return text("adaptiveGovernance", "toolPolicy", "localOnly", "LOCAL_ONLY"); }
    public String toolModeAuto() { return text("adaptiveGovernance", "toolPolicy", "auto", "AUTO"); }

    public String strategyVersion() { return text("strategicExecution", "versions", "strategyVersion", "JURIDICA_STRATEGIC_EXECUTION_V1"); }

    public String strategicProfileProtocolDocHeavyStrictV4() { return text("strategicExecution", "profiles", "protocolDocHeavyStrictV4", "LEGAL_PROTOCOL_AND_DOC_HEAVY_STRICT_V4"); }
    public String strategicProfileProtocolGuardedV4() { return text("strategicExecution", "profiles", "protocolGuardedV4", "LEGAL_PROTOCOL_GUARDED_V4"); }
    public String strategicProfileBatchDocumentIntensiveV4() { return text("strategicExecution", "profiles", "batchDocumentIntensiveV4", "LEGAL_BATCH_DOCUMENT_INTENSIVE_V4"); }
    public String strategicProfileDocumentStrictExecutionV4() { return text("strategicExecution", "profiles", "documentStrictExecutionV4", "LEGAL_DOCUMENT_STRICT_EXECUTION_V4"); }
    public String strategicProfileBatchReadStrictV3() { return text("strategicExecution", "profiles", "batchReadStrictV3", "LEGAL_BATCH_READ_STRICT_V3"); }
    public String strategicProfilePetitionExecutionV3() { return text("strategicExecution", "profiles", "petitionExecutionV3", "LEGAL_PETITION_EXECUTION_V3"); }
    public String strategicProfileBalancedStrictExecutionV3() { return text("strategicExecution", "profiles", "balancedStrictExecutionV3", "LEGAL_BALANCED_STRICT_EXECUTION_V3"); }
    public String strategicProfileBalancedExecutionV2() { return text("strategicExecution", "profiles", "balancedExecutionV2", "LEGAL_BALANCED_EXECUTION_V2"); }

    public String strategicIngestionProtocolStagedBatch() { return text("strategicExecution", "ingestion", "modes", "protocolStagedBatch", "PROTOCOL_STAGED_BATCH_INGESTION"); }
    public String strategicIngestionHierarchicalBatch() { return text("strategicExecution", "ingestion", "modes", "hierarchicalBatch", "HIERARCHICAL_BATCH_INGESTION"); }
    public String strategicIngestionStrictStagedDocument() { return text("strategicExecution", "ingestion", "modes", "strictStagedDocument", "STRICT_STAGED_DOCUMENT_INGESTION"); }
    public String strategicIngestionParallelBatchWindowed() { return text("strategicExecution", "ingestion", "modes", "parallelBatchWindowed", "PARALLEL_BATCH_WINDOWED_INGESTION"); }
    public String strategicIngestionFocusedSingleDocument() { return text("strategicExecution", "ingestion", "modes", "focusedSingleDocument", "FOCUSED_SINGLE_DOCUMENT_INGESTION"); }
    public String strategicOcrPolicyStrict() { return text("strategicExecution", "ingestion", "ocrPolicy", "strict", "PDF_TEXT_FIRST_OCR_LAST_RESORT"); }
    public String strategicOcrPolicyAssisted() { return text("strategicExecution", "ingestion", "ocrPolicy", "assisted", "PDF_TEXT_FIRST_ASSISTED_OCR"); }
    public String strategicDedupePolicySemantic() { return text("strategicExecution", "ingestion", "dedupePolicy", "semantic", "DOC_HASH_AND_SEMANTIC_DEDUPE_V2"); }
    public String strategicDedupePolicyHashOnly() { return text("strategicExecution", "ingestion", "dedupePolicy", "hashOnly", "DOC_HASH_ONLY"); }
    public String strategicLineageEnvelopeMandatoryProtocol() { return text("strategicExecution", "ingestion", "lineageEnvelope", "mandatoryProtocol", "MANDATORY_PROTOCOL_SOURCE_LINEAGE"); }
    public String strategicLineageEnvelopeStandard() { return text("strategicExecution", "ingestion", "lineageEnvelope", "standard", "STANDARD_SOURCE_LINEAGE"); }

    public String strategicBatchModeSinglePass() { return text("strategicExecution", "planner", "batchModes", "singlePass", "SINGLE_PASS"); }
    public String strategicBatchModeMultiPassHierarchical() { return text("strategicExecution", "planner", "batchModes", "multiPassHierarchical", "MULTI_PASS_HIERARCHICAL"); }
    public String strategicBatchModeMultiPassStrict() { return text("strategicExecution", "planner", "batchModes", "multiPassStrict", "MULTI_PASS_STRICT"); }
    public String strategicBatchModeBalancedMultiPass() { return text("strategicExecution", "planner", "batchModes", "balancedMultiPass", "BALANCED_MULTI_PASS"); }
    public String strategicToolPrefetchDeferred() { return text("strategicExecution", "planner", "toolPrefetch", "deferred", "DEFERRED"); }
    public String strategicToolPrefetchPinnedReadOnly() { return text("strategicExecution", "planner", "toolPrefetch", "pinnedReadOnly", "PINNED_READ_ONLY"); }
    public String strategicToolPrefetchOnDemand() { return text("strategicExecution", "planner", "toolPrefetch", "onDemand", "ON_DEMAND"); }
    public String strategicPromptCompressionStrict() { return text("strategicExecution", "planner", "promptCompression", "strict", "LEGAL_CONTEXT_COMPRESSION_STRICT_V3"); }
    public String strategicPromptCompressionBalanced() { return text("strategicExecution", "planner", "promptCompression", "balanced", "LEGAL_CONTEXT_COMPRESSION_BALANCED_V2"); }
    public String strategicResultMergingHierarchicalAuthorityRerank() { return text("strategicExecution", "planner", "resultMerging", "hierarchicalAuthorityRerank", "HIERARCHICAL_MERGE_WITH_AUTHORITY_RERANK"); }
    public String strategicResultMergingBalancedRerank() { return text("strategicExecution", "planner", "resultMerging", "balancedRerank", "BALANCED_RERANK"); }

    public String strategicVerifierProtocolAbsolute() { return text("strategicExecution", "verifier", "modes", "protocolAbsolute", "PROTOCOL_VERIFIER_ABSOLUTE"); }
    public String strategicVerifierProtocolStrict() { return text("strategicExecution", "verifier", "modes", "protocolStrict", "PROTOCOL_VERIFIER_STRICT"); }
    public String strategicVerifierStrict() { return text("strategicExecution", "verifier", "modes", "strict", "LEGAL_VERIFIER_STRICT"); }
    public String strategicVerifierBalanced() { return text("strategicExecution", "verifier", "modes", "balanced", "LEGAL_VERIFIER_BALANCED"); }
    public String strategicCitationTrailMandatory() { return text("strategicExecution", "verifier", "citationTrail", "mandatory", "MANDATORY"); }
    public String strategicCitationTrailStrongPreference() { return text("strategicExecution", "verifier", "citationTrail", "strongPreference", "STRONG_PREFERENCE"); }
    public String strategicChecklistModePreProtocolBlocking() { return text("strategicExecution", "verifier", "checklistMode", "preProtocolBlocking", "PRE_PROTOCOL_BLOCKING"); }
    public String strategicChecklistModeNonBlockingReview() { return text("strategicExecution", "verifier", "checklistMode", "nonBlockingReview", "NON_BLOCKING_REVIEW"); }
    public String strategicContradictionToleranceMinimal() { return text("strategicExecution", "verifier", "contradictionTolerance", "minimal", "MINIMAL"); }
    public String strategicContradictionToleranceLow() { return text("strategicExecution", "verifier", "contradictionTolerance", "low", "LOW"); }
    public String strategicContradictionToleranceBalanced() { return text("strategicExecution", "verifier", "contradictionTolerance", "balanced", "BALANCED"); }
    public String strategicVerifierSignatureProtocol() { return text("strategicExecution", "verifier", "signature", "protocol", "LEGAL_VERIFIER_PROTOCOL_V3"); }
    public String strategicVerifierSignatureExecution() { return text("strategicExecution", "verifier", "signature", "execution", "LEGAL_VERIFIER_EXECUTION_V2"); }

    public String strategicProtocolStageFinalProtocolOrPreProtocol() { return text("strategicExecution", "protocol", "stages", "finalProtocolOrPreProtocol", "FINAL_PROTOCOL_OR_PRE_PROTOCOL"); }
    public String strategicProtocolStageDraftProtocolPrep() { return text("strategicExecution", "protocol", "stages", "draftProtocolPrep", "DRAFT_PROTOCOL_PREP"); }
    public String strategicProtocolStageQueryOnly() { return text("strategicExecution", "protocol", "stages", "queryOnly", "QUERY_ONLY"); }
    public String strategicPackageStrategySignatureEnvelopeProtocol() { return text("strategicExecution", "protocol", "packageStrategy", "signatureEnvelopeProtocol", "SIGNATURE_ENVELOPE_AND_PROTOCOL_PACKAGE"); }
    public String strategicPackageStrategyDraftOnly() { return text("strategicExecution", "protocol", "packageStrategy", "draftOnly", "DRAFT_PACKAGE_ONLY"); }
    public String strategicPostProtocolReplayDistributionReadySnapshot() { return text("strategicExecution", "protocol", "postProtocolReplay", "distributionReadySnapshot", "DISTRIBUTION_READY_SNAPSHOT"); }
    public String strategicPostProtocolReplayOptional() { return text("strategicExecution", "protocol", "postProtocolReplay", "optional", "OPTIONAL"); }

    public String strategicCacheModeProtocolWorkspaceCheckpoint() { return text("strategicExecution", "cache", "modes", "protocolWorkspaceCheckpoint", "PROTOCOL_WORKSPACE_CHECKPOINT_CACHE"); }
    public String strategicCacheModeBatchExecutionCheckpoint() { return text("strategicExecution", "cache", "modes", "batchExecutionCheckpoint", "BATCH_EXECUTION_CHECKPOINT_CACHE"); }
    public String strategicCacheModePetitionSession() { return text("strategicExecution", "cache", "modes", "petitionSession", "PETITION_SESSION_CACHE"); }
    public String strategicCacheModeQueryPrefix() { return text("strategicExecution", "cache", "modes", "queryPrefix", "QUERY_PREFIX_CACHE"); }
    public String strategicPrefixProfileProtocol() { return text("strategicExecution", "cache", "prefixProfile", "protocol", "LEGAL_PROTOCOL_PREFIX_CACHE"); }
    public String strategicPrefixProfilePetition() { return text("strategicExecution", "cache", "prefixProfile", "petition", "LEGAL_PETITION_PREFIX_CACHE"); }
    public String strategicPrefixProfileQuery() { return text("strategicExecution", "cache", "prefixProfile", "query", "LEGAL_QUERY_PREFIX_CACHE"); }
    public String strategicCheckpointingMultiStage() { return text("strategicExecution", "cache", "checkpointing", "multiStage", "MULTI_STAGE_CHECKPOINTING"); }
    public String strategicCheckpointingSingleStage() { return text("strategicExecution", "cache", "checkpointing", "singleStage", "SINGLE_STAGE_CHECKPOINTING"); }
    public String strategicReplayEnvelopeProtocolAudit() { return text("strategicExecution", "cache", "replayEnvelope", "protocolAudit", "PROTOCOL_AUDIT_REPLAY"); }
    public String strategicReplayEnvelopeExecution() { return text("strategicExecution", "cache", "replayEnvelope", "execution", "LEGAL_EXECUTION_REPLAY"); }

    public List<String> strategicAuthorityLaneBase() { return list("strategicExecution", "authorityLanes", "base", List.of("LEGISLACAO_VIGENTE", "JURISPRUDENCIA_QUALIFICADA")); }
    public List<String> strategicAuthorityLaneStrictBase() { return list("strategicExecution", "authorityLanes", "strict", List.of("TRIBUNAIS_SUPERIORES", "REGRAS_PROCEDIMENTAIS_CANONICAS")); }
    public String strategicAuthorityLaneElectoral() { return text("strategicExecution", "authorityLanes", "electoral", "TSE_E_TRE"); }
    public String strategicAuthorityLaneLabor() { return text("strategicExecution", "authorityLanes", "labor", "TST_E_TRT"); }
    public String strategicAuthorityLaneFederal() { return text("strategicExecution", "authorityLanes", "federal", "TRF_E_TNU"); }
    public String strategicAuthorityLaneMilitary() { return text("strategicExecution", "authorityLanes", "military", "STM_E_TJM"); }

    public List<String> strategicMandatoryCheckBase() { return list("strategicExecution", "mandatoryChecks", "base", List.of("COERENCIA_FATOS_PEDIDOS", "COMPETENCIA_E_FORO", "ADEQUACAO_DO_RITO")); }
    public String strategicMandatoryCheckDocumentChecklist() { return text("strategicExecution", "mandatoryChecks", "documentChecklist", "CHECKLIST_DOCUMENTAL"); }
    public String strategicMandatoryCheckCitationAuthorityTrail() { return text("strategicExecution", "mandatoryChecks", "citationAuthorityTrail", "TRILHA_DE_CITACOES_E_AUTORIDADES"); }
    public String strategicMandatoryCheckRepresentationCapacity() { return text("strategicExecution", "mandatoryChecks", "representationCapacity", "REPRESENTACAO_E_CAPACIDADE_POSTULATORIA"); }
    public String strategicMandatoryCheckProtocolSignaturePackage() { return text("strategicExecution", "mandatoryChecks", "protocolSignaturePackage", "PACOTE_DE_PROTOCOLO_E_ASSINATURA"); }
    public String strategicMandatoryCheckFamilyUrgency() { return text("strategicExecution", "mandatoryChecks", "familyUrgency", "MELHOR_INTERESSE_E_URGENCIA"); }
    public String strategicMandatoryCheckCriminalCustody() { return text("strategicExecution", "mandatoryChecks", "criminalCustody", "LIBERDADE_E_MEDIDA_CAUTELAR"); }
    public String strategicMandatoryCheckTaxExecution() { return text("strategicExecution", "mandatoryChecks", "taxExecution", "CDA_PRESCRICAO_E_COMPETENCIA_FAZENDARIA"); }
    public String strategicMandatoryCheckSocialSecurity() { return text("strategicExecution", "mandatoryChecks", "socialSecurity", "CARACTERIZACAO_DO_BENEFICIO_E_PROVA_MINIMA"); }
    public String strategicMandatoryCheckElectoralTimeliness() { return text("strategicExecution", "mandatoryChecks", "electoralTimeliness", "TEMPESTIVIDADE_E_JANELA_ELEITORAL"); }

    public String strategicQueryHintProtocolCompetence() { return text("strategicExecution", "queryHints", "protocolCompetence", "competencia_prevenção_redistribuição_protocolo"); }
    public String strategicQueryHintProtocolChecklist() { return text("strategicExecution", "queryHints", "protocolChecklist", "checklist_documental_e_representacao"); }
    public String strategicQueryHintDocumentSummary() { return text("strategicExecution", "queryHints", "documentSummary", "sumario_hierarquico_dos_anexos"); }
    public String strategicQueryHintDocumentContradiction() { return text("strategicExecution", "queryHints", "documentContradiction", "contradições_entre_documentos"); }

    public String strategicReadingGoalPetitionWriting() { return text("strategicExecution", "readingGoals", "petitionWriting", "redacao_juridica_e_estrutura_da_peca"); }
    public String strategicReadingGoalProtocolFinalCheck() { return text("strategicExecution", "readingGoals", "protocolFinalCheck", "verificacao_final_de_protocolo_e_distribuicao"); }

    public String strategicProtocolGatePartesRepresentacao() { return text("strategicExecution", "protocolGates", "partesRepresentacao", "PARTES_E_REPRESENTACAO"); }
    public String strategicProtocolGateCompetenciaForoUnidade() { return text("strategicExecution", "protocolGates", "competenciaForoUnidade", "COMPETENCIA_FORO_UNIDADE"); }
    public String strategicProtocolGateDocumentosEssenciais() { return text("strategicExecution", "protocolGates", "documentosEssenciais", "DOCUMENTOS_ESSENCIAIS"); }
    public String strategicProtocolGateIdentidadeInstitucional() { return text("strategicExecution", "protocolGates", "identidadeInstitucional", "IDENTIDADE_INSTITUCIONAL_E_CAPACIDADE"); }
    public String strategicProtocolGateLiberdadeIntegridade() { return text("strategicExecution", "protocolGates", "liberdadeIntegridade", "LIBERDADE_E_INTEGRIDADE_DA_MEDIDA"); }
    public String strategicProtocolGateJanelaProcessual() { return text("strategicExecution", "protocolGates", "janelaProcessual", "JANELA_PROCESSUAL_E_TEMPESTIVIDADE"); }
    public String strategicProtocolGatePrevidenciarioProvaMinima() { return text("strategicExecution", "protocolGates", "previdenciarioProvaMinima", "PROVA_MINIMA_E_DADOS_CONTRIBUTIVOS"); }

    public String strategicDangerSignalStrictContext() { return text("strategicExecution", "dangerSignals", "strictContext", "STRICT_CONTEXT"); }
    public String strategicDangerSignalFinalProtocolStage() { return text("strategicExecution", "dangerSignals", "finalProtocolStage", "FINAL_PROTOCOL_STAGE"); }
    public String strategicDangerSignalHeavyDocumentContext() { return text("strategicExecution", "dangerSignals", "heavyDocumentContext", "HEAVY_DOCUMENT_CONTEXT"); }
    public String strategicDangerSignalBatchReadingActive() { return text("strategicExecution", "dangerSignals", "batchReadingActive", "BATCH_READING_ACTIVE"); }
    public String strategicDangerSignalPromptToolInjectionRisk() { return text("strategicExecution", "dangerSignals", "promptToolInjectionRisk", "PROMPT_OR_TOOL_INJECTION_RISK"); }
    public String strategicDangerSignalSigiloRestricted() { return text("strategicExecution", "dangerSignals", "sigiloRestricted", "SIGILO_OR_RESTRICTED_CONTEXT"); }
    public String strategicDangerSignalAuthorityFloorUpgraded() { return text("strategicExecution", "dangerSignals", "authorityFloorUpgraded", "AUTHORITY_FLOOR_UPGRADED"); }

    public String strategicConnectorPriorityBatchDocumentMemory() { return text("strategicExecution", "connectorPriority", "batchDocumentMemory", "BATCH_DOCUMENT_MEMORY"); }
    public String strategicConnectorPriorityLockedReadOnlyConnectors() { return text("strategicExecution", "connectorPriority", "lockedReadOnlyConnectors", "LOCKED_READ_ONLY_CONNECTORS"); }

    public String strategicDecisionReasonStrategyProfilePrefix() { return text("strategicExecution", "decisionReasons", "strategyProfilePrefix", "strategyProfile="); }
    public String strategicDecisionReasonVerifierModePrefix() { return text("strategicExecution", "decisionReasons", "verifierModePrefix", "verifierMode="); }
    public String strategicDecisionReasonComplexityPrefix() { return text("strategicExecution", "decisionReasons", "complexityPrefix", "complexity="); }
    public String strategicDecisionReasonInjectionRiskPrefix() { return text("strategicExecution", "decisionReasons", "injectionRiskPrefix", "injectionRisk="); }
    public String strategicDecisionReasonStrictContextEnabled() { return text("strategicExecution", "decisionReasons", "strictContextEnabled", "strict_context_enabled"); }
    public String strategicDecisionReasonVeryStrictFloor() { return text("strategicExecution", "decisionReasons", "veryStrictFloor", "very_strict_floor"); }
    public String strategicDecisionReasonDocumentHeavyContext() { return text("strategicExecution", "decisionReasons", "documentHeavyContext", "document_heavy_context"); }
    public String strategicDecisionReasonBatchReadEnabled() { return text("strategicExecution", "decisionReasons", "batchReadEnabled", "batch_read_enabled"); }
    public String strategicDecisionReasonProtocolStageEnabled() { return text("strategicExecution", "decisionReasons", "protocolStageEnabled", "protocol_stage_enabled"); }
    public String strategicDecisionReasonPetitionDetected() { return text("strategicExecution", "decisionReasons", "petitionDetected", "petition_detected"); }
    public String strategicDecisionReasonSigiloContext() { return text("strategicExecution", "decisionReasons", "sigiloContext", "sigilo_context"); }

    private Set<String> loadSet(String levelOne, String key, List<String> fallback) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : list(levelOne, key, fallback)) {
            String normalized = normalize(item);
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return Set.copyOf(values);
    }

    private Set<String> loadSet(String levelOne, String levelTwo, String key, List<String> fallback) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String item : list(levelOne, levelTwo, key, fallback)) {
            String normalized = normalize(item);
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return Set.copyOf(values);
    }

    private List<String> list(String levelOne, String levelTwo, String key, List<String> fallback) {
        return list(node(levelOne, levelTwo, key), fallback);
    }

    private List<String> list(String levelOne, String key, List<String> fallback) {
        return list(node(levelOne, key), fallback);
    }

    private List<String> list(JsonNode node, List<String> fallback) {
        ArrayList<String> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> {
                String value = item.asText();
                if (value != null && !value.isBlank()) {
                    values.add(value);
                }
            });
        }
        return values.isEmpty() ? List.copyOf(fallback) : List.copyOf(values);
    }

    private String text(String levelOne, String levelTwo, String levelThree, String key, String fallback) {
        String value = node(levelOne, levelTwo, levelThree, key).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String text(String levelOne, String levelTwo, String key, String fallback) {
        String value = node(levelOne, levelTwo, key).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private String text(String levelOne, String key, String fallback) {
        String value = node(levelOne, key).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private JsonNode node(String... path) {
        JsonNode current = root;
        for (String key : path) {
            current = current.path(key);
        }
        return current;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
