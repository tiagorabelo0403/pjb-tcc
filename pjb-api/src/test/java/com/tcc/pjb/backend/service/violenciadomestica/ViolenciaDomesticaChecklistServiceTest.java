package com.tcc.pjb.backend.service.violenciadomestica;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.SituacaoRisco;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.TipoMedidaProtetiva;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.TipoViolencia;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.ViolenciaDomesticaInput;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.ViolenciaDomesticaResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViolenciaDomesticaChecklistServiceTest {

    private final ViolenciaDomesticaChecklistService service = new ViolenciaDomesticaChecklistService();

    @Test
    void deveClassificarRiscoAltoComViolenciaFisicaEArma() {
        ViolenciaDomesticaInput input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.FISICA), true, true, false, true, true);

        ViolenciaDomesticaResult result = service.avaliar(input);

        assertThat(result.risco()).isEqualTo(SituacaoRisco.ALTO);
    }

    @Test
    void deveIncluirAfastamentoLarParaViolenciaFisica() {
        ViolenciaDomesticaInput input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.FISICA), false, false, false, false, false);

        ViolenciaDomesticaResult result = service.avaliar(input);

        assertThat(result.medidasRecomendadas())
                .anyMatch(m -> m.tipo() == TipoMedidaProtetiva.AFASTAMENTO_LAR);
    }

    @Test
    void deveIncluirSuspensaoArmaQuandoAgressorArmado() {
        ViolenciaDomesticaInput input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.PSICOLOGICA), false, false, false, true, false);

        ViolenciaDomesticaResult result = service.avaliar(input);

        assertThat(result.medidasRecomendadas())
                .anyMatch(m -> m.tipo() == TipoMedidaProtetiva.SUSPENSAO_POSSE_ARMA);
    }

    @Test
    void deveMarcarJecrimInaplicavel() {
        ViolenciaDomesticaInput input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.MORAL), false, false, false, false, false);

        ViolenciaDomesticaResult result = service.avaliar(input);

        assertThat(result.jecrimInaplicavel()).isTrue();
    }

    @Test
    void deveIncluirRestricaoVisitasQuandoFilhosEnvolvidos() {
        ViolenciaDomesticaInput input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.PSICOLOGICA), false, false, true, false, false);

        ViolenciaDomesticaResult result = service.avaliar(input);

        assertThat(result.medidasRecomendadas())
                .anyMatch(m -> m.tipo() == TipoMedidaProtetiva.RESTRICAO_VISITAS_FILHOS);
    }

    @Test
    void deveIncluirAlimentosProvisioriosParaViolenciaPatrimonial() {
        ViolenciaDomesticaInput input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.PATRIMONIAL), false, false, false, false, false);

        ViolenciaDomesticaResult result = service.avaliar(input);

        assertThat(result.medidasRecomendadas())
                .anyMatch(m -> m.tipo() == TipoMedidaProtetiva.PRESTACAO_ALIMENTOS_PROVISORIOS);
    }

    @Test
    void deveClassificarRiscoBaixoParaViolenciaLeve() {
        ViolenciaDomesticaInput input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.MORAL), false, false, false, false, false);

        ViolenciaDomesticaResult result = service.avaliar(input);

        assertThat(result.risco()).isEqualTo(SituacaoRisco.BAIXO);
    }
}
