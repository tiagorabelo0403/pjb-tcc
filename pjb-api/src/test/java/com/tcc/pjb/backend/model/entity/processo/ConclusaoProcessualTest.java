package com.tcc.pjb.backend.model.entity.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ConclusaoProcessualTest {

    private ConclusaoProcessual newConclusao() {
        return new ConclusaoProcessual(10L, 5L, 1L, "PARA_DECISAO", null,
                Instant.now().plusSeconds(86400));
    }

    @Test
    void isPendenteQuandoCriado() {
        assertTrue(newConclusao().isPendente());
    }

    @Test
    void naoIsPendenteAposDevolver() {
        var c = newConclusao();
        c.devolver();
        assertFalse(c.isPendente());
    }

    @Test
    void naoIsPendenteAposExpirar() {
        var c = newConclusao();
        c.expirar();
        assertFalse(c.isPendente());
    }

    @Test
    void expirarSetaStatusExpirada() {
        var c = newConclusao();
        c.expirar();
        assertEquals("EXPIRADA", c.getStatus());
    }

    @Test
    void devolverSetaStatusDevolvida() {
        var c = newConclusao();
        c.devolver();
        assertEquals("DEVOLVIDA", c.getStatus());
    }

    @Test
    void devolverSetaDataDevolucao() {
        var c = newConclusao();
        assertNotNull(c.getDataLimite());
        c.devolver();
        assertNotNull(c.getDataDevolucao());
    }
}
