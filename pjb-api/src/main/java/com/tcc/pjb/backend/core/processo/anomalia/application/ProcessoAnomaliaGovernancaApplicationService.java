package com.tcc.pjb.backend.core.processo.anomalia.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaGovernancaAggregate;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.anomalia.domain.ProcessoAnomaliaMalhaItem;
import com.tcc.pjb.backend.core.security.device.SecurityAlertService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.security.SecurityAlert;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class ProcessoAnomaliaGovernancaApplicationService {

    private final ProcessoRepository processoRepository;
    private final ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService;
    private final SecurityAlertService securityAlertService;
    private final OutboxPublisher outboxPublisher;
    private final DecisionTraceService decisionTraceService;
    private final AuditLedgerService auditLedgerService;

    public ProcessoAnomaliaGovernancaApplicationService(ProcessoRepository processoRepository,
                                                        ProcessoAnomaliaMalhaApplicationService processoAnomaliaMalhaApplicationService,
                                                        SecurityAlertService securityAlertService,
                                                        OutboxPublisher outboxPublisher,
                                                        ObjectProvider<DecisionTraceService> decisionTraceServiceProvider,
                                                        ObjectProvider<AuditLedgerService> auditLedgerServiceProvider) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.processoAnomaliaMalhaApplicationService = Objects.requireNonNull(processoAnomaliaMalhaApplicationService);
        this.securityAlertService = Objects.requireNonNull(securityAlertService);
        this.outboxPublisher = Objects.requireNonNull(outboxPublisher);
        this.decisionTraceService = decisionTraceServiceProvider.getIfAvailable();
        this.auditLedgerService = auditLedgerServiceProvider.getIfAvailable();
    }

    @PjbTransactionalBudget(operation = "processo.anomalia-governanca.escalar-se-necessario", maxMillis = 3000)
    @Transactional
    public ProcessoAnomaliaGovernancaAggregate escalarSeNecessario(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        ProcessoAnomaliaMalhaAggregate anomalia = processoAnomaliaMalhaApplicationService.detalhar(processoId);
        boolean exigePersistencia = anomalia.exigeEscalonamento() || anomalia.scoreGlobal() >= 75;
        SecurityAlert securityAlert = null;
        LinkedHashSet<String> fundamentos = new LinkedHashSet<>(anomalia.fundamentos());

        if (exigePersistencia) {
            securityAlert = securityAlertService.create(
                    processo.getUsuario(),
                    "PROCESSO_MALHA_" + anomalia.nivelGlobal(),
                    titulo(anomalia),
                    detalhe(anomalia),
                    null,
                    anomalia.scoreGlobal()
            );
        }

        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("processoId", processoId);
        payload.put("numeroProcesso", anomalia.numeroProcesso());
        payload.put("nivelGlobal", anomalia.nivelGlobal());
        payload.put("scoreGlobal", anomalia.scoreGlobal());
        payload.put("exigeEscalonamento", anomalia.exigeEscalonamento());
        payload.put("securityAlertId", securityAlert != null ? securityAlert.getId() : null);
        payload.put("itens", anomalia.itens().stream().map(this::toPayload).toList());
        payload.put("fundamentos", anomalia.fundamentos());

        outboxPublisher.enqueue(
                "processo.governanca.anomalia." + processoId,
                "PROCESSO_GOVERNANCA_ANOMALIA_AVALIADA",
                payload,
                Map.of(
                        "boundedContext", "processo.anomalia",
                        "eventVersion", "1",
                        "nivelGlobal", anomalia.nivelGlobal()
                ),
                "processo-governanca-anomalia:" + processoId + ':' + anomalia.nivelGlobal() + ':' + anomalia.scoreGlobal(),
                "PROCESSO",
                String.valueOf(processoId)
        );

        if (exigePersistencia) {
            LinkedHashMap<String, Object> livePayload = new LinkedHashMap<>();
            livePayload.put("processoId", processoId);
            livePayload.put("numeroProcesso", anomalia.numeroProcesso());
            livePayload.put("nivelGlobal", anomalia.nivelGlobal());
            livePayload.put("scoreGlobal", anomalia.scoreGlobal());
            livePayload.put("securityAlertId", securityAlert != null ? securityAlert.getId() : null);
            outboxPublisher.enqueueSecretariatLive(
                    resolveInboxGovernanca(processo),
                    "PROCESSO_ANOMALIA_GOVERNANCA",
                    Instant.now(),
                    livePayload
            );
        }

        if (decisionTraceService != null) {
            decisionTraceService.record(
                    "PROCESSO_GOVERNANCA_ANOMALIA",
                    "PROCESSO",
                    String.valueOf(processoId),
                    BigDecimal.valueOf(Math.max(0.55d, anomalia.scoreGlobal() / 100d)),
                    jsonList(anomalia.fundamentos()),
                    jsonList(anomalia.itens().stream().map(item -> item.codigo() + ':' + item.nivel()).toList()),
                    anomalia.numeroProcesso(),
                    anomalia.nivelGlobal() + ':' + exigePersistencia,
                    "PJB_GOVERNANCA_ANOMALIA_V1",
                    jsonList(List.of(resolveInboxGovernanca(processo), String.valueOf(securityAlert != null ? securityAlert.getId() : null)))
            );
        }
        if (auditLedgerService != null) {
            auditLedgerService.appendSafely(
                    "PROCESSO_GOVERNANCA_ANOMALIA_AVALIADA",
                    "PROCESSO",
                    String.valueOf(processoId),
                    anomalia.nivelGlobal() + ':' + anomalia.scoreGlobal() + ':' + exigePersistencia,
                    "Governança processual avaliou anomalia da malha nacional"
            );
        }

        return new ProcessoAnomaliaGovernancaAggregate(
                processoId,
                anomalia.numeroProcesso(),
                anomalia.nivelGlobal(),
                anomalia.scoreGlobal(),
                exigePersistencia,
                securityAlert != null ? securityAlert.getId() : null,
                resolveInboxGovernanca(processo),
                List.copyOf(fundamentos.stream().limit(60).toList()),
                Instant.now()
        );
    }

    private Map<String, Object> toPayload(ProcessoAnomaliaMalhaItem item) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("codigo", item.codigo());
        out.put("dominio", item.dominio());
        out.put("score", item.score());
        out.put("nivel", item.nivel());
        out.put("exigeEscalonamento", item.exigeEscalonamento());
        out.put("titulo", item.titulo());
        out.put("detalhe", item.detalhe());
        out.put("fundamentos", item.fundamentos());
        return out;
    }

    private String titulo(ProcessoAnomaliaMalhaAggregate anomalia) {
        if (!anomalia.itens().isEmpty()) {
            return anomalia.itens().getFirst().titulo();
        }
        return "Anomalia processual detectada pela malha nacional";
    }

    private String detalhe(ProcessoAnomaliaMalhaAggregate anomalia) {
        LinkedHashSet<String> partes = new LinkedHashSet<>();
        partes.add("Nível=" + anomalia.nivelGlobal());
        partes.add("Score=" + anomalia.scoreGlobal());
        anomalia.itens().stream().limit(4).map(ProcessoAnomaliaMalhaItem::detalhe).filter(Objects::nonNull).map(String::trim).filter(text -> !text.isBlank()).forEach(partes::add);
        return String.join(" | ", partes);
    }

    private String resolveInboxGovernanca(Processo processo) {
        LinkedHashSet<String> partes = new LinkedHashSet<>();
        partes.add("GOVERNANCA_PROCESSUAL");
        if (processo.getTribunal() != null && !processo.getTribunal().isBlank()) {
            partes.add(normalize(processo.getTribunal()));
        }
        if (processo.getUf() != null && !processo.getUf().isBlank()) {
            partes.add(normalize(processo.getUf()));
        }
        return String.join(":", partes);
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
