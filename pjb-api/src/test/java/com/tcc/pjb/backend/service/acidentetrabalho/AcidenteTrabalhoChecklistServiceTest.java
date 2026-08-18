package com.tcc.pjb.backend.service.acidentetrabalho;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.acidentetrabalho.AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput;
import com.tcc.pjb.backend.service.acidentetrabalho.AcidenteTrabalhoChecklistService.AcidenteTrabalhoResult;
import com.tcc.pjb.backend.service.acidentetrabalho.AcidenteTrabalhoChecklistService.RegimeEmprego;
import com.tcc.pjb.backend.service.acidentetrabalho.AcidenteTrabalhoChecklistService.TipoAcidente;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AcidenteTrabalhoChecklistServiceTest {

    private final AcidenteTrabalhoChecklistService service = new AcidenteTrabalhoChecklistService();

    @Test
    void deveAdicionarPendenciaCatNaoEmitida() {
        AcidenteTrabalhoInput input = new AcidenteTrabalhoInput(
                TipoAcidente.TIPICO, RegimeEmprego.CLT,
                LocalDate.now().minusDays(5), null,
                false, true, false, false, false);

        AcidenteTrabalhoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("CAT"));
    }

    @Test
    void deveNaoExigirCatParaAutonomo() {
        AcidenteTrabalhoInput input = new AcidenteTrabalhoInput(
                TipoAcidente.TIPICO, RegimeEmprego.AUTONOMO_MEI,
                LocalDate.now().minusDays(3), null,
                false, true, false, false, false);

        AcidenteTrabalhoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).noneMatch(p -> p.contains("CAT não emitida"));
    }

    @Test
    void deveDetectarEstabilidadeAtiva() {
        LocalDate altaRecente = LocalDate.now().minusMonths(6);
        AcidenteTrabalhoInput input = new AcidenteTrabalhoInput(
                TipoAcidente.TIPICO, RegimeEmprego.CLT,
                LocalDate.now().minusMonths(7), altaRecente,
                true, true, false, false, false);

        AcidenteTrabalhoResult result = service.avaliar(input);

        assertThat(result.estabilidadeEmpregoAtiva()).isTrue();
        assertThat(result.diasEstabilidadeRestantes()).isGreaterThan(0);
    }

    @Test
    void deveDetectarEstabilidadeExpirada() {
        LocalDate altaAntiga = LocalDate.now().minusMonths(18);
        AcidenteTrabalhoInput input = new AcidenteTrabalhoInput(
                TipoAcidente.TIPICO, RegimeEmprego.CLT,
                LocalDate.now().minusMonths(20), altaAntiga,
                true, true, false, false, false);

        AcidenteTrabalhoResult result = service.avaliar(input);

        assertThat(result.estabilidadeEmpregoAtiva()).isFalse();
        assertThat(result.diasEstabilidadeRestantes()).isEqualTo(0);
    }

    @Test
    void deveAdicionarPendenciaPrescricaoCivilExpirada() {
        LocalDate acidenteAntigo = LocalDate.now().minusYears(4);
        AcidenteTrabalhoInput input = new AcidenteTrabalhoInput(
                TipoAcidente.TIPICO, RegimeEmprego.CLT,
                acidenteAntigo, acidenteAntigo.plusMonths(3),
                true, true, false, false, false);

        AcidenteTrabalhoResult result = service.avaliar(input);

        assertThat(result.prescricaoCivilExpirada()).isTrue();
        assertThat(result.diasParaPrescricaoCivil()).isEqualTo(0);
    }

    @Test
    void deveAdicionarOrientacaoAcidenteTrajeto() {
        AcidenteTrabalhoInput input = new AcidenteTrabalhoInput(
                TipoAcidente.TRAJETO, RegimeEmprego.CLT,
                LocalDate.now().minusDays(10), null,
                true, true, false, false, false);

        AcidenteTrabalhoResult result = service.avaliar(input);

        assertThat(result.orientacoes()).anyMatch(o -> o.contains("trajeto"));
    }

    @Test
    void deveAdicionarOrientacaoResponsabilidadeObjetiva() {
        AcidenteTrabalhoInput input = new AcidenteTrabalhoInput(
                TipoAcidente.TIPICO, RegimeEmprego.CLT,
                LocalDate.now().minusDays(20), null,
                true, true, false, true, false);

        AcidenteTrabalhoResult result = service.avaliar(input);

        assertThat(result.orientacoes()).anyMatch(o -> o.contains("objetiva"));
    }

    @Test
    void deveAdicionarPendenciaAltaMedicaAusente() {
        AcidenteTrabalhoInput input = new AcidenteTrabalhoInput(
                TipoAcidente.TIPICO, RegimeEmprego.CLT,
                LocalDate.now().minusDays(30), null,
                true, true, false, false, false);

        AcidenteTrabalhoResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("alta médica"));
    }
}
