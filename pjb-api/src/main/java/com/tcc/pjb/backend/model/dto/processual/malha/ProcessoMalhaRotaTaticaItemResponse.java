package com.tcc.pjb.backend.model.dto.processual.malha;

public record ProcessoMalhaRotaTaticaItemResponse(
        String code,
        String severity,
        String fundamento,
        String acao,
        String navigationPath
) {
    public ProcessoMalhaRotaTaticaItemResponse {
        code = code == null ? "" : code.trim();
        severity = severity == null ? "ATENCAO" : severity.trim();
        fundamento = fundamento == null ? "" : fundamento.trim();
        acao = acao == null ? "" : acao.trim();
        navigationPath = navigationPath == null ? "" : navigationPath.trim();
    }
}
