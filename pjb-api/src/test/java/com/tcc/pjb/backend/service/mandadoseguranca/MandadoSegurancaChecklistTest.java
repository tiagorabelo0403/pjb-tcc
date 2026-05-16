package com.tcc.pjb.backend.service.mandadoseguranca;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MandadoSegurancaChecklistTest {

    private final MandadoSegurancaChecklistService svc = new MandadoSegurancaChecklistService();

    private MandadoSegurancaChecklistService.MandadoSegurancaInput inputValido(
            MandadoSegurancaChecklistService.TipoMS tipo,
            MandadoSegurancaChecklistService.AutoridadeCoatora autoridade) {
        return new MandadoSegurancaChecklistService.MandadoSegurancaInput(
                tipo, autoridade, true, true, true,
                false, false, false, false);
    }

    @Test
    void individual_autoridadeMunicipal_competenciaJuizEstadual() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_MUNICIPAL));

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(MandadoSegurancaChecklistService.TribunalCompetente.JUIZ_ESTADUAL_1GRAU);
    }

    @Test
    void individual_autoridadeEstadual_competenciaTJ() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_ESTADUAL));

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(MandadoSegurancaChecklistService.TribunalCompetente.TJ);
    }

    @Test
    void individual_autoridadeFederal_competenciaJuizFederal() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_FEDERAL));

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(MandadoSegurancaChecklistService.TribunalCompetente.JUIZ_FEDERAL_1GRAU);
        assertThat(result.competencia().fundamento()).contains("CF art. 109, VIII");
    }

    @Test
    void individual_ministroEstado_competenciaSTJ() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.MINISTRO_ESTADO));

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(MandadoSegurancaChecklistService.TribunalCompetente.STJ);
        assertThat(result.competencia().fundamento()).contains("CF art. 105, I, b");
    }

    @Test
    void individual_TCU_competenciaSTJ() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.TCU));

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(MandadoSegurancaChecklistService.TribunalCompetente.STJ);
    }

    @Test
    void individual_tribunalSuperior_competenciaSTF() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.TRIBUNAL_SUPERIOR));

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(MandadoSegurancaChecklistService.TribunalCompetente.STF);
        assertThat(result.competencia().fundamento()).contains("CF art. 102, I, d");
    }

    @Test
    void coletivo_contemFundamentoCF_LXX() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.COLETIVO,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_FEDERAL));

        assertThat(result.cabivel()).isTrue();
        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("CF art. 5°, LXX"));
        assertThat(result.observacao()).contains("Súmula 629");
    }

    @Test
    void naoCabivel_contraLeiEmTese_sumula266() {
        var input = new MandadoSegurancaChecklistService.MandadoSegurancaInput(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_FEDERAL,
                true, true, true, true, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("Súmula 266");
    }

    @Test
    void naoCabivel_transitadoJulgado() {
        var input = new MandadoSegurancaChecklistService.MandadoSegurancaInput(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.JUIZ_ESTADUAL_1GRAU,
                true, true, true, false, true, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("transitada em julgado");
    }

    @Test
    void naoCabivel_recursoComEfeitoSuspensivo() {
        var input = new MandadoSegurancaChecklistService.MandadoSegurancaInput(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.JUIZ_ESTADUAL_1GRAU,
                true, true, true, false, false, true, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("efeito suspensivo");
    }

    @Test
    void naoCabivel_gestaoComercialEmpresaPublica() {
        var input = new MandadoSegurancaChecklistService.MandadoSegurancaInput(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_FEDERAL,
                true, true, true, false, false, false, true);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("gestão comercial");
    }

    @Test
    void naoCabivel_semDireitoLiquidoCerto() {
        var input = new MandadoSegurancaChecklistService.MandadoSegurancaInput(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_ESTADUAL,
                false, true, true, false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("direito líquido e certo");
    }

    @Test
    void naoCabivel_semProvaPreConstituida() {
        var input = new MandadoSegurancaChecklistService.MandadoSegurancaInput(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_ESTADUAL,
                true, false, true, false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("prova pré-constituída");
    }

    @Test
    void naoCabivel_prazo120DiasEsgotado() {
        var input = new MandadoSegurancaChecklistService.MandadoSegurancaInput(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_FEDERAL,
                true, true, false, false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("120 dias");
    }

    @Test
    void cabivel_liminarSemprecabivel() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_FEDERAL));

        assertThat(result.liminarCabivel()).isTrue();
        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("art. 7°, III"));
    }

    @Test
    void prazoImpetracaoInformaDecadencia() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_MUNICIPAL));

        assertThat(result.prazoImpetração()).contains("120 dias");
        assertThat(result.observacao()).contains("Súmula 632");
    }

    @Test
    void observacaoMencionaReexameNecessario() {
        var result = svc.avaliar(inputValido(
                MandadoSegurancaChecklistService.TipoMS.INDIVIDUAL,
                MandadoSegurancaChecklistService.AutoridadeCoatora.AUTORIDADE_ESTADUAL));

        assertThat(result.observacao()).contains("reexame necessário");
    }
}
