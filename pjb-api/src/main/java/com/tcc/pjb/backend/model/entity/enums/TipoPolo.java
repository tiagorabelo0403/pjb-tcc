package com.tcc.pjb.backend.model.entity.enums;

public enum TipoPolo {

    ATIVO("Polo Ativo", true, true),
    PASSIVO("Polo Passivo", true, true),
    TERCEIRO("Terceiro", false, true),
    MINISTERIO_PUBLICO("Ministério Público", true, true),
    DEFENSORIA("Defensoria Pública", true, true),
    PROCURADORIA("Procuradoria", true, true),
    FAZENDA_PUBLICA("Fazenda Pública", true, true),
    ASSISTENTE_SIMPLES("Assistente Simples", false, true),
    ASSISTENTE_LITISCONSORCIAL("Assistente Litisconsorcial", true, true),
    AMICUS_CURIAE("Amicus Curiae", false, false),
    CURADOR_ESPECIAL("Curador Especial", false, true),
    INTERVENIENTE("Interveniente", false, true);

    private final String label;
    private final boolean podeRecorrer;
    private final boolean recebeCiencia;

    TipoPolo(String label, boolean podeRecorrer, boolean recebeCiencia) {
        this.label = label;
        this.podeRecorrer = podeRecorrer;
        this.recebeCiencia = recebeCiencia;
    }

    public String label() {
        return label;
    }

    public boolean podeRecorrer() {
        return podeRecorrer;
    }

    public boolean recebeCiencia() {
        return recebeCiencia;
    }

    public boolean isPrincipal() {
        return this == ATIVO || this == PASSIVO;
    }

    public boolean isInterveniente() {
        return this == TERCEIRO
                || this == ASSISTENTE_SIMPLES
                || this == ASSISTENTE_LITISCONSORCIAL
                || this == AMICUS_CURIAE
                || this == INTERVENIENTE;
    }

    public boolean isInstitucional() {
        return this == MINISTERIO_PUBLICO
                || this == DEFENSORIA
                || this == PROCURADORIA
                || this == FAZENDA_PUBLICA;
    }
}
