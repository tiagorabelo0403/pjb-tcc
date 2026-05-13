package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralForumAllocationBaseSeedResolver {

    private final NationalProceduralTerritorialAnalysisFactory territorialAnalysisFactory;
    private final NationalProceduralLinkageAnalysisFactory linkageAnalysisFactory;

    public NationalProceduralForumAllocationBaseSeedResolver(NationalProceduralTerritorialAnalysisFactory territorialAnalysisFactory,
                                                             NationalProceduralLinkageAnalysisFactory linkageAnalysisFactory) {
        this.territorialAnalysisFactory = Objects.requireNonNull(territorialAnalysisFactory);
        this.linkageAnalysisFactory = Objects.requireNonNull(linkageAnalysisFactory);
    }

    NationalProceduralForumAllocationBaseSeed resolve(NationalProceduralForumAllocationContext context) {
        Objects.requireNonNull(context);
        NationalProceduralTerritorialAnchor territorial = territorialAnalysisFactory.resolveTerritorialAnchor(
                context.payload(),
                context.corpus(),
                context.tipoJustica(),
                context.actionProfile().actionNature(),
                context.actionProfile().actionFamily(),
                context.cidadeBase(),
                context.ufBase()
        );
        NationalProceduralLinkageAnalysis linkage = linkageAnalysisFactory.resolve(context.payload(), context.corpus());
        String comarca = NationalProceduralRoutingSupport.firstNonBlank(
                context.distribution() != null ? context.distribution().comarca() : null,
                territorial.comarca(),
                context.cidadeBase()
        );
        String uf = NationalProceduralRoutingSupport.firstNonBlank(
                context.distribution() != null ? context.distribution().uf() : null,
                territorial.uf(),
                context.ufBase()
        );
        String tribunalCodigo = NationalProceduralRoutingSupport.firstNonBlank(
                context.distribution() != null ? context.distribution().tribunalCodigo() : null,
                context.tribunalCodigoBase(),
                context.canonical().tribunalCodigo(),
                context.competence().debug() != null ? NationalProceduralRoutingSupport.text(context.competence().debug().get("tribunalCodigo")) : null
        );
        String tribunalNome = NationalProceduralRoutingSupport.firstNonBlank(context.tribunalNomeBase(), context.canonical().tribunalNome(), tribunalCodigo);
        String unidadeCodigo = context.distribution() != null ? context.distribution().unidadeCodigo() : null;
        String varaSugerida = NationalProceduralRoutingSupport.firstNonBlank(unidadeCodigo, context.varaBase());
        String tipoVara = NationalProceduralRoutingSupport.firstNonBlank(
                context.distribution() != null ? context.distribution().tipoVara() : null,
                context.tipoVaraBase(),
                context.actionProfile().varaFamily()
        );
        double distributionScore = context.distribution() != null ? context.distribution().scoreFinal() : 0.0d;
        return new NationalProceduralForumAllocationBaseSeed(
                territorial,
                linkage,
                comarca,
                uf,
                tribunalCodigo,
                tribunalNome,
                unidadeCodigo,
                varaSugerida,
                tipoVara,
                distributionScore
        );
    }
}
