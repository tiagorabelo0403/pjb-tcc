package com.tcc.pjb.backend.model.entity.enums;

import java.util.Map;
import static java.util.Map.entry;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.tcc.pjb.backend.core.util.EnumText;

public enum StatusProcesso {

    DISTRIBUIDO,
    CITACAO_REALIZADA,
    BAIXADO,
    CONTESTACAO_APRESENTADA,
    AUDIENCIA_DESIGNADA,
    AUDIENCIA_REALIZADA,
    SENTENCA_PROFERIDA,
    RECURSO_INTERPOSTO,
    TRANSITO_EM_JULGADO,
    ARQUIVADO,
    JULGADO,
    EMBARGOS_DECLARACAO,
    CUMPRIMENTO_SENTENCA,
    PROTOCOLADO,
    AGUARDANDO_PARECER,
    EM_ANDAMENTO,
    CONCLUSO,
    AUDIENCIA_CUSTODIA_PENDENTE,
    PRESO_PROVISORIO,
    LIBERDADE_PROVISORIA,
    HOMOLOGACAO_ACORDO_PENDENTE,
    ACORDO_HOMOLOGADO,
    EXECUCAO_TRABALHISTA,
    SUSPENSO_TEMA_REPERCUSSAO,
    SUSPENSO_RECURSO_REPETITIVO,
    
    SUSPENSO_POR_OBITO;

    public static final StatusProcesso SUSPENSO = SUSPENSO_POR_OBITO;

    private static final Map<String, StatusProcesso> ALIASES = Map.ofEntries(
            entry("EM_TRAMITE", EM_ANDAMENTO),
            entry("TRAMITANDO", EM_ANDAMENTO),
            entry("SENTENCIADO", SENTENCA_PROFERIDA),
            entry("SENTENCA", SENTENCA_PROFERIDA),
            entry("TRANSITO", TRANSITO_EM_JULGADO),
            entry("BAIXA", BAIXADO),
            entry("ARQUIVAMENTO", ARQUIVADO),
            entry("CONCLUSO", CONCLUSO),
            entry("SUSPENSO", SUSPENSO_POR_OBITO),
            entry("SOBRESTADO_TEMA", SUSPENSO_TEMA_REPERCUSSAO),
            entry("SOBRESTADO_REPETITIVO", SUSPENSO_RECURSO_REPETITIVO)
    );

    
    public static StatusProcesso fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return EM_ANDAMENTO;
        }
        String token = EnumText.normalizeToken(raw);
        if (token.isBlank()) {
            return EM_ANDAMENTO;
        }

        StatusProcesso alias = ALIASES.get(token);
        if (alias != null) {
            return alias;
        }

        try {
            return StatusProcesso.valueOf(token);
        } catch (Exception ignored) {
            return EM_ANDAMENTO;
        }
    }

    
    @JsonCreator
    public static StatusProcesso jsonCreator(String raw) {
        return fromString(raw);
    }

    public boolean isEncerrado() {
        return this == JULGADO
                || this == ARQUIVADO
                || this == TRANSITO_EM_JULGADO;
    }

    public boolean isAtivo() {
        return !isEncerrado();
    }



    public boolean isPosDecisao() {
        return this == SENTENCA_PROFERIDA
                || this == JULGADO
                || this == RECURSO_INTERPOSTO
                || this == EMBARGOS_DECLARACAO
                || this == TRANSITO_EM_JULGADO
                || this == CUMPRIMENTO_SENTENCA;
    }

    public boolean isArquivadoOuBaixado() {
        return this == ARQUIVADO || this == BAIXADO;
    }

    public boolean isTransitado() {
        return this == TRANSITO_EM_JULGADO;
    }

    public boolean isRecursalOuEmbargos() {
        return this == RECURSO_INTERPOSTO || this == EMBARGOS_DECLARACAO;
    }

    public boolean isExecutorio() {
        return this == CUMPRIMENTO_SENTENCA;
    }

}
