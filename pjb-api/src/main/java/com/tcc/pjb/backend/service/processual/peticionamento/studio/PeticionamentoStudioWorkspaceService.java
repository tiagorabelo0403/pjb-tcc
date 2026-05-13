package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.studio.PeticionamentoStudioDraftDiffResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.studio.PeticionamentoStudioGovernedReviewResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.studio.PeticionamentoStudioQuickDraftResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.studio.PeticionamentoStudioWorkspaceResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistResponse;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoSessaoFacadeService;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioWorkspaceService {

    private final PeticionamentoSessaoFacadeService sessaoFacadeService;
    private final PeticionamentoStudioEvidenceSummaryService evidenceSummaryService;
    private final PeticionamentoStudioDraftAssemblerService draftAssemblerService;
    private final PeticionamentoStudioProcedureLensService procedureLensService;
    private final PeticionamentoStudioCaseTimelineService caseTimelineService;
    private final PeticionamentoStudioProofRequestMatrixService proofRequestMatrixService;
    private final PeticionamentoStudioProtocolChecklistService protocolChecklistService;
    private final PeticionamentoStudioRiskEngineService riskEngineService;
    private final PeticionamentoStudioDocumentGapService documentGapService;
    private final PeticionamentoStudioGovernedReviewService governedReviewService;
    private final PeticionamentoStudioDraftDiffService draftDiffService;

    public PeticionamentoStudioWorkspaceService(PeticionamentoSessaoFacadeService sessaoFacadeService,
                                                PeticionamentoStudioEvidenceSummaryService evidenceSummaryService,
                                                PeticionamentoStudioDraftAssemblerService draftAssemblerService,
                                                PeticionamentoStudioProcedureLensService procedureLensService,
                                                PeticionamentoStudioCaseTimelineService caseTimelineService,
                                                PeticionamentoStudioProofRequestMatrixService proofRequestMatrixService,
                                                PeticionamentoStudioProtocolChecklistService protocolChecklistService,
                                                PeticionamentoStudioRiskEngineService riskEngineService,
                                                PeticionamentoStudioDocumentGapService documentGapService,
                                                PeticionamentoStudioGovernedReviewService governedReviewService,
                                                PeticionamentoStudioDraftDiffService draftDiffService) {
        this.sessaoFacadeService = Objects.requireNonNull(sessaoFacadeService, "sessaoFacadeService");
        this.evidenceSummaryService = Objects.requireNonNull(evidenceSummaryService, "evidenceSummaryService");
        this.draftAssemblerService = Objects.requireNonNull(draftAssemblerService, "draftAssemblerService");
        this.procedureLensService = Objects.requireNonNull(procedureLensService, "procedureLensService");
        this.caseTimelineService = Objects.requireNonNull(caseTimelineService, "caseTimelineService");
        this.proofRequestMatrixService = Objects.requireNonNull(proofRequestMatrixService, "proofRequestMatrixService");
        this.protocolChecklistService = Objects.requireNonNull(protocolChecklistService, "protocolChecklistService");
        this.riskEngineService = Objects.requireNonNull(riskEngineService, "riskEngineService");
        this.documentGapService = Objects.requireNonNull(documentGapService, "documentGapService");
        this.governedReviewService = Objects.requireNonNull(governedReviewService, "governedReviewService");
        this.draftDiffService = Objects.requireNonNull(draftDiffService, "draftDiffService");
    }

    public PeticionamentoStudioWorkspaceResponse buildWorkspace(PeticionamentoSessaoRequest request) {
        Projection projection = buildProjection(request);
        return new PeticionamentoStudioWorkspaceResponse(
                projection.session().getStatus(),
                projection.session().getSessionKey(),
                projection.actorProfile(),
                projection.nextAction(),
                projection.draftingMode(),
                projection.dossier(),
                projection.procedure(),
                projection.evidence(),
                projection.jurisprudence(),
                projection.caseTimeline(),
                projection.proofRequestMatrix(),
                projection.protocolChecklist(),
                projection.riskMatrix(),
                projection.documentGapMatrix(),
                projection.reviewGovernance(),
                projection.assembly(),
                projection.nextSteps()
        );
    }

    public PeticionamentoStudioQuickDraftResponse buildQuickDraft(PeticionamentoSessaoRequest request) {
        Projection projection = buildProjection(request);
        PeticionamentoStudioDraftAssemblerService.QuickDraftReport draft = draftAssemblerService.assemble(
                new PeticionamentoStudioDraftAssemblerService.ResolveRequest(
                        stringValue(projection.dossier().get("title"), request == null ? null : request.getTituloCaso()),
                        mapOf(projection.dossier().get("partes")),
                        projection.procedure(),
                        projection.evidence(),
                        projection.jurisprudence(),
                        projection.caseTimeline(),
                        projection.proofRequestMatrix(),
                        projection.protocolChecklist(),
                        projection.riskMatrix(),
                        projection.manualDraft(),
                        listOfStrings(projection.dossier().get("factLines")),
                        listOfStrings(projection.dossier().get("groundLines")),
                        listOfStrings(projection.dossier().get("requestLines")),
                        mergeDistinct(
                                request == null ? List.of() : request.getProvasIndicadas(),
                                request == null ? List.of() : request.getDocumentosAnexados(),
                                request == null ? List.of() : request.getProvasDocumentais()
                        ),
                        request == null ? null : request.getValorCausa()
                )
        );
        return new PeticionamentoStudioQuickDraftResponse(
                projection.session().getStatus(),
                projection.session().getSessionKey(),
                projection.actorProfile(),
                draft.title(),
                projection.draftingMode(),
                draft.markdown(),
                projection.procedure(),
                projection.evidence(),
                projection.caseTimeline(),
                projection.proofRequestMatrix(),
                projection.protocolChecklist(),
                projection.riskMatrix(),
                projection.documentGapMatrix(),
                projection.reviewGovernance(),
                draft.checklist(),
                projection.nextSteps()
        );
    }


    public PeticionamentoStudioGovernedReviewResponse buildGovernedReview(PeticionamentoSessaoRequest request) {
        Projection projection = buildProjection(request);
        return new PeticionamentoStudioGovernedReviewResponse(
                projection.session().getStatus(),
                projection.session().getSessionKey(),
                projection.actorProfile(),
                projection.draftingMode(),
                projection.procedure(),
                projection.documentGapMatrix(),
                projection.protocolChecklist(),
                projection.riskMatrix(),
                projection.reviewGovernance(),
                projection.nextSteps()
        );
    }

    public PeticionamentoStudioDraftDiffResponse buildDraftDiff(PeticionamentoSessaoRequest request) {
        Projection projection = buildProjection(request);
        PeticionamentoStudioDraftAssemblerService.QuickDraftReport draft = draftAssemblerService.assemble(
                new PeticionamentoStudioDraftAssemblerService.ResolveRequest(
                        stringValue(projection.dossier().get("title"), request == null ? null : request.getTituloCaso()),
                        mapOf(projection.dossier().get("partes")),
                        projection.procedure(),
                        projection.evidence(),
                        projection.jurisprudence(),
                        projection.caseTimeline(),
                        projection.proofRequestMatrix(),
                        projection.protocolChecklist(),
                        projection.riskMatrix(),
                        projection.manualDraft(),
                        listOfStrings(projection.dossier().get("factLines")),
                        listOfStrings(projection.dossier().get("groundLines")),
                        listOfStrings(projection.dossier().get("requestLines")),
                        mergeDistinct(
                                request == null ? List.of() : request.getProvasIndicadas(),
                                request == null ? List.of() : request.getDocumentosAnexados(),
                                request == null ? List.of() : request.getProvasDocumentais()
                        ),
                        request == null ? null : request.getValorCausa()
                )
        );
        PeticionamentoStudioDraftDiffService.DraftDiffReport diff = draftDiffService.diff(new PeticionamentoStudioDraftDiffService.ResolveRequest(
                request != null && request.getDraftMarkdown() != null && !request.getDraftMarkdown().isBlank() ? "RASCUNHO_ATUAL" : "BASELINE_VAZIA",
                firstNonBlank(request == null ? null : request.getDraftMarkdown(), request == null ? null : request.getTextoPeticaoLivre()),
                "MINUTA_CONSOLIDADA",
                draft.markdown()
        ));
        return new PeticionamentoStudioDraftDiffResponse(
                projection.session().getStatus(),
                projection.session().getSessionKey(),
                projection.actorProfile(),
                draft.title(),
                projection.draftingMode(),
                projection.procedure(),
                diff.summary(),
                projection.reviewGovernance(),
                projection.riskMatrix(),
                Boolean.TRUE.equals(diff.summary().get("emptyBaseline"))
                        ? mergeDistinct(projection.nextSteps(), List.of("Comparar a minuta consolidada com o rascunho do usuário antes da assinatura."))
                        : projection.nextSteps()
        );
    }

    private Projection buildProjection(PeticionamentoSessaoRequest request) {
        PeticionamentoSessaoResponse session = sessaoFacadeService.abrirSessaoInicial(request);
        Map<String, Object> workspace = session.getWorkspace() == null ? Map.of() : session.getWorkspace();
        Map<String, Object> jurisprudence = mapOf(workspace.get("jurisprudenciaSugerida"));
        Map<String, Object> guardrails = mapOf(workspace.get("guardrails"));
        Map<String, Object> verifier = mapOf(workspace.get("aiProcedureVerifier"));
        Map<String, Object> batchReading = mapOf(workspace.get("aiDocumentBatchReading"));
        Map<String, Object> payloadHardening = mapOf(workspace.get("payloadHardening"));
        Map<String, Object> intake = mapOf(workspace.get("intake"));
        Map<String, Object> pericial = mapOf(workspace.get("periciaEvidence"));
        LaianePeticaoInicialDraftService.DraftView manualDraft = session.getManualDraft();
        LaianePeticaoAssistResponse assistive = session.getAssistiveAnalysis();
        ProcessMaterialDossierReport materialDossier = assistive == null ? null : assistive.getMaterialDossier();
        ProcessMaterialStrategyReport materialStrategy = assistive == null ? null : assistive.getMaterialStrategy();

        PeticionamentoStudioEvidenceSummaryService.EvidenceSummaryReport evidence = evidenceSummaryService.summarize(
                new PeticionamentoStudioEvidenceSummaryService.ResolveRequest(
                        request == null ? List.of() : request.getMidiaInline(),
                        request == null ? List.of() : request.getDocumentosAnexados(),
                        request == null ? List.of() : request.getProvasDocumentais(),
                        request == null ? List.of() : request.getDocumentosPessoais(),
                        request == null ? List.of() : request.getDocumentosRepresentacao(),
                        pericial
                )
        );

        Map<String, Object> procedure = buildProcedure(request, manualDraft, assistive, verifier, workspace, intake);
        PeticionamentoStudioProcedureLensService.ProcedureLensReport lens = procedureLensService.resolve(request, procedure, workspace);
        Map<String, Object> dossier = buildDossier(request, manualDraft, assistive, evidence, lens);
        procedure = enrichProcedure(procedure, lens, materialDossier, materialStrategy);

        PeticionamentoStudioCaseTimelineService.TimelineReport timeline = caseTimelineService.build(
                new PeticionamentoStudioCaseTimelineService.ResolveRequest(
                        stringValue(dossier.get("title"), request == null ? null : request.getTituloCaso()),
                        listOfStrings(dossier.get("factLines")),
                        listOfMaps(evidence.workspace().get("items")),
                        lens.petitionFamily(),
                        lens.appealType() == null ? null : lens.appealType().name(),
                        lens.counterReasons(),
                        lens.embargosGrounds(),
                        request != null && (request.tutelaUrgenciaResolvida() || Boolean.TRUE.equals(request.getCasoUrgente()) || Boolean.TRUE.equals(request.getRequerLiminar())),
                        materialDossier,
                        request == null ? List.of() : request.getMidiaInline()
                )
        );

        PeticionamentoStudioProofRequestMatrixService.ProofRequestMatrixReport proofRequestMatrix = proofRequestMatrixService.build(
                new PeticionamentoStudioProofRequestMatrixService.ResolveRequest(
                        listOfStrings(dossier.get("requestLines")),
                        listOfStrings(dossier.get("factLines")),
                        listOfStrings(dossier.get("groundLines")),
                        listOfMaps(evidence.workspace().get("items"))
                )
        );

        PeticionamentoStudioProtocolChecklistService.ProtocolChecklistReport protocolChecklist = protocolChecklistService.build(
                new PeticionamentoStudioProtocolChecklistService.ResolveRequest(
                        lens.petitionFamily(),
                        lens.appealType() == null ? null : lens.appealType().name(),
                        lens.counterReasons(),
                        lens.embargosGrounds(),
                        request == null ? null : request.getParteAutora(),
                        request == null ? null : request.getParteRe(),
                        listOfStrings(dossier.get("factLines")).size(),
                        listOfStrings(dossier.get("groundLines")).size(),
                        listOfStrings(dossier.get("requestLines")).size(),
                        request != null && request.getValorCausa() != null,
                        request != null && (request.tutelaUrgenciaResolvida() || Boolean.TRUE.equals(request.getCasoUrgente()) || Boolean.TRUE.equals(request.getRequerLiminar())),
                        listOfMaps(evidence.workspace().get("items")).size(),
                        hasRepresentationArtifact(request, evidence.workspace()),
                        request == null ? null : request.getTipoInstrumentoRepresentacao(),
                        hasDecisionArtifact(request, evidence.workspace(), lens),
                        hasIntimationArtifact(request, evidence.workspace(), lens),
                        materialDossier,
                        materialStrategy
                )
        );

        Map<String, Object> baseRiskMatrix = buildBaseRiskMatrix(session, batchReading, verifier, guardrails, evidence, payloadHardening, lens);
        LinkedHashMap<String, Object> riskMatrix = new LinkedHashMap<>(riskEngineService.build(
                new PeticionamentoStudioRiskEngineService.ResolveRequest(
                        lens.petitionFamily(),
                        lens.appealType() == null ? null : lens.appealType().name(),
                        lens.counterReasons(),
                        lens.embargosGrounds(),
                        listOfStrings(dossier.get("factLines")).size(),
                        listOfStrings(dossier.get("groundLines")).size(),
                        listOfStrings(dossier.get("requestLines")).size(),
                        listOfMaps(evidence.workspace().get("items")).size(),
                        request == null || request.getValorCausa() == null,
                        request != null && (request.tutelaUrgenciaResolvida() || Boolean.TRUE.equals(request.getCasoUrgente()) || Boolean.TRUE.equals(request.getRequerLiminar())),
                        hasDecisionArtifact(request, evidence.workspace(), lens),
                        hasIntimationArtifact(request, evidence.workspace(), lens),
                        listOfStrings(baseRiskMatrix.get("blockingIssues")),
                        listOfStrings(baseRiskMatrix.get("alerts")),
                        mergeDistinct(
                                listOfStrings(baseRiskMatrix.get("checklist")),
                                protocolChecklist.summary()
                        ),
                        stringValue(proofRequestMatrix.workspace().get("overallStrength"), "CRITICO"),
                        listOfMaps(protocolChecklist.workspace().get("items")),
                        materialDossier,
                        materialStrategy
                )
        ));
        put(riskMatrix, "payloadFingerprint", payloadHardening.get("fingerprint"));
        put(riskMatrix, "guardrailStatus", guardrails.get("status"));
        put(riskMatrix, "batchProfile", batchReading.get("profile"));
        put(riskMatrix, "verifierProfile", verifier.get("profile"));
        put(riskMatrix, "readinessScore", session.getAssistiveAnalysis() == null ? session.getManualDraft() == null ? null : session.getManualDraft().readinessScore() : session.getAssistiveAnalysis().getReadinessScore());

        PeticionamentoStudioDocumentGapService.DocumentGapReport documentGapMatrix = documentGapService.build(
                new PeticionamentoStudioDocumentGapService.ResolveRequest(
                        lens.petitionFamily(),
                        lens.appealType() == null ? null : lens.appealType().name(),
                        request != null && (request.tutelaUrgenciaResolvida() || Boolean.TRUE.equals(request.getCasoUrgente()) || Boolean.TRUE.equals(request.getRequerLiminar())),
                        hasText(request == null ? null : request.getParteAutora()) && hasText(request == null ? null : request.getParteRe()),
                        listOfMaps(evidence.workspace().get("items")).size(),
                        hasRepresentationArtifact(request, evidence.workspace()),
                        requiresRepresentation(request),
                        hasDecisionArtifact(request, evidence.workspace(), lens),
                        hasIntimationArtifact(request, evidence.workspace(), lens),
                        hasUrgencyEvidence(request, evidence.workspace()),
                        lens.embargosGrounds(),
                        materialDossier,
                        materialStrategy
                )
        );

        PeticionamentoStudioGovernedReviewService.ReviewGovernanceReport reviewGovernance = governedReviewService.build(
                new PeticionamentoStudioGovernedReviewService.ResolveRequest(
                        lens.petitionFamily(),
                        stringValue(workspace.get("perfilPeticionante"), session.getPapelArquitetural()),
                        resolveDraftingMode(session, manualDraft, assistive, lens),
                        request == null ? null : stringValue(request.getCtx() == null ? null : request.getCtx().get("governanceMode"), null),
                        firstNonBlank(
                                request == null ? null : stringValue(request.getCtx() == null ? null : request.getCtx().get("signerName"), null),
                                request == null ? null : stringValue(request.getCtx() == null ? null : request.getCtx().get("patronoNome"), null)
                        ),
                        firstNonBlank(
                                request == null ? null : stringValue(request.getCtx() == null ? null : request.getCtx().get("organizationalAnchor"), null),
                                request == null ? null : stringValue(request.getCtx() == null ? null : request.getCtx().get("officeName"), null),
                                stringValue(procedure.get("tribunal"), null),
                                stringValue(procedure.get("comarca"), null)
                        ),
                        Boolean.TRUE.equals(riskMatrix.get("readyForProtocol")),
                        Boolean.TRUE.equals(riskMatrix.get("blocking")),
                        Boolean.TRUE.equals(request == null ? null : request.getCtx() == null ? null : request.getCtx().get("reviewAccepted")),
                        Boolean.TRUE.equals(request == null ? null : request.getCtx() == null ? null : request.getCtx().get("forcePatronoReview")),
                        Boolean.TRUE.equals(request == null ? null : request.getCtx() == null ? null : request.getCtx().get("forceInstitutionalReview")),
                        Boolean.TRUE.equals(request == null ? null : request.getCtx() == null ? null : request.getCtx().get("forceSignerReview"))
                )
        );

        Map<String, Object> assembly = buildAssembly(request, manualDraft, assistive, procedure, evidence.workspace(), jurisprudence, riskMatrix, intake, lens, timeline.workspace(), proofRequestMatrix.workspace(), protocolChecklist.workspace());
        List<String> nextSteps = buildNextSteps(session, evidence.warnings(), timeline.warnings(), protocolChecklist.summary(), riskMatrix, lens, documentGapMatrix.nextActions(), reviewGovernance.nextActions());
        return new Projection(
                session,
                stringValue(workspace.get("perfilPeticionante"), session.getPapelArquitetural()),
                stringValue(workspace.get("nextAction"), "REVISAR_DOSSIE_E_MONTAR_MINUTA"),
                resolveDraftingMode(session, manualDraft, assistive, lens),
                dossier,
                procedure,
                evidence.workspace(),
                jurisprudence,
                timeline.workspace(),
                proofRequestMatrix.workspace(),
                protocolChecklist.workspace(),
                Map.copyOf(riskMatrix),
                documentGapMatrix.workspace(),
                reviewGovernance.workspace(),
                assembly,
                nextSteps,
                manualDraft
        );
    }

    private Map<String, Object> buildDossier(PeticionamentoSessaoRequest request,
                                             LaianePeticaoInicialDraftService.DraftView manualDraft,
                                             LaianePeticaoAssistResponse assistive,
                                             PeticionamentoStudioEvidenceSummaryService.EvidenceSummaryReport evidence,
                                             PeticionamentoStudioProcedureLensService.ProcedureLensReport lens) {
        LinkedHashMap<String, Object> dossier = new LinkedHashMap<>();
        LinkedHashMap<String, Object> partes = new LinkedHashMap<>();
        put(partes, "parteAutora", request == null ? null : request.getParteAutora());
        put(partes, "parteRe", request == null ? null : request.getParteRe());
        put(dossier, "title", firstNonBlank(
                manualDraft == null ? null : manualDraft.tituloCaso(),
                request == null ? null : request.getTituloCaso(),
                assistive == null ? null : assistive.getRequestId()
        ));
        put(dossier, "partes", partes);
        put(dossier, "textoFatosResumido", request == null ? null : request.getTextoFatosResumido());
        put(dossier, "factLines", manualDraft == null ? sanitize(request == null ? List.of() : request.getFatos()) : manualDraft.fatosEstruturados());
        put(dossier, "groundLines", manualDraft == null ? sanitize(request == null ? List.of() : request.getFundamentosJuridicos()) : manualDraft.fundamentosEstruturados());
        put(dossier, "requestLines", manualDraft == null ? sanitize(request == null ? List.of() : request.getPedidos()) : manualDraft.pedidosEstruturados());
        put(dossier, "proofLines", manualDraft == null ? sanitize(request == null ? List.of() : request.getProvasIndicadas()) : manualDraft.provasIndicadas());
        put(dossier, "valueClaim", request == null || request.getValorCausa() == null ? null : request.getValorCausa().toPlainString());
        put(dossier, "urgencyRequested", request != null && (request.tutelaUrgenciaResolvida() || Boolean.TRUE.equals(request.getCasoUrgente()) || Boolean.TRUE.equals(request.getRequerLiminar())));
        put(dossier, "evidenceSummaryProfile", evidence.profile());
        put(dossier, "evidenceWarnings", evidence.warnings());
        put(dossier, "readinessScore", assistive == null ? manualDraft == null ? null : manualDraft.readinessScore() : assistive.getReadinessScore());
        put(dossier, "petitionFamily", lens.petitionFamily());
        put(dossier, "canonicalAppealType", lens.appealType() == null ? null : lens.appealType().name());
        put(dossier, "recursalCounterReasons", lens.counterReasons());
        put(dossier, "embargosGrounds", lens.embargosGrounds());
        if (assistive != null && assistive.getMaterialDossier() != null) {
            LinkedHashMap<String, Object> material = new LinkedHashMap<>();
            put(material, "objectLabel", assistive.getMaterialDossier().objectLabel());
            put(material, "primaryRelief", assistive.getMaterialDossier().primaryRelief());
            put(material, "controversyAxes", assistive.getMaterialDossier().controversyAxes());
            put(material, "evidenceAnchors", assistive.getMaterialDossier().evidenceAnchors());
            put(material, "proofGaps", assistive.getMaterialDossier().proofGaps());
            put(material, "protocolChecklist", assistive.getMaterialDossier().protocolChecklist());
            put(dossier, "materialDossier", material);
        }
        if (assistive != null && assistive.getMaterialStrategy() != null) {
            LinkedHashMap<String, Object> strategy = new LinkedHashMap<>();
            put(strategy, "litigationPosture", assistive.getMaterialStrategy().litigationPosture());
            put(strategy, "protocolReadiness", assistive.getMaterialStrategy().protocolReadiness());
            put(strategy, "evidenceReadiness", assistive.getMaterialStrategy().evidenceReadiness());
            put(strategy, "protocolBlockers", assistive.getMaterialStrategy().protocolBlockers());
            put(strategy, "executionChecklist", assistive.getMaterialStrategy().executionChecklist());
            put(dossier, "materialStrategy", strategy);
        }
        return Map.copyOf(dossier);
    }

    private Map<String, Object> buildProcedure(PeticionamentoSessaoRequest request,
                                               LaianePeticaoInicialDraftService.DraftView manualDraft,
                                               LaianePeticaoAssistResponse assistive,
                                               Map<String, Object> verifier,
                                               Map<String, Object> workspace,
                                               Map<String, Object> intake) {
        LinkedHashMap<String, Object> procedure = new LinkedHashMap<>();
        Map<String, Object> resolvedDraftSeed = mapOf(workspace.get("resolvedDraftSeed"));
        put(procedure, "ramoDireito", firstNonBlank(
                stringValue(resolvedDraftSeed.get("ramoDireito"), null),
                manualDraft == null ? null : manualDraft.ramoDireito(),
                request == null ? null : request.getRamoDireito()
        ));
        put(procedure, "ritoProcessual", firstNonBlank(
                stringValue(resolvedDraftSeed.get("ritoProcessual"), null),
                manualDraft == null ? null : manualDraft.ritoSugerido(),
                request == null ? null : request.getRitoProcessual()
        ));
        put(procedure, "classeProcessual", firstNonBlank(
                stringValue(resolvedDraftSeed.get("classeProcessual"), null),
                manualDraft == null ? null : manualDraft.classeSugerida(),
                request == null ? null : request.getClasseProcessual()
        ));
        put(procedure, "justicaSugerida", request == null ? null : request.getTipoJustica());
        put(procedure, "cidadeFato", firstNonBlank(stringValue(resolvedDraftSeed.get("cidadeFato"), null), request == null ? null : request.getCidadeFato()));
        put(procedure, "uf", firstNonBlank(stringValue(resolvedDraftSeed.get("ufProtocolo"), null), request == null ? null : request.getUfProtocolo()));
        put(procedure, "comarca", firstNonBlank(stringValue(resolvedDraftSeed.get("cidadeProtocolo"), null), request == null ? null : request.getCidadeProtocolo()));
        put(procedure, "naturezaJuridica", firstNonBlank(stringValue(resolvedDraftSeed.get("naturezaJuridica"), null), request == null ? null : request.getNaturezaJuridica()));
        put(procedure, "verificationProfile", verifier.get("profile"));
        put(procedure, "verificationTrack", verifier.get("resolvedTrack"));
        put(procedure, "competenciaSugerida", assistive == null || assistive.getCompetencia() == null ? null : assistive.getCompetencia().tipoVara());
        put(procedure, "unitResolutionMode", verifier.get("unitResolutionMode"));
        if (!intake.isEmpty()) {
            put(procedure, "intakeProfile", intake.get("profile"));
        }
        return Map.copyOf(procedure);
    }

    private Map<String, Object> enrichProcedure(Map<String, Object> procedure,
                                                PeticionamentoStudioProcedureLensService.ProcedureLensReport lens,
                                                ProcessMaterialDossierReport materialDossier,
                                                ProcessMaterialStrategyReport materialStrategy) {
        LinkedHashMap<String, Object> enriched = new LinkedHashMap<>(procedure == null ? Map.of() : procedure);
        put(enriched, "petitionFamily", lens.petitionFamily());
        put(enriched, "draftingMode", lens.draftingMode());
        put(enriched, "recursalCounterReasons", lens.counterReasons());
        put(enriched, "canonicalAppealType", lens.appealType() == null ? null : lens.appealType().name());
        put(enriched, "recursalSpeciesType", lens.speciesType() == null ? null : lens.speciesType().name());
        put(enriched, "embargosGrounds", lens.embargosGrounds());
        put(enriched, "recursalBlueprint", lens.workspace().get("recursalBlueprint"));
        if (materialDossier != null) {
            put(enriched, "materialObjectLabel", materialDossier.objectLabel());
            put(enriched, "evidentiaryBracket", materialDossier.evidentiaryBracket());
        }
        if (materialStrategy != null) {
            put(enriched, "protocolReadiness", materialStrategy.protocolReadiness());
            put(enriched, "evidenceReadiness", materialStrategy.evidenceReadiness());
        }
        return Map.copyOf(enriched);
    }

    private Map<String, Object> buildBaseRiskMatrix(PeticionamentoSessaoResponse session,
                                                    Map<String, Object> batchReading,
                                                    Map<String, Object> verifier,
                                                    Map<String, Object> guardrails,
                                                    PeticionamentoStudioEvidenceSummaryService.EvidenceSummaryReport evidence,
                                                    Map<String, Object> payloadHardening,
                                                    PeticionamentoStudioProcedureLensService.ProcedureLensReport lens) {
        LinkedHashMap<String, Object> risk = new LinkedHashMap<>();
        List<String> blockingIssues = mergeDistinct(
                listOfStrings(guardrails.get("bloqueios")),
                listOfStrings(batchReading.get("blockingIssues")),
                listOfStrings(verifier.get("blockers"))
        );
        List<String> alerts = mergeDistinct(
                listOfStrings(guardrails.get("alertas")),
                listOfStrings(batchReading.get("alerts")),
                listOfStrings(verifier.get("alerts")),
                evidence.warnings(),
                lens.alerts()
        );
        List<String> checklist = mergeDistinct(
                listOfStrings(guardrails.get("checklist")),
                listOfStrings(batchReading.get("mandatorySequence")),
                listOfStrings(verifier.get("reviewChecklist")),
                lens.checklist()
        );
        put(risk, "blocking", !blockingIssues.isEmpty());
        put(risk, "readyForProtocol", session.getAssistiveAnalysis() != null && session.getAssistiveAnalysis().isProntaParaProtocolo());
        put(risk, "blockingIssues", blockingIssues);
        put(risk, "alerts", alerts);
        put(risk, "checklist", checklist);
        put(risk, "guardrailStatus", guardrails.get("status"));
        put(risk, "batchProfile", batchReading.get("profile"));
        put(risk, "verifierProfile", verifier.get("profile"));
        put(risk, "payloadFingerprint", payloadHardening.get("fingerprint"));
        put(risk, "readinessScore", session.getAssistiveAnalysis() == null ? session.getManualDraft() == null ? null : session.getManualDraft().readinessScore() : session.getAssistiveAnalysis().getReadinessScore());
        put(risk, "petitionFamily", lens.petitionFamily());
        put(risk, "canonicalAppealType", lens.appealType() == null ? null : lens.appealType().name());
        put(risk, "recursalCounterReasons", lens.counterReasons());
        return Map.copyOf(risk);
    }

    private Map<String, Object> buildAssembly(PeticionamentoSessaoRequest request,
                                              LaianePeticaoInicialDraftService.DraftView manualDraft,
                                              LaianePeticaoAssistResponse assistive,
                                              Map<String, Object> procedure,
                                              Map<String, Object> evidence,
                                              Map<String, Object> jurisprudence,
                                              Map<String, Object> riskMatrix,
                                              Map<String, Object> intake,
                                              PeticionamentoStudioProcedureLensService.ProcedureLensReport lens,
                                              Map<String, Object> timeline,
                                              Map<String, Object> proofRequestMatrix,
                                              Map<String, Object> protocolChecklist) {
        LinkedHashMap<String, Object> assembly = new LinkedHashMap<>();
        ArrayList<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(block("ENDERECAMENTO", "Endereçamento", "READY", procedureSummary(procedure)));
        blocks.add(block("QUALIFICACAO", "Qualificação das partes", hasText(request == null ? null : request.getParteAutora()) ? "READY" : "NEEDS_REVIEW", "Consolidar identificação da parte autora, ré e legitimidade processual."));
        blocks.add(block("TIMELINE", "Timeline do caso", listOfMaps(timeline.get("items")).size() >= 3 ? "READY" : "NEEDS_REVIEW", "Ordenar fatos, eventos probatórios e marcos procedimentais em uma trilha única e auditável."));
        blocks.add(block("FATOS", "Fatos e cronologia", hasAny(manualDraft == null ? List.of() : manualDraft.fatosEstruturados(), request == null ? List.of() : request.getFatos()) ? "READY" : "NEEDS_INPUT", "Ordenar fatos essenciais por sequência temporal, dano, inadimplemento e urgência."));
        blocks.add(block("FUNDAMENTOS", "Fundamentos jurídicos", hasAny(manualDraft == null ? List.of() : manualDraft.fundamentosEstruturados(), request == null ? List.of() : request.getFundamentosJuridicos()) ? "READY" : "NEEDS_INPUT", "Amarrar norma, rito, competência e tese principal com linguagem técnica controlada."));
        blocks.add(block("JURISPRUDENCIA", "Jurisprudência oficial", listOfMaps(jurisprudence.get("items")).isEmpty() ? "AWAITING_REFINEMENT" : "READY", "Janela de precedentes preparada para uso auditável na peça."));
        blocks.add(block("PROVAS", "Mapa probatório", listOfMaps(evidence.get("items")).isEmpty() ? "NEEDS_INPUT" : "READY", "Vincular cada anexo ou mídia a fatos e pedidos específicos."));
        blocks.add(block("MATRIZ_PROVA_PEDIDO", "Matriz prova x pedido", "ROBUSTO".equals(stringValue(proofRequestMatrix.get("overallStrength"), null)) ? "READY" : "NEEDS_REVIEW", "Conferir cobertura de fatos, fundamentos e provas para cada pedido."));
        blocks.add(block("PEDIDOS", "Pedidos", hasAny(manualDraft == null ? List.of() : manualDraft.pedidosEstruturados(), request == null ? List.of() : request.getPedidos()) ? "READY" : "NEEDS_INPUT", "Consolidar pedidos principais, subsidiários, tutela e consectários."));
        blocks.add(block("CHECKLIST_RITO", "Checklist do rito e protocolo", Boolean.TRUE.equals(protocolChecklist.get("blocking")) ? "BLOCKED" : "READY", "Checklist procedimental endurecido por rito, família da peça e lacunas documentais."));
        if (isRecursalFamily(lens.petitionFamily())) {
            blocks.add(block("CABIMENTO_RECURSAL", "Cabimento e tempestividade", hasDecisionArtifact(request, evidence, lens) && hasIntimationArtifact(request, evidence, lens) ? "READY" : "BLOCKED", "Fechar decisão atacada, janela recursal, preparo e regularidade específica do ato impugnativo."));
            blocks.add(block("DOSSIE_RECURSAL", "Dossiê documental recursal", listOfMaps(mapOf(lens.workspace().get("recursalBlueprint")).get("documentosObrigatorios")).isEmpty() ? "AWAITING_REFINEMENT" : "READY", "Organizar decisão recorrida, prova de ciência e peças obrigatórias da trilha recursal."));
            if ("EMBARGOS".equals(lens.petitionFamily())) {
                blocks.add(block("VICIO_EMBARGADO", "Vício da decisão", lens.embargosGrounds().isEmpty() ? "BLOCKED" : "READY", "Delimitar omissão, contradição, obscuridade ou erro material sem ampliar indevidamente o objeto integrativo."));
            }
        }
        blocks.add(block("REVISAO", "Revisão e protocolo", Boolean.TRUE.equals(riskMatrix.get("blocking")) ? "BLOCKED" : "READY", "Executar revisão humana, assinatura correta e protocolo governado."));
        put(assembly, "profile", isRecursalFamily(lens.petitionFamily()) ? "PETITION_STUDIO_RECURSAL_V3" : "PETITION_STUDIO_WORKSPACE_V2");
        put(assembly, "blocks", blocks);
        put(assembly, "manualDraftPresent", manualDraft != null);
        put(assembly, "assistiveDraftPresent", assistive != null && hasText(assistive.getDraftMarkdown()));
        put(assembly, "petitionFamily", lens.petitionFamily());
        put(assembly, "canonicalAppealType", lens.appealType() == null ? null : lens.appealType().name());
        put(assembly, "recursalCounterReasons", lens.counterReasons());
        put(assembly, "recursalBlueprint", lens.workspace().get("recursalBlueprint"));
        put(assembly, "timelineProfile", timeline.get("profile"));
        put(assembly, "proofMatrixProfile", proofRequestMatrix.get("profile"));
        put(assembly, "protocolChecklistProfile", protocolChecklist.get("profile"));
        if (!intake.isEmpty()) {
            put(assembly, "intakeProfile", intake.get("profile"));
            put(assembly, "questionnaireBlocks", intake.get("blocks"));
        }
        return Map.copyOf(assembly);
    }

    private List<String> buildNextSteps(PeticionamentoSessaoResponse session,
                                        List<String> evidenceWarnings,
                                        List<String> timelineWarnings,
                                        List<String> checklistSummary,
                                        Map<String, Object> riskMatrix,
                                        PeticionamentoStudioProcedureLensService.ProcedureLensReport lens,
                                        List<String> documentGapActions,
                                        List<String> reviewActions) {
        return mergeDistinct(
                session.getPassosSugeridos(),
                evidenceWarnings,
                timelineWarnings,
                checklistSummary,
                listOfStrings(riskMatrix.get("blockingIssues")),
                listOfStrings(riskMatrix.get("checklist")),
                listOfStrings(riskMatrix.get("nextActions")),
                documentGapActions,
                reviewActions,
                lens.nextSteps()
        );
    }

    private String resolveDraftingMode(PeticionamentoSessaoResponse session,
                                       LaianePeticaoInicialDraftService.DraftView manualDraft,
                                       LaianePeticaoAssistResponse assistive,
                                       PeticionamentoStudioProcedureLensService.ProcedureLensReport lens) {
        if (isRecursalFamily(lens.petitionFamily())) {
            return lens.draftingMode();
        }
        if (assistive != null && assistive.getReadinessScore() >= 80d) {
            return "RAPIDO_ASSISTIDO_COM_PRECHECK";
        }
        if (manualDraft != null && manualDraft.readinessScore() != null && manualDraft.readinessScore() >= 70) {
            return "RAPIDO_ESTRUTURADO";
        }
        return session.getModoResolvido() == null ? "RAPIDO_ASSISTIDO" : session.getModoResolvido();
    }

    private boolean hasRepresentationArtifact(PeticionamentoSessaoRequest request, Map<String, Object> evidence) {
        if (request != null && !sanitize(request.getDocumentosRepresentacao()).isEmpty()) {
            return true;
        }
        return evidenceContains(evidence, "PROCURACAO", "PROCURAÇÃO", "SUBSTABELECIMENTO", "MANDATO", "REPRESENTACAO", "REPRESENTAÇÃO");
    }

    private boolean hasDecisionArtifact(PeticionamentoSessaoRequest request,
                                        Map<String, Object> evidence,
                                        PeticionamentoStudioProcedureLensService.ProcedureLensReport lens) {
        if (!isRecursalFamily(lens.petitionFamily())) {
            return true;
        }
        return evidenceContains(evidence, "SENTENCA", "SENTENÇA", "DECISAO", "DECISÃO", "ACORDAO", "ACÓRDÃO", "DECISAO EMBARGADA", "DECISÃO EMBARGADA");
    }

    private boolean hasIntimationArtifact(PeticionamentoSessaoRequest request,
                                          Map<String, Object> evidence,
                                          PeticionamentoStudioProcedureLensService.ProcedureLensReport lens) {
        if (!isRecursalFamily(lens.petitionFamily())) {
            return true;
        }
        return evidenceContains(evidence, "INTIMACAO", "INTIMAÇÃO", "CIENCIA", "CIÊNCIA", "CERTIDAO", "CERTIDÃO", "PUBLICACAO", "PUBLICAÇÃO");
    }

    private boolean requiresRepresentation(PeticionamentoSessaoRequest request) {
        if (request == null) {
            return false;
        }
        if (!sanitize(request.getDocumentosRepresentacao()).isEmpty()) {
            return true;
        }
        Object explicit = request.getCtx() == null ? null : request.getCtx().get("requiresRepresentation");
        if (explicit instanceof Boolean value) {
            return value;
        }
        return hasText(request.getTipoInstrumentoRepresentacao());
    }

    private boolean hasUrgencyEvidence(PeticionamentoSessaoRequest request, Map<String, Object> evidence) {
        if (request == null || !(request.tutelaUrgenciaResolvida() || Boolean.TRUE.equals(request.getCasoUrgente()) || Boolean.TRUE.equals(request.getRequerLiminar()))) {
            return false;
        }
        return !sanitize(request.getProvasIndicadas()).isEmpty()
                || !sanitize(request.getProvasDocumentais()).isEmpty()
                || !sanitize(request.getDocumentosAnexados()).isEmpty()
                || evidenceContains(evidence, "URGENTE", "RISCO", "LIMINAR", "TUTELA", "LAUDO", "ATESTADO", "COMPROVANTE");
    }

    private boolean evidenceContains(Map<String, Object> evidence, String... tokens) {
        for (Map<String, Object> item : listOfMaps(evidence.get("items"))) {
            String label = normalizeForSearch(stringValue(item.get("label"), null));
            String summary = normalizeForSearch(stringValue(item.get("summary"), null));
            for (String token : tokens) {
                String normalized = normalizeForSearch(token);
                if ((label != null && label.contains(normalized)) || (summary != null && summary.contains(normalized))) {
                    return true;
                }
            }
        }
        return false;
    }

    private Map<String, Object> block(String code, String label, String status, String summary) {
        LinkedHashMap<String, Object> block = new LinkedHashMap<>();
        block.put("code", code);
        block.put("label", label);
        block.put("status", status);
        block.put("summary", summary);
        return Map.copyOf(block);
    }

    private String procedureSummary(Map<String, Object> procedure) {
        return "Justiça sugerida: " + stringValue(procedure.get("justicaSugerida"), "a definir")
                + ", rito: " + stringValue(procedure.get("ritoProcessual"), "a definir")
                + ", classe: " + stringValue(procedure.get("classeProcessual"), "a definir") + ".";
    }

    private boolean isRecursalFamily(String family) {
        String normalized = trimToNull(family);
        return normalized != null && !"PETICAO_BASE".equals(normalized);
    }

    private boolean hasAny(List<String> primary, List<String> fallback) {
        return !(sanitize(primary).isEmpty() && sanitize(fallback).isEmpty());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Map<String, Object> mapOf(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        map.forEach((key, entry) -> {
            if (key != null) {
                out.put(String.valueOf(key), entry);
            }
        });
        return Collections.unmodifiableMap(out);
    }

    private List<Map<String, Object>> listOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                LinkedHashMap<String, Object> row = new LinkedHashMap<>();
                map.forEach((key, entry) -> {
                    if (key != null) {
                        row.put(String.valueOf(key), entry);
                    }
                });
                out.add(Map.copyOf(row));
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<String> listOfStrings(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (Object item : list) {
            String normalized = trimToNull(item == null ? null : String.valueOf(item));
            if (normalized != null && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String stringValue(Object value, String fallback) {
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        return normalized == null ? fallback : normalized;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    private List<String> mergeDistinct(List<String>... blocks) {
        ArrayList<String> out = new ArrayList<>();
        if (blocks == null) {
            return List.of();
        }
        for (List<String> block : blocks) {
            for (String item : sanitize(block)) {
                if (!out.contains(item)) {
                    out.add(item);
                }
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private List<String> sanitize(List<String> values) {
        ArrayList<String> out = new ArrayList<>();
        if (values == null) {
            return List.of();
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null && !out.contains(normalized)) {
                out.add(normalized);
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private String normalizeForSearch(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Õ', 'O')
                .replace('Ô', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record Projection(PeticionamentoSessaoResponse session,
                              String actorProfile,
                              String nextAction,
                              String draftingMode,
                              Map<String, Object> dossier,
                              Map<String, Object> procedure,
                              Map<String, Object> evidence,
                              Map<String, Object> jurisprudence,
                              Map<String, Object> caseTimeline,
                              Map<String, Object> proofRequestMatrix,
                              Map<String, Object> protocolChecklist,
                              Map<String, Object> riskMatrix,
                              Map<String, Object> documentGapMatrix,
                              Map<String, Object> reviewGovernance,
                              Map<String, Object> assembly,
                              List<String> nextSteps,
                              LaianePeticaoInicialDraftService.DraftView manualDraft) {
    }
}
