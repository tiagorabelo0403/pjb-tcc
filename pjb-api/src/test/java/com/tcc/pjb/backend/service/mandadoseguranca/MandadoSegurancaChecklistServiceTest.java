package com.tcc.pjb.backend.service.mandadoseguranca;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.mandadoseguranca.MandadoSegurancaChecklistService.AutoridadeCoatora;
import com.tcc.pjb.backend.service.mandadoseguranca.MandadoSegurancaChecklistService.MandadoSegurancaInput;
import com.tcc.pjb.backend.service.mandadoseguranca.MandadoSegurancaChecklistService.MandadoSegurancaResult;
import com.tcc.pjb.backend.service.mandadoseguranca.MandadoSegurancaChecklistService.TipoMS;
import com.tcc.pjb.backend.service.mandadoseguranca.MandadoSegurancaChecklistService.TribunalCompetente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MandadoSegurancaChecklistServiceTest {

    private final MandadoSegurancaChecklistService service = new MandadoSegurancaChecklistService();

    @Test
    void deveRetornarCabivelParaMsIndividualValido() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                TipoMS.INDIVIDUAL, AutoridadeCoatora.AUTORIDADE_MUNICIPAL,
                true, true, true,
                false, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.motivoNaoCabimento()).isNull();
        assertThat(result.liminarCabivel()).isTrue();
    }

    @Test
    void deveRetornarNaoCabivelParaLeiEmTese() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                TipoMS.INDIVIDUAL, AutoridadeCoatora.AUTORIDADE_FEDERAL,
                true, true, true,
                true, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("Súmula 266");
    }

    @Test
    void deveRetornarNaoCabivelParaDecisaoTransitada() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                TipoMS.INDIVIDUAL, AutoridadeCoatora.AUTORIDADE_ESTADUAL,
                true, true, true,
                false, true, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("transitada em julgado");
    }

    @Test
    void deveRetornarNaoCabivelParaRecursoComEfeitoSuspensivo() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                TipoMS.INDIVIDUAL, AutoridadeCoatora.AUTORIDADE_FEDERAL,
                true, true, true,
                false, false, true, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("efeito suspensivo");
    }

    @Test
    void deveRetornarNaoCabivelParaPrazoEsgotado() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                TipoMS.INDIVIDUAL, AutoridadeCoatora.AUTORIDADE_MUNICIPAL,
                true, true, false,
                false, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("120 dias");
    }

    @ParameterizedTest(name = "autoridade={0} → tribunal={1}")
    @CsvSource({
            "AUTORIDADE_MUNICIPAL,  JUIZ_ESTADUAL_1GRAU",
            "AUTORIDADE_ESTADUAL,   TJ",
            "AUTORIDADE_FEDERAL,    JUIZ_FEDERAL_1GRAU",
            "MINISTRO_ESTADO,       STJ",
            "TCU,                   STJ",
            "TRIBUNAL_SUPERIOR,     STF"
    })
    void deveResolverCompetenciaConformalAutoridadeCoatora(
            AutoridadeCoatora autoridade, TribunalCompetente tribunalEsperado) {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                TipoMS.INDIVIDUAL, autoridade,
                true, true, true,
                false, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal()).isEqualTo(tribunalEsperado);
    }

    @Test
    void deveAdicionarFundamentoColetivoParaMsColetivo() {
        MandadoSegurancaInput input = new MandadoSegurancaInput(
                TipoMS.COLETIVO, AutoridadeCoatora.AUTORIDADE_ESTADUAL,
                true, true, true,
                false, false, false, false);

        MandadoSegurancaResult result = service.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("LXX"));
        assertThat(result.observacao()).contains("Súmula 629");
    }
}
