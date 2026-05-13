package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.tribunal.distribuicao.ConfiguracaoDistribuicaoVaraService;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralForumAllocationSeedResolver {

    private final NationalProceduralForumAllocationClassSeedResolver classSeedResolver;
    private final NationalProceduralForumAllocationBaseSeedResolver baseSeedResolver;
    private final NationalProceduralForumAllocationProfileResolver profileResolver;

    public NationalProceduralForumAllocationSeedResolver(NationalProceduralForumAllocationClassSeedResolver classSeedResolver,
                                                         NationalProceduralForumAllocationBaseSeedResolver baseSeedResolver,
                                                         NationalProceduralForumAllocationProfileResolver profileResolver) {
        this.classSeedResolver = Objects.requireNonNull(classSeedResolver);
        this.baseSeedResolver = Objects.requireNonNull(baseSeedResolver);
        this.profileResolver = Objects.requireNonNull(profileResolver);
    }

    NationalProceduralForumAllocationSeed resolve(NationalProceduralForumAllocationContext context) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(context.canonical());
        Objects.requireNonNull(context.tipoJustica());
        Objects.requireNonNull(context.actionProfile());
        Objects.requireNonNull(context.juizadoDecision());
        NationalProceduralForumAllocationClassSeed classSeed = classSeedResolver.resolve(context);
        NationalProceduralForumAllocationBaseSeed baseSeed = baseSeedResolver.resolve(context);
        ConfiguracaoDistribuicaoVaraService.PerfilVara perfil = resolveFallbackPerfil(context, classSeed, baseSeed);

        String unidadeCodigo = baseSeed.unidadeCodigo();
        String varaSugerida = baseSeed.varaSugerida();
        String tipoVara = baseSeed.tipoVara();
        String tribunalCodigo = baseSeed.tribunalCodigo();
        String tribunalNome = baseSeed.tribunalNome();
        String comarca = baseSeed.comarca();
        String uf = baseSeed.uf();
        double distributionScore = baseSeed.distributionScore();

        if (perfil != null) {
            unidadeCodigo = NationalProceduralRoutingSupport.firstNonBlank(perfil.varaId(), unidadeCodigo);
            varaSugerida = NationalProceduralRoutingSupport.firstNonBlank(perfil.varaDescricao(), unidadeCodigo, varaSugerida);
            tipoVara = NationalProceduralRoutingSupport.firstNonBlank(perfil.tipoVara() != null ? perfil.tipoVara().name() : null, tipoVara);
            tribunalCodigo = NationalProceduralRoutingSupport.firstNonBlank(perfil.tribunalCodigo(), tribunalCodigo);
            tribunalNome = NationalProceduralRoutingSupport.firstNonBlank(tribunalNome, tribunalCodigo);
            comarca = NationalProceduralRoutingSupport.firstNonBlank(perfil.comarcaId(), comarca);
            uf = NationalProceduralRoutingSupport.firstNonBlank(perfil.uf(), uf);
            distributionScore = Math.max(distributionScore, perfil.scoreDisponibilidade());
        }

        return new NationalProceduralForumAllocationSeed(
                classSeed.classeTpu(),
                baseSeed.territorial(),
                baseSeed.linkage(),
                comarca,
                uf,
                tribunalCodigo,
                tribunalNome,
                unidadeCodigo,
                varaSugerida,
                tipoVara,
                distributionScore,
                perfil
        );
    }

    private ConfiguracaoDistribuicaoVaraService.PerfilVara resolveFallbackPerfil(NationalProceduralForumAllocationContext context,
                                                                                  NationalProceduralForumAllocationClassSeed classSeed,
                                                                                  NationalProceduralForumAllocationBaseSeed baseSeed) {
        return profileResolver.resolve(context, classSeed, baseSeed);
    }
}
