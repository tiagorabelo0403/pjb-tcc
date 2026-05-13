package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class NationalProceduralRoutingTestFixtures {

    private NationalProceduralRoutingTestFixtures() {
    }

    public static NationalProceduralRoutingCoreResolution sampleResolution() {
        return new NationalProceduralRoutingCoreResolution(
                Map.of("valorCausa", new BigDecimal("15000.00"), "classe", "indenizacao", "uf", "CE"),
                "corpus-sintetico",
                "context",
                new NationalProceduralPartyProfile(false, false, false, true, false, false, false, false, true, List.of("ESTADO"), "AUTOR", "REU"),
                new CanonicalRitoSelector.SelectedRito(
                        Instant.parse("2026-01-01T10:00:00Z"),
                        "context",
                        new ProceduralCanonicalResolver.CanonicalContext(
                                Instant.parse("2026-01-01T09:59:00Z"),
                                RitoProcessual.COMUM_ORDINARIO,
                                "CIVEL",
                                "7",
                                "Procedimento Comum",
                                "ESTADUAL",
                                "TJCE",
                                "Tribunal de Justica do Ceara",
                                "PJE",
                                List.of("AUTOR", "REU"),
                                List.of("PETICAO_INICIAL"),
                                List.of("ESTADUAL"),
                                Map.of("classeCanonical", "INDENIZATORIA")
                        ),
                        null,
                        RitoProcessual.COMUM_ORDINARIO,
                        "CANONICAL_RITO_RESOLVED",
                        false,
                        false,
                        Map.of("effectiveRito", "COMUM_ORDINARIO")
                ),
                new ProceduralCanonicalResolver.CanonicalContext(
                        Instant.parse("2026-01-01T09:59:00Z"),
                        RitoProcessual.COMUM_ORDINARIO,
                        "CIVEL",
                        "7",
                        "Procedimento Comum",
                        "ESTADUAL",
                        "TJCE",
                        "Tribunal de Justica do Ceara",
                        "PJE",
                        List.of("AUTOR", "REU"),
                        List.of("PETICAO_INICIAL"),
                        List.of("ESTADUAL"),
                        Map.of("classeCanonical", "INDENIZATORIA")
                ),
                new CompetenceResolveResponse(
                        "cmp-1",
                        Instant.parse("2026-01-01T10:01:00Z"),
                        "ESTADUAL",
                        "COMUM_ORDINARIO",
                        0.91d,
                        List.of("competencia estadual"),
                        List.of("CF/88"),
                        Map.of("resolver", "national")
                ),
                new NationalProceduralActionProfile(
                        "INDENIZATORIA",
                        "CIVEL_PATRIMONIAL",
                        false,
                        "COMUM_ORDINARIO",
                        "CIVEL",
                        List.of("MARCADOR"),
                        List.of("acao de conhecimento"),
                        List.of("CC"),
                        List.of(),
                        List.of("validar valor da causa")
                ),
                "DOCUMENTAL",
                TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("15000.00"), LocalDate.of(2026, 1, 1)),
                new NationalProceduralJuizadoDecision(true, null, List.of("cabivel"), List.of("Lei 9.099"), List.of(), List.of("revisar competencia"), 0.88d, false),
                "MEDIA",
                "COMUM_ORDINARIO",
                TipoJustica.ESTADUAL,
                "COMUM",
                "FAZENDA_PUBLICA",
                new NationalProceduralJudicialPlacement(
                        "Foro de Fortaleza/CE",
                        "Fortaleza",
                        "CE",
                        "TJCE",
                        "Tribunal de Justica do Ceara",
                        "1a Vara da Fazenda",
                        "FAZENDA",
                        "PJE",
                        new NationalProceduralDistributionSuggestion(
                                "VARA-01",
                                "TJCE",
                                "Fortaleza",
                                "CE",
                                "FAZENDA",
                                93.1d,
                                "score territorial e material",
                                List.of(),
                                List.of("confirmar prevencao")
                        ),
                        new ProceduralForumAllocationReport(
                                Instant.parse("2026-01-01T10:02:00Z"),
                                "7",
                                "Procedimento Comum",
                                "DOMICILIO_AUTOR",
                                "Fortaleza",
                                "CE",
                                "competencia territorial padrao",
                                "NENHUM",
                                "NENHUM",
                                List.of(),
                                "TJCE",
                                "Tribunal de Justica do Ceara",
                                "VARA-01",
                                "1a Vara da Fazenda",
                                "FAZENDA",
                                false,
                                true,
                                93.1d,
                                "PJE",
                                true,
                                false,
                                false,
                                true,
                                true,
                                "APTO",
                                List.of(),
                                List.of(),
                                List.of("verificar distribuicao"),
                                Map.of("regionalizacao", "capital")
                        )
                ),
                new NationalProceduralReviewSynthesis(
                        List.of("razao sintetica"),
                        List.of("base legal"),
                        List.of("alerta"),
                        List.of("valorCausa"),
                        List.of("MARCADOR"),
                        List.of("checklist"),
                        List.of(),
                        0.84d,
                        true,
                        "MEDIUM"
                )
        );
    }

    static ProceduralEconomicGateReport sampleEconomicGate() {
        return new ProceduralEconomicGateReport(
                Instant.parse("2026-01-01T10:03:00Z"),
                "JU",
                "JEF",
                "MEDIA",
                true,
                false,
                true,
                "FAZENDA_PUBLICA",
                null,
                null,
                2026,
                "seguir",
                List.of(),
                List.of("aviso economico"),
                List.of("revisar memoria"),
                Map.of("fonte", "teste")
        );
    }
}
