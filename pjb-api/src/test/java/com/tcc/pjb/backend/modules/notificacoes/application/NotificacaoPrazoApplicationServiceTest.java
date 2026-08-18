package com.tcc.pjb.backend.modules.notificacoes.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoCommand;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoDispatchResult;
import com.tcc.pjb.backend.modules.notificacoes.api.NotificacaoPrazoPort;
import com.tcc.pjb.backend.modules.notificacoes.domain.NotificacaoPrazoDomainException;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualCalculoResult;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificacaoPrazoApplicationServiceTest {

    @Test
    void criaAlertaAPartirDoResultadoDoModuloPrazos() {
        RecordingPort port = new RecordingPort();
        NotificacaoPrazoApplicationService service = new NotificacaoPrazoApplicationService(port);

        var result = service.notificarPrazoCalculado(
                10L,
                20L,
                "0001234-56.2026.8.06.0001",
                "PRAZOS",
                "/processos/20/prazos",
                prazo(true)
        );

        assertTrue(result.aceita());
        assertEquals("CRITICA", port.last.prioridade());
        assertEquals(LocalDate.now().plusDays(10), port.last.vencimentoForense());
        assertTrue(port.last.titulo().contains("APELACAO"));
        assertTrue(port.last.corpo().contains("Conferencia manual recomendada"));
    }

    @Test
    void publicaComandoNormalizadoSemExporDtoHttp() {
        RecordingPort port = new RecordingPort();
        NotificacaoPrazoApplicationService service = new NotificacaoPrazoApplicationService(port);

        service.publicarAlertaPrazo(new NotificacaoPrazoCommand(
                10L,
                20L,
                null,
                LocalDate.now().plusDays(8),
                null,
                " Prazo ",
                " Corpo ",
                null,
                "normal",
                "prazos",
                null
        ));

        assertEquals("NORMAL", port.last.prioridade());
        assertEquals("PRAZOS", port.last.origemModulo());
        assertTrue(port.last.notificationKey().startsWith("PRAZO:"));
    }

    @Test
    void rejeitaComandoInvalidoAntesDaPorta() {
        RecordingPort port = new RecordingPort();
        NotificacaoPrazoApplicationService service = new NotificacaoPrazoApplicationService(port);

        assertThrows(NotificacaoPrazoDomainException.class, () -> service.publicarAlertaPrazo(new NotificacaoPrazoCommand(
                null,
                20L,
                null,
                LocalDate.now().plusDays(8),
                null,
                "Prazo",
                "Corpo",
                null,
                "NORMAL",
                "PRAZOS",
                null
        )));
        assertFalse(port.called);
    }

    private PrazoProcessualCalculoResult prazo(boolean conferenciaManual) {
        return new PrazoProcessualCalculoResult(
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(10),
                10,
                8,
                8,
                "APELACAO",
                "CIVIL",
                "PRIMEIRO_GRAU",
                "TJCE",
                "CE",
                "Quixada",
                true,
                "Dia util forense",
                List.of(),
                "CPC",
                "Calendario forense",
                conferenciaManual
        );
    }

    private static final class RecordingPort implements NotificacaoPrazoPort {
        private NotificacaoPrazoCommand last;
        private boolean called;

        @Override
        public NotificacaoPrazoDispatchResult publicarAlertaPrazo(NotificacaoPrazoCommand command) {
            called = true;
            last = command;
            return new NotificacaoPrazoDispatchResult(true, "PUBLICADA", command.notificationKey(), command.prioridade());
        }
    }
}
