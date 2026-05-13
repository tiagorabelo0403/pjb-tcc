package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;

class NationalProceduralRightsCoverageServiceTest {

    private final NationalProceduralRightsCoverageService service = new NationalProceduralRightsCoverageService();

    @Test
    void snapshotCoversEveryRegisteredRito() {
        NationalProceduralRightsCoverageSnapshot snapshot = service.snapshot();

        assertTrue(snapshot.supportsAllBrazilianRites());
        assertTrue(snapshot.supportsAllBrazilianRights());
        assertTrue(snapshot.supportsAllProceduralGuarantees());
        assertEquals(RitoProcessual.values().length, snapshot.totalRitos());
        assertTrue(snapshot.ritoCoverage().stream().anyMatch(row -> row.rito().equals(RitoProcessual.ESPECIAL_ACAO_DIRETA_INCONSTITUCIONALIDADE.name())));
    }

    @Test
    void describeMarksJuizadoAndCollectiveTracks() {
        NationalProceduralRightsCoverageRow juizado = service.describe("JUIZADO_ESPECIAL_CIVEL");
        NationalProceduralRightsCoverageRow coletivo = service.describe("ESPECIAL_MANDADO_SEGURANCA_COLETIVO");

        assertTrue(juizado.admiteJuizado());
        assertTrue(juizado.garantiasEssenciais().contains("ORALIDADE_SIMPLICIDADE_E_CELERIDADE"));
        assertTrue(coletivo.coletivoOuEstrutural());
    }
}
