package com.tcc.pjb.backend.service.oficial_justica;

import com.tcc.pjb.backend.model.entity.enums.DiligenciaEncerramentoTipo;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorCheckpointEvento;
import com.tcc.pjb.backend.model.entity.intelligence.DiligenciaOperadorEncerramento;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorCheckpointEventoRepository;
import com.tcc.pjb.backend.model.repository.DiligenciaOperadorEncerramentoRepository;
import com.tcc.pjb.backend.service.dashboard.PerfilDashboardContextFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OficialJusticaExecucaoVivaService {

    private final PerfilDashboardContextFactory contextFactory;
    private final DiligenciaOperadorCheckpointEventoRepository checkpointRepository;
    private final DiligenciaOperadorEncerramentoRepository encerramentoRepository;

    public OficialJusticaExecucaoVivaService(PerfilDashboardContextFactory contextFactory,
                                             DiligenciaOperadorCheckpointEventoRepository checkpointRepository,
                                             DiligenciaOperadorEncerramentoRepository encerramentoRepository) {
        this.contextFactory = Objects.requireNonNull(contextFactory);
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository);
        this.encerramentoRepository = Objects.requireNonNull(encerramentoRepository);
    }

    @Transactional(readOnly = true)
    public ExecutionState resolve(WorkItem item,
                                  Map<String, Object> securityEnvelope) {
        if (item == null || item.getId() == null) {
            return ExecutionState.pending(null, 0, List.of("WORK_ITEM_INEXISTENTE"));
        }
        Long actorId = contextFactory.build().usuario().getId();
        String reference = String.valueOf(item.getId());
        DiligenciaOperadorCheckpointEvento checkpoint = actorId == null
                ? null
                : checkpointRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByOccurredAtDesc(
                        actorId,
                        com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                        reference
                ).orElse(null);
        DiligenciaOperadorEncerramento encerramento = actorId == null
                ? null
                : encerramentoRepository.findTopByOperatorUserIdAndCanalAndDiligenceReferenceOrderByCreatedAtDesc(
                        actorId,
                        com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                        reference
                ).orElse(null);
        int attempts = actorId == null
                ? 0
                : Math.toIntExact(checkpointRepository.countByOperatorUserIdAndCanalAndDiligenceReference(
                        actorId,
                        com.tcc.pjb.backend.model.entity.enums.TelemetriaOperacionalCanal.OFICIAL_JUSTICA,
                        reference
                ));
        boolean sendAllowed = Boolean.TRUE.equals(securityEnvelope == null ? null : securityEnvelope.get("sendIntoProcessAllowed"));
        String blockedReason = securityEnvelope == null ? null : stringValue(securityEnvelope.get("blockedReason"));
        List<String> alerts = new ArrayList<>();
        if (checkpoint != null) {
            alerts.add(checkpoint.isInsideGeofence() ? "CHEGADA_VALIDADA_GEOFENCE" : "CHEGADA_FORA_GEOFENCE");
        }
        if (encerramento != null && encerramento.getOutcome() != null) {
            alerts.add("ENCERRAMENTO_" + encerramento.getOutcome().name());
        }
        if (isPositive(encerramento) || item.getStatus() == WorkItemStatus.CONCLUIDO) {
            if (!sendAllowed && blockedReason != null && !blockedReason.isBlank()) {
                alerts.add(blockedReason);
            }
            return new ExecutionState(
                    "CONCLUIDA",
                    "Concluída",
                    "CONCLUIDAS",
                    "VERDE",
                    "VERDE",
                    lastEventAt(checkpoint, encerramento, item.getUpdatedAt()),
                    attempts,
                    null,
                    "Cumprimento efetivo registrado na trilha operacional do dia.",
                    List.copyOf(alerts)
            );
        }
        if (isFrustrated(encerramento)) {
            alerts.add("RETORNO_RECOMENDADO");
            return new ExecutionState(
                    "AGUARDANDO_RETORNO",
                    "Aguardando retorno",
                    "AGUARDANDO_RETORNO",
                    "LARANJA",
                    itemOverdue(item) ? "VERMELHO" : "LARANJA",
                    lastEventAt(checkpoint, encerramento, item.getUpdatedAt()),
                    Math.max(attempts, 1),
                    computeReturnWindow(item, encerramento),
                    "Tentativa frustrada com retorno recomendado e replanejamento obrigatório.",
                    List.copyOf(alerts)
            );
        }
        if (isPartial(encerramento)) {
            alerts.add("DILIGENCIA_PARCIAL_EM_CURSO");
            return new ExecutionState(
                    itemOverdue(item) ? "ATRASADA" : "EM_DILIGENCIA",
                    itemOverdue(item) ? "Atrasada" : "Em diligência",
                    itemOverdue(item) ? "ATRASADAS" : "CUMPRIR_HOJE",
                    itemOverdue(item) ? "VERMELHO" : "AZUL",
                    itemOverdue(item) ? "VERMELHO" : "AZUL",
                    lastEventAt(checkpoint, encerramento, item.getUpdatedAt()),
                    Math.max(attempts, 1),
                    computeReturnWindow(item, encerramento),
                    "Execução parcial do dia registrada; ainda há providências pendentes no mesmo mandado.",
                    List.copyOf(alerts)
            );
        }
        if (checkpoint != null) {
            alerts.add("MOVIMENTO_DE_CAMPO_REGISTRADO");
            return new ExecutionState(
                    itemOverdue(item) ? "ATRASADA" : "EM_DILIGENCIA",
                    itemOverdue(item) ? "Atrasada" : "Em diligência",
                    itemOverdue(item) ? "ATRASADAS" : "CUMPRIR_HOJE",
                    itemOverdue(item) ? "VERMELHO" : "AZUL",
                    checkpoint.isInsideGeofence() ? "AZUL" : itemOverdue(item) ? "VERMELHO" : "AMARELO",
                    lastEventAt(checkpoint, encerramento, item.getUpdatedAt()),
                    Math.max(attempts, 1),
                    checkpoint.isInsideGeofence() ? null : computeCheckpointReturnWindow(item, checkpoint),
                    checkpoint.isInsideGeofence()
                            ? "Chegada operacional validada; a agenda do dia passa a acompanhar execução em campo."
                            : "Tentativa de chegada registrada fora da geofence; revisar endereço e janela de retorno.",
                    List.copyOf(alerts)
            );
        }
        if (itemOverdue(item)) {
            alerts.add("PRAZO_FATAL_EXPIRADO");
            return new ExecutionState(
                    "ATRASADA",
                    "Atrasada",
                    "ATRASADAS",
                    "VERMELHO",
                    "VERMELHO",
                    item.getUpdatedAt(),
                    attempts,
                    computeDefaultReturnWindow(item),
                    "Mandado vencido sem confirmação de campo; replanejamento imediato necessário.",
                    List.copyOf(alerts)
            );
        }
        return ExecutionState.pending(item.getUpdatedAt(), attempts, alerts);
    }

    public record ExecutionState(
            String statusOperacional,
            String statusLabel,
            String folderCode,
            String colorToken,
            String andamentoColorToken,
            Instant lastEventAt,
            int attempts,
            Instant returnWindow,
            String movementSummary,
            List<String> alerts
    ) {
        public ExecutionState {
            alerts = alerts == null ? List.of() : List.copyOf(alerts);
        }

        public static ExecutionState pending(Instant lastEventAt,
                                             int attempts,
                                             List<String> alerts) {
            return new ExecutionState(
                    "PENDENTE",
                    "Pendente",
                    "CUMPRIR_HOJE",
                    "AMARELO",
                    "AMARELO",
                    lastEventAt,
                    attempts,
                    null,
                    "Aguardando primeira tentativa operacional do dia.",
                    alerts
            );
        }

        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            putIfNotNull(out, "statusOperacional", statusOperacional);
            putIfNotNull(out, "statusLabel", statusLabel);
            putIfNotNull(out, "folderCode", folderCode);
            putIfNotNull(out, "colorToken", colorToken);
            putIfNotNull(out, "andamentoColorToken", andamentoColorToken);
            putIfNotNull(out, "lastEventAt", lastEventAt);
            out.put("attempts", attempts);
            putIfNotNull(out, "returnWindow", returnWindow);
            putIfNotNull(out, "movementSummary", movementSummary);
            if (!alerts.isEmpty()) {
                out.put("alerts", alerts);
            }
            return Collections.unmodifiableMap(out);
        }
    }

    private boolean isPositive(DiligenciaOperadorEncerramento encerramento) {
        return encerramento != null && encerramento.getOutcome() == DiligenciaEncerramentoTipo.CUMPRIMENTO_POSITIVO;
    }

    private boolean isFrustrated(DiligenciaOperadorEncerramento encerramento) {
        return encerramento != null && encerramento.getOutcome() == DiligenciaEncerramentoTipo.CUMPRIMENTO_FRUSTRADO;
    }

    private boolean isPartial(DiligenciaOperadorEncerramento encerramento) {
        return encerramento != null && encerramento.getOutcome() == DiligenciaEncerramentoTipo.DILIGENCIA_PARCIAL;
    }

    private boolean itemOverdue(WorkItem item) {
        return item != null && item.getDueAt() != null && !item.getDueAt().isAfter(Instant.now());
    }

    private Instant computeReturnWindow(WorkItem item,
                                        DiligenciaOperadorEncerramento encerramento) {
        if (encerramento != null && encerramento.getCreatedAt() != null) {
            return encerramento.getCreatedAt().plus(1, ChronoUnit.DAYS);
        }
        return computeDefaultReturnWindow(item);
    }

    private Instant computeCheckpointReturnWindow(WorkItem item,
                                                  DiligenciaOperadorCheckpointEvento checkpoint) {
        if (checkpoint != null && checkpoint.getOccurredAt() != null) {
            return checkpoint.getOccurredAt().plus(4, ChronoUnit.HOURS);
        }
        return computeDefaultReturnWindow(item);
    }

    private Instant computeDefaultReturnWindow(WorkItem item) {
        if (item != null && item.getDueAt() != null) {
            return item.getDueAt().plus(8, ChronoUnit.HOURS);
        }
        return Instant.now().plus(8, ChronoUnit.HOURS);
    }

    private Instant lastEventAt(DiligenciaOperadorCheckpointEvento checkpoint,
                                DiligenciaOperadorEncerramento encerramento,
                                Instant fallback) {
        Instant checkpointAt = checkpoint != null ? checkpoint.getOccurredAt() : null;
        Instant closureAt = encerramento != null ? encerramento.getCreatedAt() : null;
        if (checkpointAt != null && closureAt != null) {
            return checkpointAt.isAfter(closureAt) ? checkpointAt : closureAt;
        }
        if (checkpointAt != null) {
            return checkpointAt;
        }
        if (closureAt != null) {
            return closureAt;
        }
        return fallback;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static void putIfNotNull(Map<String, Object> target,
                                     String key,
                                     Object value) {
        if (target != null && key != null && value != null) {
            target.put(key, value);
        }
    }
}
