package com.tcc.pjb.backend.ai.juridica.policy;

import com.tcc.pjb.backend.ai.juridica.policy.support.LegalAiPolicyTextCatalogService;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class JuridicaStrategicExecutionService {

    private final LegalAiPolicyTextCatalogService policyTextCatalogService;

    public JuridicaStrategicExecutionService(LegalAiPolicyTextCatalogService policyTextCatalogService) {
        this.policyTextCatalogService = Objects.requireNonNull(policyTextCatalogService, "policyTextCatalogService");
    }

    public StrategyPlan resolve(ResolveRequest request) {
        Objects.requireNonNull(request, "request");

        String capability = normalize(request.capability());
        ApiVersion version = request.version() == null ? ApiVersion.latest() : request.version();
        String ramo = normalize(request.ramo());
        String rito = normalize(request.rito());
        String tipoJustica = normalize(request.tipoJustica());
        String procedureFamily = normalize(request.procedureFamily());
        String petitionModel = normalize(request.petitionModel());
        boolean petitionDetected = request.petitionDetected();
        boolean protocolStage = isProtocolStage(capability, request.payload());
        boolean sigilo = request.sigilo();
        boolean strict = sigilo || request.injectionRiskScore() >= 35 || protocolStage;
        boolean veryStrict = sigilo || request.injectionRiskScore() >= 50 || isStrictProcedure(procedureFamily) || capability.contains("SENTENCA");
        boolean highComplexity = request.complexityScore() >= 72;
        boolean institutional = request.tipoUsuario() != null && request.tipoUsuario().isInstitucional();
        int documentCount = documentCount(request.payload());
        int attachmentWeight = attachmentWeight(request.payload());
        int textWeight = estimateTextWeight(request.payload());
        boolean documentHeavy = documentCount >= 3 || attachmentWeight >= 16 || textWeight >= 18000 || policyTextCatalogService.isStrategicDocumentHeavyCapability(capability);
        boolean batchRead = documentCount >= 2 || attachmentWeight >= 10 || highComplexity;
        String strategyProfile = resolveStrategyProfile(version, strict, veryStrict, documentHeavy, batchRead, protocolStage, petitionDetected, highComplexity);
        String ingestionMode = resolveIngestionMode(strict, documentHeavy, batchRead, protocolStage, textWeight, attachmentWeight);
        String verifierMode = resolveVerifierMode(strict, veryStrict, protocolStage, petitionDetected, institutional, procedureFamily);
        String cacheMode = resolveCacheMode(documentHeavy, protocolStage, petitionDetected, highComplexity);
        String batchMode = resolveBatchMode(batchRead, strict, documentHeavy, highComplexity);
        int parallelLanes = resolveParallelLanes(documentHeavy, batchRead, strict, highComplexity, institutional);
        int queryBudget = resolveQueryBudget(version, documentHeavy, protocolStage, highComplexity, petitionDetected);
        int contradictionLoops = resolveContradictionLoops(strict, protocolStage, highComplexity);
        int pageBudget = resolvePageBudget(documentHeavy, highComplexity, protocolStage, attachmentWeight);
        int chunkChars = resolveChunkChars(strict, protocolStage, textWeight, batchRead);
        int overlapChars = Math.max(160, Math.min(520, chunkChars / 8));
        int summarizationWindow = resolveSummarizationWindow(strict, highComplexity, batchRead);
        List<String> authorityLanes = resolveAuthorityLanes(ramo, tipoJustica, procedureFamily, protocolStage, strict);
        List<String> mandatoryChecks = resolveMandatoryChecks(capability, ramo, tipoJustica, procedureFamily, protocolStage, petitionDetected, strict, institutional);
        List<String> queryHints = resolveQueryHints(ramo, rito, tipoJustica, procedureFamily, petitionModel, capability, request.knowledgeCadence(), request.fusionPlan(), protocolStage, documentHeavy);
        List<String> readingGoals = resolveReadingGoals(request.knowledgeCadence(), procedureFamily, petitionModel, ramo, protocolStage, petitionDetected);
        List<String> protocolGates = resolveProtocolGates(protocolStage, petitionDetected, strict, ramo, procedureFamily, institutional);
        List<String> dangerSignals = resolveDangerSignals(request, strict, protocolStage, documentHeavy, batchRead, authorityLanes);
        List<String> connectorPriority = resolveConnectorPriority(request.fusionPlan(), authorityLanes, batchRead, strict);

        LinkedHashMap<String, Object> ingestion = new LinkedHashMap<>();
        ingestion.put("mode", ingestionMode);
        ingestion.put("documentHeavy", documentHeavy);
        ingestion.put("batchRead", batchRead);
        ingestion.put("parallelLanes", parallelLanes);
        ingestion.put("pageBudget", pageBudget);
        ingestion.put("chunkChars", chunkChars);
        ingestion.put("chunkOverlapChars", overlapChars);
        ingestion.put("summarizationWindow", summarizationWindow);
        ingestion.put("attachmentWeight", attachmentWeight);
        ingestion.put("documentCount", documentCount);
        ingestion.put("textWeight", textWeight);
        ingestion.put("ocrPolicy", strict ? policyTextCatalogService.strategicOcrPolicyStrict() : policyTextCatalogService.strategicOcrPolicyAssisted());
        ingestion.put("dedupePolicy", documentHeavy ? policyTextCatalogService.strategicDedupePolicySemantic() : policyTextCatalogService.strategicDedupePolicyHashOnly());
        ingestion.put("sourceHashRequired", strict || protocolStage);
        ingestion.put("lineageEnvelope", protocolStage ? policyTextCatalogService.strategicLineageEnvelopeMandatoryProtocol() : policyTextCatalogService.strategicLineageEnvelopeStandard());

        LinkedHashMap<String, Object> planner = new LinkedHashMap<>();
        planner.put("queryBudget", queryBudget);
        planner.put("contradictionLoops", contradictionLoops);
        planner.put("preferLocalFirst", true);
        planner.put("enableSecondaryPass", documentHeavy || highComplexity || protocolStage);
        planner.put("batchMode", batchMode);
        planner.put("queryHints", queryHints);
        planner.put("readingGoals", readingGoals);
        planner.put("connectorPriority", connectorPriority);
        planner.put("toolPrefetch", strict ? policyTextCatalogService.strategicToolPrefetchDeferred() : (documentHeavy ? policyTextCatalogService.strategicToolPrefetchPinnedReadOnly() : policyTextCatalogService.strategicToolPrefetchOnDemand()));
        planner.put("promptCompression", strict ? policyTextCatalogService.strategicPromptCompressionStrict() : policyTextCatalogService.strategicPromptCompressionBalanced());
        planner.put("resultMerging", documentHeavy ? policyTextCatalogService.strategicResultMergingHierarchicalAuthorityRerank() : policyTextCatalogService.strategicResultMergingBalancedRerank());

        LinkedHashMap<String, Object> verifier = new LinkedHashMap<>();
        verifier.put("mode", verifierMode);
        verifier.put("mandatoryChecks", mandatoryChecks);
        verifier.put("authorityLanes", authorityLanes);
        verifier.put("dangerSignals", dangerSignals);
        verifier.put("citationTrail", protocolStage || petitionDetected ? policyTextCatalogService.strategicCitationTrailMandatory() : policyTextCatalogService.strategicCitationTrailStrongPreference());
        verifier.put("checklistMode", protocolStage ? policyTextCatalogService.strategicChecklistModePreProtocolBlocking() : policyTextCatalogService.strategicChecklistModeNonBlockingReview());
        verifier.put("contradictionTolerance", veryStrict ? policyTextCatalogService.strategicContradictionToleranceMinimal() : strict ? policyTextCatalogService.strategicContradictionToleranceLow() : policyTextCatalogService.strategicContradictionToleranceBalanced());
        verifier.put("humanReviewEscalation", protocolStage || sigilo || request.injectionRiskScore() >= 45);
        verifier.put("signature", protocolStage ? policyTextCatalogService.strategicVerifierSignatureProtocol() : policyTextCatalogService.strategicVerifierSignatureExecution());

        LinkedHashMap<String, Object> protocol = new LinkedHashMap<>();
        protocol.put("enabled", protocolStage || petitionDetected);
        protocol.put("stage", protocolStage ? policyTextCatalogService.strategicProtocolStageFinalProtocolOrPreProtocol() : petitionDetected ? policyTextCatalogService.strategicProtocolStageDraftProtocolPrep() : policyTextCatalogService.strategicProtocolStageQueryOnly());
        protocol.put("gates", protocolGates);
        protocol.put("requireRepresentationValidation", protocolStage || institutional);
        protocol.put("requireCompetenceValidation", protocolStage || petitionDetected);
        protocol.put("requireDocumentChecklist", petitionDetected || documentHeavy);
        protocol.put("requireFinalVerifier", protocolStage || veryStrict);
        protocol.put("packageStrategy", protocolStage ? policyTextCatalogService.strategicPackageStrategySignatureEnvelopeProtocol() : policyTextCatalogService.strategicPackageStrategyDraftOnly());
        protocol.put("postProtocolReplay", protocolStage ? policyTextCatalogService.strategicPostProtocolReplayDistributionReadySnapshot() : policyTextCatalogService.strategicPostProtocolReplayOptional());

        LinkedHashMap<String, Object> cache = new LinkedHashMap<>();
        cache.put("mode", cacheMode);
        cache.put("workspaceScoped", protocolStage || petitionDetected);
        cache.put("ttlSeconds", resolveCacheTtl(cacheMode, strict, highComplexity));
        cache.put("prefixProfile", protocolStage ? policyTextCatalogService.strategicPrefixProfileProtocol() : petitionDetected ? policyTextCatalogService.strategicPrefixProfilePetition() : policyTextCatalogService.strategicPrefixProfileQuery());
        cache.put("checkpointing", documentHeavy || batchRead ? policyTextCatalogService.strategicCheckpointingMultiStage() : policyTextCatalogService.strategicCheckpointingSingleStage());
        cache.put("replayEnvelope", protocolStage ? policyTextCatalogService.strategicReplayEnvelopeProtocolAudit() : policyTextCatalogService.strategicReplayEnvelopeExecution());

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("profile", strategyProfile);
        out.put("strategyVersion", policyTextCatalogService.strategyVersion());
        out.put("capability", capability);
        out.put("effectiveVersion", version.name());
        out.put("petitionDetected", petitionDetected);
        out.put("protocolStage", protocolStage);
        out.put("strict", strict);
        out.put("documentHeavy", documentHeavy);
        out.put("batchRead", batchRead);
        out.put("ingestion", Map.copyOf(ingestion));
        out.put("planner", Map.copyOf(planner));
        out.put("verifier", Map.copyOf(verifier));
        out.put("protocol", Map.copyOf(protocol));
        out.put("cache", Map.copyOf(cache));
        out.put("decisionReasons", resolveDecisionReasons(request, strict, veryStrict, documentHeavy, batchRead, protocolStage, strategyProfile, verifierMode));
        out.put("issuedAt", Instant.now().toString());

        return new StrategyPlan(
                strategyProfile,
                Map.copyOf(ingestion),
                Map.copyOf(planner),
                Map.copyOf(verifier),
                Map.copyOf(protocol),
                Map.copyOf(cache),
                List.copyOf(queryHints),
                List.copyOf(readingGoals),
                Collections.unmodifiableMap(out)
        );
    }

    private String resolveStrategyProfile(ApiVersion version,
                                          boolean strict,
                                          boolean veryStrict,
                                          boolean documentHeavy,
                                          boolean batchRead,
                                          boolean protocolStage,
                                          boolean petitionDetected,
                                          boolean highComplexity) {
        if (protocolStage && veryStrict && documentHeavy) return policyTextCatalogService.strategicProfileProtocolDocHeavyStrictV4();
        if (protocolStage && (strict || petitionDetected)) return policyTextCatalogService.strategicProfileProtocolGuardedV4();
        if (documentHeavy && batchRead && highComplexity) return policyTextCatalogService.strategicProfileBatchDocumentIntensiveV4();
        if (documentHeavy && strict) return policyTextCatalogService.strategicProfileDocumentStrictExecutionV4();
        if (batchRead && strict) return policyTextCatalogService.strategicProfileBatchReadStrictV3();
        if (petitionDetected && version.isAtLeast(ApiVersion.V3)) return policyTextCatalogService.strategicProfilePetitionExecutionV3();
        return strict ? policyTextCatalogService.strategicProfileBalancedStrictExecutionV3() : policyTextCatalogService.strategicProfileBalancedExecutionV2();
    }

    private String resolveIngestionMode(boolean strict,
                                        boolean documentHeavy,
                                        boolean batchRead,
                                        boolean protocolStage,
                                        int textWeight,
                                        int attachmentWeight) {
        if (protocolStage && documentHeavy) return policyTextCatalogService.strategicIngestionProtocolStagedBatch();
        if (documentHeavy && batchRead && (textWeight >= 24000 || attachmentWeight >= 18)) return policyTextCatalogService.strategicIngestionHierarchicalBatch();
        if (strict && documentHeavy) return policyTextCatalogService.strategicIngestionStrictStagedDocument();
        if (batchRead) return policyTextCatalogService.strategicIngestionParallelBatchWindowed();
        return policyTextCatalogService.strategicIngestionFocusedSingleDocument();
    }

    private String resolveVerifierMode(boolean strict,
                                       boolean veryStrict,
                                       boolean protocolStage,
                                       boolean petitionDetected,
                                       boolean institutional,
                                       String procedureFamily) {
        if (protocolStage && veryStrict) return policyTextCatalogService.strategicVerifierProtocolAbsolute();
        if (protocolStage || petitionDetected) return policyTextCatalogService.strategicVerifierProtocolStrict();
        if (strict || institutional || isStrictProcedure(procedureFamily)) return policyTextCatalogService.strategicVerifierStrict();
        return policyTextCatalogService.strategicVerifierBalanced();
    }

    private String resolveCacheMode(boolean documentHeavy,
                                    boolean protocolStage,
                                    boolean petitionDetected,
                                    boolean highComplexity) {
        if (protocolStage) return policyTextCatalogService.strategicCacheModeProtocolWorkspaceCheckpoint();
        if (documentHeavy || highComplexity) return policyTextCatalogService.strategicCacheModeBatchExecutionCheckpoint();
        if (petitionDetected) return policyTextCatalogService.strategicCacheModePetitionSession();
        return policyTextCatalogService.strategicCacheModeQueryPrefix();
    }

    private String resolveBatchMode(boolean batchRead,
                                    boolean strict,
                                    boolean documentHeavy,
                                    boolean highComplexity) {
        if (!batchRead) return policyTextCatalogService.strategicBatchModeSinglePass();
        if (documentHeavy && highComplexity) return policyTextCatalogService.strategicBatchModeMultiPassHierarchical();
        if (strict) return policyTextCatalogService.strategicBatchModeMultiPassStrict();
        return policyTextCatalogService.strategicBatchModeBalancedMultiPass();
    }

    private int resolveParallelLanes(boolean documentHeavy,
                                     boolean batchRead,
                                     boolean strict,
                                     boolean highComplexity,
                                     boolean institutional) {
        int lanes = batchRead ? 3 : 1;
        if (documentHeavy) lanes += 2;
        if (highComplexity) lanes += 1;
        if (institutional) lanes += 1;
        if (strict) lanes = Math.min(lanes, 5);
        return Math.max(1, Math.min(6, lanes));
    }

    private int resolveQueryBudget(ApiVersion version,
                                   boolean documentHeavy,
                                   boolean protocolStage,
                                   boolean highComplexity,
                                   boolean petitionDetected) {
        int base = version != null && version.isAtLeast(ApiVersion.V3) ? 12 : 8;
        if (documentHeavy) base += 2;
        if (protocolStage) base += 3;
        if (highComplexity) base += 2;
        if (petitionDetected) base += 2;
        return Math.max(6, Math.min(20, base));
    }

    private int resolveContradictionLoops(boolean strict, boolean protocolStage, boolean highComplexity) {
        int loops = strict ? 2 : 1;
        if (protocolStage) loops += 1;
        if (highComplexity) loops += 1;
        return Math.max(1, Math.min(4, loops));
    }

    private int resolvePageBudget(boolean documentHeavy,
                                  boolean highComplexity,
                                  boolean protocolStage,
                                  int attachmentWeight) {
        int pages = documentHeavy ? 90 : 32;
        if (highComplexity) pages += 40;
        if (protocolStage) pages += 28;
        pages += Math.min(48, attachmentWeight * 2);
        return Math.max(24, Math.min(220, pages));
    }

    private int resolveChunkChars(boolean strict,
                                  boolean protocolStage,
                                  int textWeight,
                                  boolean batchRead) {
        int base = strict ? 2600 : 3200;
        if (protocolStage) base -= 250;
        if (textWeight > 26000) base -= 200;
        if (batchRead) base -= 120;
        return Math.max(1800, Math.min(4200, base));
    }

    private int resolveSummarizationWindow(boolean strict, boolean highComplexity, boolean batchRead) {
        int base = strict ? 6 : 8;
        if (highComplexity) base += 2;
        if (batchRead) base += 2;
        return Math.max(4, Math.min(14, base));
    }

    private int resolveCacheTtl(String cacheMode, boolean strict, boolean highComplexity) {
        if (policyTextCatalogService.strategicCacheModeProtocolWorkspaceCheckpoint().equals(cacheMode)) return strict ? 3600 : 5400;
        if (policyTextCatalogService.strategicCacheModeBatchExecutionCheckpoint().equals(cacheMode)) return highComplexity ? 2700 : 1800;
        if (policyTextCatalogService.strategicCacheModePetitionSession().equals(cacheMode)) return 2400;
        return 1200;
    }

    private List<String> resolveAuthorityLanes(String ramo,
                                               String tipoJustica,
                                               String procedureFamily,
                                               boolean protocolStage,
                                               boolean strict) {
        LinkedHashSet<String> out = new LinkedHashSet<>(policyTextCatalogService.strategicAuthorityLaneBase());
        add(out, ramo);
        add(out, tipoJustica);
        add(out, procedureFamily);
        if (strict || protocolStage) {
            appendList(out, policyTextCatalogService.strategicAuthorityLaneStrictBase());
        }
        if ("ELEITORAL".equals(ramo)) add(out, policyTextCatalogService.strategicAuthorityLaneElectoral());
        if ("TRABALHISTA".equals(ramo)) add(out, policyTextCatalogService.strategicAuthorityLaneLabor());
        if ("FEDERAL".equals(tipoJustica) || "PREVIDENCIARIO".equals(ramo)) add(out, policyTextCatalogService.strategicAuthorityLaneFederal());
        if ("MILITAR".equals(ramo)) add(out, policyTextCatalogService.strategicAuthorityLaneMilitary());
        return List.copyOf(out);
    }

    private List<String> resolveMandatoryChecks(String capability,
                                                String ramo,
                                                String tipoJustica,
                                                String procedureFamily,
                                                boolean protocolStage,
                                                boolean petitionDetected,
                                                boolean strict,
                                                boolean institutional) {
        LinkedHashSet<String> out = new LinkedHashSet<>(policyTextCatalogService.strategicMandatoryCheckBase());
        if (petitionDetected) add(out, policyTextCatalogService.strategicMandatoryCheckDocumentChecklist());
        if (strict || protocolStage) add(out, policyTextCatalogService.strategicMandatoryCheckCitationAuthorityTrail());
        if (institutional) add(out, policyTextCatalogService.strategicMandatoryCheckRepresentationCapacity());
        if (policyTextCatalogService.isStrategicProtocolCapability(capability)) add(out, policyTextCatalogService.strategicMandatoryCheckProtocolSignaturePackage());
        if ("FAMILIA".equals(ramo)) add(out, policyTextCatalogService.strategicMandatoryCheckFamilyUrgency());
        if ("PENAL".equals(ramo) || "CUSTODIA".equals(procedureFamily)) add(out, policyTextCatalogService.strategicMandatoryCheckCriminalCustody());
        if ("EXECUCAO_FISCAL".equals(ramo) || "EXECUCAO_FISCAL".equals(procedureFamily)) add(out, policyTextCatalogService.strategicMandatoryCheckTaxExecution());
        if ("PREVIDENCIARIO".equals(ramo)) add(out, policyTextCatalogService.strategicMandatoryCheckSocialSecurity());
        if ("ELEITORAL".equals(ramo)) add(out, policyTextCatalogService.strategicMandatoryCheckElectoralTimeliness());
        return List.copyOf(out);
    }

    private List<String> resolveQueryHints(String ramo,
                                           String rito,
                                           String tipoJustica,
                                           String procedureFamily,
                                           String petitionModel,
                                           String capability,
                                           Map<String, Object> knowledgeCadence,
                                           Map<String, Object> fusionPlan,
                                           boolean protocolStage,
                                           boolean documentHeavy) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        add(out, ramo);
        add(out, rito);
        add(out, tipoJustica);
        add(out, procedureFamily);
        add(out, petitionModel);
        add(out, capability);
        appendList(out, extractList(knowledgeCadence, "queryExpansionSeeds"));
        appendList(out, extractNestedList(knowledgeCadence, "curriculum", "legislacaoChave"));
        appendList(out, extractNestedList(knowledgeCadence, "petitionBlueprint", "requiredDocuments"));
        appendList(out, extractNestedList(fusionPlan, "retrieval", "connectorFamilies"));
        appendList(out, extractNestedList(fusionPlan, "retrieval", "allowedToolClasses"));
        if (protocolStage) {
            add(out, policyTextCatalogService.strategicQueryHintProtocolCompetence());
            add(out, policyTextCatalogService.strategicQueryHintProtocolChecklist());
        }
        if (documentHeavy) {
            add(out, policyTextCatalogService.strategicQueryHintDocumentSummary());
            add(out, policyTextCatalogService.strategicQueryHintDocumentContradiction());
        }
        return out.stream().filter(s -> s != null && !s.isBlank()).limit(24).toList();
    }

    private List<String> resolveReadingGoals(Map<String, Object> knowledgeCadence,
                                             String procedureFamily,
                                             String petitionModel,
                                             String ramo,
                                             boolean protocolStage,
                                             boolean petitionDetected) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        appendList(out, extractNestedList(knowledgeCadence, "curriculum", "materiasPrioritarias"));
        appendList(out, extractNestedList(knowledgeCadence, "petitionBlueprint", "requiredDocuments"));
        add(out, procedureFamily);
        add(out, petitionModel);
        add(out, ramo);
        if (petitionDetected) add(out, policyTextCatalogService.strategicReadingGoalPetitionWriting());
        if (protocolStage) add(out, policyTextCatalogService.strategicReadingGoalProtocolFinalCheck());
        return out.stream().filter(s -> s != null && !s.isBlank()).limit(18).toList();
    }

    private List<String> resolveProtocolGates(boolean protocolStage,
                                              boolean petitionDetected,
                                              boolean strict,
                                              String ramo,
                                              String procedureFamily,
                                              boolean institutional) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (protocolStage || petitionDetected) add(out, policyTextCatalogService.strategicProtocolGatePartesRepresentacao());
        if (protocolStage || strict) add(out, policyTextCatalogService.strategicProtocolGateCompetenciaForoUnidade());
        if (petitionDetected) add(out, policyTextCatalogService.strategicProtocolGateDocumentosEssenciais());
        if (institutional) add(out, policyTextCatalogService.strategicProtocolGateIdentidadeInstitucional());
        if ("PENAL".equals(ramo) || "CUSTODIA".equals(procedureFamily)) add(out, policyTextCatalogService.strategicProtocolGateLiberdadeIntegridade());
        if ("ELEITORAL".equals(ramo)) add(out, policyTextCatalogService.strategicProtocolGateJanelaProcessual());
        if ("PREVIDENCIARIO".equals(ramo)) add(out, policyTextCatalogService.strategicProtocolGatePrevidenciarioProvaMinima());
        return List.copyOf(out);
    }

    private List<String> resolveDangerSignals(ResolveRequest request,
                                              boolean strict,
                                              boolean protocolStage,
                                              boolean documentHeavy,
                                              boolean batchRead,
                                              List<String> authorityLanes) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (strict) add(out, policyTextCatalogService.strategicDangerSignalStrictContext());
        if (protocolStage) add(out, policyTextCatalogService.strategicDangerSignalFinalProtocolStage());
        if (documentHeavy) add(out, policyTextCatalogService.strategicDangerSignalHeavyDocumentContext());
        if (batchRead) add(out, policyTextCatalogService.strategicDangerSignalBatchReadingActive());
        if (request.injectionRiskScore() >= 35) add(out, policyTextCatalogService.strategicDangerSignalPromptToolInjectionRisk());
        if (request.sigilo()) add(out, policyTextCatalogService.strategicDangerSignalSigiloRestricted());
        if (authorityLanes.contains("TRIBUNAIS_SUPERIORES")) add(out, policyTextCatalogService.strategicDangerSignalAuthorityFloorUpgraded());
        return List.copyOf(out);
    }

    private List<String> resolveConnectorPriority(Map<String, Object> fusionPlan,
                                                  List<String> authorityLanes,
                                                  boolean batchRead,
                                                  boolean strict) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        appendList(out, extractNestedList(fusionPlan, "retrieval", "connectorFamilies"));
        appendList(out, authorityLanes);
        if (batchRead) add(out, policyTextCatalogService.strategicConnectorPriorityBatchDocumentMemory());
        if (strict) add(out, policyTextCatalogService.strategicConnectorPriorityLockedReadOnlyConnectors());
        return out.stream().filter(s -> s != null && !s.isBlank()).limit(16).toList();
    }

    private List<String> resolveDecisionReasons(ResolveRequest request,
                                                boolean strict,
                                                boolean veryStrict,
                                                boolean documentHeavy,
                                                boolean batchRead,
                                                boolean protocolStage,
                                                String strategyProfile,
                                                String verifierMode) {
        ArrayList<String> reasons = new ArrayList<>();
        reasons.add(policyTextCatalogService.strategicDecisionReasonStrategyProfilePrefix() + strategyProfile);
        reasons.add(policyTextCatalogService.strategicDecisionReasonVerifierModePrefix() + verifierMode);
        reasons.add(policyTextCatalogService.strategicDecisionReasonComplexityPrefix() + request.complexityScore());
        reasons.add(policyTextCatalogService.strategicDecisionReasonInjectionRiskPrefix() + request.injectionRiskScore());
        if (strict) reasons.add(policyTextCatalogService.strategicDecisionReasonStrictContextEnabled());
        if (veryStrict) reasons.add(policyTextCatalogService.strategicDecisionReasonVeryStrictFloor());
        if (documentHeavy) reasons.add(policyTextCatalogService.strategicDecisionReasonDocumentHeavyContext());
        if (batchRead) reasons.add(policyTextCatalogService.strategicDecisionReasonBatchReadEnabled());
        if (protocolStage) reasons.add(policyTextCatalogService.strategicDecisionReasonProtocolStageEnabled());
        if (request.petitionDetected()) reasons.add(policyTextCatalogService.strategicDecisionReasonPetitionDetected());
        if (request.sigilo()) reasons.add(policyTextCatalogService.strategicDecisionReasonSigiloContext());
        return List.copyOf(reasons);
    }

    private boolean isProtocolStage(String capability, Map<String, Object> payload) {
        if (policyTextCatalogService.isStrategicProtocolCapability(capability)) return true;
        if (payload == null || payload.isEmpty()) return false;
        if (truthy(payload.get("prepararPacoteProtocolo"))) return true;
        if (truthy(payload.get("protocolPackage"))) return true;
        return truthy(payload.get("protocoloFinal"));
    }

    private boolean isStrictProcedure(String procedureFamily) {
        return policyTextCatalogService.isStrategicStrictProcedure(procedureFamily);
    }

    private int documentCount(Map<String, Object> payload) {
        if (payload == null) return 0;
        return Math.max(sizeOf(payload.get("documentosAnexados")), sizeOf(payload.get("attachments")));
    }

    private int attachmentWeight(Map<String, Object> payload) {
        int weight = 0;
        if (payload == null || payload.isEmpty()) return 0;
        weight += sizeOf(payload.get("documentosAnexados")) * 3;
        weight += sizeOf(payload.get("attachments")) * 3;
        weight += sizeOf(payload.get("anexos")) * 2;
        if (truthy(payload.get("textoPeticaoLivre"))) weight += 4;
        if (truthy(payload.get("draftMarkdown"))) weight += 4;
        if (truthy(payload.get("textoFatosResumido"))) weight += 2;
        return weight;
    }

    private int estimateTextWeight(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) return 0;
        int total = 0;
        total += lengthOf(payload.get("pergunta"));
        total += lengthOf(payload.get("textoPeticaoLivre"));
        total += lengthOf(payload.get("draftMarkdown"));
        total += lengthOf(payload.get("textoFatosResumido"));
        total += lengthOf(payload.get("resumoLivre"));
        return total;
    }

    private static List<String> extractList(Map<String, Object> map, String key) {
        if (map == null || key == null) return List.of();
        Object value = map.get(key);
        if (value instanceof Collection<?> collection) {
            ArrayList<String> out = new ArrayList<>();
            for (Object item : collection) {
                if (item == null) continue;
                String text = String.valueOf(item).trim();
                if (!text.isBlank()) out.add(text);
            }
            return List.copyOf(out);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractNestedList(Map<String, Object> map, String parent, String child) {
        if (map == null || parent == null || child == null) return List.of();
        Object nested = map.get(parent);
        if (nested instanceof Map<?, ?> nestedMap) {
            Object value = ((Map<String, Object>) nestedMap).get(child);
            if (value instanceof Collection<?> collection) {
                ArrayList<String> out = new ArrayList<>();
                for (Object item : collection) {
                    if (item == null) continue;
                    String text = String.valueOf(item).trim();
                    if (!text.isBlank()) out.add(text);
                }
                return List.copyOf(out);
            }
        }
        return List.of();
    }

    private static int sizeOf(Object value) {
        if (value instanceof Collection<?> collection) return collection.size();
        return 0;
    }

    private static int lengthOf(Object value) {
        if (value == null) return 0;
        String text = String.valueOf(value);
        return text.isBlank() ? 0 : text.trim().length();
    }

    private static boolean truthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean b) return b;
        String text = String.valueOf(value).trim();
        return !text.isBlank() && !"false".equalsIgnoreCase(text) && !"0".equals(text) && !"null".equalsIgnoreCase(text);
    }

    private static void appendList(LinkedHashSet<String> target, Collection<String> values) {
        if (target == null || values == null) return;
        for (String value : values) {
            add(target, value);
        }
    }

    private static void add(LinkedHashSet<String> target, String value) {
        if (target == null || value == null) return;
        String normalized = value.trim();
        if (!normalized.isBlank()) target.add(normalized);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Object> immutableMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) return Map.of();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) continue;
            out.put(entry.getKey(), entry.getValue());
        }
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    public record ResolveRequest(
            String capability,
            ApiVersion version,
            String ramo,
            String rito,
            String tipoJustica,
            String procedureFamily,
            String petitionModel,
            TipoUsuario tipoUsuario,
            int complexityScore,
            int injectionRiskScore,
            boolean sigilo,
            boolean petitionDetected,
            Map<String, Object> payload,
            Map<String, Object> ragPolicy,
            Map<String, Object> fusionPlan,
            Map<String, Object> knowledgeCadence
    ) {
        public ResolveRequest {
            payload = payload == null ? Map.of() : Collections.unmodifiableMap(payload);
            ragPolicy = ragPolicy == null ? Map.of() : immutableMap(ragPolicy);
            fusionPlan = fusionPlan == null ? Map.of() : immutableMap(fusionPlan);
            knowledgeCadence = knowledgeCadence == null ? Map.of() : immutableMap(knowledgeCadence);
        }
    }

    public record StrategyPlan(
            String profile,
            Map<String, Object> ingestion,
            Map<String, Object> planner,
            Map<String, Object> verifier,
            Map<String, Object> protocol,
            Map<String, Object> cache,
            List<String> queryHints,
            List<String> readingGoals,
            Map<String, Object> asMap
    ) {
        public StrategyPlan {
            profile = profile == null || profile.isBlank() ? "LEGAL_BALANCED_EXECUTION_V2" : profile;
            ingestion = ingestion == null ? Map.of() : Map.copyOf(ingestion);
            planner = planner == null ? Map.of() : Map.copyOf(planner);
            verifier = verifier == null ? Map.of() : Map.copyOf(verifier);
            protocol = protocol == null ? Map.of() : Map.copyOf(protocol);
            cache = cache == null ? Map.of() : Map.copyOf(cache);
            queryHints = queryHints == null ? List.of() : List.copyOf(queryHints);
            readingGoals = readingGoals == null ? List.of() : List.copyOf(readingGoals);
            asMap = asMap == null ? Map.of() : Map.copyOf(asMap);
        }
    }
}
