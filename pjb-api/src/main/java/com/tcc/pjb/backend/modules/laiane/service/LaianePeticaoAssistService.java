package com.tcc.pjb.backend.modules.laiane.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.core.kernel.advisory.ContextualPrecedentAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.ContextualPrecedentAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.ExplainableDecisionTrailReport;
import com.tcc.pjb.backend.core.kernel.advisory.ExplainableDecisionTrailService;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextService;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalMemoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalMemoryService;
import com.tcc.pjb.backend.core.kernel.advisory.KernelAdvisoryTelemetry;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceService;
import com.tcc.pjb.backend.core.kernel.advisory.LegalCoherenceEngine;
import com.tcc.pjb.backend.core.kernel.advisory.LegalCoherenceReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessIntegrityRadarReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessIntegrityRadarService;
import com.tcc.pjb.backend.core.kernel.advisory.ProtocolDryRunReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProtocolDryRunService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.StrategicCopilotReport;
import com.tcc.pjb.backend.core.kernel.advisory.StrategicCopilotService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralRitoNames;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionRequest;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeDraftRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianeDraftResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoValidateResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistResponse;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoProtocolPackageResponse;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolCreateRequest;
import com.tcc.pjb.backend.modules.laiane.dto.protocol.LaianeProtocolPackageDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.LaianeLawyerAttachmentValidationRequest;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.LaianeLawyerAttachmentValidationResponse;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;

@SuppressWarnings("ConstantValue")
@Service
public class LaianePeticaoAssistService {

    private final LaianeDraftService draftService;
    private final LaianePeticaoValidatorService validatorService;
    private final LaianeRitosCoverageService ritosCoverageService;
    private final LaianeLawyerService laianeLawyerService;
    private final LaianeProtocolService laianeProtocolService;
    private final TriagemNacionalIAEngine triagemNacionalIAEngine;
    private final TetoProcessualService tetoProcessualService;
    private final TerritorialProcessualService territorialProcessualService;
    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine;
    private final RadarPadroesService radarPadroesService;
    private final ProntuarioNacionalService prontuarioNacionalService;
    private final ProceduralCatalogService proceduralCatalogService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final LegalCoherenceEngine legalCoherenceEngine;
    private final ProtocolDryRunService protocolDryRunService;
    private final ProcessIntegrityRadarService processIntegrityRadarService;
    private final StrategicCopilotService strategicCopilotService;
    private final SettlementAdvisoryService settlementAdvisoryService;
    private final ProcessMaterialDossierService processMaterialDossierService;
    private final ProcessMaterialStrategyService processMaterialStrategyService;
    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService;
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService;
    private final InstitutionalMemoryService institutionalMemoryService;
    private final InstitutionalGovernanceContextService institutionalGovernanceContextService;
    private final ContextualPrecedentAdvisoryService contextualPrecedentAdvisoryService;
    private final ExplainableDecisionTrailService explainableDecisionTrailService;
    private final KernelOperationalGovernanceService kernelOperationalGovernanceService;
    private final LaianeSubmissionGuardrailService laianeSubmissionGuardrailService;

    public LaianePeticaoAssistService(LaianeDraftService draftService,
                                      LaianePeticaoValidatorService validatorService,
                                      LaianeRitosCoverageService ritosCoverageService,
                                      LaianeLawyerService laianeLawyerService,
                                      LaianeProtocolService laianeProtocolService,
                                      TriagemNacionalIAEngine triagemNacionalIAEngine,
                                      TetoProcessualService tetoProcessualService,
                                      TerritorialProcessualService territorialProcessualService,
                                      MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine,
                                      RadarPadroesService radarPadroesService,
                                      ProntuarioNacionalService prontuarioNacionalService,
                                      ProceduralCatalogService proceduralCatalogService,
                                      ProceduralCanonicalResolver proceduralCanonicalResolver,
                                      LegalCoherenceEngine legalCoherenceEngine,
                                      ProtocolDryRunService protocolDryRunService,
                                      ProcessIntegrityRadarService processIntegrityRadarService,
                                      StrategicCopilotService strategicCopilotService,
                                      SettlementAdvisoryService settlementAdvisoryService,
                                      ProcessMaterialDossierService processMaterialDossierService,
                                      ProcessMaterialStrategyService processMaterialStrategyService,
                                      NationalProceduralRoutingService nationalProceduralRoutingService,
                                      ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService,
                                      ProceduralConnectorExecutionService proceduralConnectorExecutionService,
                                      InstitutionalMemoryService institutionalMemoryService,
                                      InstitutionalGovernanceContextService institutionalGovernanceContextService,
                                      ContextualPrecedentAdvisoryService contextualPrecedentAdvisoryService,
                                      ExplainableDecisionTrailService explainableDecisionTrailService,
                                      KernelOperationalGovernanceService kernelOperationalGovernanceService,
                                      LaianeSubmissionGuardrailService laianeSubmissionGuardrailService) {
        this.draftService = Objects.requireNonNull(draftService);
        this.validatorService = Objects.requireNonNull(validatorService);
        this.ritosCoverageService = Objects.requireNonNull(ritosCoverageService);
        this.laianeLawyerService = Objects.requireNonNull(laianeLawyerService);
        this.laianeProtocolService = Objects.requireNonNull(laianeProtocolService);
        this.triagemNacionalIAEngine = Objects.requireNonNull(triagemNacionalIAEngine);
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.territorialProcessualService = Objects.requireNonNull(territorialProcessualService);
        this.mapaCompetenciaDinamicoEngine = Objects.requireNonNull(mapaCompetenciaDinamicoEngine);
        this.radarPadroesService = Objects.requireNonNull(radarPadroesService);
        this.prontuarioNacionalService = Objects.requireNonNull(prontuarioNacionalService);
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.legalCoherenceEngine = Objects.requireNonNull(legalCoherenceEngine);
        this.protocolDryRunService = Objects.requireNonNull(protocolDryRunService);
        this.processIntegrityRadarService = Objects.requireNonNull(processIntegrityRadarService);
        this.strategicCopilotService = Objects.requireNonNull(strategicCopilotService);
        this.settlementAdvisoryService = Objects.requireNonNull(settlementAdvisoryService);
        this.processMaterialDossierService = Objects.requireNonNull(processMaterialDossierService);
        this.processMaterialStrategyService = Objects.requireNonNull(processMaterialStrategyService);
        this.nationalProceduralRoutingService = Objects.requireNonNull(nationalProceduralRoutingService);
        this.proceduralSubmissionBlueprintService = Objects.requireNonNull(proceduralSubmissionBlueprintService);
        this.proceduralConnectorExecutionService = Objects.requireNonNull(proceduralConnectorExecutionService);
        this.institutionalMemoryService = Objects.requireNonNull(institutionalMemoryService);
        this.institutionalGovernanceContextService = Objects.requireNonNull(institutionalGovernanceContextService);
        this.contextualPrecedentAdvisoryService = Objects.requireNonNull(contextualPrecedentAdvisoryService);
        this.explainableDecisionTrailService = Objects.requireNonNull(explainableDecisionTrailService);
        this.kernelOperationalGovernanceService = Objects.requireNonNull(kernelOperationalGovernanceService);
        this.laianeSubmissionGuardrailService = Objects.requireNonNull(laianeSubmissionGuardrailService);
    }

