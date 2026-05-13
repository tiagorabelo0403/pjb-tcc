package com.tcc.pjb.backend.core.distribuicao;

import com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice.PjbUniversalDigitalCoreDecision;
import java.util.List;

public final class PjbDistribuicaoStrategyResolver {

    private final List<PjbDistribuicaoStrategy> strategies;

    public PjbDistribuicaoStrategyResolver() {
        this(List.of(new PjbDistribuicaoNucleoDigitalStrategy(), new PjbDistribuicaoOrdinariaStrategy()));
    }

    public PjbDistribuicaoStrategyResolver(List<PjbDistribuicaoStrategy> strategies) {
        this.strategies = strategies == null || strategies.isEmpty()
                ? List.of(new PjbDistribuicaoNucleoDigitalStrategy(), new PjbDistribuicaoOrdinariaStrategy())
                : List.copyOf(strategies);
    }

    public PjbDistribuicaoStrategy resolve(PjbUniversalDigitalCoreDecision decision) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(decision))
                .findFirst()
                .orElseGet(PjbDistribuicaoOrdinariaStrategy::new);
    }

    public PjbDistribuicaoStrategy resolveByContext(PjbDistribuicaoContext context) {
        if (context == null) {
            return new PjbDistribuicaoOrdinariaStrategy();
        }
        var rito = context.ritoCanônico();

        // Jurisdição especializada obrigatória: eleitoral e militar não admitem núcleo digital
        if (rito != null && (rito.isEleitoral() || rito.isMilitar())) {
            return new PjbDistribuicaoOrdinariaStrategy();
        }

        // Alta complexidade probatória exige vara ordinária com juiz togado
        if (context.complexidadeProbatoria() >= 8) {
            return new PjbDistribuicaoOrdinariaStrategy();
        }

        // Processo sigiloso vai para núcleo digital somente se o juízo for 100% digital
        if (context.sigilo() && !context.juizo100Digital()) {
            return new PjbDistribuicaoOrdinariaStrategy();
        }

        // Núcleo digital ou juízo 100% digital configurado
        if (context.juizo100Digital() || context.nucleo40Disponivel()) {
            return new PjbDistribuicaoNucleoDigitalStrategy();
        }

        // Juizados especiais têm preferência de tramitação digital mesmo sem núcleo 4.0 explícito
        if (rito != null && rito.isJuizado()) {
            return new PjbDistribuicaoNucleoDigitalStrategy();
        }

        return new PjbDistribuicaoOrdinariaStrategy();
    }
}
