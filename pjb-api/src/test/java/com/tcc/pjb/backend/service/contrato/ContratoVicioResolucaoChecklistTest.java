package com.tcc.pjb.backend.service.contrato;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ContratoVicioResolucaoChecklistTest {

    private final ContratoVicioResolucaoChecklistService svc = new ContratoVicioResolucaoChecklistService();

    @Test
    void doloResultaEmAnulavelComOrientacaoCC178() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                ContratoVicioResolucaoChecklistService.VicioVontade.DOLO,
                null,
                LocalDate.now().minusYears(1),
                LocalDate.now().minusMonths(6),
                false, 0, 0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.grauInvalidade()).isEqualTo(ContratoVicioResolucaoChecklistService.GrauInvalidade.ANULAVEL);
        assertThat(result.prescricaoAnulacaoExpirada()).isFalse();
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("178") || o.contains("anulável"));
    }

    @Test
    void prescricaoAnulacaoExpiradaApos4Anos() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                ContratoVicioResolucaoChecklistService.VicioVontade.COACAO,
                null,
                LocalDate.now().minusYears(6),
                LocalDate.now().minusYears(5),
                false, 0, 0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.prescricaoAnulacaoExpirada()).isTrue();
        assertThat(result.diasParaPrescricaoAnulacao()).isEqualTo(0);
        assertThat(result.sinalizacao()).contains("expirado");
    }

    @Test
    void semDataCienciaVicioGeraPendencia() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                ContratoVicioResolucaoChecklistService.VicioVontade.ERRO,
                null,
                LocalDate.now().minusYears(1),
                null,
                false, 0, 0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("ciência") || p.contains("vício"));
    }

    @Test
    void inadimplementoAbsolutoInformaCC475() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                null,
                ContratoVicioResolucaoChecklistService.MotivoResolucao.INADIMPLEMENTO_ABSOLUTO,
                LocalDate.now().minusYears(2),
                null,
                false, 0, 0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("475") || o.contains("inadimplemento"));
    }

    @Test
    void clausulaResolutivaExpressaInformaCC474() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                null,
                ContratoVicioResolucaoChecklistService.MotivoResolucao.CLAUSULA_RESOLUTIVA_EXPRESSA,
                LocalDate.now().minusYears(1),
                null,
                false, 0, 0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("474") || o.contains("pleno direito"));
    }

    @Test
    void fortuivoInformaCC393() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                null,
                ContratoVicioResolucaoChecklistService.MotivoResolucao.CASO_FORTUITO,
                LocalDate.now().minusYears(1),
                null,
                false, 0, 0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("399") || o.contains("ortuito"));
    }

    @Test
    void clausulaPenalAcimaDoValorRecomendaReducao() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                null,
                ContratoVicioResolucaoChecklistService.MotivoResolucao.INADIMPLEMENTO_ABSOLUTO,
                LocalDate.now().minusYears(1),
                null,
                true, 15000.0, 10000.0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.clausulaPenalReducaoRecomendada()).isTrue();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("413") || p.contains("redução"));
    }

    @Test
    void clausulaPenalDentroDoValorNaoRecomendaReducao() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                null,
                ContratoVicioResolucaoChecklistService.MotivoResolucao.INADIMPLEMENTO_ABSOLUTO,
                LocalDate.now().minusYears(1),
                null,
                true, 8000.0, 10000.0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.clausulaPenalReducaoRecomendada()).isFalse();
    }

    @Test
    void inadimplementoParcialComCumprimentoSubstancialInformaSTJ() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                null,
                ContratoVicioResolucaoChecklistService.MotivoResolucao.INADIMPLEMENTO_PARCIAL,
                LocalDate.now().minusYears(1),
                null,
                false, 0, 0, true, true);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("substancial") || o.contains("76.362"));
    }

    @Test
    void lesaoInformaDesproporaoCC157() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                ContratoVicioResolucaoChecklistService.VicioVontade.LESAO,
                null,
                LocalDate.now().minusMonths(18),
                LocalDate.now().minusMonths(12),
                false, 0, 0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("157") || o.contains("desproporção") || o.contains("1/5"));
    }

    @Test
    void fraudeContraCredoresInformaAcaoPauliana() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                ContratoVicioResolucaoChecklistService.VicioVontade.FRAUDE_CONTRA_CREDORES,
                null,
                LocalDate.now().minusYears(1),
                LocalDate.now().minusMonths(6),
                false, 0, 0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("pauliana") || o.contains("158"));
    }

    @Test
    void semVicioESemResolucaoNaoGeraPendencias() {
        var input = new ContratoVicioResolucaoChecklistService.ContratoVicioResolucaoInput(
                null, null,
                LocalDate.now().minusYears(1),
                null,
                false, 0, 0, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).isEmpty();
        assertThat(result.sinalizacao()).contains("advogado");
    }

    @Test
    void enumVicioVontadeTemSeisTipos() {
        assertThat(ContratoVicioResolucaoChecklistService.VicioVontade.values()).hasSize(6);
    }

    @Test
    void enumMotivoResolucaoTemCincoTipos() {
        assertThat(ContratoVicioResolucaoChecklistService.MotivoResolucao.values()).hasSize(5);
    }
}
