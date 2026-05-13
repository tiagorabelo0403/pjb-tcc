package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralReviewInputRequirementResolverOrchestrationTest {

    @Test
    void mustMergeSubphaseInputAssessmentsWithoutDuplicates() {
        NationalProceduralReviewCoreFieldRequirementResolver core = mock(NationalProceduralReviewCoreFieldRequirementResolver.class);
        NationalProceduralReviewEconomicRequirementResolver economic = mock(NationalProceduralReviewEconomicRequirementResolver.class);
        NationalProceduralReviewLocationRequirementResolver location = mock(NationalProceduralReviewLocationRequirementResolver.class);
        NationalProceduralReviewPartyRequirementResolver party = mock(NationalProceduralReviewPartyRequirementResolver.class);
        NationalProceduralReviewJurisdictionRequirementResolver jurisdiction = mock(NationalProceduralReviewJurisdictionRequirementResolver.class);
        NationalProceduralReviewInputRequirementResolver resolver = new NationalProceduralReviewInputRequirementResolver(core, economic, location, party, jurisdiction);
        NationalProceduralReviewSynthesisContext context = NationalProceduralReviewSignalCollectorTest.baseContext(Map.of());
        when(core.assess(context.payload())).thenReturn(new NationalProceduralReviewInputSlice(List.of("classeProcessual"), List.of("classe")));
        when(economic.assess(context.payload(), context.juizadoDecision(), context.actionProfile())).thenReturn(new NationalProceduralReviewInputSlice(List.of("valorCausa"), List.of("valor")));
        when(location.assess(context.cidadeSugerida(), context.ufSugerida())).thenReturn(new NationalProceduralReviewInputSlice(List.of("ufBase"), List.of()));
        when(party.assess(context.payload())).thenReturn(new NationalProceduralReviewInputSlice(List.of("parteAutora", "ufBase"), List.of()));
        when(jurisdiction.assess(context)).thenReturn(new NationalProceduralReviewInputSlice(List.of("zonaEleitoral", "parteAutora"), List.of("jurisdicao")));

        NationalProceduralReviewInputAssessment result = resolver.assess(context);

        assertTrue(result.missingInputs().contains("classeProcessual"));
        assertTrue(result.missingInputs().contains("valorCausa"));
        assertTrue(result.missingInputs().contains("ufBase"));
        assertTrue(result.missingInputs().contains("parteAutora"));
        assertTrue(result.missingInputs().contains("zonaEleitoral"));
        assertTrue(result.blockingIssues().contains("classe"));
        assertTrue(result.blockingIssues().contains("valor"));
        assertTrue(result.blockingIssues().contains("jurisdicao"));
        verify(core).assess(context.payload());
        verify(economic).assess(context.payload(), context.juizadoDecision(), context.actionProfile());
        verify(location).assess(context.cidadeSugerida(), context.ufSugerida());
        verify(party).assess(context.payload());
        verify(jurisdiction).assess(context);
    }
}
