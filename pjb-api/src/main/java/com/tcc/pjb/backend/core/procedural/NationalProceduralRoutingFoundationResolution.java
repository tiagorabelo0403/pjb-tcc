package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

public record NationalProceduralRoutingFoundationResolution(
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
        NationalProceduralJuizadoDecision juizadoDecision
) {

    public NationalProceduralRoutingFoundationResolution {
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
    }
}
