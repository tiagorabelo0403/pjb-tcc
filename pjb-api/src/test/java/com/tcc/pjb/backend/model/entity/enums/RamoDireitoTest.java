package com.tcc.pjb.backend.model.entity.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RamoDireitoTest {

    @Test
    void deveExporGettersSemDependerDeLombok() {
        assertEquals("01", RamoDireito.CIVIL.getCodigo());
        assertEquals("Direito Civil", RamoDireito.CIVIL.getDescricao());
        assertEquals("Privado", RamoDireito.CIVIL.getCategoria());
    }

    @Test
    void deveResolverAliasLegadoDoRamo() {
        assertEquals(RamoDireito.FAMILIA, RamoDireito.fromString("familia"));
        assertEquals(RamoDireito.TRIBUTARIO, RamoDireito.fromString("tributário"));
        assertNull(RamoDireito.fromString("   "));
        assertTrue(RamoDireito.FAMILIA.geraSigiloAutomatico());
    }
}
