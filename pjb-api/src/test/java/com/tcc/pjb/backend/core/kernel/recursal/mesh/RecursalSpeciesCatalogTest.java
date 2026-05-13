package com.tcc.pjb.backend.core.kernel.recursal.mesh;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class RecursalSpeciesCatalogTest {

    @Test
    void shouldExposeExtendedSpeciesCodes() {
        RecursalSpeciesCatalog catalog = new RecursalSpeciesCatalog();
        assertTrue(catalog.supports("AGINST"));
        assertTrue(catalog.supports("RR"));
        assertTrue(catalog.supports("PUILF"));
        assertEquals("Reclamação", catalog.formalNameOf("RCL"));
        assertEquals("Pedido de Uniformização de Interpretação de Lei Federal", catalog.formalNameOf("PUILF"));
    }
}
