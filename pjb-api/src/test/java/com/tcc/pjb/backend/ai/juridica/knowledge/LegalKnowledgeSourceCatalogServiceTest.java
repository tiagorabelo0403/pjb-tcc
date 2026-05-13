package com.tcc.pjb.backend.ai.juridica.knowledge;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.juridica.knowledge.support.LegalKnowledgeJsonResourceLoader;
import org.junit.jupiter.api.Test;

class LegalKnowledgeSourceCatalogServiceTest {

    @Test
    void shouldLoadOfficialAndInternalSources() {
        LegalKnowledgeSourceCatalogService service = new LegalKnowledgeSourceCatalogService(new LegalKnowledgeJsonResourceLoader(new ObjectMapper().findAndRegisterModules()));
        service.load();

        assertFalse(service.listAll().isEmpty());
        assertTrue(service.listAll().stream().anyMatch(item -> "CF88_PLANALTO".equals(item.sourceId())));
        assertTrue(service.listAll().stream().anyMatch(item -> "PJB_RITOS_PACK_2026".equals(item.sourceId())));
        assertTrue(service.listAll().stream().anyMatch(item -> "PJB_PRECEDENTES_SEED_2026".equals(item.sourceId())));
        assertTrue(service.listAll().stream().anyMatch(item -> "PJB_MATERIAL_PACK_2026".equals(item.sourceId())));
    }
}
