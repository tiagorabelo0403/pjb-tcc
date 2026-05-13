package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.tribunal.distribuicao.ConfiguracaoDistribuicaoVaraService;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralForumAllocationProfileResolver {

    private final ConfiguracaoDistribuicaoVaraService configuracaoDistribuicaoVaraService;

    public NationalProceduralForumAllocationProfileResolver(ConfiguracaoDistribuicaoVaraService configuracaoDistribuicaoVaraService) {
        this.configuracaoDistribuicaoVaraService = Objects.requireNonNull(configuracaoDistribuicaoVaraService);
    }

    ConfiguracaoDistribuicaoVaraService.PerfilVara resolve(NationalProceduralForumAllocationContext context,
                                                           NationalProceduralForumAllocationClassSeed classSeed,
                                                           NationalProceduralForumAllocationBaseSeed baseSeed) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(classSeed);
        Objects.requireNonNull(baseSeed);
        ConfiguracaoDistribuicaoVaraService.PerfilVara perfil = null;
        if (!NationalProceduralRoutingSupport.isBlank(baseSeed.unidadeCodigo())) {
            perfil = configuracaoDistribuicaoVaraService.buscarPerfil(baseSeed.unidadeCodigo()).orElse(null);
        }
        if (perfil != null) {
            return perfil;
        }
        if (NationalProceduralRoutingSupport.isBlank(baseSeed.comarca()) || NationalProceduralRoutingSupport.isBlank(baseSeed.uf())) {
            return null;
        }
        ConfiguracaoDistribuicaoVaraService.FiltroDistribuicao filtro = new ConfiguracaoDistribuicaoVaraService.FiltroDistribuicao(
                NationalProceduralRoutingSupport.firstNonBlank(
                        NationalProceduralRoutingSupport.text(context.payload().get("materia")),
                        NationalProceduralRoutingSupport.text(context.payload().get("ramoDireito")),
                        context.actionProfile().actionNature()
                ),
                context.ritoSugerido(),
                classSeed.classeTpu() != null ? String.valueOf(classSeed.classeTpu().codigoTpu()) : context.canonical().classeTpuCodigo(),
                NationalProceduralRoutingSupport.firstNonBlank(
                        NationalProceduralRoutingSupport.text(context.payload().get("assunto")),
                        NationalProceduralRoutingSupport.text(context.payload().get("objetoProcessual"))
                ),
                NationalProceduralRoutingSupport.decimal(context.payload().get("valorCausa")),
                context.juizadoDecision().admiteJuizado(),
                true,
                baseSeed.uf(),
                baseSeed.comarca(),
                NationalProceduralRoutingSupport.bool(context.payload().get("casoUrgente")),
                NationalProceduralRoutingSupport.bool(context.payload().get("preferenciaDigital")),
                false
        );
        List<ConfiguracaoDistribuicaoVaraService.PerfilVara> candidatas = configuracaoDistribuicaoVaraService.varasDisponiveisNaComarca(baseSeed.comarca(), filtro);
        if (candidatas.isEmpty()) {
            return null;
        }
        return candidatas.stream()
                .filter(item -> context.tipoJustica() == null || item.tipoJustica() == null || item.tipoJustica() == context.tipoJustica())
                .findFirst()
                .orElse(candidatas.get(0));
    }
}
