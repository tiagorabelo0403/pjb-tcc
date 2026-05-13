package com.tcc.pjb.backend.service.processual.runtime.guard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.IdempotencyBeginResult;
import com.tcc.pjb.backend.core.idempotency.IdempotencyDecision;
import com.tcc.pjb.backend.core.idempotency.IdempotencyStatus;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.processual.runtime.guard.ProcessualOperationGuardRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.service.processual.runtime.homologation.GateTravamentoHomologacaoService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProcessualOperationGuardServiceTest {

    @Test
    void emitsOutboxAndCompletesIdempotencyOnNewOperation() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        ActionIdempotencyService idempotencyService = Mockito.mock(ActionIdempotencyService.class);
        OutboxPublisher outboxPublisher = Mockito.mock(OutboxPublisher.class);
        ProcessualOperationGuardService service = new ProcessualOperationGuardService(
                processoRepository,
                currentUserService,
                authorizationService,
                idempotencyService,
                outboxPublisher,
                new ObjectMapper(),
                Mockito.mock(GateTravamentoHomologacaoService.class)
        );
        Processo processo = new Processo();
        processo.setId(42L);
        processo.setNumeroProcesso("0001-42");
        Usuario usuario = new Usuario();
        usuario.setId(9L);
        usuario.setTipoUsuario(TipoUsuario.SERVIDOR_FORUM);
        when(processoRepository.findById(42L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(idempotencyService.begin(any(), any(), any(), any())).thenReturn(new IdempotencyBeginResult(
                IdempotencyDecision.NEW,
                IdempotencyStatus.IN_PROGRESS,
                "PROCESSUAL:EXPEDIR_DOCUMENTO:42",
                "idem-1",
                "hash-1",
                null,
                null,
                null
        ));
        when(outboxPublisher.enqueueTracked(any(), any(), any(), any(), any(), any(), any())).thenReturn(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

        var response = service.guard(new ProcessualOperationGuardRequest(
                42L,
                "expedir documento",
                "idem-1",
                null,
                true,
                "PROCESSO",
                "42",
                Map.of("canal", "interno")
        ));

        assertTrue(response.accepted());
        assertEquals("123e4567-e89b-12d3-a456-426614174000", response.outboxEventId());
        verify(idempotencyService).complete(any(), eq("idem-1"), any(), any(), any(), any());
    }

    @Test
    void reusesReplayWithoutPublishingOutboxAgain() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        CurrentUserService currentUserService = Mockito.mock(CurrentUserService.class);
        PjbAuthorizationService authorizationService = Mockito.mock(PjbAuthorizationService.class);
        ActionIdempotencyService idempotencyService = Mockito.mock(ActionIdempotencyService.class);
        OutboxPublisher outboxPublisher = Mockito.mock(OutboxPublisher.class);
        ProcessualOperationGuardService service = new ProcessualOperationGuardService(
                processoRepository,
                currentUserService,
                authorizationService,
                idempotencyService,
                outboxPublisher,
                new ObjectMapper(),
                Mockito.mock(GateTravamentoHomologacaoService.class)
        );
        Processo processo = new Processo();
        processo.setId(7L);
        processo.setNumeroProcesso("0007-99");
        Usuario usuario = new Usuario();
        usuario.setId(2L);
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        when(processoRepository.findById(7L)).thenReturn(Optional.of(processo));
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(idempotencyService.begin(any(), any(), any(), any())).thenReturn(new IdempotencyBeginResult(
                IdempotencyDecision.REPLAY,
                IdempotencyStatus.COMPLETED,
                "PROCESSUAL:CONSULTAR:7",
                "idem-2",
                "hash-2",
                "PROCESSUAL_OPERATION_GUARD",
                "7",
                "{\"accepted\":true}"
        ));

        var response = service.guard(new ProcessualOperationGuardRequest(
                7L,
                "consultar",
                "idem-2",
                "hash-2",
                true,
                null,
                null,
                Map.of()
        ));

        assertTrue(response.accepted());
        assertEquals("REPLAY", response.idempotencyDecision());
        assertFalse(response.responseJson().isBlank());
        verify(outboxPublisher, never()).enqueueTracked(any(), any(), any(), any(), any(), any(), any());
    }
}
