package com.tcc.pjb.backend.service.acaoconstitucional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.acaoconstitucional.AcaoPopularReadinessService.AcaoPopularInput;
import com.tcc.pjb.backend.service.acaoconstitucional.AcaoPopularReadinessService.AcaoPopularResult;
import com.tcc.pjb.backend.service.acaoconstitucional.AcaoPopularReadinessService.ObjetoAcaoPopular;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AcaoPopularReadinessServiceTest {

    private final AcaoPopularReadinessService service = new AcaoPopularReadinessService();

    @Test
    void deveSinalizarSemPendenciasQuandoTodosRequisitosPreenchidosEPrazoVigente() {
        AcaoPopularInput input = new AcaoPopularInput(
                "P-001", true, true, true,
                ObjetoAcaoPopular.PATRIMONIO_PUBLICO,
                LocalDate.now().minusYears(1));

        AcaoPopularResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.prazoNaoDecaido()).isTrue();
        assertThat(result.anosRestantes()).isGreaterThan(0);
        assertThat(result.sinalizacao()).contains("Sem pendências");
    }

    @Test
    void deveAdicionarPendenciaQuandoAutorNaoForCidadao() {
        AcaoPopularInput input = new AcaoPopularInput(
                "P-002", false, true, true,
                ObjetoAcaoPopular.MEIO_AMBIENTE,
                LocalDate.now().minusYears(1));

        AcaoPopularResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("cidadão brasileiro"));
    }

    @Test
    void deveAdicionarPendenciaQuandoSemTituloEleitor() {
        AcaoPopularInput input = new AcaoPopularInput(
                "P-003", true, false, true,
                ObjetoAcaoPopular.MORALIDADE_ADMINISTRATIVA,
                LocalDate.now().minusYears(1));

        AcaoPopularResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("título de eleitor"));
    }

    @Test
    void deveAdicionarPendenciaQuandoPrazoPrescricionalEsgotado() {
        AcaoPopularInput input = new AcaoPopularInput(
                "P-004", true, true, true,
                ObjetoAcaoPopular.PATRIMONIO_HISTORICO_CULTURAL,
                LocalDate.now().minusYears(6));

        AcaoPopularResult result = service.avaliar(input);

        assertThat(result.prazoNaoDecaido()).isFalse();
        assertThat(result.anosRestantes()).isZero();
        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("prazo prescricional"));
    }
}
