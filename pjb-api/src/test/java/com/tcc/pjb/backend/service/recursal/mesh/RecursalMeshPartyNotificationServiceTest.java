package com.tcc.pjb.backend.service.recursal.mesh;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalAuthority;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSlaSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunal;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTribunalDetalhado;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.service.notification.NotificationService;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@ExtendWith(MockitoExtension.class)
class RecursalMeshPartyNotificationServiceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private OutboxPublisher outboxPublisher;

    @Mock
    private RecursalMeshSlaService slaService;

    @Test
    void deveNotificarPartesEmTransicaoRelevante() {
        RecursalMeshPartyNotificationService service = new RecursalMeshPartyNotificationService(notificationService, outboxPublisher, slaService);
        RecursalAggregateState aggregate = aggregate();
        RecursalStateSnapshot previous = snapshot(RecursalLifecycleState.ADMISSIBILIDADE_DESTINO, 3);
        RecursalStateSnapshot current = snapshot(RecursalLifecycleState.PROVIDO, 4);
        when(slaService.snapshot(aggregate)).thenReturn(Optional.of(new RecursalSlaSnapshot(
                RecursalLifecycleState.PROVIDO,
                RecursalTribunal.TJ,
                10,
                true,
                "Fundamento",
                java.time.LocalDate.of(2026, 3, 10),
                java.time.LocalDate.of(2026, 3, 24),
                false,
                0,
                "MONITORAR_FATAL"
        )));

        service.onTransition(aggregate, RecursalTransitionEvent.PROVER, previous, current, "relator", "cmd-1");

        verify(notificationService).notifyLawyers(eq(aggregate.getProcesso()), eq("Recurso provido"), any());
        verify(notificationService).notifyUser(eq(aggregate.getProcesso().getUsuario()), eq(aggregate.getProcesso()), eq("Recurso provido"), any(), eq(null));
        verify(outboxPublisher).enqueue(any(), eq("pjb.recursal.party.notification.requested"), any(), any(), any(), eq("RECURSAL_MESH"), eq("resp-77"));
    }

    @Test
    void naoDeveNotificarEmEstadoNaoRelevante() {
        RecursalMeshPartyNotificationService service = new RecursalMeshPartyNotificationService(notificationService, outboxPublisher, slaService);
        RecursalAggregateState aggregate = aggregate();
        RecursalStateSnapshot previous = snapshot(RecursalLifecycleState.EM_SANEAMENTO_FORMAL, 1);
        RecursalStateSnapshot current = snapshot(RecursalLifecycleState.PREPARO_CERTIFICADO, 2);

        service.onTransition(aggregate, RecursalTransitionEvent.DISPENSAR_PREPARO, previous, current, "secretaria", "cmd-2");

        verify(notificationService, never()).notifyLawyers(any(), any(), any());
        verify(outboxPublisher, never()).enqueue(any(), any(), any(), any(), any(), any(), any());
    }

    private RecursalAggregateState aggregate() {
        Processo processo = new Processo();
        processo.setId(77L);
        processo.setNumeroProcesso("0000077-00.2026.8.06.0001");
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        Usuario usuario = new Usuario();
        usuario.setId(88L);
        usuario.setNome("Parte autora");
        processo.setUsuario(usuario);

        RecursalAggregateState aggregate = new RecursalAggregateState();
        aggregate.setRecursoId("resp-77");
        aggregate.setProcesso(processo);
        aggregate.setNumeroProcesso(processo.getNumeroProcesso());
        aggregate.setSpeciesCode("RESP");
        aggregate.setSpeciesName("Recurso Especial");
        aggregate.setCurrentState(RecursalLifecycleState.PROVIDO);
        aggregate.setTribunalAtual(RecursalTribunal.TJ);
        aggregate.setTribunalDetalhadoAtual(RecursalTribunalDetalhado.TJCE);
        return aggregate;
    }

    private RecursalStateSnapshot snapshot(RecursalLifecycleState state, int revision) {
        return new RecursalStateSnapshot(
                "resp-77",
                state,
                revision,
                RecursalTribunal.TJ,
                RecursalTribunalDetalhado.TJCE,
                InstanceLevel.SECOND_INSTANCE,
                RecursalAuthority.CAMARA,
                true,
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                false,
                false,
                Instant.parse("2026-03-11T18:00:00Z")
        );
    }

    @Test
    void deveRetentarNotificacaoTransienteAntesDeMarcarSucesso() {
        org.springframework.mock.env.MockEnvironment environment = new org.springframework.mock.env.MockEnvironment()
                .withProperty("pjb.recursal.retry.notification.max-attempts", "3")
                .withProperty("pjb.recursal.retry.notification.initial-backoff-ms", "0")
                .withProperty("pjb.recursal.retry.notification.max-backoff-ms", "0");
        com.tcc.pjb.backend.service.outbox.OutboxPublisher localOutbox = org.mockito.Mockito.mock(com.tcc.pjb.backend.service.outbox.OutboxPublisher.class);
        RecursalMeshSlaService localSla = org.mockito.Mockito.mock(RecursalMeshSlaService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshOperationalTelemetryService> telemetryProvider = (ObjectProvider<RecursalMeshOperationalTelemetryService>) org.mockito.Mockito.mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RecursalMeshRetryExecutor> retryProvider = (ObjectProvider<RecursalMeshRetryExecutor>) org.mockito.Mockito.mock(ObjectProvider.class);
        RecursalMeshRetryExecutor retryExecutor = new RecursalMeshRetryExecutor(environment, telemetryProvider);
        org.mockito.Mockito.when(retryProvider.getIfAvailable()).thenReturn(retryExecutor);
        RecursalMeshPartyNotificationService service = new RecursalMeshPartyNotificationService(notificationService, localOutbox, localSla, telemetryProvider, retryProvider);
        RecursalAggregateState aggregate = aggregate();
        RecursalStateSnapshot previous = snapshot(RecursalLifecycleState.JULGAMENTO_COLEGIADO, 3);
        RecursalStateSnapshot current = snapshot(RecursalLifecycleState.PROVIDO, 4);
        java.util.concurrent.atomic.AtomicInteger attempts = new java.util.concurrent.atomic.AtomicInteger();
        org.mockito.Mockito.doAnswer(invocation -> {
            if (attempts.incrementAndGet() < 2) {
                throw new IllegalStateException("temporario");
            }
            return null;
        }).when(notificationService).notifyLawyers(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

        service.onTransition(aggregate, RecursalTransitionEvent.PROVER, previous, current, "advogado", "cmd-2");

        org.assertj.core.api.Assertions.assertThat(attempts.get()).isEqualTo(2);
        org.mockito.Mockito.verify(notificationService, org.mockito.Mockito.times(2)).notifyLawyers(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

}
