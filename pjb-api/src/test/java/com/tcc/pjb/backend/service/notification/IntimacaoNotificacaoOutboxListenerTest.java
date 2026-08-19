package com.tcc.pjb.backend.service.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.outbox.OutboxGenericDispatchedEvent;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IntimacaoNotificacaoOutboxListenerTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final IntimacaoNotificacaoOutboxListener listener = new IntimacaoNotificacaoOutboxListener(
            usuarioRepository, processoRepository, notificationService, new ObjectMapper());

    @Test
    void notificaAdvogadoQuandoIntimacaoProcessualCriadaComUsuarioEProcessoResolviveis() {
        Usuario advogado = new Usuario();
        advogado.setId(10L);
        Processo processo = new Processo();
        processo.setId(55L);
        processo.setNumeroProcesso("0009999-11.2026.8.06.0001");
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(advogado));
        when(processoRepository.findById(55L)).thenReturn(Optional.of(processo));

        String payload = "{\"usuarioId\":10,\"processoId\":55,\"numeroProcesso\":\"0009999-11.2026.8.06.0001\"}";
        OutboxGenericDispatchedEvent event = new OutboxGenericDispatchedEvent(
                "INTIMACAO_PROCESSUAL_CRIADA", "processo.intimacao", payload, null, "Processo", "55", Instant.now());

        listener.onGenericDispatched(event);

        verify(notificationService).notifyUser(eq(advogado), eq(processo), any(), any(), eq(null));
    }

    @Test
    void ignoraEventoDeOutroTipoSemConsultarRepositoriosOuNotificar() {
        OutboxGenericDispatchedEvent event = new OutboxGenericDispatchedEvent(
                "DJE_PUBLICACAO_SOLICITADA", "processo.dje", "{}", null, "Processo", "55", Instant.now());

        listener.onGenericDispatched(event);

        verifyNoInteractions(usuarioRepository, processoRepository, notificationService);
    }

    @Test
    void ignoraSilenciosamenteQuandoUsuarioReferenciadoNaoExisteMaisSemNotificar() {
        when(usuarioRepository.findById(10L)).thenReturn(Optional.empty());

        String payload = "{\"usuarioId\":10,\"processoId\":55}";
        OutboxGenericDispatchedEvent event = new OutboxGenericDispatchedEvent(
                "INTIMACAO_PROCESSUAL_CRIADA", "processo.intimacao", payload, null, "Processo", "55", Instant.now());

        listener.onGenericDispatched(event);

        verify(notificationService, never()).notifyUser(any(), any(), any(), any(), any());
    }
}
