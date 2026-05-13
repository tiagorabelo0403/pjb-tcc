package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecisionResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralRoutingFoundationAnalyzerTest {

    @Test
    void mustResolveFoundationStageThroughDedicatedCollaborators() {
        CanonicalRitoSelector canonicalRitoSelector = Mockito.mock(CanonicalRitoSelector.class);
        CompetenceResolverService competenceResolverService = Mockito.mock(CompetenceResolverService.class);
        NationalProceduralCompetenceRequestFactory competenceRequestFactory = Mockito.mock(NationalProceduralCompetenceRequestFactory.class);
        NationalProceduralTetoDiagnosticResolver tetoDiagnosticResolver = Mockito.mock(NationalProceduralTetoDiagnosticResolver.class);
        NationalProceduralActionProfileResolver actionProfileResolver = Mockito.mock(NationalProceduralActionProfileResolver.class);
        NationalProceduralJuizadoDecisionResolver juizadoDecisionResolver = Mockito.mock(NationalProceduralJuizadoDecisionResolver.class);
        NationalProceduralPartyProfileResolver partyProfileResolver = Mockito.mock(NationalProceduralPartyProfileResolver.class);
        NationalProceduralHeuristicRitoResolver heuristicRitoResolver = Mockito.mock(NationalProceduralHeuristicRitoResolver.class);
        NationalProceduralProbatoryProfileResolver probatoryProfileResolver = Mockito.mock(NationalProceduralProbatoryProfileResolver.class);

        NationalProceduralRoutingFoundationAnalyzer analyzer = new NationalProceduralRoutingFoundationAnalyzer(
                canonicalRitoSelector,
                competenceResolverService,
                competenceRequestFactory,
                tetoDiagnosticResolver,
                actionProfileResolver,
                juizadoDecisionResolver,
                partyProfileResolver,
                heuristicRitoResolver,
                probatoryProfileResolver
        );

        Map<String, Object> payload = Map.of("classe", "indenizacao", "valorCausa", new BigDecimal("15000.00"));
        NationalProceduralPartyProfile partyProfile = new NationalProceduralPartyProfile(false, false, false, true, false, false, false, false, true, List.of("ESTADO"), "AUTOR", "REU");
        NationalProceduralRoutingCoreResolution fixture = NationalProceduralRoutingTestFixtures.sampleResolution();
        CanonicalRitoSelector.SelectedRito selectedRito = fixture.selectedRito();
        ProceduralCanonicalResolver.CanonicalContext canonical = selectedRito.canonicalContext();
        CompetenceResolveRequest competenceRequest = Mockito.mock(CompetenceResolveRequest.class);
        CompetenceResolveResponse competence = fixture.competence();
        NationalProceduralActionProfile actionProfile = fixture.actionProfile();
        TetoProcessualService.DiagnosticoTetoProcessual teto = TetoProcessualService.DiagnosticoTetoProcessual.semRestricao(new BigDecimal("15000.00"), LocalDate.of(2026, 1, 1));
        NationalProceduralJuizadoDecision juizadoDecision = new NationalProceduralJuizadoDecision(true, null, List.of("cabivel"), List.of("Lei 9.099"), List.of(), List.of("revisar"), 0.87d, false);

        when(partyProfileResolver.resolve(any(), any())).thenReturn(partyProfile);
        when(heuristicRitoResolver.resolve(any(), any(), eq(partyProfile))).thenReturn(RitoProcessual.COMUM_ORDINARIO);
        when(canonicalRitoSelector.select(any(), eq("COMUM_ORDINARIO"), eq("context"))).thenReturn(selectedRito);
        when(competenceRequestFactory.create(any(), eq(canonical), eq(partyProfile))).thenReturn(competenceRequest);
        when(competenceResolverService.resolve(competenceRequest)).thenReturn(competence);
        when(actionProfileResolver.resolve(any(), eq(canonical), any(), eq(partyProfile))).thenReturn(actionProfile);
        when(probatoryProfileResolver.resolve(any(), any())).thenReturn("DOCUMENTAL");
        when(tetoDiagnosticResolver.resolve(any())).thenReturn(teto);
        when(juizadoDecisionResolver.resolve(any(), eq(competence), eq(actionProfile), eq(partyProfile), eq(teto), any())).thenReturn(juizadoDecision);

        NationalProceduralRoutingFoundationResolution result = analyzer.analyze(payload, "context");

        assertEquals("context", result.sourceLabel());
        assertSame(partyProfile, result.partyProfile());
        assertSame(selectedRito, result.selectedRito());
        assertSame(competence, result.competence());
        assertSame(actionProfile, result.actionProfile());
        assertEquals("DOCUMENTAL", result.probatoryProfile());
        assertSame(teto, result.teto());
        assertSame(juizadoDecision, result.juizadoDecision());
        verify(canonicalRitoSelector).select(any(), eq("COMUM_ORDINARIO"), eq("context"));
        verify(competenceResolverService).resolve(competenceRequest);
        verify(tetoDiagnosticResolver).resolve(any());
    }
}
