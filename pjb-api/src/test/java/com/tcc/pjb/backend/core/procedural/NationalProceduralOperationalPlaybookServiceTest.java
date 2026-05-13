package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;

class NationalProceduralOperationalPlaybookServiceTest {

    private final NationalProceduralOperationalPlaybookService service = new NationalProceduralOperationalPlaybookService(new NationalProceduralRightsCoverageService());

    @Test
    void snapshotCoversEveryRitoWithProtocolSteps() {
        NationalProceduralOperationalPlaybookSnapshot snapshot = service.snapshot();

        assertTrue(snapshot.supportsAllBrazilianRites());
        assertTrue(snapshot.supportsAllProceduralCompetenceTracks());
        assertEquals(RitoProcessual.values().length, snapshot.totalRitos());
        assertTrue(snapshot.rows().stream().allMatch(row -> row.steps().size() >= 6));
    }

    @Test
    void describeIncludesJuizadoAndPenaltyAnchors() {
        NationalProceduralOperationalPlaybookRow juizado = service.describe("JUIZADO_ESPECIAL_CIVEL");
        NationalProceduralOperationalPlaybookRow penal = service.describe("PENAL_MARIA_DA_PENHA");

        assertTrue(juizado.unitAnchors().contains("JUIZADO"));
        assertTrue(penal.unitAnchors().contains("VARA_CRIMINAL"));
    }
}
