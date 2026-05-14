package com.tcc.pjb.backend.service.processual.distribution;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public sealed interface NaturezaProcessualResolver
        permits NaturezaProcessualResolver.Civil,
                NaturezaProcessualResolver.Penal,
                NaturezaProcessualResolver.Trabalhista,
                NaturezaProcessualResolver.Eleitoral,
                NaturezaProcessualResolver.Militar,
                NaturezaProcessualResolver.Federal,
                NaturezaProcessualResolver.Especial,
                NaturezaProcessualResolver.Constitucional {

    String codigo();
    EsferaJurisdicao esfera();
    boolean admiteJuizadoEspecial();
    boolean exigeMinisterioPublico();

    record Civil() implements NaturezaProcessualResolver {
        public String codigo() { return "CIVIL"; }
        public EsferaJurisdicao esfera() { return EsferaJurisdicao.JUSTICA_ESTADUAL; }
        public boolean admiteJuizadoEspecial() { return true; }
        public boolean exigeMinisterioPublico() { return false; }
    }

    record Penal() implements NaturezaProcessualResolver {
        public String codigo() { return "PENAL"; }
        public EsferaJurisdicao esfera() { return EsferaJurisdicao.JUSTICA_ESTADUAL; }
        public boolean admiteJuizadoEspecial() { return true; }
        public boolean exigeMinisterioPublico() { return true; }
    }

    record Trabalhista() implements NaturezaProcessualResolver {
        public String codigo() { return "TRABALHISTA"; }
        public EsferaJurisdicao esfera() { return EsferaJurisdicao.JUSTICA_TRABALHO; }
        public boolean admiteJuizadoEspecial() { return false; }
        public boolean exigeMinisterioPublico() { return false; }
    }

    record Eleitoral() implements NaturezaProcessualResolver {
        public String codigo() { return "ELEITORAL"; }
        public EsferaJurisdicao esfera() { return EsferaJurisdicao.JUSTICA_ELEITORAL; }
        public boolean admiteJuizadoEspecial() { return false; }
        public boolean exigeMinisterioPublico() { return true; }
    }

    record Militar() implements NaturezaProcessualResolver {
        public String codigo() { return "MILITAR"; }
        public EsferaJurisdicao esfera() { return EsferaJurisdicao.JUSTICA_MILITAR; }
        public boolean admiteJuizadoEspecial() { return false; }
        public boolean exigeMinisterioPublico() { return true; }
    }

    record Federal() implements NaturezaProcessualResolver {
        public String codigo() { return "FEDERAL"; }
        public EsferaJurisdicao esfera() { return EsferaJurisdicao.JUSTICA_FEDERAL; }
        public boolean admiteJuizadoEspecial() { return true; }
        public boolean exigeMinisterioPublico() { return false; }
    }

    record Especial() implements NaturezaProcessualResolver {
        public String codigo() { return "ESPECIAL"; }
        public EsferaJurisdicao esfera() { return EsferaJurisdicao.JUSTICA_FEDERAL; }
        public boolean admiteJuizadoEspecial() { return false; }
        public boolean exigeMinisterioPublico() { return true; }
    }

    record Constitucional() implements NaturezaProcessualResolver {
        public String codigo() { return "CONSTITUCIONAL"; }
        public EsferaJurisdicao esfera() { return EsferaJurisdicao.JUSTICA_FEDERAL; }
        public boolean admiteJuizadoEspecial() { return false; }
        public boolean exigeMinisterioPublico() { return true; }
    }

    static NaturezaProcessualResolver resolverPorRito(RitoProcessual rito) {
        if (rito.isTrabalhista()) return new Trabalhista();
        if (rito.isEleitoral()) return new Eleitoral();
        if (rito.isMilitar()) return new Militar();
        if (rito.isEspecialConstitucional()) return new Especial();
        if (rito.isPenal()) return new Penal();
        if (rito.isPrevidenciario() || rito == RitoProcessual.HOMOLOGACAO_SENTENCA_ESTRANGEIRA
                || rito == RitoProcessual.CARTA_ROGATORIA) return new Federal();
        return new Civil();
    }
}
