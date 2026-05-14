package com.tcc.pjb.backend.service.institutional;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;

public sealed interface InstituicaoJudicialTipo
        permits InstituicaoJudicialTipo.VaraUnica,
                InstituicaoJudicialTipo.VaraEspecializada,
                InstituicaoJudicialTipo.JuizadoEspecial,
                InstituicaoJudicialTipo.JuizadoEspecialFederal,
                InstituicaoJudicialTipo.TurmaRecursal,
                InstituicaoJudicialTipo.Camara,
                InstituicaoJudicialTipo.TribunalPleno,
                InstituicaoJudicialTipo.JecItinerante,
                InstituicaoJudicialTipo.Plantao {

    String codigo();
    GrauJurisdicao grauJurisdicao();
    boolean suportaJuizSubstituto();
    boolean requerConvocacaoEspecial();
    boolean admiteJurisdicionadoSemAdvogado();

    record VaraUnica() implements InstituicaoJudicialTipo {
        public String codigo() { return "VARA_UNICA"; }
        public GrauJurisdicao grauJurisdicao() { return GrauJurisdicao.PRIMEIRO_GRAU; }
        public boolean suportaJuizSubstituto() { return true; }
        public boolean requerConvocacaoEspecial() { return false; }
        public boolean admiteJurisdicionadoSemAdvogado() { return false; }
    }

    record VaraEspecializada(String materia) implements InstituicaoJudicialTipo {
        public String codigo() { return "VARA_ESPECIALIZADA"; }
        public GrauJurisdicao grauJurisdicao() { return GrauJurisdicao.PRIMEIRO_GRAU; }
        public boolean suportaJuizSubstituto() { return true; }
        public boolean requerConvocacaoEspecial() { return false; }
        public boolean admiteJurisdicionadoSemAdvogado() { return false; }
    }

    record JuizadoEspecial() implements InstituicaoJudicialTipo {
        public String codigo() { return "JUIZADO_ESPECIAL"; }
        public GrauJurisdicao grauJurisdicao() { return GrauJurisdicao.PRIMEIRO_GRAU; }
        public boolean suportaJuizSubstituto() { return true; }
        public boolean requerConvocacaoEspecial() { return false; }
        public boolean admiteJurisdicionadoSemAdvogado() { return true; }
    }

    record JuizadoEspecialFederal() implements InstituicaoJudicialTipo {
        public String codigo() { return "JUIZADO_ESPECIAL_FEDERAL"; }
        public GrauJurisdicao grauJurisdicao() { return GrauJurisdicao.PRIMEIRO_GRAU; }
        public boolean suportaJuizSubstituto() { return true; }
        public boolean requerConvocacaoEspecial() { return false; }
        public boolean admiteJurisdicionadoSemAdvogado() { return true; }
    }

    record TurmaRecursal() implements InstituicaoJudicialTipo {
        public String codigo() { return "TURMA_RECURSAL"; }
        public GrauJurisdicao grauJurisdicao() { return GrauJurisdicao.SEGUNDO_GRAU; }
        public boolean suportaJuizSubstituto() { return true; }
        public boolean requerConvocacaoEspecial() { return true; }
        public boolean admiteJurisdicionadoSemAdvogado() { return false; }
    }

    record Camara(int numero) implements InstituicaoJudicialTipo {
        public String codigo() { return "CAMARA"; }
        public GrauJurisdicao grauJurisdicao() { return GrauJurisdicao.SEGUNDO_GRAU; }
        public boolean suportaJuizSubstituto() { return true; }
        public boolean requerConvocacaoEspecial() { return true; }
        public boolean admiteJurisdicionadoSemAdvogado() { return false; }
    }

    record TribunalPleno() implements InstituicaoJudicialTipo {
        public String codigo() { return "TRIBUNAL_PLENO"; }
        public GrauJurisdicao grauJurisdicao() { return GrauJurisdicao.SEGUNDO_GRAU; }
        public boolean suportaJuizSubstituto() { return false; }
        public boolean requerConvocacaoEspecial() { return true; }
        public boolean admiteJurisdicionadoSemAdvogado() { return false; }
    }

    record JecItinerante(String municipioAlvo) implements InstituicaoJudicialTipo {
        public String codigo() { return "JEC_ITINERANTE"; }
        public GrauJurisdicao grauJurisdicao() { return GrauJurisdicao.PRIMEIRO_GRAU; }
        public boolean suportaJuizSubstituto() { return true; }
        public boolean requerConvocacaoEspecial() { return false; }
        public boolean admiteJurisdicionadoSemAdvogado() { return true; }
    }

    record Plantao(String periodoId) implements InstituicaoJudicialTipo {
        public String codigo() { return "PLANTAO"; }
        public GrauJurisdicao grauJurisdicao() { return GrauJurisdicao.PRIMEIRO_GRAU; }
        public boolean suportaJuizSubstituto() { return true; }
        public boolean requerConvocacaoEspecial() { return true; }
        public boolean admiteJurisdicionadoSemAdvogado() { return false; }
    }
}
