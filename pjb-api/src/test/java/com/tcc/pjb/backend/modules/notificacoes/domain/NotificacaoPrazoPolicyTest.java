package com.tcc.pjb.backend.modules.notificacoes.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class NotificacaoPrazoPolicyTest {

    private final NotificacaoPrazoPolicy policy = new NotificacaoPrazoPolicy();

    @Test
    void normalizaAlertaDePrazoComChaveDeterministica() {
        var normalizada = policy.normalizar(
                10L,
                20L,
                "0001234-56.2026.8.06.0001",
                LocalDate.now().plusDays(10),
                null,
                " Prazo calculado ",
                " Vencimento forense confirmado. ",
                "/processos/20",
                "alta",
                "prazos",
                null
        );

        assertEquals(10L, normalizada.usuarioId());
        assertEquals(20L, normalizada.processoId());
        assertEquals("ALTA", normalizada.prioridade().name());
        assertEquals("PRAZOS", normalizada.origemModulo());
        assertEquals("Prazo calculado", normalizada.titulo());
        assertNotNull(normalizada.notificationKey());
        assertTrue(normalizada.notificationKey().startsWith("PRAZO:"));
    }

    @Test
    void rejeitaProcessoAusente() {
        assertThrows(NotificacaoPrazoDomainException.class, () -> policy.normalizar(
                10L,
                null,
                null,
                LocalDate.now().plusDays(5),
                null,
                "Prazo",
                "Corpo",
                null,
                "NORMAL",
                "PRAZOS",
                null
        ));
    }

    @Test
    void rejeitaDataDeNotificacaoNoPassadoOperacional() {
        assertThrows(NotificacaoPrazoDomainException.class, () -> policy.normalizar(
                10L,
                20L,
                null,
                LocalDate.now().plusDays(5),
                LocalDateTime.now().minusDays(1),
                "Prazo",
                "Corpo",
                null,
                "NORMAL",
                "PRAZOS",
                null
        ));
    }

    @Test
    void prioridadeCriticaQuandoPrazoExigeConferenciaManual() {
        assertEquals(
                NotificacaoPrazoPrioridade.CRITICA,
                policy.prioridadeParaPrazo(LocalDate.now().plusDays(30), true, true)
        );
    }

    @Test
    void prioridadeAltaQuandoPrazoEstaProximo() {
        assertEquals(
                NotificacaoPrazoPrioridade.ALTA,
                policy.prioridadeParaPrazo(LocalDate.now().plusDays(2), false, true)
        );
    }
}
