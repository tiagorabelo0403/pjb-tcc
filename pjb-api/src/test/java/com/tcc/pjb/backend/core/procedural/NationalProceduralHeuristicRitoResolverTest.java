package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralHeuristicRitoResolverTest {

    @Test
    void mustPreferExplicitRitoWhenProvided() {
        NationalProceduralActionProfileResolver actionProfileResolver = mock(NationalProceduralActionProfileResolver.class);
        NationalProceduralHeuristicRitoResolver resolver = new NationalProceduralHeuristicRitoResolver(actionProfileResolver);
        NationalProceduralPartyProfile partyProfile = new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, false, List.of(), null, null);

        RitoProcessual rito = resolver.resolve(Map.of("rito", "JUIZADO_ESPECIAL_CIVEL"), "", partyProfile);

        assertEquals(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, rito);
    }

    @Test
    void mustCloseMilitaryHabeasCorpusWhenMilitarySignalExists() {
        NationalProceduralActionProfileResolver actionProfileResolver = mock(NationalProceduralActionProfileResolver.class);
        NationalProceduralHeuristicRitoResolver resolver = new NationalProceduralHeuristicRitoResolver(actionProfileResolver);
        NationalProceduralPartyProfile partyProfile = new NationalProceduralPartyProfile(false, false, false, false, false, false, false, true, false, List.of("MATERIA_MILITAR"), null, null);

        RitoProcessual rito = resolver.resolve(Map.of(), "habeas corpus em contexto de justiça militar", partyProfile);

        assertEquals(RitoProcessual.MILITAR_HABEAS_CORPUS_MILITAR, rito);
    }

    @Test
    void mustClassifyPrevidenciarioBpcByDedicatedSignals() {
        NationalProceduralActionProfileResolver actionProfileResolver = mock(NationalProceduralActionProfileResolver.class);
        NationalProceduralHeuristicRitoResolver resolver = new NationalProceduralHeuristicRitoResolver(actionProfileResolver);
        NationalProceduralPartyProfile partyProfile = new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, false, List.of(), null, null);

        RitoProcessual rito = resolver.resolve(Map.of("valorCausa", 10000), "pedido de bpc loas e benefício assistencial do INSS", partyProfile);

        assertEquals(RitoProcessual.PREVIDENCIARIO_BPC_LOAS, rito);
    }

    @Test
    void mustClassifyMilitaryIpmBeforeGenericMilitaryTrack() {
        NationalProceduralActionProfileResolver actionProfileResolver = mock(NationalProceduralActionProfileResolver.class);
        NationalProceduralHeuristicRitoResolver resolver = new NationalProceduralHeuristicRitoResolver(actionProfileResolver);
        NationalProceduralPartyProfile partyProfile = new NationalProceduralPartyProfile(false, false, false, false, false, false, false, true, false, List.of("MATERIA_MILITAR"), null, null);

        RitoProcessual rito = resolver.resolve(Map.of(), "inquerito policial militar instaurado em auditoria militar", partyProfile);

        assertEquals(RitoProcessual.MILITAR_IPM, rito);
    }

    @Test
    void mustClassifyElectoralRcedBeforeGenericElectoralTrack() {
        NationalProceduralActionProfileResolver actionProfileResolver = mock(NationalProceduralActionProfileResolver.class);
        NationalProceduralHeuristicRitoResolver resolver = new NationalProceduralHeuristicRitoResolver(actionProfileResolver);
        NationalProceduralPartyProfile partyProfile = new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, true, List.of("MATERIA_ELEITORAL"), null, null);

        RitoProcessual rito = resolver.resolve(Map.of(), "recurso contra expedicao do diploma em materia eleitoral", partyProfile);

        assertEquals(RitoProcessual.ELEITORAL_RCED, rito);
    }

    @Test
    void mustUsePrevidenciarioDefaultWhenOnlyGenericBenefitSignalsExist() {
        NationalProceduralActionProfileResolver actionProfileResolver = mock(NationalProceduralActionProfileResolver.class);
        when(actionProfileResolver.inferPrevidenciarioDefaultRito(Map.of("valorCausa", 10000))).thenReturn("PREVIDENCIARIO_JEF");
        NationalProceduralHeuristicRitoResolver resolver = new NationalProceduralHeuristicRitoResolver(actionProfileResolver);
        NationalProceduralPartyProfile partyProfile = new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, false, List.of(), null, null);

        RitoProcessual rito = resolver.resolve(Map.of("valorCausa", 10000), "pedido de beneficio previdenciario perante o inss", partyProfile);

        assertEquals(RitoProcessual.PREVIDENCIARIO_JEF, rito);
    }


    @Test
    void mustClassifyInfanciaInfracionalBeforeGenericInfanciaTrack() {
        NationalProceduralActionProfileResolver actionProfileResolver = mock(NationalProceduralActionProfileResolver.class);
        NationalProceduralHeuristicRitoResolver resolver = new NationalProceduralHeuristicRitoResolver(actionProfileResolver);
        NationalProceduralPartyProfile partyProfile = new NationalProceduralPartyProfile(false, false, false, false, false, false, false, false, false, List.of(), null, null);

        RitoProcessual rito = resolver.resolve(Map.of(), "apuracao de ato infracional com medida socioeducativa para adolescente", partyProfile);

        assertEquals(RitoProcessual.INFANCIA_JUVENTUDE_INFRACIONAL, rito);
    }

    @Test
    void mustClassifyAdministrativoPadBeforeGenericFazendaTrack() {
        NationalProceduralActionProfileResolver actionProfileResolver = mock(NationalProceduralActionProfileResolver.class);
        NationalProceduralHeuristicRitoResolver resolver = new NationalProceduralHeuristicRitoResolver(actionProfileResolver);
        NationalProceduralPartyProfile partyProfile = new NationalProceduralPartyProfile(false, false, false, false, true, false, false, false, true, List.of(), null, null);

        RitoProcessual rito = resolver.resolve(Map.of(), "processo administrativo disciplinar com sindicancia e comissao processante", partyProfile);

        assertEquals(RitoProcessual.ADMINISTRATIVO_PAD, rito);
    }

}
