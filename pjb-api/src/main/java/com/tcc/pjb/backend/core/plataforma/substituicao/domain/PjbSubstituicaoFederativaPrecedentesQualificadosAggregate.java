package com.tcc.pjb.backend.core.plataforma.substituicao.domain;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaPrecedentesQualificadosAggregate(
        int scoreNacional,
        boolean malhaPrecedentesPronta,
        boolean incidentesMassaConectados,
        boolean temasAfetadosGovernados,
        boolean sobrestamentoGovernado,
        boolean precedentesVinculantesConectados,
        int tribunaisProntos,
        List<PjbSubstituicaoFederativaPrecedentesQualificadosTribunal> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaPrecedentesQualificadosAggregate {
        scoreNacional = Math.max(0, Math.min(100, scoreNacional));
        tribunaisProntos = Math.max(0, tribunaisProntos);
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
