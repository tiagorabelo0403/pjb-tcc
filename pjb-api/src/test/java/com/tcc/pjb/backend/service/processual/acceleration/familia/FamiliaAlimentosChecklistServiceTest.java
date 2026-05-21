package com.tcc.pjb.backend.service.processual.acceleration.familia;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.processual.acceleration.familia.FamiliaAlimentosChecklistService.AlimentosInput;
import com.tcc.pjb.backend.service.processual.acceleration.familia.FamiliaAlimentosChecklistService.ChecklistAlimentos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FamiliaAlimentosChecklistServiceTest {

    private final FamiliaAlimentosChecklistService service = new FamiliaAlimentosChecklistService();

    @Test
    void deveRetornarAptoQuandoTodosRequisitosAtendidos() {
        AlimentosInput input = new AlimentosInput(
                true, true, true, true, true, true, true);

        ChecklistAlimentos result = service.avaliar(input);

        assertThat(result.aptoParaDecisao()).isTrue();
        assertThat(result.proximaAcao()).contains("apto");
    }

    @Test
    void deveBloquearQuandoSemComprovante() {
        AlimentosInput input = new AlimentosInput(
                false, true, true, true, true, false, false);

        ChecklistAlimentos result = service.avaliar(input);

        assertThat(result.aptoParaDecisao()).isFalse();
        assertThat(result.proximaAcao()).contains("bloqueantes");
    }

    @Test
    void deveBloquearQuandoParteNaoCitada() {
        AlimentosInput input = new AlimentosInput(
                true, false, false, false, false, false, false);

        ChecklistAlimentos result = service.avaliar(input);

        assertThat(result.aptoParaDecisao()).isFalse();
        assertThat(result.itens()).anyMatch(i -> i.descricao().contains("citada") && i.bloqueante() && !i.concluido());
    }

    @Test
    void deveNaoSerAptoQuandoSemResposta() {
        AlimentosInput input = new AlimentosInput(
                true, true, true, true, false, false, false);

        ChecklistAlimentos result = service.avaliar(input);

        assertThat(result.aptoParaDecisao()).isFalse();
        assertThat(result.proximaAcao()).contains("Aguardar");
    }

    @Test
    void deveRetornarSeteItensNoChecklist() {
        AlimentosInput input = new AlimentosInput(
                false, false, false, false, false, false, false);

        ChecklistAlimentos result = service.avaliar(input);

        assertThat(result.itens()).hasSize(7);
    }

    @ParameterizedTest(name = "comprovante={0} citado={1} resposta={2} → apto={3}")
    @CsvSource({
            "true,  true,  true,  true",
            "false, true,  true,  false",
            "true,  false, true,  false",
            "true,  true,  false, false"
    })
    void deveResolverAptidaoConformalEstadoDoProcesso(
            boolean comprovante, boolean citado, boolean resposta, boolean esperado) {
        AlimentosInput input = new AlimentosInput(
                comprovante, false, false, citado, resposta, false, false);

        ChecklistAlimentos result = service.avaliar(input);

        assertThat(result.aptoParaDecisao()).isEqualTo(esperado);
    }
}