    public LaianePeticaoAssistResponse preflight(LaianePeticaoAssistRequest request) {
        return preflight(request, true, false);
    }

    public LaianePeticaoAssistResponse preflightLite(LaianePeticaoAssistRequest request) {
        return preflight(request, false, false);
    }

    public LaianePeticaoAssistResponse draftAndPreflight(LaianePeticaoAssistRequest request) {
        return preflight(request, true, true);
    }

    public LaianePeticaoAssistResponse draftAndPreflightLite(LaianePeticaoAssistRequest request) {
        return preflight(request, false, true);
    }

    public LaianePeticaoProtocolPackageResponse createProtocolPackage(LaianePeticaoAssistRequest request) {
        LaianePeticaoAssistRequest normalized = normalize(request);
        LaianePeticaoAssistResponse preflight = preflight(normalized, true, true);
        LaianeProtocolCreateRequest protocolRequest = LaianeProtocolCreateRequest.builder()
                .title(resolveProtocolTitle(normalized))
                .payload(new LinkedHashMap<>(preflight.getProtocolPayloadPreview()))
                .build();
        LaianeProtocolPackageDto protocolPackage = laianeProtocolService.create(protocolRequest);
        return LaianePeticaoProtocolPackageResponse.builder()
                .protocolPackage(protocolPackage)
                .preflight(preflight)
                .build();
    }

