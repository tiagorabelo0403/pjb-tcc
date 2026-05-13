package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbRouteGovernanceCoverageTest {

    @Test
    void routeGovernanceMustCoverPublicCatalogProntuarioMarketplaceAndInternalAudit() throws Exception {
        String content = Files.readString(Path.of("src/main/resources/application.yml"));
        assertTrue(content.contains("- name: public-catalog-ui-readonly"));
        assertTrue(content.contains("/api/v1/catalog/**"));
        assertTrue(content.contains("/api/v1/ui/**"));
        assertTrue(content.contains("/api/v1/tribunal/perfil/**"));
        assertTrue(content.contains("/api/v1/orgaos-judiciarios/**"));
        assertTrue(content.contains("- name: prontuario-sensitive"));
        assertTrue(content.contains("/api/v1/prontuarios/**"));
        assertTrue(content.contains("- name: marketplace-institutional"));
        assertTrue(content.contains("/api/marketplace/v1/**"));
        assertTrue(content.contains("- name: internal-audit-sensitive"));
        assertTrue(content.contains("/api/internal/audit/**"));
    }
}
