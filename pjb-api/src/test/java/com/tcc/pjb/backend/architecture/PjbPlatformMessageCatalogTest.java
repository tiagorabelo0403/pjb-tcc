package com.tcc.pjb.backend.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tcc.pjb.backend.core.i18n.PjbStaticMessageCatalog;
import org.junit.jupiter.api.Test;

class PjbPlatformMessageCatalogTest {

    @Test
    void static_catalog_should_resolve_known_keys() {
        assertEquals("PJB", PjbStaticMessageCatalog.text("pjb.atendimento.system.sender.name"));
        assertEquals("Sistema Judicial", PjbStaticMessageCatalog.text("pjb.atendimento.system.sender.label"));
    }
}
