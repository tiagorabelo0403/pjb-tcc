package com.tcc.pjb.backend.service.acaoconstitucional;

public enum AcaoConstitucionalTipo {

    HABEAS_CORPUS("HC", "CF art. 5º LXVIII — liberdade de locomoção"),
    MANDADO_SEGURANCA("MS", "CF art. 5º LXIX — direito líquido e certo, prazo 120 dias"),
    MANDADO_SEGURANCA_COLETIVO("MSC", "CF art. 5º LXX — impetrado por partido/entidade"),
    HABEAS_DATA("HD", "CF art. 5º LXXII — acesso/retificação de dados pessoais"),
    MANDADO_INJUNCAO("MI", "CF art. 5º LXXI — omissão normativa inconstitucional"),
    MANDADO_INJUNCAO_COLETIVO("MIC", "Lei 13.300/2016 art. 12 — efeitos erga omnes"),
    ACAO_POPULAR("AP", "CF art. 5º LXXIII, Lei 4.717/65 — cidadão, patrimônio público"),
    ARGUICAO_DESCUMPRIMENTO_PRECEITO_FUNDAMENTAL("ADPF", "CF art. 102 §1º, Lei 9.882/99 — STF");

    private final String sigla;
    private final String descricao;

    AcaoConstitucionalTipo(String sigla, String descricao) {
        this.sigla = sigla;
        this.descricao = descricao;
    }

    public String sigla() { return sigla; }
    public String descricao() { return descricao; }
}
