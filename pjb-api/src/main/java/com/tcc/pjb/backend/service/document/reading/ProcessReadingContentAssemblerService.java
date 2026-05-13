package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingContentBlockResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingContentResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingFlowResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingPreferenceResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingProcessEntryResponse;
import com.tcc.pjb.backend.model.dto.leitura.ProcessReadingSurfaceResponse;
import com.tcc.pjb.backend.model.entity.EventoProcessual;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoPagina;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.EventoProcessualRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.recursal.RecursalEffectiveSecrecyService;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ProcessReadingContentAssemblerService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ProcessoRepository processoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final DocumentoPaginaRepository paginaRepository;
    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final EventoProcessualRepository eventoRepository;
    private final PjbAuthorizationService authorizationService;
    private final RecursalEffectiveSecrecyService secrecyService;
    private final CurrentUserService currentUserService;
    private final ProcessReadingModeResolver modeResolver;
    private final ProcessReadingPresetResolver presetResolver;
    private final ProcessReadingFlowResolver flowResolver;
    private final ProcessReadingSurfaceResolver surfaceResolver;

    public ProcessReadingContentAssemblerService(ProcessoRepository processoRepository,
                                                DocumentoProcessualRepository documentoRepository,
                                                DocumentoPaginaRepository paginaRepository,
                                                MovimentacaoProcessualRepository movimentacaoRepository,
                                                EventoProcessualRepository eventoRepository,
                                                PjbAuthorizationService authorizationService,
                                                RecursalEffectiveSecrecyService secrecyService,
                                                CurrentUserService currentUserService,
                                                ProcessReadingModeResolver modeResolver,
                                                ProcessReadingPresetResolver presetResolver,
                                                ProcessReadingFlowResolver flowResolver,
                                                ProcessReadingSurfaceResolver surfaceResolver) {
        this.processoRepository = processoRepository;
        this.documentoRepository = documentoRepository;
        this.paginaRepository = paginaRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.eventoRepository = eventoRepository;
        this.authorizationService = authorizationService;
        this.secrecyService = secrecyService;
        this.currentUserService = currentUserService;
        this.modeResolver = modeResolver;
        this.presetResolver = presetResolver;
        this.flowResolver = flowResolver;
        this.surfaceResolver = surfaceResolver;
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingContentResponse assembleDocumento(UUID documentoId) {
        DocumentoProcessual documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento", documentoId));
        Processo processo = loadProcess(documento.getProcesso() != null ? documento.getProcesso().getId() : null);
        authorizationService.requireReadProcessoAtSecrecy(processo, secrecyService.effectiveSecrecyForProcesso(processo.getId()));
        authorizationService.requireReadDocumentoAtSecrecy(processo, documento, secrecyService.effectiveSecrecyForProcesso(processo.getId()));
        List<DocumentoPagina> paginas = paginaRepository.findByDocumentoId(documentoId);
        Usuario usuario = currentUserService.getOrNull();
        ProcessReadingModeProfile modeProfile = modeResolver.resolve(processo, usuario, List.of(documento), paginas);
        ProcessReadingPresetProfile presetProfile = presetResolver.resolve(usuario, processo, modeProfile);
        ProcessReadingSurfaceResponse surface = surfaceResolver.resolveDocument(documento, paginas, modeProfile, presetProfile);
        List<ProcessReadingContentBlockResponse> blocks = buildDocumentBlocks(processo, documento, paginas, modeProfile, presetProfile);
        return new ProcessReadingContentResponse(
                documentoId.toString(),
                "DOCUMENTO_PROCESSUAL",
                resolveTitle(documento),
                surface,
                toPreference(presetProfile),
                presetProfile.chronologyMode(),
                presetProfile.focusBandMode(),
                allowsCopy(surface),
                true,
                paginas.stream().anyMatch(page -> !blank(page.getTextoExtraido())),
                blocks,
                buildDocumentMetadata(processo, documento, paginas, modeProfile, presetProfile, surface, blocks)
        );
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public ProcessReadingContentResponse assembleFluxo(Long processoId, String entryId) {
        Processo processo = loadProcess(processoId);
        authorizationService.requireReadProcessoAtSecrecy(processo, secrecyService.effectiveSecrecyForProcesso(processo.getId()));
        Usuario usuario = currentUserService.getOrNull();
        long totalDocumentos = documentoRepository.countByProcesso_Id(processoId);
        DocumentoPaginaRepository.ProcessStats processStats = paginaRepository.findProcessStatsByProcessoId(processoId);
        long totalPaginas = processStats != null ? processStats.getTotalPages() : 0L;
        long paginasComTexto = processStats != null ? processStats.getPagesWithText() : 0L;
        List<MovimentacaoProcessual> movimentacoes = movimentacaoRepository.findTop80ByProcesso_IdOrderByDataMovimentacaoDesc(processoId);
        List<EventoProcessual> eventos = eventoRepository.findTop48ByProcesso_IdOrderByDataInicioDesc(processoId);
        ProcessReadingModeProfile modeProfile = modeResolver.resolve(processo, usuario, totalDocumentos, totalPaginas, paginasComTexto);
        ProcessReadingPresetProfile presetProfile = presetResolver.resolve(usuario, processo, modeProfile);
        ProcessReadingFlowResponse flow = flowResolver.resolve(processo, usuario, movimentacoes, eventos, modeProfile, presetProfile);
        if (blank(entryId)) {
            List<DocumentoProcessual> documentos = documentoRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(processoId);
            return assembleOverview(processo, modeProfile, presetProfile, flow, documentos, totalDocumentos, totalPaginas);
        }
        ProcessReadingProcessEntryResponse entry = flow.entries().stream()
                .filter(candidate -> candidate.entryId().equalsIgnoreCase(entryId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("EntradaProcessual", entryId));
        ProcessReadingSurfaceResponse surface = surfaceResolver.resolveNativeEntry(processoId, entry, modeProfile, presetProfile);
        List<ProcessReadingContentBlockResponse> blocks = buildEntryBlocks(processo, entry, movimentacoes, eventos, modeProfile, presetProfile);
        return new ProcessReadingContentResponse(
                entry.entryId(),
                "FLUXO_PROCESSUAL",
                entry.title(),
                surface,
                toPreference(presetProfile),
                presetProfile.chronologyMode(),
                presetProfile.focusBandMode(),
                allowsCopy(surface),
                entry.downloadable() || entry.pdfEndpoint() != null,
                true,
                blocks,
                buildEntryMetadata(processo, entry, flow, modeProfile, presetProfile, surface, blocks)
        );
    }

    private ProcessReadingContentResponse assembleOverview(Processo processo,
                                                           ProcessReadingModeProfile modeProfile,
                                                           ProcessReadingPresetProfile presetProfile,
                                                           ProcessReadingFlowResponse flow,
                                                           List<DocumentoProcessual> documentos,
                                                           long totalDocumentos,
                                                           long totalPaginas) {
        ProcessReadingProcessEntryResponse synthetic = new ProcessReadingProcessEntryResponse(
                "FLOW-OVERVIEW",
                "PROCESS_OVERVIEW",
                "PROCESS_OVERVIEW",
                "Leitura integrada do processo",
                safePreview(firstNonBlank(processo.getResumoIA(), processo.getObjetoProcessual(), processo.getAssunto())),
                "SISTEMA",
                timestamp(processo.getDataUltimaMovimentacao()),
                "ATOS",
                "medium",
                false,
                "/api/v1/processos/" + processo.getId() + "/painel-leitura/conteudo",
                null,
                List.of("PROCESS_OVERVIEW", modeProfile.profileCode()),
                compactMap("processoId", processo.getId())
        );
        ProcessReadingSurfaceResponse surface = surfaceResolver.resolveNativeEntry(processo.getId(), synthetic, modeProfile, presetProfile);
        ArrayList<ProcessReadingContentBlockResponse> blocks = new ArrayList<>();
        addOverviewBlock(blocks, processo, "OV-RESUMO", "RESUMO_PROCESSUAL", "Resumo processual", processo.getResumoIA(), "high");
        addOverviewBlock(blocks, processo, "OV-PEDIDOS", "PEDIDOS_CONSOLIDADOS", "Pedidos consolidados", processo.getPedidosConsolidados(), "high");
        addOverviewBlock(blocks, processo, "OV-PROVAS", "MATERIAL_PROBATORIO", "Material probatório consolidado", processo.getMaterialProbatorioResumo(), "high");
        addOverviewBlock(blocks, processo, "OV-RESULTADO", "RESULTADO_FINAL", "Resultado final consolidado", processo.getResultadoFinal(), "medium");
        blocks.add(new ProcessReadingContentBlockResponse(
                "OV-MALHA",
                "PROCESS_OVERVIEW",
                "WORKSPACE_GUIDE",
                "Superfícies disponíveis no processo",
                "Atos nativos: " + flow.totalEntries() + " | Documentos: " + totalDocumentos + " | Páginas indexadas: " + totalPaginas,
                null,
                "OV-MALHA",
                "medium",
                List.of("MALHA_LEITURA", modeProfile.profileCode()),
                compactMap(
                        "flowEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/fluxo",
                        "navigationEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/navegacao",
                        "presetCatalogEndpoint", "/api/v1/processos/" + processo.getId() + "/painel-leitura/presets",
                        "documentCount", totalDocumentos,
                        "entryCount", flow.totalEntries()
                )
        ));
        return new ProcessReadingContentResponse(
                String.valueOf(processo.getId()),
                "PROCESS_OVERVIEW",
                "Leitura integrada do processo",
                surface,
                toPreference(presetProfile),
                presetProfile.chronologyMode(),
                presetProfile.focusBandMode(),
                true,
                false,
                true,
                List.copyOf(blocks),
                compactMap(
                        "processoId", processo.getId(),
                        "numeroProcesso", processo.getNumeroProcesso(),
                        "flowEntries", flow.totalEntries(),
                        "documentCount", totalDocumentos,
                        "pageCount", totalPaginas,
                        "defaultTab", totalPaginas == 0L && flow.totalEntries() > 0 ? "ATOS" : "FOCO"
                )
        );
    }

    private List<ProcessReadingContentBlockResponse> buildDocumentBlocks(Processo processo,
                                                                         DocumentoProcessual documento,
                                                                         List<DocumentoPagina> paginas,
                                                                         ProcessReadingModeProfile modeProfile,
                                                                         ProcessReadingPresetProfile presetProfile) {
        if (paginas.isEmpty()) {
            return List.of(new ProcessReadingContentBlockResponse(
                    "DOC-EMPTY",
                    "DOCUMENTO_PROCESSUAL",
                    "DOCUMENT_PLACEHOLDER",
                    resolveTitle(documento),
                    "Documento sem páginas indexadas para leitura direta. A superfície permanece disponível para download e exibição controlada.",
                    null,
                    "DOC-EMPTY",
                    "medium",
                    List.of("SEM_PAGINACAO_INDEXADA", modeProfile.profileCode()),
                    compactMap(
                            "documentoId", documento.getId(),
                            "contentEndpoint", "/api/v1/documentos/" + documento.getId() + "/painel-leitura/conteudo",
                            "pdfEndpoint", "/api/v1/documentos/" + documento.getId() + "/pdf"
                    )
            ));
        }
        ArrayList<ProcessReadingContentBlockResponse> blocks = new ArrayList<>();
        int chunkSize = Math.max(1, presetProfile.chunkPageSize());
        for (int start = 0; start < paginas.size(); start += chunkSize) {
            int end = Math.min(paginas.size(), start + chunkSize);
            List<DocumentoPagina> chunk = paginas.subList(start, end);
            DocumentoPagina first = chunk.get(0);
            DocumentoPagina last = chunk.get(chunk.size() - 1);
            String text = joinPageChunk(chunk);
            boolean hasText = !blank(text);
            String title = chunk.size() == 1
                    ? "Página " + first.getPageNumber()
                    : "Páginas " + first.getPageNumber() + " a " + last.getPageNumber();
            LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("processoId", processo.getId());
            metadata.put("documentoId", documento.getId());
            metadata.put("pageStart", first.getPageNumber());
            metadata.put("pageEnd", last.getPageNumber());
            metadata.put("hasText", hasText);
            metadata.put("focusBandMode", presetProfile.focusBandMode());
            metadata.put("selectionMode", hasText ? "DIRECT" : "OCR_REQUIRED");
            blocks.add(new ProcessReadingContentBlockResponse(
                    "DOC-" + first.getPageNumber() + "-" + last.getPageNumber(),
                    "DOCUMENTO_PROCESSUAL",
                    hasText ? "TEXT_PAGE_CHUNK" : "OCR_REQUIRED_PAGE_CHUNK",
                    title,
                    hasText ? text : "Sem camada textual utilizável neste bloco. Priorizar OCR assistido, ampliação confortável e navegação por âncoras processuais.",
                    first.getPageNumber(),
                    "PAG-" + first.getPageNumber(),
                    chunkImportance(first.getPageNumber(), last.getPageNumber(), modeProfile),
                    List.of(hasText ? "TEXTO_EXTRAIDO" : "OCR_PENDENTE", modeProfile.profileCode()),
                    metadata
            ));
        }
        return List.copyOf(blocks);
    }

    private List<ProcessReadingContentBlockResponse> buildEntryBlocks(Processo processo,
                                                                      ProcessReadingProcessEntryResponse entry,
                                                                      List<MovimentacaoProcessual> movimentacoes,
                                                                      List<EventoProcessual> eventos,
                                                                      ProcessReadingModeProfile modeProfile,
                                                                      ProcessReadingPresetProfile presetProfile) {
        ArrayList<ProcessReadingContentBlockResponse> blocks = new ArrayList<>();
        if (entry.entryId().startsWith("INLINE-")) {
            addInlineBlocks(blocks, processo, entry, modeProfile, presetProfile);
        } else if (entry.entryId().startsWith("MOV-")) {
            MovimentacaoProcessual movimentacao = findMovement(entry.entryId(), movimentacoes);
            addMovementBlocks(blocks, processo, entry, movimentacao, modeProfile, presetProfile);
        } else if (entry.entryId().startsWith("EVT-")) {
            EventoProcessual evento = findEvent(entry.entryId(), eventos);
            addEventBlocks(blocks, processo, entry, evento, modeProfile, presetProfile);
        }
        if (blocks.isEmpty()) {
            blocks.add(new ProcessReadingContentBlockResponse(
                    entry.entryId() + "-INLINE",
                    entry.sourceType(),
                    "INLINE_BODY",
                    entry.title(),
                    firstNonBlank(entry.bodyPreview(), "Sem corpo textual estruturado para esta entrada."),
                    null,
                    entry.entryId(),
                    entry.severity(),
                    entry.tags(),
                    compactMap(
                            "processoId", processo.getId(),
                            "entryId", entry.entryId(),
                            "chronologyMode", presetProfile.chronologyMode()
                    )
            ));
        }
        return List.copyOf(blocks);
    }

    private void addInlineBlocks(List<ProcessReadingContentBlockResponse> blocks,
                                 Processo processo,
                                 ProcessReadingProcessEntryResponse entry,
                                 ProcessReadingModeProfile modeProfile,
                                 ProcessReadingPresetProfile presetProfile) {
        String body = switch (entry.originMode()) {
            case "RESUMO_PROCESSUAL" -> firstNonBlank(processo.getResumoIA(), entry.bodyPreview());
            case "PEDIDOS_CONSOLIDADOS" -> firstNonBlank(processo.getPedidosConsolidados(), entry.bodyPreview());
            case "MATERIAL_PROBATORIO" -> firstNonBlank(processo.getMaterialProbatorioResumo(), entry.bodyPreview());
            case "RESULTADO_FINAL" -> firstNonBlank(processo.getResultadoFinal(), entry.bodyPreview());
            default -> entry.bodyPreview();
        };
        blocks.add(new ProcessReadingContentBlockResponse(
                entry.entryId() + "-1",
                entry.sourceType(),
                "INLINE_BODY",
                entry.title(),
                firstNonBlank(body, "Sem corpo textual consolidado disponível para a entrada inline."),
                null,
                entry.entryId(),
                entry.severity(),
                entry.tags(),
                compactMap(
                        "processoId", processo.getId(),
                        "originMode", entry.originMode(),
                        "supportDeskMode", modeProfile.supportDeskMode(),
                        "anchorMode", presetProfile.anchorMode()
                )
        ));
    }

    private void addMovementBlocks(List<ProcessReadingContentBlockResponse> blocks,
                                   Processo processo,
                                   ProcessReadingProcessEntryResponse entry,
                                   MovimentacaoProcessual movimentacao,
                                   ProcessReadingModeProfile modeProfile,
                                   ProcessReadingPresetProfile presetProfile) {
        String body = movimentacao != null ? firstNonBlank(movimentacao.getDescricao(), entry.bodyPreview()) : entry.bodyPreview();
        blocks.add(new ProcessReadingContentBlockResponse(
                entry.entryId() + "-1",
                entry.sourceType(),
                "MOVEMENT_BODY",
                entry.title(),
                firstNonBlank(body, "Movimentação sem descrição detalhada disponível."),
                null,
                entry.entryId(),
                entry.severity(),
                entry.tags(),
                compactMap(
                        "processoId", processo.getId(),
                        "movementTimestamp", movimentacao != null ? timestamp(movimentacao.getDataMovimentacao()) : entry.occurredAt(),
                        "phaseFrom", movimentacao != null && movimentacao.getFaseDe() != null ? movimentacao.getFaseDe().name() : null,
                        "phaseTo", movimentacao != null && movimentacao.getFasePara() != null ? movimentacao.getFasePara().name() : null,
                        "operationalOverlayMode", presetProfile.operationalOverlayMode(),
                        "supportDeskMode", modeProfile.supportDeskMode()
                )
        ));
    }

    private void addEventBlocks(List<ProcessReadingContentBlockResponse> blocks,
                                Processo processo,
                                ProcessReadingProcessEntryResponse entry,
                                EventoProcessual evento,
                                ProcessReadingModeProfile modeProfile,
                                ProcessReadingPresetProfile presetProfile) {
        String body = evento != null ? firstNonBlank(evento.getDescricao(), entry.bodyPreview()) : entry.bodyPreview();
        blocks.add(new ProcessReadingContentBlockResponse(
                entry.entryId() + "-1",
                entry.sourceType(),
                "EVENT_BODY",
                entry.title(),
                firstNonBlank(body, "Evento sem descrição detalhada disponível."),
                null,
                entry.entryId(),
                entry.severity(),
                entry.tags(),
                compactMap(
                        "processoId", processo.getId(),
                        "eventType", evento != null && evento.getTipo() != null ? evento.getTipo().name() : null,
                        "eventStatus", evento != null && evento.getStatus() != null ? evento.getStatus().name() : null,
                        "eventStart", evento != null ? timestamp(evento.getDataInicio()) : entry.occurredAt(),
                        "eventEnd", evento != null ? timestamp(evento.getDataFim()) : null,
                        "timelineMode", presetProfile.chronologyMode(),
                        "supportDeskMode", modeProfile.supportDeskMode()
                )
        ));
    }

    private void addOverviewBlock(List<ProcessReadingContentBlockResponse> blocks,
                                  Processo processo,
                                  String blockId,
                                  String blockType,
                                  String title,
                                  String body,
                                  String importance) {
        if (blank(body)) {
            return;
        }
        blocks.add(new ProcessReadingContentBlockResponse(
                blockId,
                "PROCESS_OVERVIEW",
                blockType,
                title,
                body,
                null,
                blockId,
                importance,
                List.of(blockType),
                compactMap(
                        "processoId", processo.getId(),
                        "numeroProcesso", processo.getNumeroProcesso()
                )
        ));
    }

    private Map<String, Object> buildDocumentMetadata(Processo processo,
                                                      DocumentoProcessual documento,
                                                      List<DocumentoPagina> paginas,
                                                      ProcessReadingModeProfile modeProfile,
                                                      ProcessReadingPresetProfile presetProfile,
                                                      ProcessReadingSurfaceResponse surface,
                                                      List<ProcessReadingContentBlockResponse> blocks) {
        long pagesWithText = paginas.stream().filter(page -> !blank(page.getTextoExtraido())).count();
        return compactMap(
                "processoId", processo.getId(),
                "numeroProcesso", processo.getNumeroProcesso(),
                "documentoId", documento.getId(),
                "surfaceMode", surface.displayMode(),
                "ocrStatus", surface.ocrStatus(),
                "textCoverage", paginas.isEmpty() ? 0 : Math.round(pagesWithText * 100.0d / paginas.size()),
                "blockCount", blocks.size(),
                "presetCode", presetProfile.presetCode(),
                "profileCode", modeProfile.profileCode()
        );
    }

    private Map<String, Object> buildEntryMetadata(Processo processo,
                                                   ProcessReadingProcessEntryResponse entry,
                                                   ProcessReadingFlowResponse flow,
                                                   ProcessReadingModeProfile modeProfile,
                                                   ProcessReadingPresetProfile presetProfile,
                                                   ProcessReadingSurfaceResponse surface,
                                                   List<ProcessReadingContentBlockResponse> blocks) {
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("processoId", processo.getId());
        metadata.put("numeroProcesso", processo.getNumeroProcesso());
        metadata.put("entryId", entry.entryId());
        metadata.put("originMode", entry.originMode());
        metadata.put("flowEntries", flow.totalEntries());
        metadata.put("surfaceMode", surface.displayMode());
        metadata.put("selectionMode", surface.selectionMode());
        metadata.put("timelineMode", presetProfile.chronologyMode());
        metadata.put("profileCode", modeProfile.profileCode());
        metadata.put("blockCount", blocks.size());
        return Collections.unmodifiableMap(metadata);
    }

    private ProcessReadingPreferenceResponse toPreference(ProcessReadingPresetProfile presetProfile) {
        return new ProcessReadingPreferenceResponse(
                presetProfile.readingModeEnabled(),
                presetProfile.intensity(),
                presetProfile.presetCode(),
                presetProfile.resolvedTheme(),
                presetProfile.fontScalePercent(),
                presetProfile.lineHeight(),
                presetProfile.paragraphGapRem(),
                presetProfile.letterSpacingEm(),
                presetProfile.maxWidthCh(),
                presetProfile.chunkPageSize(),
                presetProfile.focusBandMode(),
                presetProfile.privacyVeilMode(),
                presetProfile.keyboardBiasMode(),
                presetProfile.chronologyMode(),
                presetProfile.citationMode(),
                presetProfile.operationalOverlayMode(),
                presetProfile.searchAssistMode(),
                presetProfile.anchorMode()
        );
    }

    private Processo loadProcess(Long processoId) {
        if (processoId == null) {
            throw new RecursoNaoEncontradoException("Processo", "(n/a)");
        }
        return processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private static MovimentacaoProcessual findMovement(String entryId, List<MovimentacaoProcessual> movimentacoes) {
        if (movimentacoes == null || blank(entryId)) {
            return null;
        }
        String raw = entryId.substring(4);
        try {
            Long id = Long.parseLong(raw);
            return movimentacoes.stream().filter(mov -> id.equals(mov.getId())).findFirst().orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static EventoProcessual findEvent(String entryId, List<EventoProcessual> eventos) {
        if (eventos == null || blank(entryId)) {
            return null;
        }
        String raw = entryId.substring(4);
        try {
            Long id = Long.parseLong(raw);
            return eventos.stream().filter(evt -> id.equals(evt.getId())).findFirst().orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static boolean allowsCopy(ProcessReadingSurfaceResponse surface) {
        return surface != null && !safe(surface.selectionMode()).contains("INDIRETA");
    }

    private static String joinPageChunk(List<DocumentoPagina> chunk) {
        StringBuilder builder = new StringBuilder();
        for (DocumentoPagina page : chunk) {
            String text = page != null ? page.getTextoExtraido() : null;
            if (blank(text)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
            }
            builder.append(text.trim());
        }
        return builder.toString();
    }

    private static String chunkImportance(int startPage, int endPage, ProcessReadingModeProfile modeProfile) {
        if (startPage == 1 || startPage <= 3) {
            return "high";
        }
        if (modeProfile.recursal() && startPage <= 20) {
            return "high";
        }
        if (endPage - startPage >= 8) {
            return "medium";
        }
        return "low";
    }

    private static String resolveTitle(DocumentoProcessual documento) {
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

    private static String timestamp(Instant instant) {
        return instant == null ? null : DATE_TIME.format(instant.atOffset(ZoneOffset.UTC));
    }

    private static String timestamp(LocalDateTime localDateTime) {
        return localDateTime == null ? null : DATE_TIME.format(localDateTime.atOffset(ZoneOffset.UTC));
    }

    private static String safePreview(String text) {
        if (blank(text)) {
            return null;
        }
        String normalized = text.replace('\n', ' ').trim();
        return normalized.length() <= 240 ? normalized : normalized.substring(0, 240).trim() + "...";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!blank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static Map<String, Object> compactMap(Object... values) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (values == null) {
            return Map.of();
        }
        for (int index = 0; index + 1 < values.length; index += 2) {
            Object key = values[index];
            Object value = values[index + 1];
            if (key instanceof String text && !text.isBlank() && value != null) {
                map.put(text, value);
            }
        }
        return map.isEmpty() ? Map.of() : Map.copyOf(map);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
