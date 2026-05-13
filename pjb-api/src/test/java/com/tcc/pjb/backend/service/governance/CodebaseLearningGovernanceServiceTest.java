package com.tcc.pjb.backend.service.governance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseLearningApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseCriticalFlow;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseExtractionBlueprint;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseExtractionLane;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseLearningAggregate;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseLearningSlice;
import com.tcc.pjb.backend.model.dto.governance.CodebaseLearningResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CodebaseLearningGovernanceServiceTest {

    @Test
    void deveMapearAggregateParaResponseInstitucional() {
        PjbCodebaseLearningApplicationService applicationService = mock(PjbCodebaseLearningApplicationService.class);
        when(applicationService.aprender(false)).thenReturn(new PjbCodebaseLearningAggregate(
                true,
                100,
                10,
                2,
                4,
                List.of(new PjbCodebaseLearningSlice(
                        "core/processo",
                        80,
                        5,
                        12,
                        7,
                        3,
                        0.0625d,
                        420,
                        "CRITICA",
                        List.of("teste"),
                        List.of("acao"),
                        List.of(new PjbCodebaseExtractionLane("prazo", 18, 3, 0.166d, "PREPARAR", List.of("sinal"), List.of("acao-inicial")))
                )),
                List.of(new PjbCodebaseExtractionBlueprint(
                        "core/processo",
                        "prazo",
                        "PREPARAR",
                        480,
                        "com.tcc.pjb.backend.core.processo.prazo",
                        "ProcessoPrazoFacade",
                        "ProcessoPrazoPort",
                        "ProcessoPrazoIT",
                        List.of("bloqueio"),
                        List.of("acao1")
                )),
                List.of(new PjbCodebaseCriticalFlow(
                        "peticao-triagem-secretaria-gabinete-decisao-publicacao",
                        "AUSENTE",
                        0.0d,
                        List.of(),
                        List.of("sinal-fluxo"),
                        List.of("acao-fluxo")
                )),
                List.of("onda1"),
                List.of("aprendizado"),
                Instant.parse("2026-04-03T12:00:00Z")
        ));
        CodebaseLearningGovernanceService service = new CodebaseLearningGovernanceService(applicationService);

        CodebaseLearningResponse response = service.report();

        assertEquals(100, response.totalMainFiles());
        assertEquals(10, response.totalTestFiles());
        assertEquals(2, response.integrationTests());
        assertEquals(4, response.mappedCoreSlices());
        assertTrue(response.criticalHotspotsPresent());
        assertEquals("core/processo", response.hotspots().getFirst().slice());
        assertEquals("prazo", response.hotspots().getFirst().extractionLanes().getFirst().name());
        assertEquals("ProcessoPrazoFacade", response.extractionBlueprints().getFirst().suggestedFacade());
        assertEquals("AUSENTE", response.criticalFlows().getFirst().status());
        assertFalse(response.priorityWaves().isEmpty());
        assertFalse(response.learnings().isEmpty());
        verify(applicationService).aprender(false);
    }

    @Test
    void devePermitirRefreshExplicitoNoRelatorio() {
        PjbCodebaseLearningApplicationService applicationService = mock(PjbCodebaseLearningApplicationService.class);
        when(applicationService.aprender(true)).thenReturn(new PjbCodebaseLearningAggregate(
                true,
                1,
                1,
                1,
                1,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Instant.parse("2026-04-03T12:00:00Z")
        ));
        CodebaseLearningGovernanceService service = new CodebaseLearningGovernanceService(applicationService);

        CodebaseLearningResponse response = service.report(true);

        assertTrue(response.available());
        verify(applicationService).aprender(true);
    }
}