    private LaianePeticaoAssistResponse preflight(LaianePeticaoAssistRequest request, boolean includeAttachmentValidation, boolean forceDraft) {
        LaianePeticaoAssistRequest req = normalize(request);
        DraftBundle draftBundle = resolveDraft(req, forceDraft);
        String textoBase = firstNonBlank(draftBundle.content(), req.getDraftMarkdown(), req.getTextoFatosResumido(), "PETICAO EM ELABORACAO");
        var triagem = triagemNacionalIAEngine.triar(buildTriagemRequest(req, textoBase));
        CanonicalContext canonical = resolveCanonicalContext(req, triagem, textoBase);
        String ritoResolvido = resolveRito(req, triagem, canonical);
        String ramoResolvido = firstNonBlank(canonical.ramoDireito(), req.getRamoDireito());
        String classeResolvida = firstNonBlank(canonical.classeTpuCodigo(), req.getClasseTpu());
        String tipoJusticaResolvida = firstNonBlank(req.getTipoJustica(), canonical.ramoJusticaNacional());
        var validator = validatorService.validate(textoBase, ritoResolvido, classeResolvida, ramoResolvido);
        var teto = tetoProcessualService.diagnosticar(
                req.getValorCausa(),
                TipoJustica.fromString(tipoJusticaResolvida),
                RamoDireito.fromString(ramoResolvido),
                ritoResolvido,
                null,
                LocalDate.now()
        );
        DynamicCompetenceDistributionResponse competencia = mapaCompetenciaDinamicoEngine
                .distribuir(buildCompetenceRequest(req, canonical))
                .orElse(null);
        var radar = radarPadroesService.analisar(buildRadarContext(req, textoBase, canonical));
        ProntuarioNacionalService.AnaliseConflitoProcessual conflito = resolveConflito(req, canonical);
        var coverage = ritosCoverageService.coverage();
        LaianeLawyerAttachmentValidationResponse attachmentValidation = includeAttachmentValidation
                ? laianeLawyerService.validateAttachments(buildAttachmentRequest(req, triagem, canonical))
                : null;
        LegalCoherenceReport coherenceReport = legalCoherenceEngine.analyzeRequest(req, canonical, ritoResolvido, validator, attachmentValidation, competencia, teto, radar, conflito);
        ProceduralRoutingReport proceduralRouting = nationalProceduralRoutingService.analyzeRequest(req);
        Processo syntheticProcess = buildSyntheticProcess(req, canonical, ritoResolvido);
        var territorial = territorialProcessualService.diagnosticar(syntheticProcess, proceduralRouting);
        double readinessScore = calculateReadiness(validator, triagem, teto, territorial, competencia, radar, attachmentValidation, conflito);
        ProtocolDryRunReport protocolDryRun = protocolDryRunService.simulateRequest(req, canonical, ritoResolvido, validator, attachmentValidation, competencia, teto, coherenceReport, readinessScore);
        ProcessIntegrityRadarReport integrityRadar = processIntegrityRadarService.analyzeRequest(req, canonical, ritoResolvido, coherenceReport, protocolDryRun, competencia, teto);
        ProcessMaterialDossierReport materialDossier = processMaterialDossierService.analyzeRequest(req, canonical, ritoResolvido);
        ProcessMaterialStrategyReport materialStrategy = processMaterialStrategyService.analyzeRequest(req, canonical, ritoResolvido, materialDossier, readinessScore, buildNegotiationSignals(req, canonical, competencia));
        ProceduralSubmissionBlueprintReport submissionBlueprint = proceduralSubmissionBlueprintService.analyzeProcess(syntheticProcess, proceduralRouting);
        ProceduralConnectorExecutionReport connectorExecution = proceduralConnectorExecutionService.analyzeProcess(syntheticProcess, proceduralRouting, submissionBlueprint);
        SettlementAdvisoryReport settlementAdvisory = req.getValorCausa() == null ? null : settlementAdvisoryService.analyze(
                syntheticProcess,
                ritoResolvido,
                req.getValorCausa(),
                mergeNegotiationSignals(req, canonical, competencia, materialDossier, materialStrategy),
                integrityRadar
        );
        StrategicCopilotReport strategicCopilot = strategicCopilotService.analyzeRequest(req, canonical, ritoResolvido, coherenceReport, protocolDryRun, integrityRadar, competencia);
        InstitutionalMemoryReport institutionalMemory = institutionalMemoryService.analyzeRequest(req, canonical, ritoResolvido, competencia, coherenceReport, integrityRadar, strategicCopilot);
        ContextualPrecedentAdvisoryReport precedentAdvisory = contextualPrecedentAdvisoryService.analyzeRequest(req, canonical, ritoResolvido, competencia, coherenceReport, strategicCopilot);
        ExplainableDecisionTrailReport explainableDecisionTrail = explainableDecisionTrailService.composeRequest(req, canonical, ritoResolvido, competencia, coherenceReport, protocolDryRun, integrityRadar, strategicCopilot, institutionalMemory, precedentAdvisory);
        InstitutionalGovernanceContextReport institutionalGovernanceContext = institutionalGovernanceContextService.analyzeRequest(req, canonical, ritoResolvido, coherenceReport, institutionalMemory, precedentAdvisory);
        KernelOperationalGovernanceReport kernelOperationalGovernance = kernelOperationalGovernanceService.analyzeRequest(req, ritoResolvido, coherenceReport, protocolDryRun, integrityRadar, institutionalGovernanceContext);
        KernelAdvisoryTelemetry advisoryTelemetry = kernelOperationalGovernanceService.buildTelemetry("PETITION_ASSIST", ritoResolvido, validator, triagem, coherenceReport, protocolDryRun, integrityRadar, materialDossier, materialStrategy, strategicCopilot, settlementAdvisory, institutionalMemory, precedentAdvisory, explainableDecisionTrail, institutionalGovernanceContext, kernelOperationalGovernance);
        List<String> orientacoes = mergeDistinct(
                buildOrientacoes(validator, triagem, teto, territorial, competencia, radar, attachmentValidation, conflito, coherenceReport, protocolDryRun, integrityRadar, materialDossier, materialStrategy, strategicCopilot, settlementAdvisory, institutionalMemory, precedentAdvisory, explainableDecisionTrail, institutionalGovernanceContext, kernelOperationalGovernance),
                proceduralRouting != null ? proceduralRouting.reasons() : List.of(),
                proceduralRouting != null ? proceduralRouting.blockingIssues() : List.of(),
                proceduralRouting != null ? proceduralRouting.reviewChecklist() : List.of(),
                proceduralRouting != null && proceduralRouting.economicGate() != null ? proceduralRouting.economicGate().reasons() : List.of(),
                proceduralRouting != null && proceduralRouting.economicGate() != null ? proceduralRouting.economicGate().rerouteOptions() : List.of(),
                proceduralRouting != null && proceduralRouting.economicGate() != null ? proceduralRouting.economicGate().reviewChecklist() : List.of(),
                territorial != null ? territorial.alertas() : List.of(),
                territorial != null ? territorial.reviewChecklist() : List.of(),
                safeSingleton(territorial == null ? null : territorial.sugestaoOperacional())
        );
        boolean prontaParaProtocolo = readinessScore >= 0.80
                && validator.isOk()
                && triagem.aprovado()
                && !teto.bloqueante()
                && (territorial == null || !territorial.bloqueante())
                && !radar.temCritico()
                && (attachmentValidation == null || attachmentValidation.isOk())
                && (competencia == null || competencia.distribuicaoAutomatica())
                && !coherenceReport.blocking()
                && protocolDryRun.apto()
                && !integrityRadar.blocking();

        Map<String, Object> protocolPayloadPreview = buildProtocolPayload(req, textoBase, draftBundle.warnings(), validator, triagem, teto, territorial, competencia, radar, conflito, attachmentValidation, prontaParaProtocolo, readinessScore, canonical, materialDossier, materialStrategy);
        if (proceduralRouting != null) {
            protocolPayloadPreview.put("proceduralRouting", proceduralRouting.toMap());
        }
        if (submissionBlueprint != null) {
            protocolPayloadPreview.put("submissionBlueprint", submissionBlueprint.toMap());
        }
        if (connectorExecution != null) {
            protocolPayloadPreview.put("connectorExecution", connectorExecution.toMap());
        }
        protocolPayloadPreview = laianeSubmissionGuardrailService.enrichPayload(protocolPayloadPreview);

        return LaianePeticaoAssistResponse.builder()
                .requestId("LAI-PET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT))
                .generatedAt(Instant.now())
                .draftMarkdown(textoBase)
                .draftWarnings(draftBundle.warnings())
                .validator(validator)
                .attachmentValidation(attachmentValidation)
                .triagem(triagem)
                .competencia(competencia)
                .tetoProcessual(teto)
                .territorialProcessual(territorial)
                .radar(radar)
                .conflitoProcessual(conflito)
                .ritosCoverage(coverage)
                .prontaParaProtocolo(prontaParaProtocolo)
                .readinessScore(round(readinessScore))
                .orientacoes(orientacoes)
                .protocolPayloadPreview(protocolPayloadPreview)
                .coherenceReport(coherenceReport)
                .protocolDryRun(protocolDryRun)
                .integrityRadar(integrityRadar)
                .materialDossier(materialDossier)
                .materialStrategy(materialStrategy)
                .proceduralRouting(proceduralRouting)
                .submissionBlueprint(submissionBlueprint)
                .connectorExecution(connectorExecution)
                .strategicCopilot(strategicCopilot)
                .settlementAdvisory(settlementAdvisory)
                .institutionalMemory(institutionalMemory)
                .precedentAdvisory(precedentAdvisory)
                .explainableDecisionTrail(explainableDecisionTrail)
                .institutionalGovernanceContext(institutionalGovernanceContext)
                .kernelOperationalGovernance(kernelOperationalGovernance)
                .advisoryTelemetry(advisoryTelemetry)
                .strategicInsights(buildStrategicInsights(coherenceReport, protocolDryRun, integrityRadar, materialDossier, materialStrategy, submissionBlueprint, connectorExecution, strategicCopilot, settlementAdvisory, institutionalMemory, precedentAdvisory, explainableDecisionTrail, institutionalGovernanceContext, kernelOperationalGovernance))
                .build();
    }

    private DraftBundle resolveDraft(LaianePeticaoAssistRequest request, boolean forceDraft) {
        String provided = trimToNull(request.getDraftMarkdown());
        if (!forceDraft && provided != null) {
            return new DraftBundle(provided, List.of());
        }
        if (!forceDraft  && (request.getCtx() == null || request.getCtx().isEmpty()) && trimToNull(request.getKind()) == null) {
            return new DraftBundle(firstNonBlank(request.getTextoFatosResumido(), "PETICAO EM ELABORACAO"), List.of());
        }
        LaianeDraftRequest draftRequest = LaianeDraftRequest.builder()
                .kind(firstNonBlank(request.getKind(), "PETICAO_INICIAL"))
                .ctx(request.getCtx() != null ? request.getCtx() : Map.of())
                .build();
        LaianeDraftResponse response = draftService.draft(draftRequest);
        List<String> warnings = response.getWarnings() == null ? List.of() : List.copyOf(new LinkedHashSet<>(response.getWarnings()));
        return new DraftBundle(firstNonBlank(response.getDraftMarkdown(), provided, request.getTextoFatosResumido(), "PETICAO EM ELABORACAO"), warnings);
    }

    private TriagemNacionalIAEngine.PedidoTriagem buildTriagemRequest(LaianePeticaoAssistRequest request, String textoBase) {
        return new TriagemNacionalIAEngine.PedidoTriagem(
                firstNonBlank(request.getNupn(), "LAIANE-" + UUID.randomUUID()),
                request.getClasseTpu(),
                request.getAssuntoTpu(),
                request.getRamoDireito(),
                request.getValorCausa(),
                firstNonBlank(request.getTextoFatosResumido(), textoBase),
                request.getCpfCnpjAutor(),
                request.getCpfCnpjReu(),
                request.getOabAdvogado(),
                request.getUfAdvogado(),
                request.getDocumentosAnexados() == null ? List.of() : request.getDocumentosAnexados(),
                request.getDataFatoGerador(),
                Boolean.TRUE.equals(request.getRequerLiminar()),
                Boolean.TRUE.equals(request.getAtoJurisdicionalAnterior()),
                request.getProcessoId()
        );
    }

    private DynamicCompetenceDistributionRequest buildCompetenceRequest(LaianePeticaoAssistRequest request, CanonicalContext canonical) {
        return new DynamicCompetenceDistributionRequest(
                request.getNupn(),
                firstNonBlank(canonical.classeTpuCodigo(), request.getClasseTpu()),
                request.getAssuntoTpu(),
                firstNonBlank(canonical.ramoDireito(), request.getRamoDireito()),
                request.getValorCausa(),
                request.getUfAutor(),
                request.getComarcaAutor(),
                request.getUfReu(),
                request.getComarcaReu(),
                Boolean.TRUE.equals(request.getRequerJuizadoEspecial()),
                Boolean.TRUE.equals(request.getRequerVaraEspecializada()),
                firstNonBlank(request.getMateriaPrincipal(), canonical.classeTpuNome(), canonical.ramoDireito()),
                firstNonBlank(request.getTipoJustica(), canonical.ramoJusticaNacional()),
                Boolean.TRUE.equals(request.getCasoUrgente()),
                Boolean.TRUE.equals(request.getPreferenciaDigital()),
                request.getProcessoId()
        );
    }

    private RadarPadroesService.ContextoRadar buildRadarContext(LaianePeticaoAssistRequest request, String textoBase, CanonicalContext canonical) {
        RadarPadroesService.ContextoRadar contexto = new RadarPadroesService.ContextoRadar(
                request.getProcessoId(),
                firstNonBlank(request.getNupn(), "LAIANE-" + UUID.randomUUID()),
                request.getCpfCnpjAutor(),
                request.getCpfCnpjReu(),
                firstNonBlank(request.getEscritorioOab(), request.getUfAdvogado() != null && request.getOabAdvogado() != null ? request.getUfAdvogado() + ":" + request.getOabAdvogado() : null),
                canonical.tribunalCodigo(),
                firstNonBlank(canonical.ramoDireito(), request.getRamoDireito()),
                firstNonBlank(canonical.classeTpuCodigo(), request.getClasseTpu()),
                request.getAssuntoTpu(),
                request.getValorCausa(),
                firstNonBlank(request.getTextoFatosResumido(), textoBase),
                LocalDate.now(),
                null,
                null,
                null
        );
        return new RadarPadroesService.ContextoRadar(
                contexto.processoId(),
                contexto.nupn(),
                contexto.documentoAutor(),
                contexto.documentoReu(),
                contexto.escritorioOab(),
                contexto.tribunalCodigo(),
                contexto.ramoDireito(),
                contexto.classeProcessual(),
                contexto.assunto(),
                contexto.valorCausa(),
                contexto.resumoFatos(),
                contexto.dataAjuizamento(),
                contexto.statusProcesso(),
                contexto.resultadoFinal(),
                radarPadroesService.construirFingerprint(contexto)
        );
    }

    private ProntuarioNacionalService.AnaliseConflitoProcessual resolveConflito(LaianePeticaoAssistRequest request, CanonicalContext canonical) {
        try {
            if (trimToNull(request.getCpfCnpjAutor()) == null || trimToNull(request.getCpfCnpjReu()) == null) {
                return null;
            }
            RamoDireito ramo = RamoDireito.fromString(firstNonBlank(canonical.ramoDireito(), request.getRamoDireito()));
            if (ramo == null) {
                return null;
            }
            return prontuarioNacionalService.detectarLitispendenciaOuCoisaJulgada(request.getCpfCnpjAutor(), request.getCpfCnpjReu(), ramo);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LaianeLawyerAttachmentValidationRequest buildAttachmentRequest(LaianePeticaoAssistRequest request,
                                                                           TriagemNacionalIAEngine.ResultadoTriagem triagem,
                                                                           CanonicalContext canonical) {
        return LaianeLawyerAttachmentValidationRequest.builder()
                .rito(resolveRito(request, triagem, canonical))
                .anexos(request.getDocumentosAnexados() == null ? List.of() : request.getDocumentosAnexados())
                .build();
    }

    private String resolveRito(LaianePeticaoAssistRequest request,
                               TriagemNacionalIAEngine.ResultadoTriagem triagem,
                               CanonicalContext canonical) {
        return firstNonBlank(
                canonical.rito() != null ? canonical.rito().name() : null,
                request.getRitoSugerido(),
                triagem != null && triagem.competencia() != null ? triagem.competencia().ritoSugerido() : null,
                proceduralCatalogService.resolveRito(request.getRitoSugerido(), request.getRamoDireito(), request.getClasseTpu()).name(),
                "COMUM_ORDINARIO"
        );
    }

    private CanonicalContext resolveCanonicalContext(LaianePeticaoAssistRequest request,
                                                    TriagemNacionalIAEngine.ResultadoTriagem triagem,
                                                    String textoBase) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rito", firstNonBlank(request.getRitoSugerido(), triagem != null && triagem.competencia() != null ? triagem.competencia().ritoSugerido() : null));
        payload.put("classeTpu", firstNonBlank(request.getClasseTpu(), triagem != null && triagem.classificacao() != null ? triagem.classificacao().classeTpu() : null));
        payload.put("classe", firstNonBlank(request.getClasseTpu(), triagem != null && triagem.classificacao() != null ? triagem.classificacao().classeTpu() : null));
        payload.put("ramoDireito", request.getRamoDireito());
        payload.put("tipoJustica", request.getTipoJustica());
        payload.put("materia", firstNonBlank(request.getMateriaPrincipal(), request.getAssuntoTpu()));
        payload.put("texto", firstNonBlank(request.getTextoFatosResumido(), textoBase));
        payload.put("narrativa", firstNonBlank(request.getTextoFatosResumido(), textoBase));
        payload.put("uf", firstNonBlank(request.getUfAutor(), request.getUfReu(), request.getUfAdvogado()));
        payload.put("tribunalCodigo", extractString(request != null ? request.getCtx() : null, "tribunalCodigo"));
        return proceduralCanonicalResolver.resolve(payload);
    }

    private String resolveProtocolTitle(LaianePeticaoAssistRequest request) {
        return firstNonBlank(request.getProtocolTitle(), "Pacote Laiane - " + firstNonBlank(request.getClasseTpu(), request.getAssuntoTpu(), "Peticao"));
    }

    private List<String> buildOrientacoes(LaianePeticaoValidateResponse validator,
                                          TriagemNacionalIAEngine.ResultadoTriagem triagem,
                                          TetoProcessualService.DiagnosticoTetoProcessual teto,
                                          TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
                                          DynamicCompetenceDistributionResponse competencia,
                                          RadarPadroesService.AnaliseRadarResultado radar,
                                          LaianeLawyerAttachmentValidationResponse attachmentValidation,
                                          ProntuarioNacionalService.AnaliseConflitoProcessual conflito,
                                          LegalCoherenceReport coherenceReport,
                                          ProtocolDryRunReport protocolDryRun,
                                          ProcessIntegrityRadarReport integrityRadar,
                                          ProcessMaterialDossierReport materialDossier,
                                          ProcessMaterialStrategyReport materialStrategy,
                                          StrategicCopilotReport strategicCopilot,
                                          SettlementAdvisoryReport settlementAdvisory,
                                          InstitutionalMemoryReport institutionalMemory,
                                          ContextualPrecedentAdvisoryReport precedentAdvisory,
                                          ExplainableDecisionTrailReport explainableDecisionTrail,
                                          InstitutionalGovernanceContextReport institutionalGovernanceContext,
                                          KernelOperationalGovernanceReport kernelOperationalGovernance) {
        LinkedHashSet<String> orientacoes = new LinkedHashSet<>();
        if (!validator.isOk()) {
            orientacoes.add("Corrija os erros estruturais da peticao antes do protocolo.");
        }
        if (coherenceReport != null && coherenceReport.blocking()) {
            orientacoes.add("A coerencia juridica identificou bloqueios materiais. Corrija a estrategia antes do protocolo.");
        }
        if (protocolDryRun != null && !protocolDryRun.apto()) {
            orientacoes.add("O ensaio de protocolo identificou pontos impeditivos ou dependentes de revisao adicional.");
        }
        if (validator.getWarnings() != null && !validator.getWarnings().isEmpty()) {
            orientacoes.add("Revise os placeholders e campos incompletos apontados pela validacao da Laiane.");
        }
        if (triagem.veredito() == TriagemNacionalIAEngine.VereditoTriagem.BLOQUEADO) {
            orientacoes.add("A triagem nacional bloqueou o fluxo. Ajuste competencia, valor da causa, documentos ou duplicidade antes de prosseguir.");
        } else if (triagem.veredito() == TriagemNacionalIAEngine.VereditoTriagem.PENDENTE_CORRECAO || triagem.veredito() == TriagemNacionalIAEngine.VereditoTriagem.REQUER_REVISAO_HUMANA) {
            orientacoes.add("A triagem exige correcao ou revisao humana antes do protocolo definitivo.");
        }
        if (teto.bloqueante()) {
            orientacoes.add("O valor da causa ultrapassa a alcada economica indicada para o rito ou competencia atual. Recalcule antes do protocolo.");
        } else if (teto.proximoAoLimite()) {
            orientacoes.add("O valor da causa esta muito proximo do teto economico da competencia sugerida. Confirme memoria de calculo e eventual renuncia ao excedente.");
        }
        if (territorial != null) {
            if (territorial.bloqueante()) {
                orientacoes.add(firstNonBlank(territorial.fundamentoLegal(), "A malha territorial detectou incoerência bloqueante entre foro, comarca, unidade ou jurisdição."));
            } else if (territorial.alerta()) {
                orientacoes.add(firstNonBlank(territorial.fundamentoLegal(), "A malha territorial recomenda revisão humana antes do protocolo definitivo."));
            }
            orientacoes.addAll(territorial.alertas());
            orientacoes.addAll(territorial.reviewChecklist());
            String sugestaoTerritorial = firstNonBlank(territorial.sugestaoOperacional(), null);
            if (sugestaoTerritorial != null) {
                orientacoes.add(sugestaoTerritorial);
            }
        }
        if (competencia != null && !competencia.distribuicaoAutomatica()) {
            orientacoes.add("A distribuicao automatica apontou necessidade de revisao humana da competencia territorial ou especializada.");
        }
        if (attachmentValidation != null && !attachmentValidation.isOk()) {
            orientacoes.add("Ha anexos obrigatorios faltantes para o rito indicado pela Laiane.");
        }
        if (radar.temCritico()) {
            orientacoes.add("O radar identificou padrao critico. Revise repeticao estrutural, valor da causa e documentos antes de protocolar.");
        }
        if (conflito != null && (conflito.litispendenciaPotencial() || conflito.coisaJulgadaPotencial())) {
            orientacoes.add("O prontuario nacional encontrou conflito potencial com processos anteriores. Reforce distincao ou previna duplicidade.");
        }
        if (integrityRadar != null) {
            orientacoes.addAll(integrityRadar.nextActions());
        }
        if (materialDossier != null) {
            orientacoes.add(materialDossier.objectLabel());
            orientacoes.add(materialDossier.primaryRelief());
            orientacoes.addAll(materialDossier.proofGaps());
            orientacoes.addAll(materialDossier.protocolChecklist());
        }
        if (materialStrategy != null) {
            orientacoes.add(materialStrategy.litigationPosture());
            orientacoes.add(materialStrategy.protocolReadiness());
            orientacoes.add(materialStrategy.negotiationStance());
            orientacoes.add(materialStrategy.evidenceReadiness());
            orientacoes.addAll(materialStrategy.protocolBlockers());
            orientacoes.addAll(materialStrategy.evidenceAgenda());
            orientacoes.addAll(materialStrategy.executionChecklist());
        }
        if (strategicCopilot != null) {
            strategicCopilot.immediateActions().stream().map(StrategicCopilotReport.Action::title).forEach(orientacoes::add);
            strategicCopilot.proceduralActions().stream().map(StrategicCopilotReport.Action::title).forEach(orientacoes::add);
        }
        if (settlementAdvisory != null) {
            orientacoes.addAll(settlementAdvisory.nextMoves());
        }
        if (institutionalMemory != null) {
            orientacoes.addAll(institutionalMemory.reusablePlaybooks());
            orientacoes.addAll(institutionalMemory.officeAlerts());
        }
        if (precedentAdvisory != null) {
            orientacoes.addAll(precedentAdvisory.narrativeAngles());
            orientacoes.addAll(precedentAdvisory.cautionPoints());
        }
        if (explainableDecisionTrail != null) {
            orientacoes.addAll(explainableDecisionTrail.openQuestions());
        }
        if (institutionalGovernanceContext != null) {
            orientacoes.addAll(institutionalGovernanceContext.policyGuards());
            orientacoes.addAll(institutionalGovernanceContext.governanceAlerts());
        }
        if (kernelOperationalGovernance != null) {
            orientacoes.addAll(kernelOperationalGovernance.nextActions());
            orientacoes.addAll(kernelOperationalGovernance.watchpoints());
        }
        if (orientacoes.isEmpty()) {
            orientacoes.add("Peticao consistente para protocolo assistido pela Laiane, mantida a revisao final do advogado.");
        }
        return List.copyOf(orientacoes);
    }

    private double calculateReadiness(LaianePeticaoValidateResponse validator,
                                      TriagemNacionalIAEngine.ResultadoTriagem triagem,
                                      TetoProcessualService.DiagnosticoTetoProcessual teto,
                                      TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
                                      DynamicCompetenceDistributionResponse competencia,
                                      RadarPadroesService.AnaliseRadarResultado radar,
                                      LaianeLawyerAttachmentValidationResponse attachmentValidation,
                                      ProntuarioNacionalService.AnaliseConflitoProcessual conflito) {
        double score = 1.0;
        if (!validator.isOk()) {
            score -= 0.30;
        }
        score -= Math.min(0.15, sizeOf(validator.getWarnings()) * 0.03);
        switch (triagem.veredito()) {
            case BLOQUEADO -> score -= 0.35;
            case PENDENTE_CORRECAO -> score -= 0.25;
            case REQUER_REVISAO_HUMANA -> score -= 0.18;
            case APROVADO_COM_RESSALVAS -> score -= 0.08;
            default -> score -= 0.0;
        }
        if (teto.bloqueante()) {
            score -= 0.25;
        } else if (teto.proximoAoLimite()) {
            score -= 0.10;
        }
        if (territorial != null) {
            if (territorial.bloqueante()) {
                score -= 0.18;
            } else if (territorial.alerta()) {
                score -= 0.08;
            }
        }
        if (competencia != null && !competencia.distribuicaoAutomatica()) {
            score -= 0.08;
        }
        if (attachmentValidation != null && !attachmentValidation.isOk()) {
            score -= 0.12;
        }
        if (radar.temCritico()) {
            score -= 0.12;
        } else {
            score -= Math.min(0.08, sizeOf(radar.alertas()) * 0.02);
        }
        if (conflito != null) {
            if (conflito.litispendenciaPotencial()) {
                score -= 0.12;
            }
            if (conflito.coisaJulgadaPotencial()) {
                score -= 0.15;
            }
        }
        return Math.max(0.0, Math.min(1.0, score));
    }

    private Map<String, Object> buildProtocolPayload(LaianePeticaoAssistRequest request,
                                                     String draftMarkdown,
                                                     List<String> draftWarnings,
                                                     LaianePeticaoValidateResponse validator,
                                                     TriagemNacionalIAEngine.ResultadoTriagem triagem,
                                                     TetoProcessualService.DiagnosticoTetoProcessual teto,
                                                     TerritorialProcessualService.DiagnosticoTerritorialProcessual territorial,
                                                     DynamicCompetenceDistributionResponse competencia,
                                                     RadarPadroesService.AnaliseRadarResultado radar,
                                                     ProntuarioNacionalService.AnaliseConflitoProcessual conflito,
                                                     LaianeLawyerAttachmentValidationResponse attachmentValidation,
                                                     boolean prontaParaProtocolo,
                                                     double readinessScore,
                                                     CanonicalContext canonical,
                                                     ProcessMaterialDossierReport materialDossier,
                                                     ProcessMaterialStrategyReport materialStrategy) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channel", "laiane_peticao_assistida");
        payload.put("nupn", request.getNupn());
        payload.put("processoId", request.getProcessoId());
        payload.put("classeTpu", firstNonBlank(canonical.classeTpuCodigo(), request.getClasseTpu()));
        payload.put("assuntoTpu", request.getAssuntoTpu());
        payload.put("ramoDireito", firstNonBlank(canonical.ramoDireito(), request.getRamoDireito()));
        payload.put("valorCausa", request.getValorCausa());
        payload.put("tipoJustica", firstNonBlank(request.getTipoJustica(), canonical.ramoJusticaNacional()));
        payload.put("competenciaSugerida", competencia != null ? competencia.unidadeCodigo() : null);
        payload.put("tribunalCodigo", firstNonBlank(competencia != null ? competencia.tribunalCodigo() : null, canonical.tribunalCodigo()));
        payload.put("ritoSugerido", resolveRito(request, triagem, canonical));
        payload.put("canonicalContext", canonical.toMap());
        payload.put("draftMarkdown", draftMarkdown);
        payload.put("draftWarnings", draftWarnings);
        Map<String, Object> validatorMap = new LinkedHashMap<>();
        validatorMap.put("ok", validator.isOk());
        validatorMap.put("errors", validator.getErrors());
        validatorMap.put("warnings", validator.getWarnings());
        payload.put("validator", validatorMap);

        Map<String, Object> triagemMap = new LinkedHashMap<>();
        triagemMap.put("veredito", triagem.veredito().name());
        triagemMap.put("resumo", triagem.resumoDecisao());
        triagemMap.put("confiancaGeral", triagem.confiancaGeral());
        triagemMap.put("aprovacaoAutomatica", triagem.aprovacaoAutomatica());
        payload.put("triagem", triagemMap);

        Map<String, Object> tetoMap = new LinkedHashMap<>();
        tetoMap.put("violacao", teto.violacao());
        tetoMap.put("bloqueante", teto.bloqueante());
        tetoMap.put("alerta", teto.alerta());
        tetoMap.put("codigoDiagnostico", teto.codigoDiagnostico());
        tetoMap.put("fundamentoLegal", teto.fundamentoLegal());
        tetoMap.put("competenciaSugerida", teto.competenciaSugerida());
        tetoMap.put("ritoSugerido", teto.ritoSugerido());
        payload.put("tetoProcessual", tetoMap);

        Map<String, Object> territorialMap = new LinkedHashMap<>();
        if (territorial != null) {
            territorialMap.put("violacao", territorial.violacao());
            territorialMap.put("bloqueante", territorial.bloqueante());
            territorialMap.put("alerta", territorial.alerta());
            territorialMap.put("codigoDiagnostico", territorial.codigoDiagnostico());
            territorialMap.put("fundamentoLegal", territorial.fundamentoLegal());
            territorialMap.put("territorialMode", territorial.territorialMode());
            territorialMap.put("comarcaSugerida", territorial.comarcaSugerida());
            territorialMap.put("ufSugerida", territorial.ufSugerida());
            territorialMap.put("varaSugerida", territorial.varaSugerida());
            territorialMap.put("foroSugerido", territorial.foroSugerido());
            territorialMap.put("sugestaoOperacional", territorial.sugestaoOperacional());
            territorialMap.put("alertas", territorial.alertas());
            territorialMap.put("reviewChecklist", territorial.reviewChecklist());
        }
        payload.put("territorialProcessual", territorialMap.isEmpty() ? null : territorialMap);

        Map<String, Object> radarMap = new LinkedHashMap<>();
        radarMap.put("scoreGeral", radar.scoreGeral());
        radarMap.put("quantidadeAlertas", sizeOf(radar.alertas()));
        radarMap.put("temCritico", radar.temCritico());
        radarMap.put("resumoTecnico", radar.resumoTecnico());
        payload.put("radar", radarMap);

        if (conflito != null) {
            Map<String, Object> conflitoMap = new LinkedHashMap<>();
            conflitoMap.put("litispendenciaSuspeita", conflito.litispendenciaPotencial());
            conflitoMap.put("coisaJulgadaSuspeita", conflito.coisaJulgadaPotencial());
            conflitoMap.put("nupnsAtivos", conflito.nupnsEmAndamento());
            conflitoMap.put("nupnsEncerrados", conflito.nupnsArquivados());
            payload.put("conflitoProcessual", conflitoMap);
        } else {
            payload.put("conflitoProcessual", null);
        }

        if (attachmentValidation != null) {
            Map<String, Object> attachmentMap = new LinkedHashMap<>();
            attachmentMap.put("ok", attachmentValidation.isOk());
            attachmentMap.put("rito", attachmentValidation.getRito());
            attachmentMap.put("missing", attachmentValidation.getMissing());
            payload.put("attachmentValidation", attachmentMap);
        } else {
            payload.put("attachmentValidation", null);
        }
        payload.put("documentosAnexados", request.getDocumentosAnexados());
        if (materialDossier != null) {
            Map<String, Object> dossierMap = new LinkedHashMap<>();
            dossierMap.put("objectLabel", materialDossier.objectLabel());
            dossierMap.put("primaryRelief", materialDossier.primaryRelief());
            dossierMap.put("evidentiaryBracket", materialDossier.evidentiaryBracket());
            dossierMap.put("negotiationBracket", materialDossier.negotiationBracket());
            dossierMap.put("controversyAxes", materialDossier.controversyAxes());
            dossierMap.put("evidenceAnchors", materialDossier.evidenceAnchors());
            dossierMap.put("proofGaps", materialDossier.proofGaps());
            dossierMap.put("protocolChecklist", materialDossier.protocolChecklist());
            payload.put("materialDossier", dossierMap);
        } else {
            payload.put("materialDossier", null);
        }
        if (materialStrategy != null) {
            Map<String, Object> strategyMap = new LinkedHashMap<>();
            strategyMap.put("litigationPosture", materialStrategy.litigationPosture());
            strategyMap.put("protocolReadiness", materialStrategy.protocolReadiness());
            strategyMap.put("negotiationStance", materialStrategy.negotiationStance());
            strategyMap.put("evidenceReadiness", materialStrategy.evidenceReadiness());
            strategyMap.put("pleadingBlueprint", materialStrategy.pleadingBlueprint());
            strategyMap.put("evidenceAgenda", materialStrategy.evidenceAgenda());
            strategyMap.put("protocolBlockers", materialStrategy.protocolBlockers());
            strategyMap.put("negotiationGuardrails", materialStrategy.negotiationGuardrails());
            strategyMap.put("executionChecklist", materialStrategy.executionChecklist());
            strategyMap.put("controlPoints", materialStrategy.controlPoints());
            payload.put("materialStrategy", strategyMap);
        } else {
            payload.put("materialStrategy", null);
        }
        payload.put("prontaParaProtocolo", prontaParaProtocolo);
        payload.put("readinessScore", round(readinessScore));
        return payload;
    }

    private LaianePeticaoAssistRequest normalize(LaianePeticaoAssistRequest request) {
        LaianePeticaoAssistRequest source = request == null ? new LaianePeticaoAssistRequest() : request;
        source.setCtx(sanitizeCtx(source.getCtx()));
        source.setDocumentosAnexados(sanitizeStringList(source.getDocumentosAnexados()));
        return source;
    }

    private static LinkedHashMap<String, Object> sanitizeCtx(Map<String, Object> source) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        source.forEach((key, value) -> {
            String normalizedKey = trimToNull(key);
            Object normalizedValue = sanitizeCtxValue(value);
            if (normalizedKey != null && normalizedValue != null) {
                out.put(normalizedKey, normalizedValue);
            }
        });
        return out;
    }

