package com.tcc.pjb.backend.service.exception.enums;

public enum TipoViolacaoTerritorial {
    BASE_TERRITORIAL_INSUFICIENTE("TERR-BASE-001", "Base territorial insuficiente"),
    DIVERGENCIA_UF("TERR-UF-002", "Divergência territorial de UF"),
    DIVERGENCIA_COMARCA("TERR-COMARCA-003", "Divergência territorial de comarca"),
    DIVERGENCIA_VARA("TERR-VARA-004", "Divergência territorial de vara"),
    DIVERGENCIA_FORO_FEDERAL("TERR-FORO-005", "Divergência territorial de foro ou subseção"),
    REVISAO_TERRITORIAL_OBRIGATORIA("TERR-REVISAO-006", "Revisão territorial obrigatória");

    private final String codigo;
    private final String tituloJuridico;

    TipoViolacaoTerritorial(String codigo, String tituloJuridico) {
        this.codigo = codigo;
        this.tituloJuridico = tituloJuridico;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getTituloJuridico() {
        return tituloJuridico;
    }
}
