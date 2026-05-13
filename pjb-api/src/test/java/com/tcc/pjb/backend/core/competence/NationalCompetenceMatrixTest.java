package com.tcc.pjb.backend.core.competence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.RamoJusticaNacional;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import org.junit.jupiter.api.Test;

class NationalCompetenceMatrixTest {

    @Test
    void deveResolverTribunaisSuperioresExpandidos() {
        assertEquals(NationalCompetenceMatrix.STJ, NationalCompetenceMatrix.resolver("BR", RamoJusticaNacional.SUPERIOR).orElseThrow());
        assertEquals(NationalCompetenceMatrix.STF, NationalCompetenceMatrix.resolver("BR", RamoJusticaNacional.SUPERIOR_STF).orElseThrow());
        assertEquals(NationalCompetenceMatrix.TSE, NationalCompetenceMatrix.resolver("BR", RamoJusticaNacional.ELEITORAL_SUPERIOR).orElseThrow());
        assertEquals(NationalCompetenceMatrix.TST, NationalCompetenceMatrix.resolver("BR", RamoJusticaNacional.TRABALHO_SUPERIOR).orElseThrow());
        assertEquals(NationalCompetenceMatrix.STM, NationalCompetenceMatrix.resolver("BR", RamoJusticaNacional.MILITAR_SUPERIOR).orElseThrow());
    }

    @Test
    void deveResolverPorCodigoNormalizado() {
        assertEquals(NationalCompetenceMatrix.TRE_CE, NationalCompetenceMatrix.porCodigo("tre-ce").orElseThrow());
        assertEquals(NationalCompetenceMatrix.TJM_SP, NationalCompetenceMatrix.porCodigo("tjmsp").orElseThrow());
    }

    @Test
    void deveExporConectoresEMapaMinimo() {
        NationalCompetenceMatrix matrix = NationalCompetenceMatrix.TJSP;
        assertEquals(JudicialSystem.ESAJ, matrix.connectorPreferido());
        assertTrue(matrix.toMap().containsKey("codigo"));
        assertTrue(matrix.toMap().containsKey("sistemaJudicialPrimario"));
    }
}
