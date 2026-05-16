package com.tcc.pjb.backend.service.retroatividadepenal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetroatividadePenalChecklistTest {

    private final RetroatividadePenalChecklistService svc = new RetroatividadePenalChecklistService();

    // --- Abolitio criminis ---

    @Test
    void abolitio_cumprendoPena_retroageExtinguePunibilidade() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.ABOLITIO_CRIMINIS,
                RetroatividadePenalChecklistService.SituacaoProcessual.CUMPRINDO_PENA,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.LEI_NOVA);
        assertThat(result.retroageImediatamente()).isTrue();
        assertThat(result.exigeRevisaoDeOficio()).isTrue();
        assertThat(result.efeito()).contains("extingue a punibilidade");
        assertThat(result.fundamentoLegal()).contains("CP art. 2°, caput");
    }

    @Test
    void abolitio_penaDenunciado_retroageImediatamente() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.ABOLITIO_CRIMINIS,
                RetroatividadePenalChecklistService.SituacaoProcessual.DENUNCIADO,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.retroageImediatamente()).isTrue();
        assertThat(result.fundamentoConstitucional()).contains("CF art. 5°, XL");
    }

    @Test
    void abolitio_penaJaExtinta_naoProduzefeitos() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.ABOLITIO_CRIMINIS,
                RetroatividadePenalChecklistService.SituacaoProcessual.PENA_JA_EXTINTA,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.retroageImediatamente()).isFalse();
        assertThat(result.efeito()).contains("Pena já extinta");
    }

    // --- Lei mais benéfica ---

    @Test
    void leiMaisBenefica_condenadoTransitadoJulgado_retroageDeOficio() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.LEI_MAIS_BENEFICA,
                RetroatividadePenalChecklistService.SituacaoProcessual.CONDENADO_TRANSITADO_EM_JULGADO,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.LEI_NOVA);
        assertThat(result.retroageImediatamente()).isTrue();
        assertThat(result.exigeRevisaoDeOficio()).isTrue();
        assertThat(result.fundamentoLegal()).contains("CP art. 2°, parágrafo único");
        assertThat(result.observacao()).contains("Súmula 611");
    }

    @Test
    void leiMaisBenefica_investigado_retroageImediato() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.LEI_MAIS_BENEFICA,
                RetroatividadePenalChecklistService.SituacaoProcessual.INVESTIGADO,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.retroageImediatamente()).isTrue();
        assertThat(result.exigeRevisaoDeOficio()).isFalse();
    }

    @Test
    void leiMaisBenefica_penaExtinta_naoProduzefeitos() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.LEI_MAIS_BENEFICA,
                RetroatividadePenalChecklistService.SituacaoProcessual.PENA_JA_EXTINTA,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.LEI_ANTIGA);
        assertThat(result.retroageImediatamente()).isFalse();
    }

    @Test
    void leiMaisBenefica_combinacaoLeis_vedada_sumula501() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.LEI_MAIS_BENEFICA,
                RetroatividadePenalChecklistService.SituacaoProcessual.CUMPRINDO_PENA,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, true);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.AMBAS_SE_BENEFICA);
        assertThat(result.retroageImediatamente()).isFalse();
        assertThat(result.fundamentoLegal()).contains("Súmula 501");
        assertThat(result.efeito()).contains("Combinação de leis vedada");
    }

    // --- Lei mais grave ---

    @Test
    void leiMaisGrave_crimeInstantaneo_naoRetroage() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.LEI_MAIS_GRAVE,
                RetroatividadePenalChecklistService.SituacaoProcessual.DENUNCIADO,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.LEI_ANTIGA);
        assertThat(result.retroageImediatamente()).isFalse();
        assertThat(result.efeito()).contains("tempus regit actum");
        assertThat(result.fundamentoConstitucional()).contains("CF art. 5°, XL");
    }

    @Test
    void leiMaisGrave_crimePermanente_leiVigenteAntesCessacao_sumula711() {
        // Réu em sequestro: lei mais grave entrou em vigor ANTES de ele soltar a vítima
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.LEI_MAIS_GRAVE,
                RetroatividadePenalChecklistService.SituacaoProcessual.DENUNCIADO,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.PERMANENTE,
                true, false);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.LEI_NOVA);
        assertThat(result.fundamentoLegal()).contains("Súmula 711");
        assertThat(result.efeito()).contains("crime permanente");
    }

    @Test
    void leiMaisGrave_crimeContinuado_leiVigenteAntesCessacao_sumula711() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.LEI_MAIS_GRAVE,
                RetroatividadePenalChecklistService.SituacaoProcessual.DENUNCIADO,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.CONTINUADO,
                true, false);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.LEI_NOVA);
        assertThat(result.fundamentoLegal()).contains("Súmula 711");
    }

    @Test
    void leiMaisGrave_crimePermanente_leiAposcessacao_naoRetroage() {
        // Lei mais grave entrou em vigor DEPOIS da cessação: não aplica Súmula 711
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.LEI_MAIS_GRAVE,
                RetroatividadePenalChecklistService.SituacaoProcessual.DENUNCIADO,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.PERMANENTE,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.LEI_ANTIGA);
        assertThat(result.efeito()).contains("tempus regit actum");
    }

    @Test
    void leiMaisGrave_condenadoCumprendoPena_naoRetroage() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.LEI_MAIS_GRAVE,
                RetroatividadePenalChecklistService.SituacaoProcessual.CUMPRINDO_PENA,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.LEI_ANTIGA);
        assertThat(result.retroageImediatamente()).isFalse();
    }

    // --- Novatio legis incriminadora ---

    @Test
    void novatioIncriminadora_naoRetroage_nullumCrimen() {
        var input = new RetroatividadePenalChecklistService.RetroatividadeInput(
                RetroatividadePenalChecklistService.TipoLeiNova.NOVATIO_LEGIS_INCRIMINADORA,
                RetroatividadePenalChecklistService.SituacaoProcessual.INVESTIGADO,
                RetroatividadePenalChecklistService.TipoCrimeQuantoAoDurar.INSTANTANEO,
                false, false);

        var result = svc.avaliar(input);

        assertThat(result.leiAplicavel())
                .isEqualTo(RetroatividadePenalChecklistService.LeiAplicavel.LEI_ANTIGA);
        assertThat(result.retroageImediatamente()).isFalse();
        assertThat(result.fundamentoLegal()).contains("nullum crimen");
        assertThat(result.efeito()).contains("atípica");
    }
}
