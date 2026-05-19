package com.tcc.pjb.backend.model.entity.enums;

public enum CanalCiencia {

    PORTAL_PJB("Portal PJB", true, true),
    DJE_TRIBUNAL("DJe do Tribunal", false, false),
    DJE_STF("DJe do STF", false, false),
    DJE_STJ("DJe do STJ", false, false),
    DJE_TST("DJe do TST", false, false),
    DJE_TSE("DJe do TSE", false, false),
    EMAIL("E-mail", true, false),
    WHATSAPP("WhatsApp", true, true),
    MANDADO_JUDICIAL("Mandado judicial", false, true),
    CARTA_AR("Carta com AR", false, false),
    EDITAL("Edital", false, false),
    NOTIFICACAO_TRABALHISTA("Notificação postal CLT", false, false),
    AUTOMATICO("Automático", true, false);

    private final String label;
    private final boolean registraIp;
    private final boolean exigeConfirmacaoAtiva;

    CanalCiencia(String label, boolean registraIp, boolean exigeConfirmacaoAtiva) {
        this.label = label;
        this.registraIp = registraIp;
        this.exigeConfirmacaoAtiva = exigeConfirmacaoAtiva;
    }

    public String label() { return label; }
    public boolean registraIp() { return registraIp; }
    public boolean exigeConfirmacaoAtiva() { return exigeConfirmacaoAtiva; }

    public boolean isDje() {
        return name().startsWith("DJE_");
    }

    public boolean isPostal() {
        return this == CARTA_AR || this == MANDADO_JUDICIAL
                || this == NOTIFICACAO_TRABALHISTA || this == EDITAL;
    }

    public boolean diesAQuoNoDiaUtilSeguinte() {
        return isDje();
    }
}
