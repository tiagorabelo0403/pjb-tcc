package com.tcc.pjb.backend.core.financeiro.custas;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.financeiro.custas.domain.IsencaoCustaResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import org.junit.jupiter.api.Test;

class CustaIsencaoPorRitoPolicyTest {

    private static final String CUSTAS_INICIAIS = "CUSTAS_INICIAIS";
    private final CustaIsencaoPorRitoPolicy policy = new CustaIsencaoPorRitoPolicy();

    @Test
    void juizadoEspecialCivelEmPrimeiroGrauIsentoComFundamentoDaLei9099() {
        Processo processo = processo(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL);

        IsencaoCustaResult resultado = policy.verificar(processo, CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 9.099/95, art. 54");
    }

    @Test
    void juizadoEspecialFederalEmPrimeiroGrauIsentoComFundamentoDaLei10259() {
        Processo processo = processo(RamoDireito.PREVIDENCIARIO, RitoProcessual.JUIZADO_ESPECIAL_FEDERAL);

        IsencaoCustaResult resultado = policy.verificar(processo, CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 10.259/2001");
    }

    @Test
    void previdenciarioJefIsentoComFundamentoDaLei10259() {
        Processo processo = processo(RamoDireito.PREVIDENCIARIO, RitoProcessual.PREVIDENCIARIO_JEF);

        IsencaoCustaResult resultado = policy.verificar(processo, CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 10.259/2001");
    }

    @Test
    void juizadoEspecialFazendaPublicaIsentoComFundamentoDaLei12153() {
        Processo processo = processo(RamoDireito.ADMINISTRATIVO, RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA);

        IsencaoCustaResult resultado = policy.verificar(processo, CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 12.153/2009");
    }

    @Test
    void infanciaJuventudePermaneceIsentaIndependenteDoRitoComFundamentoNoEca() {
        Processo processo = processo(RamoDireito.INFANCIA_JUVENTUDE, RitoProcessual.COMUM_ORDINARIO);

        IsencaoCustaResult resultado = policy.verificar(processo, CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 8.069/90 (ECA), art. 141");
    }

    @Test
    void infanciaJuventudeIsentaTambemQuandoTipoCustaEhNulo() {
        Processo processo = processo(RamoDireito.INFANCIA_JUVENTUDE, RitoProcessual.COMUM_ORDINARIO);

        IsencaoCustaResult resultado = policy.verificar(processo, null);

        assertThat(resultado.isento()).isTrue();
        assertThat(resultado.motivo()).contains("Lei 8.069/90 (ECA), art. 141");
    }

    @Test
    void juizadoEspecialCivelNaoEhIsentoEmPreparoRecursal() {
        Processo processo = processo(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL);

        IsencaoCustaResult resultado = policy.verificar(processo, "PREPARO_RECURSAL");

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void juizadoEspecialCivelNaoEhIsentoQuandoTipoCustaEhNulo() {
        Processo processo = processo(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL);

        IsencaoCustaResult resultado = policy.verificar(processo, null);

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void juizadoEspecialCivelNaoEhIsentoQuandoTipoCustaEhVazio() {
        Processo processo = processo(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL);

        IsencaoCustaResult resultado = policy.verificar(processo, "");

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void ritoCivelComumNaoEhIsentoEmPrimeiroGrau() {
        Processo processo = processo(RamoDireito.CIVIL, RitoProcessual.COMUM_ORDINARIO);

        IsencaoCustaResult resultado = policy.verificar(processo, CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void ritoTrabalhistaNaoRecebeIsencaoPorEssaPolitica() {
        Processo processo = processo(RamoDireito.TRABALHISTA, RitoProcessual.TRABALHISTA_ORDINARIO);

        IsencaoCustaResult resultado = policy.verificar(processo, CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void juizadoEspecialCivelNaoEhIsentoQuandoTipoCustaVemEmMinusculo() {
        Processo processo = processo(RamoDireito.CIVIL, RitoProcessual.JUIZADO_ESPECIAL_CIVEL);

        IsencaoCustaResult resultado = policy.verificar(processo, "custas_iniciais");

        assertThat(resultado.isento()).isFalse();
    }

    @Test
    void ritoNuloNaoEhIsentoAindaQueSejaCustasIniciais() {
        Processo processo = processo(RamoDireito.CIVIL, null);

        IsencaoCustaResult resultado = policy.verificar(processo, CUSTAS_INICIAIS);

        assertThat(resultado.isento()).isFalse();
    }

    private static Processo processo(RamoDireito ramo, RitoProcessual rito) {
        Processo processo = new Processo();
        processo.setRamoDireito(ramo);
        processo.setRito(rito);
        return processo;
    }
}
