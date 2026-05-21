package com.tcc.pjb.backend.service.previdenciario;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.previdenciario.BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput;
import com.tcc.pjb.backend.service.previdenciario.BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeResult;
import com.tcc.pjb.backend.service.previdenciario.BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus;
import com.tcc.pjb.backend.service.previdenciario.BeneficioIncapacidadeChecklistService.TipoIncapacidadeIndicada;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class BeneficioIncapacidadeChecklistServiceTest {

    private final BeneficioIncapacidadeChecklistService service =
            new BeneficioIncapacidadeChecklistService();

    @Test
    void deveIndicarTemporariaQuandoLaudoSemPermanencia() {
        BeneficioIncapacidadeInput input = new BeneficioIncapacidadeInput(
                "111.111.111-11", 24, QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, true, false, false,
                LocalDate.now().minusDays(30), LocalDate.now().minusMonths(3));

        BeneficioIncapacidadeResult result = service.avaliar(input);

        assertThat(result.tipoIndicado()).isEqualTo(TipoIncapacidadeIndicada.TEMPORARIA);
        assertThat(result.requisitosVerificados()).anyMatch(v -> v.contains("temporária"));
    }

    @Test
    void deveIndicarPermanenteQuandoLaudoComPermanencia() {
        BeneficioIncapacidadeInput input = new BeneficioIncapacidadeInput(
                "111.111.111-11", 24, QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, true, true, false,
                LocalDate.now().minusDays(60), LocalDate.now().minusMonths(4));

        BeneficioIncapacidadeResult result = service.avaliar(input);

        assertThat(result.tipoIndicado()).isEqualTo(TipoIncapacidadeIndicada.PERMANENTE);
        assertThat(result.requisitosVerificados()).anyMatch(v -> v.contains("permanente"));
    }

    @Test
    void deveAdicionarPendenciaCarenciaInsuficiente() {
        BeneficioIncapacidadeInput input = new BeneficioIncapacidadeInput(
                "111.111.111-11", 5, QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, false, false, false,
                LocalDate.now().minusDays(10), LocalDate.now().minusMonths(1));

        BeneficioIncapacidadeResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("carência"));
    }

    @Test
    void deveIsentarCarenciaParaAcidenteDeTrabalho() {
        BeneficioIncapacidadeInput input = new BeneficioIncapacidadeInput(
                "111.111.111-11", 2, QualidadeSeguradoraStatus.ATIVO,
                0, true, false, false, false, false, false,
                LocalDate.now().minusDays(5), LocalDate.now().minusMonths(1));

        BeneficioIncapacidadeResult result = service.avaliar(input);

        assertThat(result.requisitosVerificados()).anyMatch(v -> v.contains("isenção de carência"));
    }

    @Test
    void deveAdicionarPendenciaQualidadeSeguradoraPerdida() {
        BeneficioIncapacidadeInput input = new BeneficioIncapacidadeInput(
                "111.111.111-11", 20, QualidadeSeguradoraStatus.PERDIDA,
                18, false, false, false, true, false, false,
                LocalDate.now().minusDays(10), LocalDate.now().minusMonths(18));

        BeneficioIncapacidadeResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("qualidade de segurado"));
    }

    @Test
    void deveAdicionarPendenciaLaudoNaoApresentado() {
        BeneficioIncapacidadeInput input = new BeneficioIncapacidadeInput(
                "111.111.111-11", 24, QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, false, false, false,
                LocalDate.now().minusDays(15), LocalDate.now().minusMonths(2));

        BeneficioIncapacidadeResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("laudo"));
        assertThat(result.tipoIndicado()).isEqualTo(TipoIncapacidadeIndicada.NAO_DETERMINADA);
    }

    @ParameterizedTest(name = "qualidade={0}")
    @EnumSource(QualidadeSeguradoraStatus.class)
    void deveAvaliarTodasQualidadesDeSeguradora(QualidadeSeguradoraStatus qualidade) {
        BeneficioIncapacidadeInput input = new BeneficioIncapacidadeInput(
                "111.111.111-11", 20, qualidade,
                qualidade == QualidadeSeguradoraStatus.PERDIDA ? 18 : 0,
                false, false, false, true, false, false,
                LocalDate.now().minusDays(20), LocalDate.now().minusMonths(3));

        BeneficioIncapacidadeResult result = service.avaliar(input);

        assertThat(result).isNotNull();
    }
}
