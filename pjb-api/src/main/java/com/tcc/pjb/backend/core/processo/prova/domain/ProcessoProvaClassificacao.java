package com.tcc.pjb.backend.core.processo.prova.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.List;

public record ProcessoProvaClassificacao(
        String naturezaProbatoria,
        boolean sensivel,
        boolean exigeReforcoSigilo,
        boolean cooperacaoInstitucional,
        boolean aptaCompartilhamentoControlado,
        NivelSigilo nivelSigiloEfetivo,
        List<String> marcadores
) {
    public ProcessoProvaClassificacao {
        nivelSigiloEfetivo = nivelSigiloEfetivo == null ? NivelSigilo.PUBLICO : nivelSigiloEfetivo;
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }
}
