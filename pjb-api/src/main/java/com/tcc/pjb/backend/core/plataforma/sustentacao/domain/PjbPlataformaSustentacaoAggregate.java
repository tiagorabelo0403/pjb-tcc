package com.tcc.pjb.backend.core.plataforma.sustentacao.domain;

import java.time.Instant;
import java.util.List;

public record PjbPlataformaSustentacaoAggregate(
        int scoreGeral,
        boolean aptoPreBuild,
        int eixosProntos,
        int totalEixos,
        List<PjbPlataformaSustentacaoEixo> eixos,
        List<PjbPlataformaSustentacaoModulo> modulos,
        List<PjbPlataformaSustentacaoCenario> cenariosDourados,
        List<String> bloqueadoresCriticos,
        List<String> proximasAcoes,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbPlataformaSustentacaoAggregate {
        eixos = eixos == null ? List.of() : List.copyOf(eixos);
        modulos = modulos == null ? List.of() : List.copyOf(modulos);
        cenariosDourados = cenariosDourados == null ? List.of() : List.copyOf(cenariosDourados);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
