package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJudicialPlacementFinalizer {

    private final NationalProceduralForumLabelFactory forumLabelFactory;
    private final NationalProceduralForumAllocationResolver forumAllocationResolver;

    public NationalProceduralJudicialPlacementFinalizer(NationalProceduralForumLabelFactory forumLabelFactory,
                                                        NationalProceduralForumAllocationResolver forumAllocationResolver) {
        this.forumLabelFactory = Objects.requireNonNull(forumLabelFactory);
        this.forumAllocationResolver = Objects.requireNonNull(forumAllocationResolver);
    }

    NationalProceduralJudicialPlacement finalizePlacement(NationalProceduralJudicialPlacementContext context,
                                                          NationalProceduralJudicialPlacementSeed seed) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(seed);
        ProceduralForumAllocationReport forumAllocation = forumAllocationResolver.resolve(
                new NationalProceduralForumAllocationContext(
                        context.payload(),
                        context.corpus(),
                        context.canonical(),
                        context.competence(),
                        context.tipoJustica(),
                        context.ritoSugerido(),
                        context.actionProfile(),
                        context.juizadoDecision(),
                        seed.cidadeSugerida(),
                        seed.ufSugerida(),
                        seed.tribunalCodigo(),
                        seed.tribunalNome(),
                        seed.varaSugerida(),
                        seed.tipoVaraSugerido(),
                        seed.distribution()
                )
        );

        String cidadeSugerida = firstNonBlank(forumAllocation.comarcaSugerida(), seed.cidadeSugerida());
        String ufSugerida = firstNonBlank(forumAllocation.ufSugerida(), seed.ufSugerida());
        String tribunalCodigo = firstNonBlank(forumAllocation.tribunalCodigo(), seed.tribunalCodigo());
        String tribunalNome = firstNonBlank(forumAllocation.tribunalNome(), seed.tribunalNome(), tribunalCodigo);
        String varaSugerida = firstNonBlank(forumAllocation.unidadeJudiciariaCodigo(), forumAllocation.varaSugerida(), seed.varaSugerida());
        String tipoVaraSugerido = firstNonBlank(forumAllocation.tipoVaraSugerido(), seed.tipoVaraSugerido());
        String judicialSystem = firstNonBlank(forumAllocation.connectorSystem(), seed.judicialSystem());
        String foroSugerido = forumLabelFactory.buildForoLabel(cidadeSugerida, ufSugerida, context.tipoJustica());

        return new NationalProceduralJudicialPlacement(
                foroSugerido,
                cidadeSugerida,
                ufSugerida,
                tribunalCodigo,
                tribunalNome,
                varaSugerida,
                tipoVaraSugerido,
                judicialSystem,
                seed.distribution(),
                forumAllocation
        );
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }
}
