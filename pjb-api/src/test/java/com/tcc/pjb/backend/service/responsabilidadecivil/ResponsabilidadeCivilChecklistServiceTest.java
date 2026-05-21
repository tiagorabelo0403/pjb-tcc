package com.tcc.pjb.backend.service.responsabilidadecivil;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.responsabilidadecivil.ResponsabilidadeCivilChecklistService.OrigemResponsabilidade;
import com.tcc.pjb.backend.service.responsabilidadecivil.ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilInput;
import com.tcc.pjb.backend.service.responsabilidadecivil.ResponsabilidadeCivilChecklistService.ResponsabilidadeCivilResult;
import com.tcc.pjb.backend.service.responsabilidadecivil.ResponsabilidadeCivilChecklistService.TipoDano;
import com.tcc.pjb.backend.service.responsabilidadecivil.ResponsabilidadeCivilChecklistService.TipoResponsabilidade;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResponsabilidadeCivilChecklistServiceTest {

    private final ResponsabilidadeCivilChecklistService service = new ResponsabilidadeCivilChecklistService();

    @Test
    void deveClassificarObjetivaParaAtividadeRisco() {
        ResponsabilidadeCivilInput input = new ResponsabilidadeCivilInput(
                OrigemResponsabilidade.ATIVIDADE_RISCO,
                true, true, true, LocalDate.now().minusYears(1),
                false, true, List.of(TipoDano.MATERIAL));

        ResponsabilidadeCivilResult result = service.avaliar(input);

        assertThat(result.tipoResponsabilidade()).isEqualTo(TipoResponsabilidade.OBJETIVA);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
    }

    @Test
    void deveClassificarPresumidaParaAcidenteConsumo() {
        ResponsabilidadeCivilInput input = new ResponsabilidadeCivilInput(
                OrigemResponsabilidade.ACIDENTE_CONSUMO,
                true, true, true, LocalDate.now().minusYears(1),
                false, true, List.of(TipoDano.MORAL));

        ResponsabilidadeCivilResult result = service.avaliar(input);

        assertThat(result.tipoResponsabilidade()).isEqualTo(TipoResponsabilidade.PRESUMIDA);
    }

    @Test
    void deveDetectarPrescricaoExpiradaFatoProprioMaisDe3Anos() {
        ResponsabilidadeCivilInput input = new ResponsabilidadeCivilInput(
                OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true, LocalDate.now().minusYears(4),
                false, true, List.of());

        ResponsabilidadeCivilResult result = service.avaliar(input);

        assertThat(result.prescricaoExpirada()).isTrue();
        assertThat(result.diasParaPrescricao()).isEqualTo(0);
    }

    @Test
    void deveAdicionarPendenciaQuandoSemProvaAtoIlicito() {
        ResponsabilidadeCivilInput input = new ResponsabilidadeCivilInput(
                OrigemResponsabilidade.FATO_PROPRIO,
                false, true, true, LocalDate.now().minusYears(1),
                false, true, List.of());

        ResponsabilidadeCivilResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("ato ilícito"));
    }

    @Test
    void deveAdicionarOrientacaoCumulacaoDanosMoralEMaterial() {
        ResponsabilidadeCivilInput input = new ResponsabilidadeCivilInput(
                OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true, LocalDate.now().minusYears(1),
                false, true, List.of(TipoDano.MORAL, TipoDano.MATERIAL));

        ResponsabilidadeCivilResult result = service.avaliar(input);

        assertThat(result.orientacoesJuridicas()).anyMatch(o -> o.contains("37"));
    }

    @Test
    void deveUsarPrescricaoQuinquenalParaEstado() {
        ResponsabilidadeCivilInput input = new ResponsabilidadeCivilInput(
                OrigemResponsabilidade.RESPONSABILIDADE_ESTADO,
                true, true, true, LocalDate.now().minusYears(4),
                false, true, List.of());

        ResponsabilidadeCivilResult result = service.avaliar(input);

        assertThat(result.prescricaoExpirada()).isFalse();
        assertThat(result.diasParaPrescricao()).isPositive();
    }

    @Test
    void deveMencionarCulpaConcorrente() {
        ResponsabilidadeCivilInput input = new ResponsabilidadeCivilInput(
                OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true, LocalDate.now().minusYears(1),
                true, true, List.of());

        ResponsabilidadeCivilResult result = service.avaliar(input);

        assertThat(result.orientacoesJuridicas()).anyMatch(o -> o.toLowerCase().contains("culpa concorrente"));
    }

    @Test
    void deveAdicionarOrientacaoDanoEstetico() {
        ResponsabilidadeCivilInput input = new ResponsabilidadeCivilInput(
                OrigemResponsabilidade.FATO_PROPRIO,
                true, true, true, LocalDate.now().minusYears(1),
                false, true, List.of(TipoDano.ESTETICO));

        ResponsabilidadeCivilResult result = service.avaliar(input);

        assertThat(result.orientacoesJuridicas()).anyMatch(o -> o.contains("387") || o.contains("estético"));
    }
}
