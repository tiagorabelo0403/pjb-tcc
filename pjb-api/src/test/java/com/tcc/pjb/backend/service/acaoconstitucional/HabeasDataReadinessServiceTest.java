package com.tcc.pjb.backend.service.acaoconstitucional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.acaoconstitucional.HabeasDataReadinessService.FinalidadeHD;
import com.tcc.pjb.backend.service.acaoconstitucional.HabeasDataReadinessService.HabeasDataInput;
import com.tcc.pjb.backend.service.acaoconstitucional.HabeasDataReadinessService.HabeasDataResult;
import org.junit.jupiter.api.Test;

class HabeasDataReadinessServiceTest {

    private final HabeasDataReadinessService service = new HabeasDataReadinessService();

    @Test
    void deveSinalizarSemPendenciasQuandoTodosRequisitosPreenchidos() {
        HabeasDataInput input = new HabeasDataInput(
                "P-001", true, true, true, FinalidadeHD.CONHECER_INFORMACOES);

        HabeasDataResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.sinalizacao()).contains("Sem pendências");
        assertThat(result.requisitosVerificados()).hasSize(4);
    }

    @Test
    void deveAdicionarPendenciaQuandoSemRequerimentoAdministrativoPrevia() {
        HabeasDataInput input = new HabeasDataInput(
                "P-002", false, true, true, FinalidadeHD.RETIFICAR_DADOS);

        HabeasDataResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("requerimento administrativo"));
    }

    @Test
    void deveAdicionarPendenciaQuandoBancoNaoForPublico() {
        HabeasDataInput input = new HabeasDataInput(
                "P-003", true, true, false, FinalidadeHD.ANOTAR_INFORMACOES_CONTESTADAS);

        HabeasDataResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("banco de dados"));
    }

    @Test
    void deveIncluirDescricaoDaFinalidadeNosRequisitosVerificados() {
        HabeasDataInput input = new HabeasDataInput(
                "P-004", true, true, true, FinalidadeHD.RETIFICAR_DADOS);

        HabeasDataResult result = service.avaliar(input);

        assertThat(result.requisitosVerificados())
                .anyMatch(r -> r.contains("retificação de dados"));
    }
}
