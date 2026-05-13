package com.tcc.pjb.backend.core.processo.juizado.procedural;

import com.tcc.pjb.backend.core.procedural.NationalProceduralActionProfile;
import com.tcc.pjb.backend.core.procedural.NationalProceduralPartyProfile;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJuizadoDecisionResolver {

    private final NationalProceduralJuizadoExclusionResolver exclusionResolver;
    private final NationalProceduralJuizadoTrackResolver trackResolver;

    public NationalProceduralJuizadoDecisionResolver(NationalProceduralJuizadoExclusionResolver exclusionResolver,
                                                     NationalProceduralJuizadoTrackResolver trackResolver) {
        this.exclusionResolver = Objects.requireNonNull(exclusionResolver);
        this.trackResolver = Objects.requireNonNull(trackResolver);
    }

    public NationalProceduralJuizadoDecision resolve(Map<String, Object> payload,
                                              CompetenceResolveResponse competence,
                                              NationalProceduralActionProfile actionProfile,
                                              NationalProceduralPartyProfile partyProfile,
                                              TetoProcessualService.DiagnosticoTetoProcessual teto,
                                              String corpus) {
        NationalProceduralJuizadoDecisionContext context = new NationalProceduralJuizadoDecisionContext(payload, competence, actionProfile, partyProfile, teto, corpus);
        return exclusionResolver.resolve(context).orElseGet(() -> trackResolver.resolve(context));
    }
}
