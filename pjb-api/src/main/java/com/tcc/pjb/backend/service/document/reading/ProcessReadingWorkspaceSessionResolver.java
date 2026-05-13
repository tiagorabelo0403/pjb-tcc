package com.tcc.pjb.backend.service.document.reading;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

final class ProcessReadingWorkspaceSessionResolver {

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

    ProcessReadingWorkspaceSessionResolver(ProcessoRepository processoRepository,
                                           DocumentoProcessualRepository documentoRepository,
                                           DocumentoPaginaRepository paginaRepository,
                                           MovimentacaoProcessualRepository movimentacaoRepository,
                                           EventoProcessualRepository eventoRepository,
                                           PjbAuthorizationService authorizationService,
                                           RecursalEffectiveSecrecyService secrecyService,
                                           CurrentUserService currentUserService,
                                           ProcessReadingModeResolver modeResolver,
                                           ProcessReadingPresetResolver presetResolver) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.paginaRepository = Objects.requireNonNull(paginaRepository);
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.eventoRepository = Objects.requireNonNull(eventoRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.secrecyService = Objects.requireNonNull(secrecyService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.modeResolver = Objects.requireNonNull(modeResolver);
        this.presetResolver = Objects.requireNonNull(presetResolver);
    }

    ProcessReadingWorkspaceSession resolveProcessSession(Long processoId) {
        return finalizeSession(loadProcessContext(processoId));
    }

    ProcessReadingWorkspaceSession resolveDocumentSession(UUID documentoId) {
        DocumentoProcessual documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Documento", documentoId));
        Processo processo = carregarProcesso(documento.getProcesso() != null ? documento.getProcesso().getId() : null);
        authorizationService.requireReadProcessoAtSecrecy(processo, secrecyService.effectiveSecrecyForProcesso(processo.getId()));
        authorizationService.requireReadDocumentoAtSecrecy(processo, documento, secrecyService.effectiveSecrecyForProcesso(processo.getId()));
        List<DocumentoPagina> paginas = paginaRepository.findByDocumentoId(documentoId);
        List<MovimentacaoProcessual> movimentacoes = processo.getId() == null ? List.of() : movimentacaoRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(processo.getId());
        List<EventoProcessual> eventos = processo.getId() == null ? List.of() : eventoRepository.findTop36ByProcesso_IdOrderByDataInicioDesc(processo.getId());
        Map<UUID, ProcessReadingPageCounter> documentStats = documento.getId() == null
                ? Map.of()
                : Map.of(documento.getId(), new ProcessReadingPageCounter(paginas.size(), paginas.stream().filter(page -> !blank(page.getTextoExtraido())).count()));
        ProcessReadingWorkspaceContext context = new ProcessReadingWorkspaceContext(
                processo,
                List.of(documento),
                paginas,
                paginas,
                movimentacoes,
                eventos,
                1,
                paginas.size(),
                paginas.stream().filter(page -> !blank(page.getTextoExtraido())).count(),
                documentStats
        );
        return finalizeSession(context);
    }

    private ProcessReadingWorkspaceSession finalizeSession(ProcessReadingWorkspaceContext context) {
        Usuario usuario = currentUserService.getOrNull();
        ProcessReadingModeProfile modeProfile = modeResolver.resolve(context.processo(), usuario, context.totalDocumentos(), context.totalPaginas(), context.paginasComTexto());
        ProcessReadingPresetProfile presetProfile = presetResolver.resolve(usuario, context.processo(), modeProfile);
        return new ProcessReadingWorkspaceSession(context, usuario, modeProfile, presetProfile);
    }

    private ProcessReadingWorkspaceContext loadProcessContext(Long processoId) {
        Processo processo = carregarProcesso(processoId);
        authorizationService.requireReadProcessoAtSecrecy(processo, secrecyService.effectiveSecrecyForProcesso(processoId));
        long totalDocumentos = documentoRepository.countByProcesso_Id(processoId);
        DocumentoPaginaRepository.ProcessStats processStats = paginaRepository.findProcessStatsByProcessoId(processoId);
        long totalPaginas = processStats != null ? processStats.getTotalPages() : 0L;
        long paginasComTexto = processStats != null ? processStats.getPagesWithText() : 0L;
        List<DocumentoProcessual> documentos = documentoRepository.findTop18ByProcesso_IdOrderByCriadoEmDesc(processoId);
        List<UUID> documentoIds = documentos.stream()
                .map(DocumentoProcessual::getId)
                .filter(Objects::nonNull)
                .toList();
        List<DocumentoPagina> paginas = documentoIds.isEmpty() ? List.of() : paginaRepository.findByDocumentoIds(documentoIds);
        Map<UUID, ProcessReadingPageCounter> documentStats = documentoIds.isEmpty() ? Map.of() : paginaRepository.findDocumentStatsByDocumentoIds(documentoIds).stream()
                .collect(Collectors.toMap(DocumentoPaginaRepository.DocumentStats::getDocumentoId, stats -> new ProcessReadingPageCounter(stats.getTotalPages(), stats.getPagesWithText()), (left, right) -> left, LinkedHashMap::new));
        List<DocumentoPagina> navigationPages = paginaRepository.findTopNavigationPagesByProcessoId(processoId, 720);
        List<MovimentacaoProcessual> movimentacoes = movimentacaoRepository.findTop60ByProcesso_IdOrderByDataMovimentacaoDesc(processoId);
        List<EventoProcessual> eventos = eventoRepository.findTop36ByProcesso_IdOrderByDataInicioDesc(processoId);
        return new ProcessReadingWorkspaceContext(processo, documentos, paginas, navigationPages, movimentacoes, eventos, totalDocumentos, totalPaginas, paginasComTexto, documentStats);
    }

    private Processo carregarProcesso(Long processoId) {
        if (processoId == null) {
            throw new RecursoNaoEncontradoException("Processo", null);
        }
        return processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
