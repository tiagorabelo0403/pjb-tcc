package com.tcc.pjb.backend.shared.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PJeSharedDtoTest {

    @Test
    void deveNormalizarMetadadosDaAutenticacaoSemLombok() {
        PJeAutenticacaoResponse response = new PJeAutenticacaoResponse(
                "token-x",
                Instant.now().plusSeconds(60),
                "TJCE",
                "corr-1",
                null
        );
        assertTrue(response.isValido());
        assertTrue(response.metadados().isEmpty());
    }

    @Test
    void deveCopiarMetadadosDaSubmissaoDeModoImutavel() {
        PJeSubmissaoResponse response = new PJeSubmissaoResponse(
                "0001",
                "proto-1",
                "SUCESSO",
                Instant.now(),
                "corr-2",
                Map.of("origem", "PJB")
        );
        assertEquals("PJB", response.metadados().get("origem"));
        assertThrows(UnsupportedOperationException.class, () -> response.metadados().put("x", "y"));
    }

    @Test
    void deveCopiarAndamentosEExtrasDeModoImutavel() {
        PJeAndamentoResponse.Andamento andamento = new PJeAndamentoResponse.Andamento(
                Instant.now(),
                "Distribuído",
                "1a Vara",
                Map.of("sequencia", 1)
        );
        PJeAndamentoResponse response = new PJeAndamentoResponse(
                "0002",
                "EM_ANDAMENTO",
                Instant.now(),
                "corr-3",
                List.of(andamento),
                Map.of("fonte", "mock")
        );
        assertEquals(1, response.andamentos().size());
        assertEquals(1, response.andamentos().get(0).extras().get("sequencia"));
        assertThrows(UnsupportedOperationException.class, () -> response.andamentos().add(andamento));
        assertThrows(UnsupportedOperationException.class, () -> response.andamentos().get(0).extras().put("a", 2));
    }
}
