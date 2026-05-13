package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PjbInstitutionalRouteGovernanceCoverageTest {

    @Test
    void routeGovernanceMustCoverOnlyInstitutionalCanonicalSurface() {
        String content = ApiSurfaceTestSupport.applicationYaml();
        assertTrue(content.contains("- name: institutional-governance-sensitive"));
        assertTrue(content.contains("/api/v1/institucional/**"));
        assertFalse(content.contains("/api/v1/processual/comunicacoes/institucional/**"));
        assertTrue(content.contains("PJB_API_ROUTE_GOVERNANCE_INSTITUTIONAL_MAX_REQUEST_BYTES"));
        assertTrue(content.contains("PJB_API_ROUTE_GOVERNANCE_INSTITUTIONAL_RATE_LIMIT"));
    }
}
