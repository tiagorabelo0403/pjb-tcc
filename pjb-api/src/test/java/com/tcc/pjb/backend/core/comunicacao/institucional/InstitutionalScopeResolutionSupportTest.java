package com.tcc.pjb.backend.core.comunicacao.institucional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import org.junit.jupiter.api.Test;

class InstitutionalScopeResolutionSupportTest {

    @Test
    void fallbackShouldDefaultToGenericInstitutionalScope() {
        assertEquals(InstitutionalOrganizationScope.GENERICO_INSTITUCIONAL, InstitutionalScopeResolutionSupport.fallback(null));
        assertEquals("GENERICO_INSTITUCIONAL", InstitutionalScopeResolutionSupport.code(null));
    }

    @Test
    void matchFilterShouldNormalizeCaseSpacesAndDashes() {
        assertTrue(InstitutionalScopeResolutionSupport.matchesFilter("Ministerio_Publico", "ministerio-publico"));
    }
}
