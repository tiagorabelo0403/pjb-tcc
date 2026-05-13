package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PjbPublicRouteGovernanceHardeningTest {

    @Test
    void routeGovernanceMustCoverBootstrapPublicReadAndSignedIngressSurfaces() {
        String content = ApiSurfaceTestSupport.applicationYaml();
        assertTrue(content.contains("- name: auth-security"));
        assertTrue(content.contains("/api/v1/auth/**"));
        assertTrue(content.contains("/api/v1/security/**"));
        assertTrue(content.contains("- name: public-readonly"));
        assertTrue(content.contains("/api/v1/public/**"));
        assertTrue(content.contains("/api/v1/consultas-rapidas/**"));
        assertTrue(content.contains("- name: public-catalog-ui-readonly"));
        assertTrue(content.contains("/api/v1/catalog/**"));
        assertTrue(content.contains("/api/v1/ui/**"));
        assertTrue(content.contains("/api/v1/tribunal/perfil/**"));
        assertTrue(content.contains("/api/v1/orgaos-judiciarios/**"));
        assertTrue(content.contains("- name: notification-tracking-public"));
        assertTrue(content.contains("/api/v1/notificacoes/track/**"));
        assertTrue(content.contains("- name: integration-signed-ingress"));
        assertTrue(content.contains("/api/v1/integrations/n8n/workitems/generate"));
        assertTrue(content.contains("- name: legacy-public-forward"));
        assertTrue(content.contains("/com/tcc/pjb/backend/api/modulos/advocacia/clientes/**"));
        assertTrue(content.contains("allowed-content-types: [\"application/json\"]"));
        assertTrue(content.contains("rate-limit-key-strategy: ip"));
        assertTrue(content.contains("no-store-response: true"));
    }
}
