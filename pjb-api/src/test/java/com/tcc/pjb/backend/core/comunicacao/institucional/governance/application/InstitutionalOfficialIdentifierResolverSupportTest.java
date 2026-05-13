package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalOfficialIdentifierResolverSupportTest {

    @Test
    void shouldDeriveOfficialIdentifiersFromKnownPatterns() {
        assertEquals("12495454000160", InstitutionalOfficialIdentifierResolverSupport.normalizeCnpj("12.495.454/0001-60"));
        assertTrue(InstitutionalOfficialIdentifierResolverSupport.isValidCnpj("12.495.454/0001-60"));
        assertEquals("2304400", InstitutionalOfficialIdentifierResolverSupport.deriveIbgeMunicipioCode("FORO 2304400", "UNID-1", List.of()));
        assertEquals("api_publica_tjce", InstitutionalOfficialIdentifierResolverSupport.deriveDataJudAlias("TJCE"));
        assertEquals("12345", InstitutionalOfficialIdentifierResolverSupport.deriveSiorgUnitCode("UNID-12345"));
        assertEquals("https://servicodados.ibge.gov.br/api/v1/localidades/municipios/2304400", InstitutionalOfficialIdentifierResolverSupport.buildIbgeLookupUrl("2304400"));
    }

    @Test
    void shouldClassifyGovernmentAndJudiciaryScopesCorrectly() {
        assertTrue(InstitutionalOfficialIdentifierResolverSupport.isJudiciaryScope(InstitutionalOrganizationScope.FORUM, "TJCE"));
        assertTrue(InstitutionalOfficialIdentifierResolverSupport.isFederalExecutiveScope("Federal", InstitutionalOrganizationScope.PROCURADORIA_PUBLICA));
        assertTrue(InstitutionalOfficialIdentifierResolverSupport.isOfficialInstitutionalDomain("tjce.jus.br"));
    }
}
