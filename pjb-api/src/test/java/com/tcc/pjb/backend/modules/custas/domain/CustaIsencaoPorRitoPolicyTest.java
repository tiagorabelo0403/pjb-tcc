package com.tcc.pjb.backend.modules.custas.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;

class CustaIsencaoPorRitoPolicyTest {

    private final CustaIsencaoPorRitoPolicy policy = new CustaIsencaoPorRitoPolicy();

    @Test
    void juizadoEspecialCivelEmPrimeiroGrauIsentoComFundamentoDaLei9099() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, TipoCusta.CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 9.099/95, art. 54");
    }

    @Test
    void juizadoEspecialFederalEmPrimeiroGrauIsentoComFundamentoDaLei10259() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.PREVIDENCIARIO, RitoProcessual.JUIZADO_ESPECIAL_FEDERAL, TipoCusta.CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 10.259/2001");
    }

    @Test
    void previdenciarioJefIsentoComFundamentoDaLei10259() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.PREVIDENCIARIO, RitoProcessual.PREVIDENCIARIO_JEF, TipoCusta.CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 10.259/2001");
    }

    @Test
    void juizadoEspecialFazendaPublicaIsentoComFundamentoDaLei12153() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.ADMINISTRATIVO, RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA, TipoCusta.CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 12.153/2009");
    }

    @Test
    void infanciaJuventudePermaneceIsentaIndependenteDoRitoComFundamentoNoEca() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.INFANCIA_JUVENTUDE, RitoProcessual.COMUM_ORDINARIO, TipoCusta.CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 8.069/90 (ECA), art. 141");
    }

    @Test
    void infanciaJuventudeIsentaTambemQuandoTipoCustaEhNulo() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.INFANCIA_JUVENTUDE, RitoProcessual.COMUM_ORDINARIO, null);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 8.069/90 (ECA), art. 141");
    }

    @Test
    void juizadoEspecialCivelNaoEhIsentoEmPreparoRecursal() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, TipoCusta.PREPARO_RECURSAL);

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void juizadoEspecialCivelNaoEhIsentoQuandoTipoCustaEhNulo() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, null);

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void juizadoEspecialCivelNaoEhIsentoEmHonorariosPericiais() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, TipoCusta.HONORARIOS_PERICIAIS);

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void juizadoEspecialCivelNaoEhIsentoEmMultaPorLitiganciaMaFe() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, TipoCusta.MULTA_LITIGANCIA_MA_FE);

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void ritoCivelComumNaoEhIsentoEmPrimeiroGrau() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.CIVIL, RitoProcessual.COMUM_ORDINARIO, TipoCusta.CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void ritoTrabalhistaNaoRecebeIsencaoPorEssaPolitica() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.TRABALHISTA, RitoProcessual.TRABALHISTA_ORDINARIO, TipoCusta.CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void ritoNuloNaoEhIsentoAindaQueSejaCustasIniciais() {
        IsencaoCustaResult resultado = policy.verificar(RamoDireito.CIVIL, null, TipoCusta.CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isFalse();
    }
}
