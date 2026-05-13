package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJudicialPlacementSeedResolver {

    private final NationalProceduralDistributionResolver distributionResolver;
    private final NationalProceduralForumLabelFactory forumLabelFactory;

    public NationalProceduralJudicialPlacementSeedResolver(NationalProceduralDistributionResolver distributionResolver,
                                                           NationalProceduralForumLabelFactory forumLabelFactory) {
        this.distributionResolver = Objects.requireNonNull(distributionResolver);
        this.forumLabelFactory = Objects.requireNonNull(forumLabelFactory);
    }

    NationalProceduralJudicialPlacementSeed resolve(NationalProceduralJudicialPlacementContext context) {
        Objects.requireNonNull(context);
        Map<String, Object> payload = context.payload() == null ? Map.of() : context.payload();
        String cidadeSugerida = firstNonBlank(
                text(payload.get("comarcaAutor")),
                text(payload.get("cidadeAutor")),
                text(payload.get("foro")),
                text(payload.get("comarcaReu")),
                text(payload.get("cidadeReu"))
        );
        String ufSugerida = firstNonBlank(text(payload.get("ufAutor")), text(payload.get("ufReu")));

        NationalProceduralDistributionSuggestion distribution = distributionResolver.resolve(
                new NationalProceduralDistributionContext(
                        payload,
                        context.canonical(),
                        context.competence(),
                        context.ritoSugerido(),
                        context.tipoJustica(),
                        context.juizadoDecision(),
                        cidadeSugerida,
                        ufSugerida,
                        context.actionProfile()
                )
        ).orElse(null);
        if (distribution != null) {
            cidadeSugerida = firstNonBlank(distribution.comarca(), cidadeSugerida);
            ufSugerida = firstNonBlank(distribution.uf(), ufSugerida);
        }

        String tribunalCodigo = resolveTribunalCodigo(payload, context.canonical(), context.competence(), distribution);
        String tribunalNome = firstNonBlank(distribution != null ? distribution.tribunalCodigo() : null, context.canonical().tribunalNome(), tribunalCodigo);
        String varaSugerida = firstNonBlank(
                distribution != null ? distribution.unidadeCodigo() : null,
                forumLabelFactory.buildVaraLabel(context.ritoSugerido(), context.actionProfile(), context.tipoJustica(), context.juizadoDecision(), payload)
        );
        String tipoVaraSugerido = firstNonBlank(distribution != null ? distribution.tipoVara() : null, context.actionProfile().varaFamily(), context.proceduralTrack());
        String judicialSystem = firstNonBlank(context.canonical().judicialSystemPreferido(), ProceduralRitoNames.suggestedProtocolSystem(context.ritoSugerido(), context.tipoJustica().name()));

        return new NationalProceduralJudicialPlacementSeed(
                cidadeSugerida,
                ufSugerida,
                tribunalCodigo,
                tribunalNome,
                varaSugerida,
                tipoVaraSugerido,
                judicialSystem,
                distribution
        );
    }

    private static String resolveTribunalCodigo(Map<String, Object> payload,
                                                ProceduralCanonicalResolver.CanonicalContext canonical,
                                                CompetenceResolveResponse competence,
                                                NationalProceduralDistributionSuggestion distribution) {
        return firstNonBlank(
                distribution != null ? distribution.tribunalCodigo() : null,
                canonical.tribunalCodigo(),
                competence.debug() != null ? text(competence.debug().get("tribunalCodigo")) : null,
                text(payload.get("tribunalCodigo"))
        );
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static String text(Object value) {
        return NationalProceduralRoutingSupport.text(value);
    }
}
