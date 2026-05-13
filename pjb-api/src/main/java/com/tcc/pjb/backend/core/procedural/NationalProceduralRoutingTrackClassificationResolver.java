package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingTrackClassificationResolver {

    private final NationalProceduralComplexityBandResolver complexityBandResolver;
    private final NationalProceduralTipoJusticaResolver tipoJusticaResolver;
    private final NationalProceduralClassificationResolver classificationResolver;

    public NationalProceduralRoutingTrackClassificationResolver(NationalProceduralComplexityBandResolver complexityBandResolver,
                                                               NationalProceduralTipoJusticaResolver tipoJusticaResolver,
                                                               NationalProceduralClassificationResolver classificationResolver) {
        this.complexityBandResolver = Objects.requireNonNull(complexityBandResolver);
        this.tipoJusticaResolver = Objects.requireNonNull(tipoJusticaResolver);
        this.classificationResolver = Objects.requireNonNull(classificationResolver);
    }

    NationalProceduralRoutingClassificationSnapshot resolve(NationalProceduralRoutingFoundationResolution foundation) {
        Objects.requireNonNull(foundation);
        String complexityBand = complexityBandResolver.resolve(
                new NationalProceduralComplexityContext(
                        foundation.actionProfile(),
                        foundation.probatoryProfile(),
                        foundation.partyProfile(),
                        foundation.payload(),
                        foundation.teto(),
                        foundation.juizadoDecision()
                )
        );
        String ritoSugerido = NationalProceduralRoutingSupport.firstNonBlank(
                foundation.juizadoDecision().ritoOverride(),
                foundation.selectedRito().rito() != null ? foundation.selectedRito().rito().name() : null,
                foundation.competence().ritoSugerido(),
                foundation.actionProfile().defaultRito(),
                "COMUM_ORDINARIO"
        );
        TipoJustica tipoJustica = tipoJusticaResolver.resolve(
                foundation.payload().get("tipoJustica"),
                foundation.competence(),
                foundation.canonical(),
                ritoSugerido,
                foundation.partyProfile()
        );
        String proceduralRegime = classificationResolver.resolveProceduralRegime(
                ritoSugerido,
                foundation.actionProfile(),
                foundation.juizadoDecision()
        );
        String proceduralTrack = classificationResolver.resolveProceduralTrack(
                ritoSugerido,
                foundation.actionProfile(),
                foundation.juizadoDecision(),
                tipoJustica
        );
        return new NationalProceduralRoutingClassificationSnapshot(
                complexityBand,
                ritoSugerido,
                tipoJustica,
                proceduralRegime,
                proceduralTrack
        );
    }
}
