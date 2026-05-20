package com.tcc.pjb.backend.service.recursal.mesh;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.inject.Inject;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class RecursalMeshPartyNotificationService {

    private static final String EVENT_TYPE = "pjb.recursal.party.notification.requested";

    private final NotificationService notificationService;
    private final OutboxPublisher outboxPublisher;
    private final RecursalMeshSlaService slaService;
    private final ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider;
    private final ObjectProvider<RecursalMeshRetryExecutor> retryExecutorProvider;

    RecursalMeshPartyNotificationService(NotificationService notificationService,
                                         OutboxPublisher outboxPublisher,
                                         RecursalMeshSlaService slaService) {
        this(notificationService, outboxPublisher, slaService, null, null);
    }

    RecursalMeshPartyNotificationService(NotificationService notificationService,
                                         OutboxPublisher outboxPublisher,
                                         RecursalMeshSlaService slaService,
                                         ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider) {
        this(notificationService, outboxPublisher, slaService, telemetryProvider, null);
    }

    @Inject
    @Autowired
    public RecursalMeshPartyNotificationService(NotificationService notificationService,
                                                OutboxPublisher outboxPublisher,
                                                RecursalMeshSlaService slaService,
                                                ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider,
                                                ObjectProvider<RecursalMeshRetryExecutor> retryExecutorProvider) {
        this.notificationService = notificationService;
        this.outboxPublisher = outboxPublisher;
        this.slaService = slaService;
        this.telemetryProvider = telemetryProvider;
        this.retryExecutorProvider = retryExecutorProvider;
    }

    public void onTransition(RecursalAggregateState aggregate,
                             RecursalTransitionEvent event,
                             RecursalStateSnapshot previous,
                             RecursalStateSnapshot current,
                             String actor,
                             String commandId) {
        if (aggregate == null || aggregate.getProcesso() == null || current == null || !shouldNotify(event, previous, current)) {
            return;
        }
        Processo processo = aggregate.getProcesso();
        String title = titleFor(current.state(), event);
        String message = messageFor(aggregate, event, current, actor);
        notifySafely("lawyers", () -> notificationService.notifyLawyers(processo, title, message));
        if (processo.getUsuario() != null && !processo.getUsuario().isAdvogado()) {
            notifySafely("user", () -> notificationService.notifyUser(processo.getUsuario(), processo, title, message, null));
        }
        notifySafely("outbox", () -> enqueue(aggregate, event, previous, current, actor, commandId, title, message));
    }

    private void notifySafely(String channel, Runnable action) {
        try {
            executeWithRetry(channel, action);
            telemetry(channel, true);
        } catch (RuntimeException ex) {
            telemetry(channel, false);
        }
    }

    private void executeWithRetry(String channel, Runnable action) {
        RecursalMeshRetryExecutor retryExecutor = retryExecutorProvider == null ? null : retryExecutorProvider.getIfAvailable();
        if (retryExecutor == null) {
            action.run();
            return;
        }
        retryExecutor.executeVoid("notification", channel, action);
    }

    private void telemetry(String channel, boolean success) {
        if (telemetryProvider == null) {
            return;
        }
        RecursalMeshOperationalTelemetryService telemetryService = telemetryProvider.getIfAvailable();
        if (telemetryService != null) {
            telemetryService.recordNotificationDelivery(channel, success);
        }
    }

    private boolean shouldNotify(RecursalTransitionEvent event, RecursalStateSnapshot previous, RecursalStateSnapshot current) {
        if (current == null || current.state() == null) {
            return false;
        }
        if (previous != null && previous.state() == current.state()
                && event != RecursalTransitionEvent.ADMITIR
                && event != RecursalTransitionEvent.PEDIR_PAUTA_SUSTENTACAO) {
            return false;
        }
        return switch (current.state()) {
            case REMESSA_EM_CURSO, AUTOS_EM_TRANSITO, ADMISSIBILIDADE_DESTINO, JULGAMENTO_COLEGIADO,
                    PAUTA_SUSTENTACAO_DESIGNADA, SUSTENTACAO_REALIZADA, PRECEDENTE_APLICADO, CASO_DISTINGUIDO,
                    PROVIDO, PARCIALMENTE_PROVIDO, IMPROVIDO, NAO_CONHECIDO, INADMITIDO,
                    AGUARDANDO_REQUISICAO_PAGAMENTO_PUBLICO, AGUARDANDO_PRECATORIO, RPV_EXPEDIDA, PRECATORIO_EXPEDIDO,
                    PAGAMENTO_PUBLICO_LIBERADO, BAIXADO, TRANSITADO_EM_JULGADO -> true;
            default -> false;
        };
    }

    private String titleFor(RecursalLifecycleState state, RecursalTransitionEvent event) {
        return switch (state) {
            case REMESSA_EM_CURSO, AUTOS_EM_TRANSITO -> "Recurso admitido e em remessa";
            case ADMISSIBILIDADE_DESTINO -> "Recurso em admissibilidade no tribunal de destino";
            case JULGAMENTO_COLEGIADO -> event == RecursalTransitionEvent.AFETAR_ORGAO_JULGADOR
                    ? "Recurso distribuído para julgamento colegiado"
                    : "Recurso pronto para julgamento colegiado";
            case PAUTA_SUSTENTACAO_DESIGNADA -> "Sustentação oral pautada";
            case SUSTENTACAO_REALIZADA -> "Sustentação oral realizada";
            case PRECEDENTE_APLICADO -> "Precedente aplicado ao recurso";
            case CASO_DISTINGUIDO -> "Caso distinguido do precedente";
            case PROVIDO -> "Recurso provido";
            case PARCIALMENTE_PROVIDO -> "Recurso parcialmente provido";
            case IMPROVIDO -> "Recurso improvido";
            case NAO_CONHECIDO -> "Recurso não conhecido";
            case INADMITIDO -> "Recurso inadmitido";
            case AGUARDANDO_REQUISICAO_PAGAMENTO_PUBLICO -> "Providência de pagamento público pendente";
            case AGUARDANDO_PRECATORIO -> "Preparação de precatório pendente";
            case RPV_EXPEDIDA -> "RPV expedida";
            case PRECATORIO_EXPEDIDO -> "Precatório expedido";
            case PAGAMENTO_PUBLICO_LIBERADO -> "Pagamento público liberado";
            case BAIXADO -> "Recurso baixado";
            case TRANSITADO_EM_JULGADO -> "Recurso transitado em julgado";
            default -> "Atualização recursal";
        };
    }

    private String messageFor(RecursalAggregateState aggregate, RecursalTransitionEvent event, RecursalStateSnapshot current, String actor) {
        StringBuilder sb = new StringBuilder();
        sb.append(aggregate.getSpeciesName() == null ? aggregate.getSpeciesCode() : aggregate.getSpeciesName());
        sb.append(" no processo ");
        sb.append(aggregate.getNumeroProcesso() == null ? aggregate.getRecursoId() : aggregate.getNumeroProcesso());
        sb.append(" avançou para ");
        sb.append(current.state().name());
        if (aggregate.getTribunalDetalhadoAtual() != null) {
            sb.append(" em ").append(aggregate.getTribunalDetalhadoAtual().name());
        }
        if (event != null) {
            sb.append(" após o evento ").append(event.name());
        }
        if (actor != null && !actor.isBlank()) {
            sb.append(" por ").append(actor.trim());
        }
        Optional<RecursalSlaSnapshot> sla = slaService.snapshot(aggregate);
        sla.ifPresent(snapshot -> {
            sb.append(". SLA previsto: ")
                    .append(snapshot.diasUteis())
                    .append(" dias úteis, saída estimada em ")
                    .append(snapshot.dataPrevistaSaida());
            if (snapshot.vencido()) {
                sb.append(". Atenção: SLA vencido em ")
                        .append(snapshot.diasUteisExcedidos())
                        .append(" dias úteis.");
            }
        });
        return sb.toString();
    }

    private void enqueue(RecursalAggregateState aggregate,
                         RecursalTransitionEvent event,
                         RecursalStateSnapshot previous,
                         RecursalStateSnapshot current,
                         String actor,
                         String commandId,
                         String title,
                         String message) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("recursoId", aggregate.getRecursoId());
        payload.put("processoId", aggregate.getProcesso().getId());
        payload.put("numeroProcesso", aggregate.getNumeroProcesso());
        payload.put("speciesCode", aggregate.getSpeciesCode());
        payload.put("speciesName", aggregate.getSpeciesName());
        payload.put("previousState", previous == null || previous.state() == null ? null : previous.state().name());
        payload.put("currentState", current.state().name());
        payload.put("event", event == null ? null : event.name());
        payload.put("title", title);
        payload.put("message", message);
        payload.put("actor", actor);
        Optional<RecursalSlaSnapshot> sla = slaService.snapshot(aggregate);
        sla.ifPresent(snapshot -> {
            payload.put("slaSeveridade", snapshot.severidade());
            payload.put("slaPrevistaSaida", snapshot.dataPrevistaSaida().toString());
            payload.put("slaVencido", snapshot.vencido());
            payload.put("slaDiasUteisExcedidos", snapshot.diasUteisExcedidos());
        });
        outboxPublisher.enqueue(
                "recursal.party.notification." + aggregate.getRecursoId(),
                EVENT_TYPE,
                payload,
                Map.of("module", "recursal-mesh", "channel", "notification"),
                commandId != null && !commandId.isBlank()
                        ? "recursal-party-notification:" + commandId
                        : "recursal-party-notification:" + aggregate.getRecursoId() + ':' + current.state().name(),
                "RECURSAL_MESH",
                aggregate.getRecursoId()
        );
    }
}
