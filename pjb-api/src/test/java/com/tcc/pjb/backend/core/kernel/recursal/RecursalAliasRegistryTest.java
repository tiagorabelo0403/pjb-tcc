package com.tcc.pjb.backend.core.kernel.recursal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class RecursalAliasRegistryTest {

    @Test
    void shouldResolveExtendedRecursalAliases() {
        RecursalAliasRegistry registry = new RecursalAliasRegistry();
        assertEquals("AGINST", registry.resolve("agravo de instrumento"));
        assertEquals("AIRR", registry.resolve("agravo_recurso_revista"));
        assertEquals("RINOM", registry.resolve("ri"));
        assertEquals("PUILF", registry.resolve("pedilef"));
        assertEquals("PUILF", registry.resolve("pedido de uniformizacao"));
    }
}
