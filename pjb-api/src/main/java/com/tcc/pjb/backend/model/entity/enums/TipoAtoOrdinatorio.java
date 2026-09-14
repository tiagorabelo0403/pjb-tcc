package com.tcc.pjb.backend.model.entity.enums;

public enum TipoAtoOrdinatorio {

    JUNTADA_PETICAO_DOCUMENTO(
            "Juntada de petição ou documento",
            "CPC art. 203, §4º — juntada, ato ordinatório expressamente citado na lei"),
    VISTA_PARTE_CONTRARIA(
            "Vista à parte contrária",
            "CPC art. 203, §4º — vista obrigatória, ato ordinatório expressamente citado na lei"),
    VISTA_AMBAS_PARTES(
            "Vista simultânea às partes",
            "CPC art. 203, §4º — vista obrigatória, aplicada às duas partes"),
    AGUARDE_DECURSO_PRAZO(
            "Aguarde-se o decurso do prazo em curso",
            "CPC art. 203, §4º — ato de mero impulso processual, sem juízo de valor"),
    REMESSA_ORGAO_AUXILIAR(
            "Remessa dos autos a órgão auxiliar para cumprimento de decisão já proferida",
            "CPC art. 203, §4º — cumprimento de decisão já tomada, sem nova determinação"),
    EXPEDICAO_CUMPRIMENTO_DECISAO(
            "Expedição de ofício/mandado em cumprimento de decisão já proferida",
            "CPC art. 203, §4º — mero cumprimento, sem decidir nada novo");

    private final String label;
    private final String fundamentoLegal;

    TipoAtoOrdinatorio(String label, String fundamentoLegal) {
        this.label = label;
        this.fundamentoLegal = fundamentoLegal;
    }

    public String label() {
        return label;
    }

    public String fundamentoLegal() {
        return fundamentoLegal;
    }
}
