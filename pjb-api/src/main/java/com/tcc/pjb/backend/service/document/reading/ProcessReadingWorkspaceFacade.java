package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingActionResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingContentResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSurfaceResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingDocumentResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingEcosystemResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingLaneResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingNavigationResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingPresetCatalogResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingPreferenceResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSearchHitResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSummaryResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProceduralContextResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSpecializationResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.dto.ui.presentation.UiReadingIntensity;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.EventoProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional
final class ProcessReadingWorkspaceFacade {

    private final DocumentoPaginaRepository paginaRepository;
    private final ProcessReadingWorkspaceSessionResolver sessionResolver;
    private final ProcessReadingPresetCatalogResolver presetCatalogResolver;
    private final ProcessReadingNavigationResolver navigationResolver;
    private final ProcessReadingFlowResolver flowResolver;
    private final ProcessReadingProceduralContextResolver proceduralContextResolver;
    private final ProcessReadingSpecializationResolver specializationResolver;
    private final ProcessReadingEcosystemResolver ecosystemResolver;
    private final ProcessReadingSurfaceResolver surfaceResolver;
    private final ProcessReadingContentAssemblerService contentAssemblerService;

    ProcessReadingWorkspaceFacade(ProcessoRepository processoRepository,
                                          DocumentoProcessualRepository documentoRepository,
                                          DocumentoPaginaRepository paginaRepository,
                                          MovimentacaoProcessualRepository movimentacaoRepository,
                                          EventoProcessualRepository eventoRepository,
                                          PjbAuthorizationService authorizationService,
                                          RecursalEffectiveSecrecyService secrecyService,
                                          CurrentUserService currentUserService,
                                          ProcessReadingModeResolver modeResolver,
                                          ProcessReadingPresetResolver presetResolver,
                                          ProcessReadingNavigationResolver navigationResolver,
                                          ProcessReadingFlowResolver flowResolver,
                                          ProcessReadingProceduralContextResolver proceduralContextResolver,
                                          ProcessReadingSpecializationResolver specializationResolver,
                                          ProcessReadingEcosystemResolver ecosystemResolver,
                                          ProcessReadingSurfaceResolver surfaceResolver,
                                          ProcessReadingContentAssemblerService contentAssemblerService) {
        this.paginaRepository = paginaRepository;
        this.sessionResolver = new ProcessReadingWorkspaceSessionResolver(
                processoRepository,
                documentoRepository,
                paginaRepository,
                movimentacaoRepository,
                eventoRepository,
                authorizationService,
                secrecyService,
                currentUserService,
                modeResolver,
                presetResolver
        );
        this.presetCatalogResolver = new ProcessReadingPresetCatalogResolver();
        this.navigationResolver = navigationResolver;
        this.flowResolver = flowResolver;
        this.proceduralContextResolver = proceduralContextResolver;
        this.specializationResolver = specializationResolver;
        this.ecosystemResolver = ecosystemResolver;
        this.surfaceResolver = surfaceResolver;
        this.contentAssemblerService = contentAssemblerService;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingWorkspaceResponse assembleProcesso(Long processoId) {
        return assemble(sessionResolver.resolveProcessSession(processoId));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingWorkspaceResponse assembleDocumento(UUID documentoId) {
        return assemble(sessionResolver.resolveDocumentSession(documentoId));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingNavigationResponse navigation(Long processoId) {
        ProcessReadingWorkspaceSession session = sessionResolver.resolveProcessSession(processoId);
        return buildNavigation(session);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingFlowResponse flow(Long processoId) {
        ProcessReadingWorkspaceSession session = sessionResolver.resolveProcessSession(processoId);
        return buildProcessFlow(session);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingContentResponse contentDocumento(UUID documentoId) {
        return contentAssemblerService.assembleDocumento(documentoId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingContentResponse contentFluxo(Long processoId, String entryId) {
        return contentAssemblerService.assembleFluxo(processoId, entryId);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingPresetCatalogResponse presetCatalog(Long processoId) {
        return presetCatalogResolver.resolve(processoId, sessionResolver.resolveProcessSession(processoId));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingProceduralContextResponse proceduralContext(Long processoId) {
        ProcessReadingWorkspaceSession session = sessionResolver.resolveProcessSession(processoId);
        return buildProceduralContext(session, buildProcessFlow(session), buildNavigation(session));
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingSpecializationResponse specialization(Long processoId) {
        ProcessReadingWorkspaceSession session = sessionResolver.resolveProcessSession(processoId);
        ProcessReadingFlowResponse processFlow = buildProcessFlow(session);
        ProcessReadingNavigationResponse navigation = buildNavigation(session);
        ProcessReadingProceduralContextResponse proceduralContext = buildProceduralContext(session, processFlow, navigation);
        return buildSpecialization(session, processFlow, proceduralContext);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingEcosystemResponse ecosystem(Long processoId) {
        ProcessReadingWorkspaceSession session = sessionResolver.resolveProcessSession(processoId);
        ProcessReadingFlowResponse processFlow = buildProcessFlow(session);
        ProcessReadingNavigationResponse navigation = buildNavigation(session);
        ProcessReadingProceduralContextResponse proceduralContext = buildProceduralContext(session, processFlow, navigation);
        ProcessReadingSpecializationResponse specialization = buildSpecialization(session, processFlow, proceduralContext);
        return ecosystemResolver.resolve(session.context().processo(), session.modeProfile(), proceduralContext, specialization);
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<ProcessReadingSearchHitResponse> search(Long processoId, String query) {
        ProcessReadingWorkspaceSession session = sessionResolver.resolveProcessSession(processoId);
        ProcessReadingWorkspaceContext context = session.context();
        String normalized = normalizeQuery(query);
        if (normalized == null) {
            return List.of();
        }
        List<DocumentoPagina> hits = paginaRepository.searchInProcess(processoId, normalized, 25);
        ArrayList<ProcessReadingSearchHitResponse> out = new ArrayList<>();
        hits.stream()
                .map(page -> new ProcessReadingSearchHitResponse(
                        page.getDocumento() != null ? page.getDocumento().getId() : null,
                        page.getPageId(),
                        page.getPageNumber() == null ? 0 : page.getPageNumber(),
                        resolveTitulo(page.getDocumento()),
                        fragment(page.getTextoExtraido(), normalized),
                        inferLane(page.getDocumento(), normalized),
                        page.getDocumento() != null ? "/api/v1/documentos/" + page.getDocumento().getId() + "/painel-leitura/conteudo#page=" + (page.getPageNumber() == null ? 1 : page.getPageNumber()) : null,
                        "DOCUMENTO_PAGINA",
                        page.getPageId(),
                        resolveTitulo(page.getDocumento()),
                        mapOfEntries(entry("readerEndpoint", page.getDocumento() != null ? "/api/v1/documentos/" + page.getDocumento().getId() + "/painel-leitura" : null), entry("contentEndpoint", page.getDocumento() != null ? "/api/v1/documentos/" + page.getDocumento().getId() + "/painel-leitura/conteudo" : null))
                ))
                .forEach(out::add);
        ProcessReadingFlowResponse processFlow = buildProcessFlow(session);
        processFlow.entries().stream()
                .filter(entry -> containsNormalized(entry.title(), normalized) || containsNormalized(entry.bodyPreview(), normalized) || entry.tags().stream().anyMatch(tag -> containsNormalized(tag, normalized)))
                .limit(Math.max(0, 25 - out.size()))
                .map(entry -> new ProcessReadingSearchHitResponse(
                        null,
                        entry.entryId(),
                        0,
                        entry.title(),
                        fragment(entry.bodyPreview(), normalized),
                        entry.lane(),
                        "/api/v1/processos/" + processoId + "/painel-leitura/conteudo?entryId=" + entry.entryId(),
                        entry.sourceType(),
                        entry.entryId(),
                        entry.originMode(),
                        mapOfEntries(entry("severity", entry.severity()), entry("actor", entry.actor()), entry("contentEndpoint", "/api/v1/processos/" + processoId + "/painel-leitura/conteudo?entryId=" + entry.entryId()))
                ))
                .forEach(out::add);
        return List.copyOf(out);
    }

    private ProcessReadingWorkspaceResponse assemble(ProcessReadingWorkspaceSession session) {
        ProcessReadingWorkspaceContext context = session.context();
        Processo processo = context.processo();
        List<DocumentoProcessual> documentos = context.documentos();
        List<DocumentoPagina> paginas = context.paginas();
        List<MovimentacaoProcessual> movimentacoes = context.movimentacoes();
        List<EventoProcessual> eventos = context.eventos();
        Usuario usuario = session.usuario();
        ProcessReadingModeProfile modeProfile = session.modeProfile();
        ProcessReadingPresetProfile presetProfile = session.presetProfile();
        Map<UUID, List<DocumentoPagina>> paginasPorDocumento = paginas.stream()
                .filter(page -> page.getDocumento() != null && page.getDocumento().getId() != null)
                .collect(Collectors.groupingBy(page -> page.getDocumento().getId()));
        ProcessReadingPreferenceResponse preference = ProcessReadingPresetCatalogResolver.toPreferenceResponse(presetProfile);
        ProcessReadingFlowResponse processFlow = buildProcessFlow(processo, usuario, movimentacoes, eventos, modeProfile, presetProfile);
        ProcessReadingNavigationResponse navigation = navigationResolver.resolve(processo, usuario, documentos, context.navigationPages(), modeProfile, presetProfile);
        ProcessReadingProceduralContextResponse proceduralContext = proceduralContextResolver.resolve(processo, modeProfile, processFlow, navigation, (int) context.totalDocumentos(), (int) context.totalPaginas());
        ProcessReadingSummaryResponse summary = new ProcessReadingSummaryResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                processo.getTribunal(),
                processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null,
                processo.getMateria() != null ? processo.getMateria().name() : null,
                processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null,
                processo.getRito() != null ? processo.getRito().name() : null,
                modeProfile.profileCode(),
                presetProfile.presetCode(),
                presetProfile.intensity(),
                presetProfile.resolvedTheme(),
                modeProfile.contrastMode(),
                Integer.toString(presetProfile.fontScalePercent()),
                ProcessReadingPresetCatalogResolver.formatLineSpacing(presetProfile.lineHeight()),
                modeProfile.navigationMode(),
                presetProfile.chronologyMode(),
                presetProfile.citationMode(),
                presetProfile.maxWidthCh(),
                presetProfile.chunkPageSize(),
                context.totalDocumentos(),
                context.totalPaginas(),
                modeProfile.coberturaTextualPercentual(),
                modeProfile.sigiloReforcado(),
                modeProfile.recursal(),
                modeProfile.volumeExtenso(),
                processFlow.totalEntries()
        );
        List<ProcessReadingDocumentResponse> docs = documentos.stream()
                .map(documento -> toDocumentResponse(documento, paginasPorDocumento.getOrDefault(documento.getId(), List.of()), context.documentStats().get(documento.getId()), modeProfile, presetProfile))
                .toList();
        ProcessReadingSpecializationResponse specialization = specializationResolver.resolve(processo, modeProfile, processFlow, proceduralContext, docs);
        ProcessReadingEcosystemResponse ecosystem = ecosystemResolver.resolve(processo, modeProfile, proceduralContext, specialization);
        List<ProcessReadingLaneResponse> lanes = buildLanes(processo, usuario, modeProfile, presetProfile, docs, navigation, processFlow, proceduralContext, specialization, ecosystem);
        List<ProcessReadingActionResponse> actions = buildActions(processo, modeProfile, presetProfile, docs, navigation, processFlow, proceduralContext, specialization, ecosystem);
        LinkedHashMap<String, Object> integrity = buildIntegrity(modeProfile, presetProfile, paginas, navigation, processFlow, proceduralContext, specialization);
        LinkedHashMap<String, Object> frontend = buildFrontend(processo, modeProfile, presetProfile, lanes, preference, navigation, processFlow, proceduralContext, specialization, ecosystem);
        frontend.put("ecosystemEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/ecossistema");
        frontend.put("ecosystem", ecosystem);
        List<String> alerts = mergeAlerts(modeProfile, presetProfile, navigation, processFlow, proceduralContext, specialization);
        alerts = new ArrayList<>(alerts);
        alerts.add("Plataforma convergente ativa: " + ecosystem.convergenceMode() + ", assinatura " + ecosystem.signatureMode() + " e trilha documental " + ecosystem.documentPipelineMode() + ".");
        return new ProcessReadingWorkspaceResponse(
                processo.getId(),
                processo.getNumeroProcesso(),
                summary,
                preference,
                navigation,
                processFlow,
                proceduralContext,
                specialization,
                docs,
                lanes,
                actions,
                alerts,
                integrity,
                frontend
        );
    }

    private ProcessReadingFlowResponse buildProcessFlow(Processo processo,
                                                      Usuario usuario,
                                                      List<MovimentacaoProcessual> movimentacoes,
                                                      List<EventoProcessual> eventos,
                                                      ProcessReadingModeProfile modeProfile,
                                                      ProcessReadingPresetProfile presetProfile) {
        return flowResolver.resolve(processo, usuario, movimentacoes, eventos, modeProfile, presetProfile);
    }

    private ProcessReadingFlowResponse buildProcessFlow(ProcessReadingWorkspaceSession session) {
        return buildProcessFlow(session.context().processo(), session.usuario(), session.context().movimentacoes(), session.context().eventos(), session.modeProfile(), session.presetProfile());
    }

    private ProcessReadingNavigationResponse buildNavigation(ProcessReadingWorkspaceSession session) {
        return navigationResolver.resolve(session.context().processo(), session.usuario(), session.context().documentos(), session.context().navigationPages(), session.modeProfile(), session.presetProfile());
    }

    private ProcessReadingProceduralContextResponse buildProceduralContext(ProcessReadingWorkspaceSession session,
                                                                           ProcessReadingFlowResponse processFlow,
                                                                           ProcessReadingNavigationResponse navigation) {
        return proceduralContextResolver.resolve(session.context().processo(), session.modeProfile(), processFlow, navigation, (int) session.context().totalDocumentos(), (int) session.context().totalPaginas());
    }

    private ProcessReadingSpecializationResponse buildSpecialization(ProcessReadingWorkspaceSession session,
                                                                     ProcessReadingFlowResponse processFlow,
                                                                     ProcessReadingProceduralContextResponse proceduralContext) {
        Map<UUID, List<DocumentoPagina>> paginasPorDocumento = session.context().paginas().stream()
                .filter(page -> page.getDocumento() != null && page.getDocumento().getId() != null)
                .collect(Collectors.groupingBy(page -> page.getDocumento().getId()));
        List<ProcessReadingDocumentResponse> docs = session.context().documentos().stream()
                .map(documento -> toDocumentResponse(documento, paginasPorDocumento.getOrDefault(documento.getId(), List.of()), session.context().documentStats().get(documento.getId()), session.modeProfile(), session.presetProfile()))
                .toList();
        return specializationResolver.resolve(session.context().processo(), session.modeProfile(), processFlow, proceduralContext, docs);
    }




    private ProcessReadingDocumentResponse toDocumentResponse(DocumentoProcessual documento,
                                                              List<DocumentoPagina> paginas,
                                                              ProcessReadingPageCounter pageCounter,
                                                              ProcessReadingModeProfile modeProfile,
                                                              ProcessReadingPresetProfile presetProfile) {
        long totalPaginas = pageCounter != null ? pageCounter.totalPages() : paginas == null ? 0L : paginas.size();
        long pagComTexto = pageCounter != null ? pageCounter.pagesWithText() : paginas == null ? 0L : paginas.stream().filter(page -> !blank(page.getTextoExtraido())).count();
        int cobertura = totalPaginas == 0L ? 0 : (int) Math.round((pagComTexto * 100.0d) / totalPaginas);
        ArrayList<String> markers = new ArrayList<>();
        if (totalPaginas >= 40L) {
            markers.add("PECA_EXTENSA");
        }
        if (cobertura < 65 && totalPaginas > 0L) {
            markers.add("OCR_PENDENTE");
        }
        if (modeProfile.recursal()) {
            markers.add("TRILHA_RECURSAL");
        }
        if (modeProfile.sigiloReforcado()) {
            markers.add("SIGILO_REFORCADO");
        }
        ProcessReadingSurfaceResponse surface = surfaceResolver.resolveDocument(documento, totalPaginas, pagComTexto, modeProfile, presetProfile);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        putIfPresent(metadata, "viewerEndpoint", documento.getId() != null ? "/api/v1/documentos/" + documento.getId() + "/pdf" : null);
        putIfPresent(metadata, "readerEndpoint", documento.getId() != null ? "/api/v1/documentos/" + documento.getId() + "/painel-leitura" : null);
        putIfPresent(metadata, "contentEndpoint", documento.getId() != null ? "/api/v1/documentos/" + documento.getId() + "/painel-leitura/conteudo" : null);
        putIfPresent(metadata, "createdAt", documento.getCriadoEm() != null ? documento.getCriadoEm().toString() : null);
        putIfPresent(metadata, "storageBackend", documento.getStorageBackend());
        putIfPresent(metadata, "nivelSigilo", documento.getNivelSigilo() != null ? documento.getNivelSigilo().name() : null);
        putIfPresent(metadata, "chunkPageSize", presetProfile.chunkPageSize());
        putIfPresent(metadata, "focusBandMode", presetProfile.focusBandMode());
        putIfPresent(metadata, "citationMode", presetProfile.citationMode());
        putIfPresent(metadata, "surfaceDisplayMode", surface.displayMode());
        putIfPresent(metadata, "surfaceSelectionMode", surface.selectionMode());
        putIfPresent(metadata, "ocrStatus", surface.ocrStatus());
        putIfPresent(metadata, "preservationMode", surface.preservationMode());
        return new ProcessReadingDocumentResponse(
                documento.getId(),
                resolveTitulo(documento),
                documento.getCategoria() != null ? documento.getCategoria().name() : null,
                documento.getContentType(),
                documento.getTamanhoBytes() == null ? 0L : documento.getTamanhoBytes(),
                totalPaginas,
                cobertura,
                suggestedDocumentMode(documento, totalPaginas, modeProfile, presetProfile),
                List.copyOf(markers),
                metadata
        );
    }

    private List<ProcessReadingLaneResponse> buildLanes(Processo processo,
                                                        Usuario usuario,
                                                        ProcessReadingModeProfile modeProfile,
                                                        ProcessReadingPresetProfile presetProfile,
                                                        List<ProcessReadingDocumentResponse> documents,
                                                        ProcessReadingNavigationResponse navigation,
                                                        ProcessReadingFlowResponse processFlow,
                                                        ProcessReadingProceduralContextResponse proceduralContext,
                                                        ProcessReadingSpecializationResponse specialization,
                                                        ProcessReadingEcosystemResponse ecosystem) {
        List<ProcessReadingLaneResponse> lanes = new ArrayList<>();
        lanes.add(lane("VISUAL", "ACTIVE", presetProfile.resolvedTheme(), List.of(
                "Tema visual: " + presetProfile.resolvedTheme(),
                "Controle de brilho: " + modeProfile.glareControlMode(),
                "Contraste: " + modeProfile.contrastMode(),
                "Fonte: " + presetProfile.fontScalePercent() + "%"
        ), Map.of(
                "visualTheme", presetProfile.resolvedTheme(),
                "glareControlMode", modeProfile.glareControlMode(),
                "contrastMode", modeProfile.contrastMode(),
                "lineHeight", presetProfile.lineHeight(),
                "paragraphGapRem", presetProfile.paragraphGapRem()
        )));
        lanes.add(lane("FOCO", "READY", presetProfile.focusBandMode(), List.of(
                "Preset: " + presetProfile.presetCode(),
                "Faixa de foco: " + presetProfile.focusBandMode(),
                "Privacidade: " + presetProfile.privacyVeilMode(),
                "Largura útil: " + presetProfile.maxWidthCh() + "ch"
        ), Map.of(
                "presetCode", presetProfile.presetCode(),
                "focusBandMode", presetProfile.focusBandMode(),
                "privacyVeilMode", presetProfile.privacyVeilMode(),
                "maxWidthCh", presetProfile.maxWidthCh(),
                "chunkPageSize", presetProfile.chunkPageSize()
        )));
        lanes.add(lane("TEXTO", modeProfile.coberturaTextualPercentual() >= 65 ? "READY" : "ATTENTION", modeProfile.summaryMode(), List.of(
                "Cobertura textual: " + modeProfile.coberturaTextualPercentual() + "%",
                "Segmentação: " + modeProfile.segmentationMode(),
                "Sinopse: " + modeProfile.summaryMode(),
                "Busca: " + presetProfile.searchAssistMode()
        ), Map.of(
                "coverage", modeProfile.coberturaTextualPercentual(),
                "segmentationMode", modeProfile.segmentationMode(),
                "summaryMode", modeProfile.summaryMode(),
                "searchAssistMode", presetProfile.searchAssistMode()
        )));
        lanes.add(lane("JURISDICAO", "READY", proceduralContext.instanciaLeitura(), List.of(
                "Justiça: " + proceduralContext.justiceTrack(),
                "Tier: " + proceduralContext.tribunalTier(),
                "Rito família: " + proceduralContext.ritoFamily(),
                "Recursal: " + proceduralContext.recursalTrack()
        ), mapOfEntries(
                entry("justiceTrack", proceduralContext.justiceTrack()),
                entry("tribunalTier", proceduralContext.tribunalTier()),
                entry("ramo", proceduralContext.ramo()),
                entry("materia", proceduralContext.materia()),
                entry("rito", proceduralContext.rito()),
                entry("ritoFamily", proceduralContext.ritoFamily()),
                entry("recursalTrack", proceduralContext.recursalTrack()),
                entry("embargoTrack", proceduralContext.embargoTrack())
        )));
        lanes.add(lane("ESPECIALIZACAO", "READY", specialization.scopeCode(), List.of(
                "Decisão: " + specialization.decisionMode(),
                "Recurso: " + specialization.resourceMode(),
                "Embargos: " + specialization.embargoMode(),
                "Abertura: " + String.join(" → ", specialization.openingSequence().stream().limit(4).toList())
        ), mapOfEntries(
                entry("scopeCode", specialization.scopeCode()),
                entry("chamberMode", specialization.chamberMode()),
                entry("decisionMode", specialization.decisionMode()),
                entry("evidenceMode", specialization.evidenceMode()),
                entry("resourceMode", specialization.resourceMode()),
                entry("embargoMode", specialization.embargoMode()),
                entry("hearingMode", specialization.hearingMode()),
                entry("executionMode", specialization.executionMode())
        )));
        lanes.add(lane("ECOSSISTEMA", "READY", ecosystem.convergenceMode(), List.of(
                "Sistema primário: " + ecosystem.primarySystem(),
                "Fallback: " + ecosystem.fallbackSystem(),
                "Assinatura: " + ecosystem.signatureMode(),
                "Prazos: " + ecosystem.deadlineAggregationMode()
        ), mapOfEntries(
                entry("convergenceMode", ecosystem.convergenceMode()),
                entry("legacyMigrationMode", ecosystem.legacyMigrationMode()),
                entry("browserAccessMode", ecosystem.browserAccessMode()),
                entry("signatureMode", ecosystem.signatureMode()),
                entry("mfaMode", ecosystem.mfaMode()),
                entry("documentPipelineMode", ecosystem.documentPipelineMode()),
                entry("ocrMode", ecosystem.ocrMode()),
                entry("aiAssistMode", ecosystem.aiAssistMode()),
                entry("deadlineAggregationMode", ecosystem.deadlineAggregationMode())
        )));
        lanes.add(lane("SUPERFICIE", processFlow.totalEntries() > 0 || !documents.isEmpty() ? "READY" : "IDLE", proceduralContext.nativeActTrack(), List.of(
                "Atos nativos: " + processFlow.totalEntries(),
                "Documentos: " + documents.size(),
                "Leitura híbrida: " + (!documents.isEmpty() && processFlow.totalEntries() > 0 ? "ATIVA" : "PARCIAL"),
                "Conteúdo unificado: /painel-leitura/conteudo"
        ), Map.of(
                "processEntries", processFlow.totalEntries(),
                "documentCount", documents.size(),
                "hasNativeActs", processFlow.totalEntries() > 0,
                "hasDocuments", !documents.isEmpty(),
                "supportsUnifiedContentSurface", true,
                "htmlInlinePreferred", proceduralContext.htmlInlinePreferred(),
                "pdfSignedPreferred", proceduralContext.pdfSignedPreferred(),
                "signatureTrack", proceduralContext.signatureTrack()
        )));
        lanes.add(lane("NAVEGACAO", "READY", modeProfile.navigationMode(), List.of(
                "Navegação: " + modeProfile.navigationMode(),
                "Âncoras: " + presetProfile.anchorMode(),
                "Nodos mapeados: " + navigation.totalNodes(),
                "Páginas totais: " + modeProfile.totalPaginas()
        ), Map.of(
                "navigationMode", modeProfile.navigationMode(),
                "anchorMode", presetProfile.anchorMode(),
                "documentCount", modeProfile.totalDocumentos(),
                "pageCount", modeProfile.totalPaginas(),
                "nodeCount", navigation.totalNodes()
        )));
        lanes.add(lane("PROVA", "READY", modeProfile.evidenceMode(), List.of(
                "Leitura probatória: " + modeProfile.evidenceMode(),
                "Ramo: " + (processo.getRamoDireito() != null ? processo.getRamoDireito().name() : "NAO_INFORMADO"),
                "Objeto: " + safeText(processo.getObjetoProcessual(), "Objeto não consolidado"),
                "Pedido principal: " + safeText(processo.getPedidoPrincipal(), "Pedido principal não consolidado")
        ), mapOfEntries(
                entry("evidenceMode", modeProfile.evidenceMode()),
                entry("ramo", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null),
                entry("objetoProcessual", processo.getObjetoProcessual()),
                entry("pedidoPrincipal", processo.getPedidoPrincipal())
        )));
        lanes.add(lane("CITACOES", navigation.totalNodes() > 0 ? "READY" : "IDLE", presetProfile.citationMode(), List.of(
                "Modo de citações: " + presetProfile.citationMode(),
                "Cronologia: " + presetProfile.chronologyMode(),
                "Atalhos: " + presetProfile.keyboardBiasMode(),
                "Overlay operacional: " + presetProfile.operationalOverlayMode()
        ), Map.of(
                "citationMode", presetProfile.citationMode(),
                "chronologyMode", presetProfile.chronologyMode(),
                "keyboardBiasMode", presetProfile.keyboardBiasMode(),
                "operationalOverlayMode", presetProfile.operationalOverlayMode()
        )));
        lanes.add(lane("RECURSAL", modeProfile.recursal() ? "ACTIVE" : "IDLE", modeProfile.recursalMode(), List.of(
                "Fase atual: " + (processo.getFaseAtual() != null ? processo.getFaseAtual().name() : "NAO_INFORMADA"),
                "Modo recursal: " + modeProfile.recursalMode(),
                "Classe processual: " + safeText(processo.getClasseProcessual(), "Não informada"),
                "Assunto: " + safeText(processo.getAssunto(), "Não informado")
        ), mapOfEntries(
                entry("faseAtual", processo.getFaseAtual() != null ? processo.getFaseAtual().name() : null),
                entry("recursalMode", modeProfile.recursalMode()),
                entry("classeProcessual", processo.getClasseProcessual()),
                entry("assunto", processo.getAssunto())
        )));
        lanes.add(lane("ATOS", processFlow.totalEntries() > 0 ? "READY" : "IDLE", processFlow.defaultOpenMode(), List.of(
                "Entradas processuais: " + processFlow.totalEntries(),
                "Atos em texto nativo: " + processFlow.totalInlineActs(),
                "Movimentações: " + processFlow.totalMovements(),
                "Eventos: " + processFlow.totalEvents()
        ), mapOfEntries(
                entry("chronologyMode", processFlow.chronologyMode()),
                entry("defaultOpenMode", processFlow.defaultOpenMode()),
                entry("totalEntries", processFlow.totalEntries()),
                entry("totalInlineActs", processFlow.totalInlineActs()),
                entry("totalMovements", processFlow.totalMovements()),
                entry("totalEvents", processFlow.totalEvents())
        )));
        lanes.add(lane("EQUIPE", usuario != null ? "READY" : "IDLE", modeProfile.supportDeskMode(), List.of(
                "Perfil de leitura: " + modeProfile.profileCode(),
                "Trilha de equipe: " + modeProfile.supportDeskMode(),
                "Usuário: " + (usuario != null ? usuario.getNome() : "NÃO IDENTIFICADO"),
                "Papel: " + resolveRoleLabel(usuario)
        ), mapOfEntries(
                entry("profileCode", modeProfile.profileCode()),
                entry("supportDeskMode", modeProfile.supportDeskMode()),
                entry("userRole", resolveRoleLabel(usuario)),
                entry("userCluster", resolveCluster(usuario)),
                entry("documentCount", documents.size())
        )));
        return List.copyOf(lanes);
    }

    private List<ProcessReadingActionResponse> buildActions(Processo processo,
                                                            ProcessReadingModeProfile modeProfile,
                                                            ProcessReadingPresetProfile presetProfile,
                                                            List<ProcessReadingDocumentResponse> documents,
                                                            ProcessReadingNavigationResponse navigation,
                                                            ProcessReadingFlowResponse processFlow,
                                                            ProcessReadingProceduralContextResponse proceduralContext,
                                                            ProcessReadingSpecializationResponse specialization,
                                                            ProcessReadingEcosystemResponse ecosystem) {
        List<ProcessReadingActionResponse> actions = new ArrayList<>();
        actions.add(action("ATIVAR_TEMA_LEITURA", "Aplicar preset visual da leitura", "low", true, null,
                Map.of("theme", presetProfile.resolvedTheme(), "presetCode", presetProfile.presetCode())));
        actions.add(action("AJUSTAR_FONTE", "Ajustar fonte sem alterar a peça", "low", true, null,
                Map.of("fontScale", presetProfile.fontScalePercent(), "lineHeight", presetProfile.lineHeight(), "maxWidthCh", presetProfile.maxWidthCh())));
        actions.add(action("AGRUPAR_PECAS", "Agrupar peças, blocos e documentos extensos", modeProfile.volumeExtenso() ? "high" : "medium", true, null,
                Map.of("segmentationMode", modeProfile.segmentationMode(), "chunkPageSize", presetProfile.chunkPageSize())));
        actions.add(action("ABRIR_BUSCA_INTERNA", "Buscar termos, fundamentos e páginas relevantes", "medium", true,
                "/api/v1/processos/" + processo.getId() + "/painel-leitura/busca", Map.of("q", "")));
        actions.add(action("ABRIR_MAPA_NAVEGACAO", "Abrir mapa de peças, eventos, provas e fundamentos", navigation.totalNodes() > 0 ? "medium" : "low", true,
                "/api/v1/processos/" + processo.getId() + "/painel-leitura/navegacao", Map.of("nodeCount", navigation.totalNodes())));
        actions.add(action("ABRIR_SUPERFICIE_UNIFICADA", "Abrir leitura unificada do processo com atos nativos e documentos", processFlow.totalEntries() > 0 || !documents.isEmpty() ? "high" : "low", true,
                "/api/v1/processos/" + processo.getId() + "/painel-leitura/conteudo", Map.of("nativeActs", processFlow.totalEntries(), "documents", documents.size())));
        actions.add(action("ABRIR_CONTEXTO_PROCEDIMENTAL", "Abrir malha de rito, ramo, justiça, tribunal e trilha recursal", "medium", true,
                "/api/v1/processos/" + processo.getId() + "/painel-leitura/contexto-procedimental", mapOfEntries(
                        entry("justiceTrack", proceduralContext.justiceTrack()),
                        entry("ritoFamily", proceduralContext.ritoFamily()),
                        entry("recursalTrack", proceduralContext.recursalTrack()),
                        entry("embargoTrack", proceduralContext.embargoTrack())
                )));
        actions.add(action("ABRIR_TRILHA_ESPECIALIZADA", "Abrir trilha de leitura especializada por rito, ramo, tribunal, recurso e embargos", "high", true,
                "/api/v1/processos/" + processo.getId() + "/painel-leitura/especializacao", mapOfEntries(
                        entry("scopeCode", specialization.scopeCode()),
                        entry("decisionMode", specialization.decisionMode()),
                        entry("resourceMode", specialization.resourceMode()),
                        entry("embargoMode", specialization.embargoMode())
                )));
        actions.add(action("ABRIR_ECOSSISTEMA_CONVERGENTE", "Abrir malha nacional de convergência, migração, assinatura em nuvem, OCR e IA", "high", true,
                "/api/v1/processos/" + processo.getId() + "/painel-leitura/ecossistema", mapOfEntries(
                        entry("convergenceMode", ecosystem.convergenceMode()),
                        entry("signatureMode", ecosystem.signatureMode()),
                        entry("documentPipelineMode", ecosystem.documentPipelineMode()),
                        entry("aiAssistMode", ecosystem.aiAssistMode()),
                        entry("deadlineAggregationMode", ecosystem.deadlineAggregationMode())
                )));
        actions.add(action("ABRIR_ATOS_PRIORITARIOS", "Abrir sequência prioritária de peças, atos e blocos do caso", "medium", true, null,
                mapOfEntries(
                        entry("openingSequence", specialization.openingSequence()),
                        entry("preferredActModes", specialization.preferredActModes())
                )));
        if (proceduralContext.htmlInlinePreferred()) {
            actions.add(action("ABRIR_ATO_HTML_ASSISTIDO", "Abrir ato textual nativo com conferência de assinatura e exportação controlada", "high", true,
                    "/api/v1/processos/" + processo.getId() + "/painel-leitura/fluxo", mapOfEntries(
                            entry("nativeActTrack", proceduralContext.nativeActTrack()),
                            entry("signatureTrack", proceduralContext.signatureTrack())
                    )));
        }
        if (processFlow.totalEntries() > 0) {
            actions.add(action("ABRIR_FLUXO_PROCESSUAL", "Abrir despachos, decisões, movimentações e eventos em texto nativo", "high", true,
                    "/api/v1/processos/" + processo.getId() + "/painel-leitura/fluxo", Map.of("entryCount", processFlow.totalEntries(), "inlineActs", processFlow.totalInlineActs())));
        }
        if (specialization.signedPdfInspectionRequired()) {
            actions.add(action("ABRIR_CONFERENCIA_PDF_ASSINADO", "Conferir a versão formal assinada sem perder o contexto HTML do ato", "medium", true, null,
                    mapOfEntries(entry("signatureTrack", proceduralContext.signatureTrack()), entry("nativeHtmlPriority", specialization.nativeHtmlPriority()))));
        }
        if (!"EXECUCAO_NAO_PRIORITARIA".equals(specialization.executionMode())) {
            actions.add(action("ABRIR_TRILHA_EXECUTIVA", "Abrir leitura da malha executiva, cálculos, incidentes e satisfação", "medium", true, null,
                    mapOfEntries(entry("executionMode", specialization.executionMode()), entry("openingSequence", specialization.openingSequence()))));
        }
        actions.add(action("ATIVAR_FAIXA_DE_FOCO", "Fixar faixa de foco e largura confortável", "medium", true, null,
                Map.of("focusBandMode", presetProfile.focusBandMode(), "privacyVeilMode", presetProfile.privacyVeilMode())));
        if (modeProfile.coberturaTextualPercentual() < 65 && !documents.isEmpty()) {
            actions.add(action("DESTACAR_PAGINAS_SEM_TEXTO", "Evidenciar páginas sem texto extraído", "high", true, null,
                    Map.of("mode", "OCR_PENDENTE")));
        }
        if (modeProfile.recursal()) {
            actions.add(action("ABRIR_TRILHA_RECURSAL", "Fixar decisão atacada, razões e contrarrazões", "high", true, null,
                    Map.of("mode", modeProfile.recursalMode(), "citationMode", presetProfile.citationMode())));
            actions.add(action("ABRIR_EMBARGOS_E_INCIDENTES", "Destacar embargos, agravos, apelações e peças integrativas", "high", true, null,
                    mapOfEntries(entry("embargoTrack", proceduralContext.embargoTrack()), entry("recursalTrack", proceduralContext.recursalTrack()))));
        }
        if (navigation.totalNodes() > 0) {
            actions.add(action("PINAR_ANCORAS", "Fixar âncoras de citação, prova e eventos", "medium", true, null,
                    Map.of("anchorMode", presetProfile.anchorMode(), "chronologyMode", presetProfile.chronologyMode())));
        }
        if (modeProfile.totalPaginas() >= 120L) {
            actions.add(action("FIXAR_SINOPSE_PROGRESSIVA", "Exibir sinopse lateral por blocos", "medium", true, null,
                    Map.of("summaryMode", modeProfile.summaryMode(), "chunkPageSize", presetProfile.chunkPageSize())));
        }
        return List.copyOf(actions);
    }

    private LinkedHashMap<String, Object> buildIntegrity(ProcessReadingModeProfile modeProfile,
                                                         ProcessReadingPresetProfile presetProfile,
                                                         List<DocumentoPagina> paginas,
                                                         ProcessReadingNavigationResponse navigation,
                                                         ProcessReadingFlowResponse processFlow,
                                                         ProcessReadingProceduralContextResponse proceduralContext,
                                                         ProcessReadingSpecializationResponse specialization) {
        LinkedHashMap<String, Object> integrity = new LinkedHashMap<>();
        integrity.put("hasPages", !paginas.isEmpty());
        integrity.put("textCoverageHealthy", modeProfile.coberturaTextualPercentual() >= 65);
        integrity.put("supportsFocusReading", modeProfile.totalPaginas() > 0L || processFlow.totalEntries() > 0);
        integrity.put("supportsAmberMode", true);
        integrity.put("supportsChunkNavigation", modeProfile.totalPaginas() >= 12L);
        integrity.put("supportsDocumentSearch", true);
        integrity.put("supportsInstitutionalPreset", true);
        integrity.put("supportsNavigationMap", navigation.totalNodes() > 0);
        integrity.put("supportsNativeActs", processFlow.totalEntries() > 0);
        integrity.put("supportsInlineDecisions", processFlow.totalInlineActs() > 0);
        integrity.put("supportsPrivacyVeil", modeProfile.sigiloReforcado() || !"SEM_MASCARA".equals(presetProfile.privacyVeilMode()));
        integrity.put("supportsKeyboardBias", true);
        integrity.put("supportsProceduralContextMesh", true);
        integrity.put("supportsAllBrazilianRites", true);
        integrity.put("supportsAllBrazilianRights", true);
        integrity.put("supportsAllProceduralGuarantees", true);
        integrity.put("supportsInlineHtmlActs", proceduralContext.htmlInlinePreferred());
        integrity.put("supportsReadingSpecialization", true);
        integrity.put("supportsAllInstancesAndEmbargos", true);
        integrity.put("supportsOpeningSequence", !specialization.openingSequence().isEmpty());
        integrity.put("supportsSignedPdfInspection", specialization.signedPdfInspectionRequired());
        return integrity;
    }

    private LinkedHashMap<String, Object> buildFrontend(Processo processo,
                                                        ProcessReadingModeProfile modeProfile,
                                                        ProcessReadingPresetProfile presetProfile,
                                                        List<ProcessReadingLaneResponse> lanes,
                                                        ProcessReadingPreferenceResponse preference,
                                                        ProcessReadingNavigationResponse navigation,
                                                        ProcessReadingFlowResponse processFlow,
                                                        ProcessReadingProceduralContextResponse proceduralContext,
                                                        ProcessReadingSpecializationResponse specialization,
                                                        ProcessReadingEcosystemResponse ecosystem) {
        LinkedHashMap<String, Object> frontend = new LinkedHashMap<>();
        frontend.put("defaultTab", resolveDefaultTab(modeProfile, processFlow));
        frontend.put("tabs", lanes.stream().map(ProcessReadingLaneResponse::code).toList());
        frontend.put("refreshEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura");
        frontend.put("searchEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/busca");
        frontend.put("navigationEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/navegacao");
        frontend.put("flowEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/fluxo");
        frontend.put("flowContentEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/conteudo");
        frontend.put("presetCatalogEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/presets");
        frontend.put("proceduralContextEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/contexto-procedimental");
        frontend.put("specializationEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/especializacao");
        frontend.put("ecosystemEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/ecossistema");
        frontend.put("documentEndpointTemplate", "/api/v1/documentos/{documentoId}/painel-leitura");
        frontend.put("documentContentEndpointTemplate", "/api/v1/documentos/{documentoId}/painel-leitura/conteudo");
        frontend.put("pdfEndpointTemplate", "/api/v1/documentos/{documentoId}/pdf");
        frontend.put("themes", List.of("AMBAR_JURIDICO", "AMBAR_RESERVADO", "MARFIM_SUAVE", "NEUTRO_ESTATUARIO", "CONTRASTE_REFORCADO"));
        frontend.put("fontScaleOptions", List.of("92", "100", "108", "112", "120", "128"));
        frontend.put("lineSpacingOptions", List.of("COMPACTO", "PADRAO_LIMPO", "EXPANDIDO"));
        frontend.put("readerPreset", preference);
        frontend.put("navigationMetadata", navigation.metadata());
        frontend.put("featureFlags", mapOfEntries(
                entry("supportsYellowTint", true),
                entry("supportsDocumentGrouping", true),
                entry("supportsSearchHits", true),
                entry("supportsChunkSummary", true),
                entry("supportsOperationalOverlay", true),
                entry("supportsEvidenceLane", true),
                entry("supportsNavigationMap", true),
                entry("supportsNativeActs", processFlow.totalEntries() > 0),
                entry("supportsInlineDecisions", processFlow.totalInlineActs() > 0),
                entry("supportsUnifiedContentSurface", true),
                entry("supportsInlineCopyModes", true),
                entry("supportsOcrAwareDocuments", true),
                entry("supportsReaderPreset", true),
                entry("supportsPrivacyVeil", true),
                entry("supportsKeyboardBias", true),
                entry("supportsProceduralContextMesh", true),
                entry("supportsAllBrazilianRites", true),
                entry("supportsAllBrazilianRights", true),
                entry("supportsAllProceduralGuarantees", true),
                entry("supportsInlineHtmlActs", proceduralContext.htmlInlinePreferred()),
                entry("supportsSignedPdfInspection", proceduralContext.pdfSignedPreferred()),
                entry("supportsReadingSpecialization", true),
                entry("supportsAllInstancesAndEmbargos", true),
                entry("supportsOpeningSequence", !specialization.openingSequence().isEmpty()),
                entry("supportsHybridHtmlPdfInspection", specialization.nativeHtmlPriority() && specialization.signedPdfInspectionRequired()),
                entry("supportsPecaSpecialization", true),
                entry("supportsNationalConvergenceMesh", true),
                entry("supportsCloudSigning", true),
                entry("supportsAICopilotHtml", true),
                entry("supportsPdfAOcrPipeline", true),
                entry("supportsDeadlineAggregation", true)
        ));
        frontend.put("stickyWidgets", List.of("summary", "navigation", "flow", "focusBand", "actions"));
        frontend.put("flowMetadata", processFlow.metadata());
        frontend.put("proceduralContext", proceduralContext);
        frontend.put("specialization", specialization);
        frontend.put("chunkPageSize", presetProfile.chunkPageSize());
        frontend.put("ecosystemMode", ecosystem.convergenceMode());
        frontend.put("signatureMode", ecosystem.signatureMode());
        frontend.put("documentPipelineMode", ecosystem.documentPipelineMode());
        return frontend;
    }

    private List<String> mergeAlerts(ProcessReadingModeProfile modeProfile,
                                     ProcessReadingPresetProfile presetProfile,
                                     ProcessReadingNavigationResponse navigation,
                                     ProcessReadingFlowResponse processFlow,
                                     ProcessReadingProceduralContextResponse proceduralContext,
                                     ProcessReadingSpecializationResponse specialization) {
        ArrayList<String> alerts = new ArrayList<>(modeProfile.alerts());
        if (navigation.totalNodes() == 0 && modeProfile.totalPaginas() > 0L) {
            alerts.add("Mapa de navegação ainda raso: reforçar OCR e indexação para peças sem texto estruturado.");
        }
        if (modeProfile.sigiloReforcado() && !"SEM_MASCARA".equals(presetProfile.privacyVeilMode())) {
            alerts.add("Sigilo reforçado ativo: habilitar máscara de exposição e evitar pré-visualizações laterais persistentes.");
        }
        if (modeProfile.volumeExtenso()) {
            alerts.add("Preset adaptativo aplicado: dividir leitura em lotes de até " + presetProfile.chunkPageSize() + " páginas por ciclo de foco.");
        }
        if (processFlow.totalEntries() > 0) {
            alerts.add("Leitura nativa do processo disponível: despachos, decisões, movimentações e eventos podem ser abertos sem depender de PDF.");
        }
        if (modeProfile.totalPaginas() == 0L && processFlow.totalEntries() > 0) {
            alerts.add("Processo com trilha textual nativa ativa: priorizar fluxo processual e atos inline como superfície primária de leitura.");
        }
        alerts.add("Contexto procedimental ativo: leitura adaptada para " + proceduralContext.justiceTrack() + ", " + proceduralContext.tribunalTier() + " e família " + proceduralContext.ritoFamily() + ".");
        if (proceduralContext.htmlInlinePreferred()) {
            alerts.add("Ato textual nativo em HTML priorizado: conferir assinatura, histórico e exportação PDF apenas quando necessário.");
        }
        if (!"SEM_MALHA_DE_EMBARGOS".equals(proceduralContext.embargoTrack())) {
            alerts.add("Malha recursal/embargos identificada: destacar integração entre decisão, recurso, contrarrazões e embargos.");
        }
        alerts.add("Trilha especializada ativa: " + specialization.decisionMode() + " com sequência padrão " + String.join(" → ", specialization.openingSequence().stream().limit(5).toList()) + ".");
        if (specialization.signedPdfInspectionRequired()) {
            alerts.add("Conferência formal disponível: usar PDF assinado apenas para validação formal, mantendo HTML nativo como superfície de leitura primária quando possível.");
        }
        return List.copyOf(alerts);
    }

    private ProcessReadingActionResponse action(String action,
                                                String label,
                                                String severity,
                                                boolean enabled,
                                                String endpoint,
                                                Map<String, Object> payload) {
        return new ProcessReadingActionResponse(action, label, severity, enabled, endpoint, payload);
    }

    private ProcessReadingLaneResponse lane(String code,
                                            String status,
                                            String descriptor,
                                            List<String> highlights,
                                            Map<String, Object> metadata) {
        return new ProcessReadingLaneResponse(code, status, descriptor, highlights, metadata);
    }


    private static String resolveDefaultTab(ProcessReadingModeProfile modeProfile, ProcessReadingFlowResponse processFlow) {
        if (modeProfile.totalPaginas() == 0L && processFlow.totalEntries() > 0) {
            return "ATOS";
        }
        if (modeProfile.recursal()) {
            return "RECURSAL";
        }
        if (processFlow.totalEntries() > 0 && modeProfile.coberturaTextualPercentual() < 65) {
            return "ATOS";
        }
        if (modeProfile.coberturaTextualPercentual() < 65) {
            return "TEXTO";
        }
        return modeProfile.volumeExtenso() ? "FOCO" : "VISUAL";
    }

    private static String suggestedDocumentMode(DocumentoProcessual documento,
                                                long totalPaginas,
                                                ProcessReadingModeProfile modeProfile,
                                                ProcessReadingPresetProfile presetProfile) {
        String titulo = resolveTitulo(documento).toUpperCase(Locale.ROOT);
        if (titulo.contains("SENTENCA") || titulo.contains("DECISAO") || titulo.contains("ACORDAO")) {
            return modeProfile.recursal() ? "PECA_DECISORIA_RECURSAL_ASSISTIDA" : "PECA_DECISORIA_ASSISTIDA";
        }
        if (titulo.contains("PETICAO") || titulo.contains("CONTESTACAO") || titulo.contains("RECURSO")) {
            return presetProfile.citationMode().contains("TEMAS") ? "PECA_ARGUMENTATIVA_COM_MAPA_DE_CITACOES" : "PECA_ARGUMENTATIVA";
        }
        if (titulo.contains("LAUDO") || titulo.contains("PERICIA") || titulo.contains("EXTRATO") || titulo.contains("CONTRATO")) {
            return "PECA_PROBATORIA_ASSISTIDA";
        }
        if (totalPaginas >= 60L) {
            return "LEITURA_LONGA_SEGMENTADA";
        }
        return "LEITURA_LINEAR_ASSISTIDA";
    }


    private static String fragment(String text, String query) {
        if (blank(text)) {
            return "Página sem texto extraído disponível.";
        }
        String normalizedText = text.replace('\n', ' ').trim();
        String lower = normalizedText.toLowerCase(Locale.ROOT);
        String q = query.toLowerCase(Locale.ROOT);
        int index = lower.indexOf(q);
        if (index < 0) {
            return normalizedText.length() <= 180 ? normalizedText : normalizedText.substring(0, 180) + "...";
        }
        int start = Math.max(0, index - 70);
        int end = Math.min(normalizedText.length(), index + q.length() + 110);
        String slice = normalizedText.substring(start, end).trim();
        if (start > 0) {
            slice = "..." + slice;
        }
        if (end < normalizedText.length()) {
            slice = slice + "...";
        }
        return slice;
    }

    private static String inferLane(DocumentoProcessual documento, String query) {
        String titulo = resolveTitulo(documento).toUpperCase(Locale.ROOT);
        String q = query.toUpperCase(Locale.ROOT);
        if (titulo.contains("RECURSO") || titulo.contains("ACORDAO") || titulo.contains("CONTRARRAZOES") || q.contains("RECURSO")) {
            return "RECURSAL";
        }
        if (titulo.contains("LAUDO") || titulo.contains("PROVA") || titulo.contains("DOCUMENTO") || q.contains("PROVA")) {
            return "PROVA";
        }
        if (q.contains("ART.") || q.contains("TEMA") || q.contains("SUMULA") || q.contains("PRECEDENTE")) {
            return "CITACOES";
        }
        return "TEXTO";
    }

    private static boolean containsNormalized(String text, String query) {
        return text != null && query != null && text.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
    }

    private static String normalizeQuery(String query) {
        if (query == null) {
            return null;
        }
        String text = query.trim();
        return text.isBlank() ? null : text;
    }

    private static String resolveTitulo(DocumentoProcessual documento) {
        if (documento == null) {
            return "Documento";
        }
        if (!blank(documento.getTitulo())) {
            return documento.getTitulo().trim();
        }
        if (!blank(documento.getNomeOriginal())) {
            return documento.getNomeOriginal().trim();
        }
        return documento.getId() != null ? documento.getId().toString() : "Documento";
    }

    private static String resolveRoleLabel(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return "ANONIMO";
        }
        return usuario.getTipoUsuario().name();
    }

    private static String resolveCluster(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return null;
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo.isMagistratura()) {
            return "MAGISTRATURA";
        }
        if (tipo.isAssessor()) {
            return "ASSESSORIA";
        }
        if (tipo.isMinisterioPublico()) {
            return "MINISTERIO_PUBLICO";
        }
        if (tipo.isDefensoriaPublica()) {
            return "DEFENSORIA_PUBLICA";
        }
        if (tipo.isProcuradoria()) {
            return "PROCURADORIA";
        }
        if (tipo.isAdvocacia()) {
            return "ADVOCACIA";
        }
        if (tipo.isServidorJudiciario()) {
            return "SERVIDOR_JUDICIARIO";
        }
        return tipo.name();
    }

    private static String safeText(String value, String fallback) {
        return blank(value) ? fallback : value.trim();
    }

    @SafeVarargs
    private static Map<String, Object> mapOfEntries(Map.Entry<String, Object>... entries) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            if (entry != null && entry.getValue() != null) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    private static Map.Entry<String, Object> entry(String key, Object value) {
        return Map.entry(key, value == null ? "" : value);
    }

    private static void putIfPresent(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }


    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }


}
