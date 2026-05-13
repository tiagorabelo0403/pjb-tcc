package com.tcc.pjb.backend.core.security.device.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;

class SecurityActionCatalogTest {

    @Test
    void resolvesSensitiveWritePathsWithJudicialHardening() {
        DeviceSecurityProperties props = new DeviceSecurityProperties();
        SecurityActionCatalog catalog = new SecurityActionCatalog(props);

        SecurityActionDecision publication = catalog.resolve("POST", "/api/v1/processos/10/publicacao/acordao");
        SecurityActionDecision alvara = catalog.resolve("POST", "/api/v1/processos/10/alvara-soltura");
        SecurityActionDecision mandado = catalog.resolve("POST", "/api/v1/processos/10/mandado-prisao");

        assertEquals(SecurityAction.PUBLISH_JUDICIAL_ACT, publication.action());
        assertEquals(SecurityAction.ISSUE_RELEASE_ORDER, alvara.action());
        assertEquals(SecurityAction.ISSUE_MANDATE, mandado.action());
        assertTrue(catalog.effectivePolicy(publication.action()).stepUpRequired());
        assertTrue(catalog.effectivePolicy(alvara.action()).dualApprovalRequired());
        assertTrue(catalog.effectivePolicy(mandado.action()).bindStrongAuthToDevice());
    }
}