    private static Object sanitizeCtxValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return trimToNull(text);
        }
        if (value instanceof List<?> list) {
            ArrayList<Object> out = new ArrayList<>();
            for (Object item : list) {
                Object normalized = sanitizeCtxValue(item);
                if (normalized != null) {
                    out.add(normalized);
                }
            }
            return out.isEmpty() ? null : List.copyOf(out);
        }
        if (value instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String normalizedKey = trimToNull(entry.getKey() == null ? null : String.valueOf(entry.getKey()));
                Object normalizedValue = sanitizeCtxValue(entry.getValue());
                if (normalizedKey != null && normalizedValue != null) {
                    out.put(normalizedKey, normalizedValue);
                }
            }
            return out.isEmpty() ? null : Map.copyOf(out);
        }
        return value;
    }

    private static ArrayList<String> sanitizeStringList(List<String> source) {
        ArrayList<String> out = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return out;
        }
        LinkedHashSet<String> deduplicated = new LinkedHashSet<>();
        for (String value : source) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                deduplicated.add(normalized);
            }
        }
        out.addAll(deduplicated);
        return out;
    }

    @SafeVarargs
    private static List<String> mergeDistinct(List<String>... groups) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (groups != null) {
            for (List<String> group : groups) {
                if (group == null) {
                    continue;
                }
                for (String item : group) {
                    if (item != null && !item.isBlank()) {
                        out.add(item.trim());
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    private static int sizeOf(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static List<String> safeSingleton(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? List.of() : List.of(normalized);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String trimmed = trimToNull(value);
            if (trimmed != null) {
                return trimmed;
            }
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }


private static String extractString(Map<String, Object> source, String key) {
    if (source == null || key == null || key.isBlank()) {
        return null;
    }
    Object value = source.get(key);
    if (value == null) {
        return null;
    }
    String text = String.valueOf(value).trim();
    return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
}

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }

    private List<String> buildStrategicInsights(LegalCoherenceReport coherenceReport,
                                               ProtocolDryRunReport protocolDryRun,
                                               ProcessIntegrityRadarReport integrityRadar,
                                               ProcessMaterialDossierReport materialDossier,
                                               ProcessMaterialStrategyReport materialStrategy,
                                               ProceduralSubmissionBlueprintReport submissionBlueprint,
                                               ProceduralConnectorExecutionReport connectorExecution,
                                               StrategicCopilotReport strategicCopilot,
                                               SettlementAdvisoryReport settlementAdvisory,
                                               InstitutionalMemoryReport institutionalMemory,
                                               ContextualPrecedentAdvisoryReport precedentAdvisory,
                                               ExplainableDecisionTrailReport explainableDecisionTrail,
                                               InstitutionalGovernanceContextReport institutionalGovernanceContext,
                                               KernelOperationalGovernanceReport kernelOperationalGovernance) {
        LinkedHashSet<String> insights = new LinkedHashSet<>();
        if (coherenceReport != null) {
            insights.addAll(coherenceReport.strengths());
            insights.addAll(coherenceReport.strategicRecommendations());
        }
        if (protocolDryRun != null) {
            insights.addAll(protocolDryRun.nextActions());
        }
        if (integrityRadar != null) {
            insights.addAll(integrityRadar.nextActions());
            insights.addAll(integrityRadar.watchpoints());
        }
        if (materialDossier != null) {
            insights.add(materialDossier.objectLabel());
            insights.add(materialDossier.primaryRelief());
            insights.addAll(materialDossier.controversyAxes());
            insights.addAll(materialDossier.thesisVectors());
            insights.addAll(materialDossier.evidenceAnchors());
            insights.addAll(materialDossier.proofGaps());
            insights.addAll(materialDossier.protocolChecklist());
        }
        if (materialStrategy != null) {
            insights.add(materialStrategy.litigationPosture());
            insights.add(materialStrategy.protocolReadiness());
            insights.add(materialStrategy.negotiationStance());
            insights.add(materialStrategy.evidenceReadiness());
            insights.addAll(materialStrategy.pleadingBlueprint());
            insights.addAll(materialStrategy.evidenceAgenda());
            insights.addAll(materialStrategy.protocolBlockers());
            insights.addAll(materialStrategy.negotiationGuardrails());
            insights.addAll(materialStrategy.executionChecklist());
            insights.addAll(materialStrategy.controlPoints());
        }
        if (submissionBlueprint != null) {
            insights.add(submissionBlueprint.blueprintStatus());
            insights.add(submissionBlueprint.localCorrelationMode());
            insights.add(submissionBlueprint.tribunalCodigo());
            insights.add(submissionBlueprint.unidadeJudiciariaCodigo());
            insights.add(submissionBlueprint.judicialSystem() != null ? submissionBlueprint.judicialSystem().name() : null);
            insights.add(submissionBlueprint.dryRunStatus());
            insights.addAll(submissionBlueprint.relatedLocalProcessNumbers());
            insights.addAll(submissionBlueprint.blockingIssues());
            insights.addAll(submissionBlueprint.reviewChecklist());
            insights.addAll(submissionBlueprint.warnings());
        }
        if (connectorExecution != null) {
            insights.add(connectorExecution.executionMode());
            insights.add(connectorExecution.submissionLane());
            insights.add(connectorExecution.tribunalTargetKey());
            insights.add(connectorExecution.protocolClassCode());
            insights.add(connectorExecution.signerMode());
            insights.add(connectorExecution.retryPolicy());
            insights.addAll(connectorExecution.phases());
            insights.addAll(connectorExecution.executionChecklist());
            insights.addAll(connectorExecution.blockers());
            insights.addAll(connectorExecution.warnings());
        }
        if (strategicCopilot != null) {
            strategicCopilot.immediateActions().stream().map(StrategicCopilotReport.Action::title).forEach(insights::add);
            strategicCopilot.evidenceActions().stream().map(StrategicCopilotReport.Action::title).forEach(insights::add);
            strategicCopilot.proceduralActions().stream().map(StrategicCopilotReport.Action::title).forEach(insights::add);
            strategicCopilot.jurisprudentialActions().stream().map(StrategicCopilotReport.Action::title).forEach(insights::add);
            strategicCopilot.negotiationActions().stream().map(StrategicCopilotReport.Action::title).forEach(insights::add);
            insights.addAll(strategicCopilot.watchpoints());
        }
        if (settlementAdvisory != null) {
            insights.addAll(settlementAdvisory.nextMoves());
            insights.addAll(settlementAdvisory.executionSafeguards());
        }
        if (institutionalMemory != null) {
            insights.addAll(institutionalMemory.learnedPatterns());
            insights.addAll(institutionalMemory.reusablePlaybooks());
        }
        if (precedentAdvisory != null) {
            insights.addAll(precedentAdvisory.recommendedQueries());
            insights.addAll(precedentAdvisory.targetDecisionProfiles());
        }
        if (explainableDecisionTrail != null) {
            explainableDecisionTrail.nodes().stream().map(ExplainableDecisionTrailReport.DecisionNode::title).forEach(insights::add);
            insights.addAll(explainableDecisionTrail.openQuestions());
        }
        if (institutionalGovernanceContext != null) {
            insights.addAll(institutionalGovernanceContext.anchorDimensions());
            insights.addAll(institutionalGovernanceContext.policyGuards());
            insights.addAll(institutionalGovernanceContext.escalationPlaybooks());
        }
        if (kernelOperationalGovernance != null) {
            insights.addAll(kernelOperationalGovernance.controls());
            insights.addAll(kernelOperationalGovernance.nextActions());
            insights.addAll(kernelOperationalGovernance.watchpoints());
        }
        return List.copyOf(insights);
    }

    private Processo buildSyntheticProcess(LaianePeticaoAssistRequest request,
                                           CanonicalContext canonical,
                                           String ritoName) {
        RamoDireito ramoDireito = RamoDireito.fromString(firstNonBlank(canonical != null ? canonical.ramoDireito() : null, request.getRamoDireito()));
        FaseProcessual fase = truthy(request.getAtoJurisdicionalAnterior()) ? FaseProcessual.RECURSAL : FaseProcessual.CONHECIMENTO;
        return Processo.builder()
                .id(request.getProcessoId())
                .numeroUnificado(firstNonBlank(request.getNupn(), request.getProtocolTitle()))
                .ramoDireito(ramoDireito)
                .classeProcessual(firstNonBlank(canonical != null ? canonical.classeTpuCodigo() : null, request.getClasseTpu()))
                .assunto(firstNonBlank(request.getAssuntoTpu(), request.getMateriaPrincipal(), stringCtx(request, "assunto", "assunto_tpu")))
                .objetoProcessual(firstNonBlank(stringCtx(request, "objeto", "objeto_processual", "objetoProcessual"), request.getAssuntoTpu(), request.getMateriaPrincipal()))
                .pedidoPrincipal(firstNonBlank(stringCtx(request, "pedido", "pedido_principal", "pedidoPrincipal"), request.getProtocolTitle(), request.getAssuntoTpu()))
                .pedidosConsolidados(firstNonBlank(stringCtx(request, "pedidos", "pedidos_consolidados", "pedidosConsolidados"), stringCtx(request, "pedido", "pedido_principal", "pedidoPrincipal")))
                .materialProbatorioResumo(firstNonBlank(stringCtx(request, "provas", "documentos", "material_probatorio_resumo", "materialProbatorioResumo"), structuredList(request.getDocumentosAnexados())))
                .faseAtual(fase)
                .rito(ProceduralRitoNames.parse(ritoName))
                .valorCausa(request.getValorCausa())
                .parteAutoraCpf(request.getCpfCnpjAutor())
                .parteReuCpf(request.getCpfCnpjReu())
                .build();
    }

    private List<String> buildNegotiationSignals(LaianePeticaoAssistRequest request,
                                                 CanonicalContext canonical,
                                                 DynamicCompetenceDistributionResponse competencia) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (truthy(request.getRequerJuizadoEspecial())) {
            out.add("O caso indica trilha potencialmente conciliatória de juizado.");
        }
        if (truthy(request.getCasoUrgente())) {
            out.add("A urgência declarada recomenda proposta simples, objetiva e de rápida executabilidade.");
        }
        if (competencia != null && competencia.distribuicaoAutomatica()) {
            out.add("A competência já está fechada, o que reduz incerteza institucional para composição.");
        }
        if (canonical != null && canonical.ramoDireito() != null) {
            out.add("Ramo consolidado: " + canonical.ramoDireito());
        }
        return List.copyOf(out);
    }

    private List<String> mergeNegotiationSignals(LaianePeticaoAssistRequest request,
                                                 CanonicalContext canonical,
                                                 DynamicCompetenceDistributionResponse competencia,
                                                 ProcessMaterialDossierReport materialDossier,
                                                 ProcessMaterialStrategyReport materialStrategy) {
        LinkedHashSet<String> out = new LinkedHashSet<>(buildNegotiationSignals(request, canonical, competencia));
        if (materialDossier != null) {
            out.addAll(materialDossier.settlementLevers());
            out.addAll(materialDossier.evidenceAnchors());
            out.addAll(materialDossier.proofGaps());
        }
        if (materialStrategy != null) {
            out.add(materialStrategy.litigationPosture());
            out.add(materialStrategy.protocolReadiness());
            out.add(materialStrategy.negotiationStance());
            out.addAll(materialStrategy.protocolBlockers());
            out.addAll(materialStrategy.negotiationGuardrails());
            out.addAll(materialStrategy.controlPoints());
        }
        out.removeIf(s -> s == null || s.isBlank());
        return List.copyOf(out);
    }

    private static String stringCtx(LaianePeticaoAssistRequest request, String... keys) {
        if (request == null || request.getCtx() == null || request.getCtx().isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            Object value = request.getCtx().get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof List<?> list) {
                LinkedHashSet<String> out = new LinkedHashSet<>();
                for (Object item : list) {
                    String cleaned = trimToNull(item == null ? null : String.valueOf(item));
                    if (cleaned != null) {
                        out.add(cleaned);
                    }
                }
                if (!out.isEmpty()) {
                    return String.join(System.lineSeparator(), out.stream().map(v -> "- " + v).toList());
                }
                continue;
            }
            String cleaned = trimToNull(String.valueOf(value));
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    private static String structuredList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String value : values) {
            String cleaned = trimToNull(value);
            if (cleaned != null) {
                out.add("- " + cleaned);
            }
        }
        return out.isEmpty() ? null : String.join(System.lineSeparator(), out);
    }

    private boolean truthy(Boolean value) {
        return Boolean.TRUE.equals(value);
    }

    private record DraftBundle(String content, List<String> warnings) {
    }
}
