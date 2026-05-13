package com.tcc.pjb.backend.core.processo.painel.domain;

public record ProcessoPainelRotaTaticaItem(
        String code,
        String severity,
        String fundamento,
        String acao,
        String navigationPath
) {
    public ProcessoPainelRotaTaticaItem {
        severity = severity == null ? "ATENCAO" : severity;
        fundamento = fundamento == null ? "" : fundamento;
        acao = acao == null ? "" : acao;
        navigationPath = navigationPath == null ? "" : navigationPath;
    }


    public ProcessoPainelRotaTaticaItem(String code, String severity, String fundamento, String acao, boolean destacado, int prioridade) {
        this(code, severity, fundamento, acao, (destacado ? "highlight" : "") + ":" + prioridade);
    }
}
