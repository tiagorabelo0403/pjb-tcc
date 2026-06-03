package com.tcc.pjb.backend.core.protocolo.completude.domain;

import com.tcc.pjb.backend.model.entity.enums.processual.completude.ProtocoloCompletudeStatus;
import com.tcc.pjb.backend.model.entity.enums.processual.completude.SeveridadeCompletude;
import java.util.List;

public record ResultadoValidacao(
        ProtocoloCompletudeStatus statusResultante,
        List<ViolacaoCompletude> violacoes,
        String versaoRegraAplicada
) {
    public boolean temBloqueante() {
        return violacoes.stream()
                .anyMatch(v -> v.severidade() == SeveridadeCompletude.BLOQUEANTE);
    }

    public List<ViolacaoCompletude> bloqueantes() {
        return violacoes.stream()
                .filter(v -> v.severidade() == SeveridadeCompletude.BLOQUEANTE)
                .toList();
    }

    public List<ViolacaoCompletude> advertencias() {
        return violacoes.stream()
                .filter(v -> v.severidade() == SeveridadeCompletude.ADVERTENCIA)
                .toList();
    }
}
