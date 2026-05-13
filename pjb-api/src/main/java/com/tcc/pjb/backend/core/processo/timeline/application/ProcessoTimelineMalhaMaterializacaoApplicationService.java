package com.tcc.pjb.backend.core.processo.timeline.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineMalhaAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineMalhaEvento;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineMalhaMaterializacaoAggregate;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessoTimelineMalhaMaterializacaoApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoTimelineMalhaApplicationService processoTimelineMalhaApplicationService;
    private final OutboxPublisher outboxPublisher;
    private final DecisionTraceService decisionTraceService;
    private final AuditLedgerService auditLedgerService;

    public ProcessoTimelineMalhaMaterializacaoApplicationService(ProcessoRepository processoRepository,
                                                                 ProcessoTimelineMalhaApplicationService processoTimelineMalhaApplicationService,
                                                                 OutboxPublisher outboxPublisher,
                                                                 ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                                 ObjectProvider<AuditLedgerService> auditLedgerServiceProvider) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoTimelineMalhaApplicationService = Objects.requireNonNull(processoTimelineMalhaApplicationService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.auditLedgerService = auditLedgerServiceProvider.getIfAvailable();
    }

    @Transactional
    public ProcessoTimelineMalhaMaterializacaoAggregate materializar(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoTimelineMalhaAggregate timeline = processoTimelineMalhaApplicationService.detalhar(processoId);
        processo.setDataUltimaMovimentacao(LocalDateTime.now());
        processoRepository.save(processo);

        ArrayList<String> canais = new ArrayList<>();
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(timeline.fundamentos());
        canais.add("OUTBOX_PROCESSUAL");
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("processoId", timeline.processoId());
        payload.put("numeroProcesso", timeline.numeroProcesso());
        payload.put("totalEventosMalha", timeline.totalEventosMalha());
        payload.put("totalBloqueiosMalha", timeline.totalBloqueiosMalha());
        payload.put("proximaAcaoOperacional", timeline.proximaAcaoOperacional());
        payload.put("hotspots", timeline.hotspots());
        payload.put("eventos", timeline.eventos().stream().map(this::toPayload).toList());
        payload.put("fundamentos", timeline.fundamentos());

        outboxPublisher.enqueue(
                "processo.timeline.malha." + processoId,
                "PROCESSO_TIMELINE_MALHA_MATERIALIZADA",
                payload,
                Map.of(
                        "boundedContext", "processo.timeline",
                        "eventVersion", "1",
                        "kind", "MATERIALIZACAO"
                ),
                "processo-timeline-malha:" + processoId + ':' + timeline.totalEventosMalha() + ':' + timeline.totalBloqueiosMalha(),
                "PROCESSO",
                String.valueOf(processoId)
        );

        String inboxOperacional = resolveInboxOperacional(processo);
        outboxPublisher.enqueueSecretariatLive(
                inboxOperacional,
                "PROCESSO_TIMELINE_MALHA",
                Instant.now(),
                Map.of(
                        "processoId", timeline.processoId(),
                        "numeroProcesso", timeline.numeroProcesso(),
                        "bloqueios", timeline.totalBloqueiosMalha(),
                        "acao", timeline.proximaAcaoOperacional(),
                        "hotspots", timeline.hotspots()
                )
        );
        canais.add("SECRETARIAT_INBOX");

        outboxPublisher.enqueue(
                "processo.ui.history." + processoId,
                OutboxPublisher.EVT_UI_HISTORY_LIVE,
                Map.of(
                        "processoId", timeline.processoId(),
                        "numeroProcesso", timeline.numeroProcesso(),
                        "acao", timeline.proximaAcaoOperacional(),
                        "totalEventos", timeline.totalEventosMalha(),
                        "totalBloqueios", timeline.totalBloqueiosMalha()
                ),
                Map.of(
                        "boundedContext", "processo.timeline",
                        "eventVersion", "1",
                        "view", "history-live"
                ),
                "processo-ui-history-malha:" + processoId + ':' + timeline.totalEventosMalha(),
                "PROCESSO",
                String.valueOf(processoId)
        );
        canais.add("UI_HISTORY_LIVE");

        if (decisionTraceService != null) {
            decisionTraceService.record(
                    "PROCESSO_TIMELINE_MALHA_MATERIALIZADA",
                    "PROCESSO",
                    String.valueOf(processoId),
                    BigDecimal.valueOf(confianca(timeline)),
                    jsonList(timeline.fundamentos()),
                    jsonList(timeline.eventos().stream().map(item -> item.codigo() + ':' + item.severidade()).toList()),
                    timeline.numeroProcesso(),
                    timeline.proximaAcaoOperacional() + ':' + timeline.totalBloqueiosMalha(),
                    "PJB_TIMELINE_MALHA_MATERIALIZACAO_V1",
                    jsonList(canais)
            );
        }
        if (auditLedgerService != null) {
            auditLedgerService.appendSafely(
                    "PROCESSO_TIMELINE_MALHA_MATERIALIZADA",
                    "PROCESSO",
                    String.valueOf(processoId),
                    timeline.proximaAcaoOperacional() + ':' + timeline.totalEventosMalha() + ':' + timeline.totalBloqueiosMalha(),
                    "Timeline viva da malha nacional materializada operacionalmente"
            );
        }

        return new ProcessoTimelineMalhaMaterializacaoAggregate(
                processoId,
                timeline.numeroProcesso(),
                timeline.eventos().size(),
                true,
                true,
                true,
                List.copyOf(canais),
                List.copyOf(fundamentos.stream().limit(60).toList()),
                Instant.now()
        );
    }

    private Map<String, Object> toPayload(ProcessoTimelineMalhaEvento evento) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("codigo", evento.codigo());
        out.put("eixo", evento.eixo());
        out.put("severidade", evento.severidade());
        out.put("instante", evento.instante());
        out.put("bloqueante", evento.bloqueante());
        out.put("titulo", evento.titulo());
        out.put("acao", evento.acao());
        out.put("navigationPath", evento.navigationPath());
        out.put("fundamentos", evento.fundamentos());
        return out;
    }

    private String resolveInboxOperacional(Processo processo) {
        LinkedHashSet<String> partes = new LinkedHashSet<>();
        partes.add("PROCESSO_MALHA_TIMELINE");
        if (processo.getUf() != null && !processo.getUf().isBlank()) {
            partes.add(processo.getUf().trim().toUpperCase());
        }
        if (processo.getComarca() != null && !processo.getComarca().isBlank()) {
            partes.add(normalize(processo.getComarca()));
        }
        return String.join(":", partes);
    }

    private double confianca(ProcessoTimelineMalhaAggregate timeline) {
        if (timeline.totalEventosMalha() == 0) {
            return 0.55d;
        }
        double base = 0.62d + Math.min(0.28d, timeline.totalEventosMalha() * 0.015d);
        if (timeline.totalBloqueiosMalha() > 0) {
            base += Math.min(0.08d, timeline.totalBloqueiosMalha() * 0.02d);
        }
        return Math.min(0.98d, base);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
    }

    private String jsonList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "[]";
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(text -> !text.isBlank())
                .map(text -> '"' + text.replace("\\", "\\\\").replace("\"", "\\\"") + '"')
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }
}
