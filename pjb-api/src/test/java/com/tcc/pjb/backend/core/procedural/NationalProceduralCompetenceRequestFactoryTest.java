package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralCompetenceRequestFactoryTest {

    @Test
    void mustTranslatePayloadAndPartyProfileIntoCompetenceRequest() {
        NationalProceduralCompetenceRequestFactory factory = new NationalProceduralCompetenceRequestFactory();

        CompetenceResolveRequest request = factory.create(
                Map.of(
                        "assunto", "Obrigacao de fazer cumulada com indenizacao",
                        "classe", "7",
                        "materia", "CIVIL",
                        "ufAutor", "CE",
                        "cidadeAutor", "Fortaleza",
                        "valorCausa", new BigDecimal("18500.00"),
                        "pedidoPrincipal", "fornecimento de medicamento"
                ),
                canonical(),
                new NationalProceduralPartyProfile(true, true, false, false, true, false, false, false, true, List.of("ENTE_PUBLICO"), "AUTOR", "REU")
        );

        assertEquals("Obrigacao de fazer cumulada com indenizacao", request.assunto());
        assertEquals("7", request.classeProcessual());
        assertEquals("CIVIL", request.materia());
        assertEquals("CE", request.uf());
        assertEquals("Fortaleza", request.comarca());
        assertEquals(new BigDecimal("18500.00"), request.valorCausa());
        assertTrue(request.envolveUniao());
        assertTrue(request.envolveAutarquiaFederal());
        assertTrue(request.envolveEstado());
        assertFalse(request.envolveMilitar());
        assertTrue(request.textoCaso().contains("fornecimento de medicamento"));
    }

    private static ProceduralCanonicalResolver.CanonicalContext canonical() {
        return new ProceduralCanonicalResolver.CanonicalContext(
                Instant.now(),
                RitoProcessual.COMUM_ORDINARIO,
                "CIVIL",
                "7",
                "Procedimento Comum",
                "ESTADUAL",
                "TJCE",
                "TJCE",
                "PJE",
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
    }
}
