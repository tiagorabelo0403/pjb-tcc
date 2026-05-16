package com.tcc.pjb.backend.service.acidentetrabalho;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AcidenteTrabalhoChecklistTest {

    private final AcidenteTrabalhoChecklistService svc = new AcidenteTrabalhoChecklistService();

    @Test
    void catNaoEmitidaGeraPendencia() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(2),
                LocalDate.now().minusMonths(1),
                false, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("CAT") || p.contains("22"));
    }

    @Test
    void catEmitidaNaoGeraPendenciaCat() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(2),
                LocalDate.now().minusMonths(1),
                true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).noneMatch(p -> p.contains("CAT não emitida"));
    }

    @Test
    void regimeNaoCltNaoExigeCAT() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.SERVIDOR_PUBLICO,
                LocalDate.now().minusMonths(2),
                null,
                false, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("CLT") || o.contains("previdenciária"));
        assertThat(result.pendenciasIdentificadas()).noneMatch(p -> p.contains("CAT não emitida"));
    }

    @Test
    void estabilidadeAtivaQuandoAltaRecenteEClt() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(14),
                LocalDate.now().minusMonths(6),
                true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.estabilidadeEmpregoAtiva()).isTrue();
        assertThat(result.diasEstabilidadeRestantes()).isGreaterThan(0);
    }

    @Test
    void estabilidadeExpiradaQuandoAltaAntiga() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusYears(3),
                LocalDate.now().minusYears(2),
                true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.estabilidadeEmpregoAtiva()).isFalse();
        assertThat(result.diasEstabilidadeRestantes()).isEqualTo(0);
    }

    @Test
    void semAltaMedicaGeraPendenciaEstabilidade() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(3),
                null,
                true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("alta") || p.contains("118"));
    }

    @Test
    void prescricaoCivilExpiradaApos3Anos() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusYears(4),
                LocalDate.now().minusYears(3),
                true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.prescricaoCivilExpirada()).isTrue();
        assertThat(result.diasParaPrescricaoCivil()).isEqualTo(0);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("prescrita") || p.contains("206"));
    }

    @Test
    void prescricaoCivilNaoExpiradaDentro3Anos() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusYears(1),
                LocalDate.now().minusMonths(6),
                true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.prescricaoCivilExpirada()).isFalse();
        assertThat(result.diasParaPrescricaoCivil()).isGreaterThan(0);
    }

    @Test
    void atividadeDeRiscoInformaResponsabilidadeObjetiva() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(6),
                LocalDate.now().minusMonths(3),
                true, true, false, true, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("927") || o.contains("objetiva"));
    }

    @Test
    void culpaExclusivaVitimaInformaExclusao() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(6),
                LocalDate.now().minusMonths(3),
                true, true, false, false, true);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("exclusiva") || o.contains("945"));
    }

    @Test
    void doencaOcupacionalInformaDataDiagnostico() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.DOENCA_OCUPACIONAL,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(8),
                LocalDate.now().minusMonths(4),
                true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("23") || o.contains("laudo") || o.contains("nexo"));
    }

    @Test
    void acidenteTrajetoInformaArt21() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TRAJETO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(5),
                LocalDate.now().minusMonths(2),
                true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("21") || o.contains("trajeto"));
    }

    @Test
    void nexoCausalNaoComprovadoGeraPendencia() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(4),
                LocalDate.now().minusMonths(2),
                true, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("NTEP") || o.contains("nexo") || o.contains("NTP"));
    }

    @Test
    void incapacidadePermanenteInformaAposentadoria() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(10),
                LocalDate.now().minusMonths(6),
                true, true, true, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("B32") || o.contains("B94") || o.contains("permanente"));
    }

    @Test
    void semPendenciasRetornaSinalizacaoCorreta() {
        var input = new AcidenteTrabalhoChecklistService.AcidenteTrabalhoInput(
                AcidenteTrabalhoChecklistService.TipoAcidente.TIPICO,
                AcidenteTrabalhoChecklistService.RegimeEmprego.CLT,
                LocalDate.now().minusMonths(6),
                LocalDate.now().minusMonths(3),
                true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.sinalizacao()).contains("advogado");
    }

    @Test
    void enumTipoAcidenteTemQuatroTipos() {
        assertThat(AcidenteTrabalhoChecklistService.TipoAcidente.values()).hasSize(4);
    }

    @Test
    void enumRegimeEmpregoTemQuatroTipos() {
        assertThat(AcidenteTrabalhoChecklistService.RegimeEmprego.values()).hasSize(4);
    }
}
