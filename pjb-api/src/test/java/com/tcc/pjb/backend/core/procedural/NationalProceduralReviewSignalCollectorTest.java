package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralReviewSignalCollectorTest {

    @Test
    void mustCollectContextualSignalsAlertsAndReviewChecklist() {
        NationalProceduralReviewSignalCollector collector = new NationalProceduralReviewSignalCollector(
                new NationalProceduralReviewReasonCollector(),
                new NationalProceduralReviewPolicySignalResolver(new NationalProceduralReviewMessages())
        );

        NationalProceduralReviewDraft draft = collector.collect(baseContext(Map.of("foro", "Fortaleza", "ufAutor", "CE")));

        assertTrue(draft.alerts().contains("Distribuição dinâmica não retornou unidade cadastrada; manter sugestão de família de vara e revisar malha local."));
        assertTrue(draft.reviewChecklist().contains("Verificar se a especialização fazendária ou administrativa local exige vara exclusiva."));
        assertTrue(draft.reviewChecklist().contains("Conferir aderência do rito escolhido ao pedido e ao órgão jurisdicional."));
        assertTrue(draft.reviewChecklist().contains("Conferir distribuição por dependência, prevenção, conexão ou continência antes do protocolo final."));
        assertTrue(draft.actionMarkers().contains("MARCADOR"));
    }

    static NationalProceduralReviewSynthesisContext baseContext(Map<String, Object> payload) {
        return new NationalProceduralReviewSynthesisContext(
                payload,
                new CanonicalRitoSelector.SelectedRito(
                        Instant.now(),
                        "test",
                        new ProceduralCanonicalResolver.CanonicalContext(
                                Instant.now(),
                                RitoProcessual.COMUM_ORDINARIO,
                                "ADMINISTRATIVO",
                                "80",
                                "Fazenda Pública",
                                "ESTADUAL",
                                "TJCE",
                                "TJCE",
                                "PJE",
                                List.of(),
                                List.of(),
                                List.of(),
                                Map.of()
                        ),
                        null,
                        RitoProcessual.COMUM_ORDINARIO,
                        "HEURISTIC_COMPATIBILITY",
                        true,
                        false,
                        Map.of()
                ),
                new CompetenceResolveResponse("req", Instant.now(), "ESTADUAL", "COMUM_ORDINARIO", 0.77d, List.of("competência estadual"), List.of("CF/88"), Map.of()),
                new NationalProceduralActionProfile(
                        "FAZENDA_PUBLICA",
                        "FAZENDA_PUBLICA",
                        true,
                        "FAZENDA_PUBLICA_CONHECIMENTO",
                        "FAZENDA",
                        List.of("MARCADOR"),
                        List.of("ação fazendária"),
                        List.of("Lei 12.153/2009"),
                        List.of(),
                        List.of("revisar ente público")
                ),
                new NationalProceduralJuizadoDecision(false, null, List.of("sem juizado"), List.of(), List.of(), List.of(), 0.61d, false),
                new NationalProceduralPartyProfile(false, false, false, true, false, false, false, false, true, List.of("PARTE_ESTADUAL"), "autor", "estado"),
                TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("0.00"), LocalDate.now()),
                new ProceduralForumAllocationReport(
                        Instant.now(),
                        "80",
                        "Fazenda Pública",
                        "TERRITORIAL_OK",
                        "Fortaleza",
                        "CE",
                        "fundamento",
                        "PREVENCAO",
                        "PROCESSOS_RELACIONADOS",
                        List.of("1234567-89.2024.8.06.0001"),
                        "TJCE",
                        "TJCE",
                        "VARA-FAZENDA",
                        "Vara da Fazenda Pública",
                        "FAZENDA",
                        true,
                        false,
                        0.74d,
                        "PJE",
                        true,
                        false,
                        false,
                        true,
                        false,
                        "STRUCTURAL_REVIEW_REQUIRED",
                        List.of(),
                        List.of("warning"),
                        List.of("check forum"),
                        Map.of()
                ),
                null,
                TipoJustica.ESTADUAL,
                "Fortaleza",
                "CE"
        );
    }
}
