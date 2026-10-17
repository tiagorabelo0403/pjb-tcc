package com.tcc.pjb.backend.core.security.access;

import java.util.Locale;

public enum VinculoProcessualDoUsuario {
    PARTE,
    ADVOGADO_DO_AJUIZAMENTO,
    ADVOGADO_COM_PROCURACAO,
    ORGAO_NO_POLO,
    PROCESSO_CONCLUSO,
    TAREFA_ATRIBUIDA,
    FUNCAO_NA_UNIDADE,
    DESIGNACAO_FORMAL,
    PERITO_NOMEADO,
    OFICIAL_COM_MANDADO;

    public String motivo() {
        return "vinculo_" + name().toLowerCase(Locale.ROOT);
    }
}
