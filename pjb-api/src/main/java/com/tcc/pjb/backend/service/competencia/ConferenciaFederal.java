package com.tcc.pjb.backend.service.competencia;

enum ConferenciaFederal {
    NENHUMA(null, null),
    ALCADA_DO_JEF(
            "Causa federal dentro da alcada do juizado especial federal distribuida a unidade comum; conferir a competencia absoluta do JEF, as exclusoes e as partes admitidas (Lei 10.259, arts. 3 e 6)",
            "DISTRIBUICAO_CAUSA_FEDERAL_NA_ALCADA_FORA_DO_JUIZADO"),
    ACIDENTARIO_DA_JUSTICA_ESTADUAL(
            "Acao acidentaria contra o INSS e da Justica estadual (CF, art. 109, I; Sumula 15 do STJ); conferir a competencia antes de processar na Justica Federal",
            "DISTRIBUICAO_ACIDENTARIO_NA_JUSTICA_FEDERAL"),
    VALOR_DA_CAUSA_AUSENTE(
            "Causa federal sem valor da causa distribuida a unidade comum; conferir se ela e da competencia absoluta do JEF (Lei 10.259, art. 3)",
            "DISTRIBUICAO_CAUSA_FEDERAL_SEM_VALOR_DA_CAUSA");

    private final String fator;
    private final String eventoAuditoria;

    ConferenciaFederal(String fator, String eventoAuditoria) {
        this.fator = fator;
        this.eventoAuditoria = eventoAuditoria;
    }

    String fator() {
        return fator;
    }

    String eventoAuditoria() {
        return eventoAuditoria;
    }
}
