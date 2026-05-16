package com.tcc.pjb.backend.service.responsabilidadecivil;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponsabilidadeCivilChecklistTest {

    private final ResponsabilidadeCivilChecklistService svc = new ResponsabilidadeCivilChecklistService();

    @Test
    void subjetivaComTodosRequisitosNaoGeraPendencias() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true,
                LocalDate.now().minusMonths(6),
                false, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MORAL));
        var result = svc.avaliar(input);
        assertThat(result.tipoResponsabilidade()).isEqualTo(ResponsabilidadeCivilChecklistService.TipoResponsabilidade.SUBJETIVA);
        assertThat(result.prescricaoExpirada()).isFalse();
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.sinalizacao()).contains("advogado");
    }

    @Test
    void semProvaAtoIlicitoGeraPendencia() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.FATO_PROPRIO,
                false, true, true,
                LocalDate.now().minusMonths(6),
                false, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MATERIAL));
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("ato ilícito"));
    }

    @Test
    void semProvaNexoCausalGeraPendencia() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.FATO_PROPRIO,
                true, true, false,
                LocalDate.now().minusMonths(12),
                false, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MATERIAL));
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("nexo"));
    }

    @Test
    void prescricaoTrienalExpiradaMarcaComoExpirada() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true,
                LocalDate.now().minusYears(4),
                false, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MORAL));
        var result = svc.avaliar(input);
        assertThat(result.prescricaoExpirada()).isTrue();
        assertThat(result.diasParaPrescricao()).isEqualTo(0);
        assertThat(result.sinalizacao()).contains("prescricional");
    }

    @Test
    void responsabilidadeEstadoEhObjetivaComPrescricaoQuinquenal() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.RESPONSABILIDADE_ESTADO,
                true, true, true,
                LocalDate.now().minusYears(3),
                false, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MATERIAL));
        var result = svc.avaliar(input);
        assertThat(result.tipoResponsabilidade()).isEqualTo(ResponsabilidadeCivilChecklistService.TipoResponsabilidade.OBJETIVA);
        assertThat(result.prescricaoExpirada()).isFalse();
        assertThat(result.diasParaPrescricao()).isGreaterThan(0);
        assertThat(result.orientacoesJuridicas()).anyMatch(o -> o.contains("DL 4.597") || o.contains("quinquenal"));
    }

    @Test
    void atividadeRiscoEhObjetiva() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.ATIVIDADE_RISCO,
                true, true, true,
                LocalDate.now().minusMonths(8),
                false, false,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MORAL,
                        ResponsabilidadeCivilChecklistService.TipoDano.MATERIAL));
        var result = svc.avaliar(input);
        assertThat(result.tipoResponsabilidade()).isEqualTo(ResponsabilidadeCivilChecklistService.TipoResponsabilidade.OBJETIVA);
        assertThat(result.orientacoesJuridicas()).anyMatch(o -> o.contains("Súmula 37") || o.contains("moral e material"));
    }

    @Test
    void acidenteConsumoEhPresumida() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.ACIDENTE_CONSUMO,
                true, true, true,
                LocalDate.now().minusYears(2),
                false, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MATERIAL));
        var result = svc.avaliar(input);
        assertThat(result.tipoResponsabilidade()).isEqualTo(ResponsabilidadeCivilChecklistService.TipoResponsabilidade.PRESUMIDA);
        assertThat(result.prescricaoExpirada()).isFalse();
    }

    @Test
    void danoEsteticoInformaSumula387() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true,
                LocalDate.now().minusMonths(10),
                false, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MORAL,
                        ResponsabilidadeCivilChecklistService.TipoDano.ESTETICO));
        var result = svc.avaliar(input);
        assertThat(result.orientacoesJuridicas()).anyMatch(o -> o.contains("387") || o.contains("estético"));
    }

    @Test
    void culpaConcorrenteInformaCC945() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true,
                LocalDate.now().minusMonths(5),
                true, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MATERIAL));
        var result = svc.avaliar(input);
        assertThat(result.orientacoesJuridicas()).anyMatch(o -> o.contains("945") || o.contains("concorrente"));
    }

    @Test
    void lucrosCessantesInformaCC402() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true,
                LocalDate.now().minusMonths(4),
                false, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.LUCROS_CESSANTES));
        var result = svc.avaliar(input);
        assertThat(result.orientacoesJuridicas()).anyMatch(o -> o.contains("402") || o.contains("lucros"));
    }

    @Test
    void semDataFatoGeraPendenciaDePrescricao() {
        var input = new ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput(
                ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true,
                null,
                false, true,
                List.of(ResponsabilidadeCivilChecklistService.TipoDano.MORAL));
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("data") || p.contains("prescrição"));
    }

    @Test
    void enumOrigemTemCincoModalidades() {
        assertThat(ResponsabilidadeCivilChecklistService.OrigemResponsabilidade.values()).hasSize(5);
    }

    @Test
    void enumTipoDanoTemCincoModalidades() {
        assertThat(ResponsabilidadeCivilChecklistService.TipoDano.values()).hasSize(5);
    }
}
