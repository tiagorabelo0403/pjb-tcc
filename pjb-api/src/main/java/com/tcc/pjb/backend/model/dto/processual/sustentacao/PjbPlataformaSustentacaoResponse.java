package com.tcc.pjb.backend.model.dto.processual.sustentacao;

import java.time.Instant;
import java.util.List;

public record PjbPlataformaSustentacaoResponse(
        int scoreGeral,
        boolean aptoPreBuild,
        int eixosProntos,
        int totalEixos,
        List<PjbPlataformaSustentacaoEixoResponse> eixos,
        List<PjbPlataformaSustentacaoModuloResponse> modulos,
        List<PjbPlataformaSustentacaoCenarioResponse> cenariosDourados,
        List<String> bloqueadoresCriticos,
        List<String> proximasAcoes,
        List<String> fundamentos,
        Instant geradoEm
) {
}
