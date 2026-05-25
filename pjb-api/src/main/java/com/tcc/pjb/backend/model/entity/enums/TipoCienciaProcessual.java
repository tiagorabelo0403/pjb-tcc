package com.tcc.pjb.backend.model.entity.enums;

public enum TipoCienciaProcessual {

    CITACAO_INICIAL(15, true, true, "Citação inicial"),
    INTIMACAO_DECISAO(15, true, true, "Intimacao de decisao"),
    INTIMACAO_DECISAO_INTERLOCUTORIA(15, true, true, "Intimação de decisão"),
    INTIMACAO_SENTENCA(15, true, true, "Intimação de sentença"),
    INTIMACAO_ACORDAO(15, true, true, "Intimação de acórdão"),
    INTIMACAO_EMBARGOS_DECLARACAO(5, true, true, "Intimação — ED"),
    INTIMACAO_AGRAVO_INTERNO(15, true, true, "Intimação — agravo interno"),
    INTIMACAO_RESP(15, true, true, "Intimação — REsp"),
    INTIMACAO_RE(15, true, true, "Intimação — RE"),
    INTIMACAO_TRANSITO_JULGADO(0, true, false, "Ciência trânsito em julgado"),
    VISTA_MINISTERIO_PUBLICO(5, true, false, "Vista ao MP"),
    VISTA_DEFENSOR_PUBLICO(5, true, false, "Vista à Defensoria"),
    VISTA_PROCURADORIA(5, true, false, "Vista à Procuradoria"),

    INTIMACAO_JUIZADO_CIVEL(10, true, true, "Juizado Especial Cível"),
    INTIMACAO_JUIZADO_FEDERAL(10, true, true, "Juizado Especial Federal"),
    INTIMACAO_JUIZADO_FAZENDARIO(10, true, true, "Juizado Especial Fazendário"),
    INTIMACAO_JECRIM(5, true, true, "JECRIM"),

    CITACAO_PENAL_ORDINARIO(10, false, true, "Citação penal ordinário"),
    CITACAO_PENAL_SUMARIO(10, false, true, "Citação penal sumário"),
    INTIMACAO_PRONUNCIA(20, false, true, "Intimação de pronúncia — Júri"),
    INTIMACAO_SENTENCA_PENAL(5, false, true, "Intimação sentença penal"),
    INTIMACAO_ACUSACAO_RESPOSTA(10, false, true, "Resposta à acusação"),
    INTIMACAO_MARIA_DA_PENHA(48, false, true, "Maria da Penha (horas)"),

    NOTIFICACAO_RECLAMADO_TRABALHISTA(5, false, true, "Notificação trabalhista"),
    INTIMACAO_ACORDAO_TRABALHISTA(8, false, true, "Acórdão trabalhista"),
    INTIMACAO_RO_TRABALHISTA(8, false, true, "Recurso Ordinário trabalhista"),

    INTIMACAO_AIME(3, true, true, "AIME"),
    INTIMACAO_AIJE(5, true, true, "AIJE"),
    INTIMACAO_RCED(3, true, true, "RCED"),
    INTIMACAO_REPRESENTACAO_ELEITORAL(5, true, true, "Representação eleitoral"),

    CITACAO_MILITAR(10, false, true, "Citação militar"),
    INTIMACAO_CONSELHO_JUSTICA(5, false, true, "Conselho de Justiça Militar"),

    AUTOMATICA(1, true, false, "Ciência automática"),
    FICTA(10, true, false, "Ciência ficta — art. 231 CPC");

    private final int prazoRespostaDias;
    private final boolean emDiasUteis;
    private final boolean exigeConfirmacaoExplicita;
    private final String label;

    TipoCienciaProcessual(int prazoRespostaDias, boolean emDiasUteis,
                          boolean exigeConfirmacaoExplicita, String label) {
        this.prazoRespostaDias = prazoRespostaDias;
        this.emDiasUteis = emDiasUteis;
        this.exigeConfirmacaoExplicita = exigeConfirmacaoExplicita;
        this.label = label;
    }

    public int prazoRespostaDias() { return prazoRespostaDias; }
    public boolean emDiasUteis() { return emDiasUteis; }
    public boolean exigeConfirmacaoExplicita() { return exigeConfirmacaoExplicita; }
    public String label() { return label; }

    public boolean isPrazoFictaAplicavel() {
        return prazoRespostaDias > 0;
    }

    public boolean isRecursal() {
        String n = name();
        return n.contains("ACORDAO") || n.contains("RESP") || n.contains("_RE")
                || n.contains("AGRAVO") || n.contains("_RO_") || n.contains("RCED")
                || n.contains("AIME") || n.contains("AIJE") || n.contains("PRONUNCIA")
                || n.contains("EMBARGOS");
    }

    public boolean isPenal() {
        String n = name();
        return n.contains("PENAL") || n.contains("PRONUNCIA") || n.contains("ACUSACAO")
                || n.contains("MARIA_DA_PENHA") || n.contains("JECRIM")
                || n.contains("MILITAR") || n.contains("CONSELHO_JUSTICA");
    }

    public boolean isTrabalhista() {
        String n = name();
        return n.contains("TRABALHISTA") || n.contains("RECLAMADO");
    }

    public boolean isEleitoral() {
        String n = name();
        return n.contains("AIME") || n.contains("AIJE") || n.contains("RCED")
                || n.contains("ELEITORAL");
    }

    public boolean isMilitar() {
        String n = name();
        return n.contains("MILITAR") || n.contains("CONSELHO_JUSTICA");
    }

    public boolean prazoEmHoras() {
        return this == INTIMACAO_MARIA_DA_PENHA;
    }
}
