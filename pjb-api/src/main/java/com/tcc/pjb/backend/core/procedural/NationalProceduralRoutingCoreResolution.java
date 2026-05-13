package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

public record NationalProceduralRoutingCoreResolution(
        Map<String, Object> payload,
        String corpus,
        String sourceLabel,
        NationalProceduralPartyProfile partyProfile,
        CanonicalRitoSelector.SelectedRito selectedRito,
        ProceduralCanonicalResolver.CanonicalContext canonical,
        CompetenceResolveResponse competence,
        NationalProceduralActionProfile actionProfile,
        String probatoryProfile,
        TetoProcessualService.DiagnosticoTetoProcessual teto,
        NationalProceduralJuizadoDecision juizadoDecision,
        String complexityBand,
        String ritoSugerido,
        TipoJustica tipoJustica,
        String proceduralRegime,
        String proceduralTrack,
        NationalProceduralJudicialPlacement judicialPlacement,
        NationalProceduralReviewSynthesis reviewSynthesis
) {

    public NationalProceduralRoutingCoreResolution {
        payload = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        corpus = Objects.requireNonNullElse(corpus, "");
        sourceLabel = Objects.requireNonNullElse(sourceLabel, "context");
        Objects.requireNonNull(partyProfile);
        Objects.requireNonNull(selectedRito);
        Objects.requireNonNull(canonical);
        Objects.requireNonNull(competence);
        Objects.requireNonNull(actionProfile);
        Objects.requireNonNull(probatoryProfile);
        Objects.requireNonNull(teto);
        Objects.requireNonNull(juizadoDecision);
        Objects.requireNonNull(complexityBand);
        Objects.requireNonNull(ritoSugerido);
        Objects.requireNonNull(tipoJustica);
        Objects.requireNonNull(proceduralRegime);
        Objects.requireNonNull(proceduralTrack);
        Objects.requireNonNull(judicialPlacement);
        Objects.requireNonNull(reviewSynthesis);
    }
}
