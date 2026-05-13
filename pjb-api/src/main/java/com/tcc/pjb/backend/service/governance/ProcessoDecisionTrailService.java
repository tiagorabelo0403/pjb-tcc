package com.tcc.pjb.backend.service.governance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerRepository;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTrace;
import com.tcc.pjb.backend.core.explainability.DecisionTraceRepository;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.model.dto.governance.DecisionTrailResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.pericia.CadeiaCustodiaDigitalLedgerEntry;
import com.tcc.pjb.backend.model.repository.CadeiaCustodiaDigitalLedgerEntryRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;

@Service
public class ProcessoDecisionTrailService {

    private final ProcessoRepository processoRepository;
    private final AuditLedgerRepository auditLedgerRepository;
    private final AuditLedgerService auditLedgerService;
    private final DecisionTraceRepository decisionTraceRepository;
    private final DecisionTraceService decisionTraceService;
    private final CadeiaCustodiaDigitalLedgerEntryRepository cadeiaCustodiaRepository;

    public ProcessoDecisionTrailService(ProcessoRepository processoRepository,
                                        AuditLedgerRepository auditLedgerRepository,
                                        AuditLedgerService auditLedgerService,
                                        DecisionTraceRepository decisionTraceRepository,
                                        DecisionTraceService decisionTraceService,
                                        CadeiaCustodiaDigitalLedgerEntryRepository cadeiaCustodiaRepository) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.auditLedgerRepository = Objects.requireNonNull(auditLedgerRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.decisionTraceRepository = Objects.requireNonNull(decisionTraceRepository);
        this.decisionTraceService = Objects.requireNonNull(decisionTraceService);
        this.cadeiaCustodiaRepository = Objects.requireNonNull(cadeiaCustodiaRepository);
    }

    public DecisionTrailResponse timeline(Long processoId) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        String resourceId = String.valueOf(processoId);
        String numero = processo.getNumeroProcesso();
        List<DecisionTrailResponse.DecisionTrailEntryView> eventos = new ArrayList<>();
        auditLedgerRepository.search(null, null, "PROCESSO", resourceId, PageRequest.of(0, 100)).getContent().forEach(entry ->
                eventos.add(new DecisionTrailResponse.DecisionTrailEntryView(
                        "AUDIT_LEDGER",
                        entry.getAction(),
                        entry.getCreatedAt() == null ? null : entry.getCreatedAt().toString(),
                        resolveActor(entry.getActorUserId(), entry.getActorKey()),
                        entry.getJustificativa(),
                        entry.getEntryHash(),
                        entry.getRequestId()
                ))
        );
        if (numero != null && !numero.isBlank()) {
            auditLedgerRepository.search(null, null, "PROCESSO", numero, PageRequest.of(0, 50)).getContent().forEach(entry ->
                    eventos.add(new DecisionTrailResponse.DecisionTrailEntryView(
                            "AUDIT_LEDGER",
                            entry.getAction(),
                            entry.getCreatedAt() == null ? null : entry.getCreatedAt().toString(),
                            resolveActor(entry.getActorUserId(), entry.getActorKey()),
                            entry.getJustificativa(),
                            entry.getEntryHash(),
                            entry.getRequestId()
                    ))
            );
        }
        decisionTraceRepository.findTop200BySubjectTypeAndSubjectIdOrderByCreatedAtDesc("PROCESSO", resourceId).forEach(trace ->
                eventos.add(decisionTraceEntry(trace))
        );
        if (numero != null && !numero.isBlank()) {
            decisionTraceRepository.findTop200BySubjectTypeAndSubjectIdOrderByCreatedAtDesc("PROCESSO", numero).forEach(trace ->
                    eventos.add(decisionTraceEntry(trace))
            );
        }
        cadeiaCustodiaRepository.findTop200ByChaveCustodiaOrderBySealedAtDesc("proc:" + processoId).forEach(entry ->
                eventos.add(new DecisionTrailResponse.DecisionTrailEntryView(
                        "CADEIA_CUSTODIA",
                        "DOCUMENT_TRUST_SEAL",
                        entry.getSealedAt() == null ? null : entry.getSealedAt().toString(),
                        entry.getSealedByPerfil(),
                        entry.getEvidenceNome(),
                        entry.getEntryHash(),
                        entry.getRequestId()
                ))
        );
        List<DecisionTrailResponse.DecisionTrailEntryView> deduplicado = deduplicate(eventos).stream()
                .sorted(Comparator.comparing(DecisionTrailResponse.DecisionTrailEntryView::ocorridoEm, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        return new DecisionTrailResponse(
                processoId,
                processo.getNumeroProcesso(),
                deduplicado.size(),
                deduplicado
        );
    }

    public DecisionTrailResponse.DecisionTrailEntryView registrarSnapshot(Long processoId,
                                                                          String decisionType,
                                                                          BigDecimal confidence,
                                                                          String reasonsJson,
                                                                          String citationsJson,
                                                                          String inputDigest,
                                                                          String outputDigest,
                                                                          String modelVersion,
                                                                          String metadataJson) {
        Processo processo = processoRepository.findById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        DecisionTrace trace = decisionTraceService.record(
                decisionType,
                "PROCESSO",
                String.valueOf(processoId),
                confidence,
                reasonsJson,
                citationsJson,
                inputDigest,
                outputDigest,
                modelVersion,
                metadataJson
        );
        auditLedgerService.appendSafely(
                "DECISION_SNAPSHOT_RECORDED",
                "PROCESSO",
                String.valueOf(processoId),
                trace.getOutputDigest() == null ? trace.getInputDigest() : trace.getOutputDigest()
        );
        return decisionTraceEntry(trace);
    }

    private DecisionTrailResponse.DecisionTrailEntryView decisionTraceEntry(DecisionTrace trace) {
        return new DecisionTrailResponse.DecisionTrailEntryView(
                "DECISION_TRACE",
                trace.getDecisionType(),
                trace.getCreatedAt() == null ? null : trace.getCreatedAt().toString(),
                resolveActor(trace.getActorUserId(), trace.getActorKey()),
                trace.getReasonsJson(),
                trace.getOutputDigest() == null ? trace.getInputDigest() : trace.getOutputDigest(),
                trace.getRequestId()
        );
    }

    private String resolveActor(Long actorUserId, String actorKey) {
        if (actorUserId != null) {
            return "uid:" + actorUserId;
        }
        return actorKey;
    }

    private List<DecisionTrailResponse.DecisionTrailEntryView> deduplicate(List<DecisionTrailResponse.DecisionTrailEntryView> eventos) {
        Map<String, DecisionTrailResponse.DecisionTrailEntryView> map = new LinkedHashMap<>();
        for (DecisionTrailResponse.DecisionTrailEntryView evento : eventos) {
            String key = String.join("|",
                    safe(evento.origem()),
                    safe(evento.tipo()),
                    safe(evento.hash()),
                    safe(evento.correlacao()),
                    safe(evento.ocorridoEm()));
            map.putIfAbsent(key, evento);
        }
        return new ArrayList<>(map.values());
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
