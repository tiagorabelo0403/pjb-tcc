package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralForumAllocationResolverTest {

    @Test
    void mustOrchestrateSeedReadinessAndReportAssembly() {
        NationalProceduralForumAllocationSeedResolver seedResolver = mock(NationalProceduralForumAllocationSeedResolver.class);
        NationalProceduralForumRoutingReadinessResolver readinessResolver = mock(NationalProceduralForumRoutingReadinessResolver.class);
        NationalProceduralForumAllocationReportAssembler reportAssembler = mock(NationalProceduralForumAllocationReportAssembler.class);
        NationalProceduralForumAllocationResolver resolver = new NationalProceduralForumAllocationResolver(seedResolver, readinessResolver, reportAssembler);
        NationalProceduralForumAllocationContext context = context(Map.of("classe", "indenizacao"));
        NationalProceduralForumAllocationSeed seed = new NationalProceduralForumAllocationSeed(
                null,
                new NationalProceduralTerritorialAnchor("DOMICILIO_AUTOR", "Fortaleza", "CE", "fundamento"),
                new NationalProceduralLinkageAnalysis("NENHUM", "NENHUM", List.of(), List.of()),
                "Fortaleza",
                "CE",
                "TJCE",
                "TJCE",
                "VARA-01",
                "1a Vara",
                "CIVEL",
                90.0d,
                null
        );
        NationalProceduralForumRoutingReadiness readiness = new NationalProceduralForumRoutingReadiness(null, null, null, false, false, false, "NOT_EVALUATED", List.of(), List.of(), List.of());
        ProceduralForumAllocationReport report = new ProceduralForumAllocationReport(
                Instant.now(), "7", "Procedimento Comum", "DOMICILIO_AUTOR", "Fortaleza", "CE", "fundamento", "NENHUM", "NENHUM", List.of(), "TJCE", "TJCE", "VARA-01", "1a Vara", "CIVEL", false, true, 90.0d, "PJE", false, false, false, false, false, "NOT_EVALUATED", List.of(), List.of(), List.of(), Map.of()
        );

        when(seedResolver.resolve(context)).thenReturn(seed);
        when(readinessResolver.resolve(context, seed)).thenReturn(readiness);
        when(reportAssembler.assemble(context, seed, readiness)).thenReturn(report);

        ProceduralForumAllocationReport result = resolver.resolve(context);

        assertSame(report, result);
        verify(seedResolver).resolve(context);
        verify(readinessResolver).resolve(context, seed);
        verify(reportAssembler).assemble(context, seed, readiness);
    }

    private static NationalProceduralForumAllocationContext context(Map<String, Object> payload) {
        return new NationalProceduralForumAllocationContext(
                payload,
                "obrigacao de fazer civel",
                new ProceduralCanonicalResolver.CanonicalContext(
                        Instant.now(),
                        RitoProcessual.COMUM_ORDINARIO,
                        "CIVEL",
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
                ),
                new CompetenceResolveResponse("req", Instant.now(), "ESTADUAL", "COMUM_ORDINARIO", 0.87d, List.of(), List.of(), Map.of()),
                TipoJustica.ESTADUAL,
                "COMUM_ORDINARIO",
                new NationalProceduralActionProfile("INDENIZATORIA", "CIVEL", false, "COMUM_ORDINARIO", "CIVEL", List.of(), List.of(), List.of(), List.of(), List.of()),
                new NationalProceduralJuizadoDecision(false, null, List.of(), List.of(), List.of(), List.of(), 0.84d, false),
                "Fortaleza",
                "CE",
                "TJCE",
                "TJCE",
                "VARA-01",
                "CIVEL",
                null
        );
    }
}
