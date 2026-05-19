package com.tcc.pjb.backend.core.processo.estado.domain;

import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ProcessoEstadoMaquina {

    private static final Map<StatusProcesso, Set<StatusProcesso>> TRANSICOES = Map.ofEntries(
            Map.entry(StatusProcesso.PETICIONADO,
                    EnumSet.of(StatusProcesso.AUTUADO)),
            Map.entry(StatusProcesso.AUTUADO,
                    EnumSet.of(StatusProcesso.DISTRIBUIDO)),
            Map.entry(StatusProcesso.DISTRIBUIDO,
                    EnumSet.of(StatusProcesso.CONCLUSO_JUIZ, StatusProcesso.CONCLUSO_RELATOR,
                            StatusProcesso.EM_INSTRUCAO, StatusProcesso.SUSPENSO)),
            Map.entry(StatusProcesso.CONCLUSO_JUIZ,
                    EnumSet.of(StatusProcesso.SENTENCIADO, StatusProcesso.DISTRIBUIDO,
                            StatusProcesso.SUSPENSO, StatusProcesso.EM_INSTRUCAO)),
            Map.entry(StatusProcesso.CONCLUSO_RELATOR,
                    EnumSet.of(StatusProcesso.PAUTADO, StatusProcesso.SUSPENSO,
                            StatusProcesso.SOBRESTADO)),
            Map.entry(StatusProcesso.EM_INSTRUCAO,
                    EnumSet.of(StatusProcesso.SANEADO, StatusProcesso.CONCLUSO_JUIZ,
                            StatusProcesso.SUSPENSO)),
            Map.entry(StatusProcesso.SANEADO,
                    EnumSet.of(StatusProcesso.PAUTADO, StatusProcesso.EM_INSTRUCAO,
                            StatusProcesso.CONCLUSO_JUIZ)),
            Map.entry(StatusProcesso.PAUTADO,
                    EnumSet.of(StatusProcesso.EM_JULGAMENTO, StatusProcesso.SUSPENSO,
                            StatusProcesso.SOBRESTADO)),
            Map.entry(StatusProcesso.EM_JULGAMENTO,
                    EnumSet.of(StatusProcesso.SENTENCIADO, StatusProcesso.PAUTADO,
                            StatusProcesso.SUSPENSO)),
            Map.entry(StatusProcesso.SUSPENSO,
                    EnumSet.of(StatusProcesso.DISTRIBUIDO, StatusProcesso.CONCLUSO_JUIZ,
                            StatusProcesso.EM_INSTRUCAO, StatusProcesso.PAUTADO)),
            Map.entry(StatusProcesso.SOBRESTADO,
                    EnumSet.of(StatusProcesso.PAUTADO, StatusProcesso.EM_JULGAMENTO,
                            StatusProcesso.DISTRIBUIDO)),
            Map.entry(StatusProcesso.PARALISADO,
                    EnumSet.of(StatusProcesso.DISTRIBUIDO)),
            Map.entry(StatusProcesso.SENTENCIADO,
                    EnumSet.of(StatusProcesso.TRANSITADO, StatusProcesso.EM_CUMPRIMENTO,
                            StatusProcesso.DISTRIBUIDO)),
            Map.entry(StatusProcesso.TRANSITADO,
                    EnumSet.of(StatusProcesso.EM_CUMPRIMENTO, StatusProcesso.ARQUIVADO)),
            Map.entry(StatusProcesso.EM_CUMPRIMENTO,
                    EnumSet.of(StatusProcesso.ARQUIVADO)),
            Map.entry(StatusProcesso.EXTINTO_SEM_RESOLUCAO,
                    EnumSet.of(StatusProcesso.ARQUIVADO)),
            Map.entry(StatusProcesso.ARQUIVADO,
                    EnumSet.noneOf(StatusProcesso.class))
    );

    public TransicaoEstadoResult avaliar(StatusProcesso atual, StatusProcesso proximo) {
        Set<StatusProcesso> possiveis = TRANSICOES.getOrDefault(atual, EnumSet.noneOf(StatusProcesso.class));
        if (possiveis.contains(proximo)) {
            return new TransicaoEstadoResult.Permitida(atual, proximo);
        }
        return new TransicaoEstadoResult.Negada(atual, proximo,
                "Transição de " + atual + " para " + proximo + " não é permitida.");
    }

    public boolean podeTransitar(StatusProcesso atual, StatusProcesso proximo) {
        return avaliar(atual, proximo) instanceof TransicaoEstadoResult.Permitida;
    }

    public Set<StatusProcesso> transicoesPossiveis(StatusProcesso atual) {
        return TRANSICOES.getOrDefault(atual, EnumSet.noneOf(StatusProcesso.class));
    }
}
