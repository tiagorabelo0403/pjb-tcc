package com.tcc.pjb.backend.service.habeascorpus;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HabeasCorpusChecklistTest {

    private final HabeasCorpusChecklistService svc = new HabeasCorpusChecklistService();

    @Test
    void liberatorio_delegaciaEstadual_semJustaCausa_competenciaTJ() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.DELEGACIA_ESTADUAL,
                List.of(HabeasCorpusChecklistService.MotivoIlegalidade.SEM_JUSTA_CAUSA),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(HabeasCorpusChecklistService.TribunalCompetente.TJ);
        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("art. 648, I"));
        assertThat(result.prazo()).contains("Sem prazo");
    }

    @Test
    void preventivo_delegaciaFederal_excesspPrazo_competenciaTRF() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.PREVENTIVO,
                HabeasCorpusChecklistService.OrgaoCoator.DELEGACIA_FEDERAL,
                List.of(HabeasCorpusChecklistService.MotivoIlegalidade.EXCESSO_PRAZO),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(HabeasCorpusChecklistService.TribunalCompetente.TRF);
        assertThat(result.observacao()).contains("salvo-conduto");
    }

    @Test
    void liberatorio_juizEstadual_nulidade_competenciaTJ() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.JUIZ_ESTADUAL,
                List.of(HabeasCorpusChecklistService.MotivoIlegalidade.NULIDADE_PROCESSO),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(HabeasCorpusChecklistService.TribunalCompetente.TJ);
        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("art. 648, VI"));
    }

    @Test
    void liberatorio_juizFederal_competenciaTRF() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.JUIZ_FEDERAL,
                List.of(HabeasCorpusChecklistService.MotivoIlegalidade.PRISAO_ALEM_PRAZO),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(HabeasCorpusChecklistService.TribunalCompetente.TRF);
    }

    @Test
    void liberatorio_tribunalEstadual_competenciaSTJ() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.TRIBUNAL_ESTADUAL,
                List.of(HabeasCorpusChecklistService.MotivoIlegalidade.SEM_JUSTA_CAUSA),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(HabeasCorpusChecklistService.TribunalCompetente.STJ);
        assertThat(result.competencia().fundamentoCompetencia()).contains("CF art. 105, I, c");
    }

    @Test
    void liberatorio_STJ_competenciaSTF() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.STJ,
                List.of(HabeasCorpusChecklistService.MotivoIlegalidade.INCOMPETENCIA_AUTORIDADE),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(HabeasCorpusChecklistService.TribunalCompetente.STF);
        assertThat(result.competencia().fundamentoCompetencia()).contains("CF art. 102, I, i");
    }

    @Test
    void naoCabivel_penaExtinta_sumula694() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.JUIZ_ESTADUAL,
                List.of(),
                true, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("Súmula 694");
    }

    @Test
    void naoCabivel_transgressaoDisciplinarMilitar_sumula693() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.DELEGACIA_FEDERAL,
                List.of(),
                false, true, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("Súmula 693");
    }

    @Test
    void naoCabivel_multaIsolada_sumula695() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.JUIZ_ESTADUAL,
                List.of(),
                false, false, true, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("Súmula 695");
    }

    @Test
    void naoCabivel_jecrimTurmaRecursal_sumula606() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.TRIBUNAL_ESTADUAL,
                List.of(),
                false, false, false, true);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isFalse();
        assertThat(result.motivoNaoCabimento()).contains("Súmula 606");
    }

    @Test
    void liberatorio_multosMotivos_todosListados() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.JUIZ_ESTADUAL,
                List.of(
                        HabeasCorpusChecklistService.MotivoIlegalidade.EXCESSO_PRAZO,
                        HabeasCorpusChecklistService.MotivoIlegalidade.NULIDADE_PROCESSO,
                        HabeasCorpusChecklistService.MotivoIlegalidade.EXTINCAO_PUNIBILIDADE),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.fundamentosLegais())
                .anyMatch(f -> f.contains("648, II"))
                .anyMatch(f -> f.contains("648, VI"))
                .anyMatch(f -> f.contains("648, VII"));
    }

    @Test
    void reparatorio_contendeFundamentoConstitucional() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.REPARATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.DELEGACIA_ESTADUAL,
                List.of(HabeasCorpusChecklistService.MotivoIlegalidade.SEM_JUSTA_CAUSA),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("CF art. 5°, LXVIII"));
        assertThat(result.observacao()).contains("reparatória");
    }

    @Test
    void medidasSeguranancaIlegal_competenciaCorreta() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.LIBERATORIO,
                HabeasCorpusChecklistService.OrgaoCoator.JUIZ_FEDERAL,
                List.of(HabeasCorpusChecklistService.MotivoIlegalidade.MEDIDA_SEGURANCA_ILEGAL),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.competencia().tribunal())
                .isEqualTo(HabeasCorpusChecklistService.TribunalCompetente.TRF);
        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("medida de segurança"));
    }

    @Test
    void preventivo_observacaoMencionaLiminar() {
        var input = new HabeasCorpusChecklistService.HabeasCorpusInput(
                HabeasCorpusChecklistService.TipoHC.PREVENTIVO,
                HabeasCorpusChecklistService.OrgaoCoator.DELEGACIA_ESTADUAL,
                List.of(HabeasCorpusChecklistService.MotivoIlegalidade.SEM_JUSTA_CAUSA),
                false, false, false, false);

        var result = svc.avaliar(input);

        assertThat(result.cabivel()).isTrue();
        assertThat(result.observacao()).contains("CPP art. 660, §2°");
        assertThat(result.fundamentosLegais()).anyMatch(f -> f.contains("660, §1°"));
    }
}
