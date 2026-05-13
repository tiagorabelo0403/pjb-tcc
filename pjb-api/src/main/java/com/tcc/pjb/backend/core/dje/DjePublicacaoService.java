package com.tcc.pjb.backend.core.dje;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.dje.domain.*;
import com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrail;
import com.tcc.pjb.backend.core.prazos.auditoria.PrazoAuditTrailService;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calculo.PrazosEngine;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.judicial.DjePublicacao;
import com.tcc.pjb.backend.model.repository.DjePublicacaoRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DjePublicacaoService {

    private final ProcessoRepository processoRepository;
    private final DjePublicacaoRepository djeRepository;
    private final DjeHttpClient djeHttpClient;
    private final PrazosEngine prazosEngine;
    private final AuditLedgerService auditLedger;
    private final ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy;
    private final PrazoAuditTrailService prazoAuditTrailService;
    private final DjePartesNotificacaoService djePartesNotificacaoService;

    public DjePublicacaoService(ProcessoRepository processoRepository,
                                DjePublicacaoRepository djeRepository,
                                DjeHttpClient djeHttpClient,
                                PrazosEngine prazosEngine,
                                AuditLedgerService auditLedger,
                                ReadAfterWriteConsistencyPolicy readAfterWriteConsistencyPolicy,
                                PrazoAuditTrailService prazoAuditTrailService,
                                DjePartesNotificacaoService djePartesNotificacaoService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.djeRepository = Objects.requireNonNull(djeRepository);
        this.djeHttpClient = Objects.requireNonNull(djeHttpClient);
        this.prazosEngine = Objects.requireNonNull(prazosEngine);
        this.auditLedger = Objects.requireNonNull(auditLedger);
        this.readAfterWriteConsistencyPolicy = Objects.requireNonNull(readAfterWriteConsistencyPolicy);
        this.prazoAuditTrailService = Objects.requireNonNull(prazoAuditTrailService);
        this.djePartesNotificacaoService = Objects.requireNonNull(djePartesNotificacaoService);
    }

    @Transactional
    @CircuitBreaker(name = "dje-publicacao")
    @Retry(name = "dje-publicacao")
    @Bulkhead(name = "dje-publicacao")
    public DjePublicacaoResult publicar(DjePublicacaoPayload payload) {
        Objects.requireNonNull(payload);
        return publicar(payload.toCommand());
    }

    @Transactional
    @CircuitBreaker(name = "dje-publicacao")
    @Retry(name = "dje-publicacao")
    @Bulkhead(name = "dje-publicacao")
    public DjePublicacaoResult publicar(DjePublicacaoCommand command) {
        Objects.requireNonNull(command);
        return publicar(command.processoId(), command.tipoAto(), command.conteudo(), command.tribunalCodigo());
    }

    @Transactional
    @CircuitBreaker(name = "dje-publicacao")
    @Retry(name = "dje-publicacao")
    @Bulkhead(name = "dje-publicacao")
    public DjePublicacaoResult publicar(Long processoId, String tipoAto, String conteudo, String tribunalCodigo) {
        var processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
        String hash = Hashes.sha256Hex(conteudo == null ? "" : conteudo);
        LocalDateTime disponibilizacao = LocalDate.now().atTime(LocalTime.MIDNIGHT).plusDays(1);
        LocalDateTime publicacao = prazosEngine.calcularTermino(
                disponibilizacao,
                1,
                PrazoRegime.UTEIS,
                processo.getUf(),
                processo.getComarca());
        LocalDateTime prazoComeca = prazosEngine.calcularTermino(
                publicacao,
                1,
                PrazoRegime.UTEIS,
                processo.getUf(),
                processo.getComarca());
        PrazoAuditTrail trail = prazoAuditTrailService.build(
                processoId,
                "DJE_PUBLICACAO",
                1,
                PrazoRegime.UTEIS,
                disponibilizacao,
                prazoComeca,
                processo.getUf(),
                processo.getComarca());
        DjePublicacao entity = DjePublicacao.builder()
                .processoId(processoId)
                .tipoAto(tipoAto)
                .conteudoHash(hash)
                .tribunalCodigo(tribunalCodigo)
                .dataDisponibilizacao(disponibilizacao.toLocalDate())
                .dataPublicacao(publicacao.toLocalDate())
                .prazoComecaEm(prazoComeca.toLocalDate())
                .status("PENDENTE_ENVIO")
                .partesNotificadas(false)
                .createdAt(Instant.now())
                .build();
        djeRepository.save(entity);
        try {
            DjeEnvioResult envio = djeHttpClient.enviar(new DjeEnvioCommand(tribunalCodigo, conteudo, tipoAto));
            entity.setEdicaoDje(envio.edicao());
            entity.setEnviadoEm(Instant.now());
            entity.setStatus("ENVIADO");
            djeRepository.save(entity);
            readAfterWriteConsistencyPolicy.markWrite();
            auditLedger.appendSafely(
                    "DJE_ENVIADO",
                    "PROCESSO",
                    String.valueOf(processoId),
                    "edicao=" + envio.edicao() + " prazoComeca=" + entity.getPrazoComecaEm() + " calendarioHash=" + trail.calendarioVersaoHash());
        } catch (Exception e) {
            entity.setStatus("FALHA");
            entity.setFailureReason(e.getMessage());
            djeRepository.save(entity);
            readAfterWriteConsistencyPolicy.markWrite();
            auditLedger.appendSafely("DJE_ENVIO_FALHA", "PROCESSO", String.valueOf(processoId), "erro=" + e.getMessage());
        }
        return new DjePublicacaoResult(entity.getId(), entity.getDataDisponibilizacao(), entity.getDataPublicacao(), entity.getPrazoComecaEm());
    }

    @Transactional
    public DjeConsolidacaoResult consolidarPublicadas(DjeConsolidarPublicadasCommand command) {
        Objects.requireNonNull(command);
        return new DjeConsolidacaoResult(consolidarPublicadas(command.hoje(), command.batchSize()));
    }

    @Transactional
    public DjeConsolidacaoResult consolidarPublicadas(DjeLifecycleCommand command) {
        Objects.requireNonNull(command);
        return new DjeConsolidacaoResult(consolidarPublicadas(command.hoje(), command.batchSize()));
    }

    @Transactional
    public int consolidarPublicadas(LocalDate hoje, int batchSize) {
        List<DjePublicacao> candidatas = djeRepository.findByStatusAndDataPublicacaoLessThanEqualOrderByDataPublicacaoAscIdAsc(
                "ENVIADO",
                hoje,
                PageRequest.of(0, Math.max(1, batchSize)));
        for (DjePublicacao publicacao : candidatas) {
            publicacao.setStatus("PUBLICADO");
            publicacao.setPublicadoEm(Instant.now());
            djeRepository.save(publicacao);
            auditLedger.appendSafely("DJE_PUBLICADO", "PROCESSO", String.valueOf(publicacao.getProcessoId()), "djeId=" + publicacao.getId());
        }
        if (!candidatas.isEmpty()) {
            readAfterWriteConsistencyPolicy.markWrite();
        }
        return candidatas.size();
    }

    @Transactional
    public DjeNotificacaoLoteResult notificarPartesPublicadas(DjeNotificarPartesCommand command) {
        Objects.requireNonNull(command);
        return new DjeNotificacaoLoteResult(notificarPartesPublicadas(command.hoje(), command.batchSize()));
    }

    @Transactional
    public DjeNotificacaoLoteResult notificarPartesPublicadas(DjeLifecycleCommand command) {
        Objects.requireNonNull(command);
        return new DjeNotificacaoLoteResult(notificarPartesPublicadas(command.hoje(), command.batchSize()));
    }

    @Transactional
    public int notificarPartesPublicadas(LocalDate hoje, int batchSize) {
        List<DjePublicacao> candidatas = djeRepository.findByStatusAndPrazoComecaEmLessThanEqualAndPartesNotificadasFalseOrderByPrazoComecaEmAscIdAsc(
                "PUBLICADO",
                hoje,
                PageRequest.of(0, Math.max(1, batchSize)));
        for (DjePublicacao publicacao : candidatas) {
            djePartesNotificacaoService.notificarPartes(publicacao);
            publicacao.setPartesNotificadas(true);
            djeRepository.save(publicacao);
            auditLedger.appendSafely("DJE_PARTES_NOTIFICADAS", "PROCESSO", String.valueOf(publicacao.getProcessoId()), "djeId=" + publicacao.getId());
        }
        if (!candidatas.isEmpty()) {
            readAfterWriteConsistencyPolicy.markWrite();
        }
        return candidatas.size();
    }

    @Transactional
    public DjeLifecycleExecutionSummary executarLifecycle(DjeLifecycleCommand command) {
        Objects.requireNonNull(command);
        return executarLifecycle(command.hoje(), command.batchSize());
    }

    @Transactional
    public DjeLifecycleExecutionSummary executarLifecycle(LocalDate hoje, int batchSize) {
        int consolidadas = consolidarPublicadas(hoje, batchSize);
        int notificadas = notificarPartesPublicadas(hoje, batchSize);
        return new DjeLifecycleExecutionSummary(consolidadas, notificadas);
    }

    @Transactional(readOnly = true)
    public DjeNotificacaoSnapshot notificacaoSnapshot(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjeNotificacaoSnapshot(publicacao.getId(), publicacao.getProcessoId(), publicacao.isPartesNotificadas(), publicacao.getPrazoComecaEm());
    }

    @Transactional(readOnly = true)
    public DjePublicacaoSnapshot snapshot(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjePublicacaoSnapshot(
                publicacao.getId(),
                publicacao.getProcessoId(),
                publicacao.getStatus(),
                publicacao.getTipoAto(),
                publicacao.getDataPublicacao(),
                publicacao.getPrazoComecaEm());
    }

    @Transactional(readOnly = true)
    public DjePublicacaoConsultaResult consultar(DjePublicacaoConsultaCommand command) {
        Objects.requireNonNull(command);
        DjePublicacaoSnapshot publicacao = snapshot(command.djeId());
        DjeNotificacaoSnapshot notificacao = notificacaoSnapshot(command.djeId());
        DjeLifecycleExecutionSummary lifecycle = new DjeLifecycleExecutionSummary(
                "PUBLICADO".equalsIgnoreCase(publicacao.status()) ? 1 : 0,
                notificacao.partesNotificadas() ? 1 : 0);
        return new DjePublicacaoConsultaResult(publicacao, notificacao, lifecycle);
    }

    @Transactional(readOnly = true)
    public DjeStatusResult status(DjeStatusQuery query) {
        Objects.requireNonNull(query);
        DjeStatusSnapshot status = statusSnapshot(query.djeId());
        return new DjeStatusResult(status, publicationHealth(query.djeId()));
    }

    @Transactional(readOnly = true)
    public DjeStatusSnapshot statusSnapshot(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjeStatusSnapshot(
                publicacao.getId(),
                publicacao.getProcessoId(),
                publicacao.getStatus(),
                publicacao.getEnviadoEm(),
                publicacao.getPublicadoEm());
    }

    @Transactional(readOnly = true)
    public DjePrazoProjection prazoProjection(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjePrazoProjection(publicacao.getDataDisponibilizacao(), publicacao.getDataPublicacao(), publicacao.getPrazoComecaEm());
    }

    @Transactional(readOnly = true)
    public DjeConsultaPrazoResult consultarPrazo(DjeConsultaPrazoCommand command) {
        Objects.requireNonNull(command);
        return new DjeConsultaPrazoResult(command.djeId(), prazoProjection(command.djeId()));
    }

    @Transactional(readOnly = true)
    public DjeFalhaResult falha(DjeFalhaQuery query) {
        Objects.requireNonNull(query);
        return new DjeFalhaResult(failureResult(query.djeId()), failureAudit(query.djeId()));
    }

    @Transactional(readOnly = true)
    public DjeFalhaEnvioResult failureResult(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjeFalhaEnvioResult(djeId, publicacao.getFailureReason(), "FALHA".equalsIgnoreCase(publicacao.getStatus()));
    }

    @Transactional(readOnly = true)
    public DjeFailureAuditSnapshot failureAudit(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjeFailureAuditSnapshot(djeId, publicacao.getStatus(), publicacao.getFailureReason());
    }

    @Transactional(readOnly = true)
    public DjeHealthResult health(DjeHealthQuery query) {
        Objects.requireNonNull(query);
        DjePublicacao publicacao = load(query.djeId());
        return new DjeHealthResult(
                query.djeId(),
                "PUBLICADO".equalsIgnoreCase(publicacao.getStatus()),
                publicacao.isPartesNotificadas(),
                "FALHA".equalsIgnoreCase(publicacao.getStatus()));
    }

    @Transactional(readOnly = true)
    public DjeLifecycleHealthView lifecycleHealth(Long djeId) {
        return new DjeLifecycleHealthView(health(new DjeHealthQuery(djeId)), publicationHealth(djeId), notificationHealth(djeId));
    }

    @Transactional(readOnly = true)
    public DjePublicationHealthSnapshot publicationHealth(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjePublicationHealthSnapshot(publicacao.getId(), publicacao.getStatus(), Instant.now());
    }

    @Transactional(readOnly = true)
    public DjeNotificationHealthSnapshot notificationHealth(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjeNotificationHealthSnapshot(publicacao.getId(), publicacao.isPartesNotificadas(), Instant.now());
    }

    @Transactional(readOnly = true)
    public DjePrazoHealthResult prazoHealth(DjePrazoHealthQuery query) {
        Objects.requireNonNull(query);
        DjePublicacao publicacao = load(query.djeId());
        return new DjePrazoHealthResult(prazoProjection(query.djeId()), "PUBLICADO".equalsIgnoreCase(publicacao.getStatus()), publicacao.isPartesNotificadas());
    }

    @Transactional(readOnly = true)
    public DjeDeadlineWindowView deadlineWindow(Long djeId) {
        DjePrazoProjection projection = prazoProjection(djeId);
        return new DjeDeadlineWindowView(projection.disponibilizacao(), projection.publicacao(), projection.prazoComecaEm());
    }

    @Transactional(readOnly = true)
    public DjePrazoAuditView prazoAudit(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjePrazoAuditView(
                djeId,
                publicacao.getDataDisponibilizacao(),
                publicacao.getDataPublicacao(),
                publicacao.getPrazoComecaEm(),
                publicacao.isPartesNotificadas());
    }

    @Transactional(readOnly = true)
    public DjeNotificacaoResult notificacao(DjeNotificacaoQuery query) {
        Objects.requireNonNull(query);
        return new DjeNotificacaoResult(notificacaoSnapshot(query.djeId()), notificationAudit(query.djeId()));
    }

    @Transactional(readOnly = true)
    public DjeNotificationAudit notificationAudit(Long djeId) {
        DjeNotificacaoSnapshot snapshot = notificacaoSnapshot(djeId);
        return new DjeNotificationAudit(snapshot.djeId(), snapshot.processoId(), snapshot.partesNotificadas(), snapshot.prazoComecaEm());
    }

    @Transactional(readOnly = true)
    public DjeTimelineView timeline(DjeTimelineQuery query) {
        Objects.requireNonNull(query);
        return timeline(query.djeId());
    }

    @Transactional(readOnly = true)
    public DjeTimelineView timeline(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        List<DjeTimelineEntry> entries = new ArrayList<>();
        entries.add(new DjeTimelineEntry("CRIADO", publicacao.getCreatedAt(), publicacao.getTipoAto()));
        if (publicacao.getEnviadoEm() != null) {
            entries.add(new DjeTimelineEntry("ENVIADO", publicacao.getEnviadoEm(), publicacao.getEdicaoDje()));
        }
        if (publicacao.getPublicadoEm() != null) {
            entries.add(new DjeTimelineEntry("PUBLICADO", publicacao.getPublicadoEm(), publicacao.getStatus()));
        }
        if (publicacao.isPartesNotificadas()) {
            entries.add(new DjeTimelineEntry("PARTES_NOTIFICADAS", Instant.now(), String.valueOf(publicacao.getPrazoComecaEm())));
        }
        if (publicacao.getFailureReason() != null && !publicacao.getFailureReason().isBlank()) {
            entries.add(new DjeTimelineEntry("FALHA", Instant.now(), publicacao.getFailureReason()));
        }
        return new DjeTimelineView(djeId, List.copyOf(entries));
    }

    @Transactional(readOnly = true)
    public DjeConsultaTimelineResult consultarTimeline(DjeConsultaTimelineCommand command) {
        Objects.requireNonNull(command);
        return new DjeConsultaTimelineResult(command.djeId(), timeline(command.djeId()).entries());
    }

    @Transactional(readOnly = true)
    public DjeTimelineAuditSnapshot timelineAudit(Long djeId) {
        DjeTimelineView timeline = timeline(djeId);
        boolean publicado = timeline.entries().stream().anyMatch(entry -> "PUBLICADO".equalsIgnoreCase(entry.evento()));
        boolean notificadas = timeline.entries().stream().anyMatch(entry -> "PARTES_NOTIFICADAS".equalsIgnoreCase(entry.evento()));
        return new DjeTimelineAuditSnapshot(djeId, timeline.entries().size(), publicado, notificadas);
    }

    @Transactional(readOnly = true)
    public DjeProcessoPublicationView processoPublicationView(Long processoId) {
        DjePublicacao publicacao = djeRepository.findTop100ByProcessoIdOrderByCreatedAtDesc(processoId).stream().findFirst().orElse(null);
        if (publicacao == null) {
            return new DjeProcessoPublicationView(processoId, null, "NAO_ENCONTRADO", null);
        }
        return new DjeProcessoPublicationView(publicacao.getProcessoId(), publicacao.getId(), publicacao.getStatus(), publicacao.getTribunalCodigo());
    }

    @Transactional(readOnly = true)
    public DjeTribunalPublicationView tribunalPublicationView(String tribunalCodigo) {
        DjePublicacao publicacao = djeRepository.findTop100ByTribunalCodigoIgnoreCaseOrderByCreatedAtDesc(tribunalCodigo).stream().findFirst().orElse(null);
        if (publicacao == null) {
            return new DjeTribunalPublicationView(tribunalCodigo, null, null, "NAO_ENCONTRADO");
        }
        return new DjeTribunalPublicationView(tribunalCodigo, publicacao.getId(), publicacao.getEdicaoDje(), publicacao.getStatus());
    }

    @Transactional(readOnly = true)
    public DjePublicationMetricsView publicationMetrics() {
        return new DjePublicationMetricsView(
                safeInt(djeRepository.countByStatus("PENDENTE_ENVIO")),
                safeInt(djeRepository.countByStatus("ENVIADO")),
                safeInt(djeRepository.countByStatus("PUBLICADO")),
                safeInt(djeRepository.countByStatus("FALHA")));
    }

    @Transactional(readOnly = true)
    public DjeTribunalPublicationResult tribunalPublication(DjeTribunalPublicationQuery query) {
        Objects.requireNonNull(query);
        return new DjeTribunalPublicationResult(
                query.tribunalCodigo(),
                djeRepository.countByTribunalCodigoIgnoreCase(query.tribunalCodigo()),
                djeRepository.countByTribunalCodigoIgnoreCaseAndStatusIgnoreCase(query.tribunalCodigo(), "ENVIADO"),
                djeRepository.countByTribunalCodigoIgnoreCaseAndStatusIgnoreCase(query.tribunalCodigo(), "PUBLICADO"),
                djeRepository.countByTribunalCodigoIgnoreCaseAndStatusIgnoreCase(query.tribunalCodigo(), "FALHA"));
    }

    @Transactional(readOnly = true)
    public DjeTribunalHealthResult tribunalHealth(DjeTribunalHealthQuery query) {
        Objects.requireNonNull(query);
        long total = djeRepository.countByTribunalCodigoIgnoreCase(query.referencia());
        return new DjeTribunalHealthResult(total >= 0, "total=" + total, Instant.now());
    }

    @Transactional(readOnly = true)
    public DjeTribunalHealthView tribunalHealthView(String tribunalCodigo) {
        DjeTribunalPublicationResult result = tribunalPublication(new DjeTribunalPublicationQuery(tribunalCodigo, null, null));
        return new DjeTribunalHealthView(tribunalCodigo, result.falhas() > 0 ? "DEGRADADO" : "OK", "total=" + result.total());
    }

    @Transactional(readOnly = true)
    public DjeConteudoHashResult conteudoHash(DjeConteudoHashQuery query) {
        Objects.requireNonNull(query);
        boolean exists = djeRepository.existsByProcessoIdAndTipoAtoAndConteudoHash(query.processoId(), query.tipoAto(), query.conteudoHash());
        return new DjeConteudoHashResult(query.processoId(), query.tipoAto(), query.conteudoHash(), exists, exists ? "EXISTS" : "ABSENT");
    }

    @Transactional(readOnly = true)
    public DjeConteudoHashView conteudoHashView(Long djeId) {
        DjePublicacao publicacao = load(djeId);
        return new DjeConteudoHashView(djeId, publicacao.getConteudoHash(), publicacao.getTipoAto());
    }

    @Transactional(readOnly = true)
    public DjeEdicaoResult edicao(DjeEdicaoQuery query) {
        Objects.requireNonNull(query);
        long total = djeRepository.countByEdicaoDje(query.reference());
        return new DjeEdicaoResult(total > 0, "publicacoes=" + total, total);
    }

    @Transactional(readOnly = true)
    public DjeEdicaoHealthSnapshot edicaoHealth(String edicao) {
        return new DjeEdicaoHealthSnapshot(edicao, djeRepository.countByEdicaoDje(edicao) > 0, safeInt(djeRepository.countByEdicaoDje(edicao)));
    }

    @Transactional(readOnly = true)
    public DjeEdicaoMetricsView edicaoMetrics(String edicao) {
        return new DjeEdicaoMetricsView(
                edicao,
                djeRepository.countByEdicaoDje(edicao),
                djeRepository.countByEdicaoDjeAndStatusIgnoreCase(edicao, "FALHA"),
                djeRepository.countByEdicaoDjeAndPartesNotificadasFalse(edicao));
    }

    @Transactional(readOnly = true)
    public DjePublicationConsistencyView consistencyView(Long djeId) {
        return new DjePublicationConsistencyView(String.valueOf(djeId), statusSnapshot(djeId).status(), Instant.now());
    }

    @Transactional(readOnly = true)
    public DjePublicationConsistencyAuditView consistencyAudit(Long djeId) {
        DjeStatusSnapshot status = statusSnapshot(djeId);
        String detalhe = status.publicadoEm() != null ? "publicado" : "em_fluxo";
        return new DjePublicationConsistencyAuditView(String.valueOf(djeId), status.status(), detalhe);
    }

    @Transactional(readOnly = true)
    public DjePrazoWindowResult prazoWindow(DjePrazoWindowQuery query) {
        Objects.requireNonNull(query);
        return new DjePrazoWindowResult(true, "referencia=" + query.referencia(), Instant.now());
    }

    @Transactional(readOnly = true)
    public DjeNotificationAuditView notificationAuditView(Long djeId) {
        DjeNotificacaoSnapshot snapshot = notificacaoSnapshot(djeId);
        return new DjeNotificationAuditView(String.valueOf(djeId), snapshot.partesNotificadas() ? "NOTIFICADO" : "PENDENTE", Instant.now());
    }

    @Transactional(readOnly = true)
    public DjeBudgetView budgetView(String tribunalCodigo) {
        DjeTribunalPublicationResult result = tribunalPublication(new DjeTribunalPublicationQuery(tribunalCodigo, null, null));
        return new DjeBudgetView(tribunalCodigo, result.falhas() > 0 ? "PRESSAO" : "OK", "total=" + result.total() + " falhas=" + result.falhas());
    }

    @Transactional(readOnly = true)
    public DjeConteudoHealthView conteudoHealthView(Long djeId) {
        DjeConteudoHashView view = conteudoHashView(djeId);
        return new DjeConteudoHealthView(String.valueOf(djeId), view.conteudoHash() == null ? "SEM_HASH" : "HASH_OK", view.tipoAto());
    }

    @Transactional(readOnly = true)
    public DjeDeadlineHealthView deadlineHealthView(Long djeId) {
        DjePrazoProjection projection = prazoProjection(djeId);
        return new DjeDeadlineHealthView(String.valueOf(djeId), projection.prazoComecaEm() == null ? "SEM_PRAZO" : "PRAZO_OK", String.valueOf(projection.prazoComecaEm()));
    }

    @Transactional(readOnly = true)
    public DjePublicationWindowHealthView publicationWindowHealthView(String tribunalCodigo) {
        DjeTribunalPublicationResult result = tribunalPublication(new DjeTribunalPublicationQuery(tribunalCodigo, null, null));
        return new DjePublicationWindowHealthView(tribunalCodigo, result.total() > 0 ? "ATIVO" : "VAZIO", "enviadas=" + result.enviadas());
    }

    @Transactional(readOnly = true)
    public DjeTribunalEditionResult tribunalEdition(DjeTribunalEditionQuery query) {
        Objects.requireNonNull(query);
        return new DjeTribunalEditionResult(query.referencia(), djeRepository.countByEdicaoDje(query.referencia()) > 0 ? "EDICAO_ATIVA" : "EDICAO_AUSENTE");
    }

    @Transactional(readOnly = true)
    public DjeDispatchHealthResult dispatchHealth(DjeDispatchHealthQuery query) {
        Objects.requireNonNull(query);
        long pendentes = djeRepository.countByStatus("PENDENTE_ENVIO") + djeRepository.countByStatus("FALHA");
        return new DjeDispatchHealthResult(query.referencia(), pendentes > 0 ? "BACKLOG" : "ESTAVEL");
    }

    @Transactional(readOnly = true)
    public DjeDispatchWindowView dispatchWindow(String tribunalCodigo, int limit) {
        int size = Math.min(Math.max(1, limit), djeRepository.findTop100ByTribunalCodigoIgnoreCaseOrderByCreatedAtDesc(tribunalCodigo).size());
        return new DjeDispatchWindowView(LocalDate.now(), LocalDate.now(), size);
    }

    @Transactional(readOnly = true)
    public DjeEnvelopeView envelopeView(Long djeId) {
        DjeStatusSnapshot status = statusSnapshot(djeId);
        return new DjeEnvelopeView("DJE_ENVELOPE", status.status(), "processo=" + status.processoId(), Instant.now(), djeId);
    }

    @Transactional(readOnly = true)
    public DjeSignalView signalView(Long djeId) {
        DjeStatusSnapshot status = statusSnapshot(djeId);
        String detalhe = status.publicadoEm() != null ? "PUBLICADO" : "EM_FLUXO";
        return new DjeSignalView("DJE_SIGNAL", status.status(), detalhe, Instant.now(), djeId);
    }

    @Transactional(readOnly = true)
    public DjeOwnerView ownerView(Long djeId) {
        DjeStatusSnapshot status = statusSnapshot(djeId);
        return new DjeOwnerView("DJE_OWNER", status.status(), "processo=" + status.processoId(), Instant.now(), djeId);
    }

    @Transactional(readOnly = true)
    public DjeWindowView windowView(Long djeId) {
        DjePrazoProjection projection = prazoProjection(djeId);
        return new DjeWindowView("DJE_WINDOW", projection.prazoComecaEm() == null ? "SEM_PRAZO" : "PRAZO_OK", String.valueOf(projection.prazoComecaEm()), Instant.now(), djeId);
    }

    @Transactional(readOnly = true)
    public DjeAuditEntryView auditEntryView(Long djeId) {
        DjeStatusSnapshot status = statusSnapshot(djeId);
        return new DjeAuditEntryView("DJE_AUDIT", status.status(), "processo=" + status.processoId(), Instant.now(), djeId);
    }

    private DjePublicacao load(Long djeId) {
        return djeRepository.findById(djeId)
                .orElseThrow(() -> new IllegalArgumentException("DJe não encontrado: " + djeId));
    }

    private int safeInt(long value) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, Math.max(0L, value)));
    }
}
