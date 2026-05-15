package com.tcc.pjb.backend.service.previdenciario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PrevidenciarioChecklistTest {

    private final BeneficioIncapacidadeChecklistService checklist = new BeneficioIncapacidadeChecklistService();

    @Test
    void semPendenciasSeguradorAtivoCarenciaLaudoPermanente() {
        var input = new BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput(
                "123.456.789-00", 24,
                BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, true, true, false, null, null);
        var result = checklist.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.tipoIndicado()).isEqualTo(BeneficioIncapacidadeChecklistService.TipoIncapacidadeIndicada.PERMANENTE);
        assertThat(result.sinalizacao()).contains("INSS");
    }

    @Test
    void semPendenciasLaudoTemporario() {
        var input = new BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput(
                "123.456.789-01", 15,
                BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, true, false, false, null, null);
        var result = checklist.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.tipoIndicado()).isEqualTo(BeneficioIncapacidadeChecklistService.TipoIncapacidadeIndicada.TEMPORARIA);
    }

    @Test
    void pendenciaCarenciaInsuficiente() {
        var input = new BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput(
                "123.456.789-02", 6,
                BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, true, false, false, null, null);
        var result = checklist.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("carência insuficiente"));
        assertThat(result.sinalizacao()).contains("Pendências identificadas");
    }

    @Test
    void isencaoCarenciaAcidenteDeTrabalho() {
        var input = new BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput(
                "123.456.789-03", 2,
                BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.ATIVO,
                0, true, false, false, true, false, false, null, null);
        var result = checklist.avaliar(input);
        assertThat(result.requisitosVerificados()).anyMatch(r -> r.contains("isenção de carência"));
        assertThat(result.pendenciasIdentificadas()).noneMatch(p -> p.contains("carência"));
    }

    @Test
    void pendenciaQualidadeSeguradoraPerdida() {
        var input = new BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput(
                "123.456.789-04", 30,
                BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.PERDIDA,
                20, false, false, false, true, false, false, null, null);
        var result = checklist.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("qualidade de segurado"));
    }

    @Test
    void periodoGracaGeraVerificado() {
        var input = new BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput(
                "123.456.789-05", 20,
                BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.PERIODO_GRACA,
                8, false, false, false, true, false, false, null, null);
        var result = checklist.avaliar(input);
        assertThat(result.requisitosVerificados()).anyMatch(r -> r.contains("período de graça"));
        assertThat(result.pendenciasIdentificadas()).noneMatch(p -> p.contains("qualidade de segurado possivelmente perdida"));
    }

    @Test
    void pendenciaLaudoNaoApresentado() {
        var input = new BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput(
                "123.456.789-06", 15,
                BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, false, false, false, null, null);
        var result = checklist.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("laudo"));
        assertThat(result.tipoIndicado()).isEqualTo(BeneficioIncapacidadeChecklistService.TipoIncapacidadeIndicada.NAO_DETERMINADA);
    }

    @Test
    void isencaoCarenciaInternacaoHospitalar() {
        var input = new BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput(
                "123.456.789-07", 3,
                BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, true, true, true, null, null);
        var result = checklist.avaliar(input);
        assertThat(result.requisitosVerificados()).anyMatch(r -> r.contains("isenção de carência"));
    }

    @Test
    void pendenciaDIDAnteriorUltimaContribuicao() {
        var did = LocalDate.now().minusMonths(18);
        var ultimaContrib = LocalDate.now().minusMonths(12);
        var input = new BeneficioIncapacidadeChecklistService.BeneficioIncapacidadeInput(
                "123.456.789-08", 20,
                BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.ATIVO,
                0, false, false, false, true, false, false, did, ultimaContrib);
        var result = checklist.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("DID"));
    }

    @Test
    void tiposIncapacidadeEnumSaoCompletos() {
        assertThat(BeneficioIncapacidadeChecklistService.TipoIncapacidadeIndicada.values()).hasSize(3);
        assertThat(BeneficioIncapacidadeChecklistService.QualidadeSeguradoraStatus.values()).hasSize(3);
    }
}
