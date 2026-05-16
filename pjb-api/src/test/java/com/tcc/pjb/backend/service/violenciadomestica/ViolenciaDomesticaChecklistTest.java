package com.tcc.pjb.backend.service.violenciadomestica;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.MedidaProtetiva;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.SituacaoRisco;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.TipoMedidaProtetiva;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.TipoViolencia;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.ViolenciaDomesticaInput;
import com.tcc.pjb.backend.service.violenciadomestica.ViolenciaDomesticaChecklistService.ViolenciaDomesticaResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ViolenciaDomesticaChecklistTest {

    private final ViolenciaDomesticaChecklistService service = new ViolenciaDomesticaChecklistService();

    @Test
    void fisica_comFlagrante_deveRetornarRiscoAlto() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.FISICA), true, false, false, false, false);
        ViolenciaDomesticaResult result = service.avaliar(input);
        assertThat(result.risco()).isEqualTo(SituacaoRisco.ALTO);
    }

    @Test
    void fisica_comFlagrante_deveConterAfastamentoEProibicaoAproximacao() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.FISICA), true, false, false, false, false);
        List<TipoMedidaProtetiva> tipos = service.avaliar(input).medidasRecomendadas()
                .stream().map(MedidaProtetiva::tipo).toList();
        assertThat(tipos).contains(
                TipoMedidaProtetiva.AFASTAMENTO_LAR,
                TipoMedidaProtetiva.PROIBICAO_APROXIMACAO,
                TipoMedidaProtetiva.PROIBICAO_CONTATO);
    }

    @Test
    void fisica_semFlagrante_deveRetornarRiscoMedio() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.FISICA), false, false, false, false, false);
        assertThat(service.avaliar(input).risco()).isEqualTo(SituacaoRisco.MEDIO);
    }

    @Test
    void sexual_deveSempreRetornarRiscoAlto() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.SEXUAL), false, false, false, false, false);
        assertThat(service.avaliar(input).risco()).isEqualTo(SituacaoRisco.ALTO);
    }

    @Test
    void agressorComArma_deveSuspenderPosse() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.PSICOLOGICA), false, false, false, true, false);
        List<TipoMedidaProtetiva> tipos = service.avaliar(input).medidasRecomendadas()
                .stream().map(MedidaProtetiva::tipo).toList();
        assertThat(tipos).contains(TipoMedidaProtetiva.SUSPENSAO_POSSE_ARMA);
    }

    @Test
    void patrimonial_deveIncluirAlimentosProvisiorios() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.PATRIMONIAL), false, false, false, false, false);
        List<TipoMedidaProtetiva> tipos = service.avaliar(input).medidasRecomendadas()
                .stream().map(MedidaProtetiva::tipo).toList();
        assertThat(tipos).contains(TipoMedidaProtetiva.PRESTACAO_ALIMENTOS_PROVISORIOS);
    }

    @Test
    void filhosEnvolvidos_deveRestringirVisitas() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.PSICOLOGICA), false, false, true, false, false);
        List<TipoMedidaProtetiva> tipos = service.avaliar(input).medidasRecomendadas()
                .stream().map(MedidaProtetiva::tipo).toList();
        assertThat(tipos).contains(TipoMedidaProtetiva.RESTRICAO_VISITAS_FILHOS);
    }

    @Test
    void flagranteAtual_deveExigirAudienciaCustodia() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.FISICA), true, false, false, false, false);
        assertThat(service.avaliar(input).audienciaCustodiaObrigatoria()).isTrue();
    }

    @Test
    void semFlagrante_naoExigeAudienciaCustodia() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.PSICOLOGICA), false, false, false, false, false);
        assertThat(service.avaliar(input).audienciaCustodiaObrigatoria()).isFalse();
    }

    @Test
    void jecrimSempreInaplicavel_lei1134006Art41() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.MORAL), false, false, false, false, false);
        assertThat(service.avaliar(input).jecrimInaplicavel()).isTrue();
    }

    @Test
    void fisica_deveGerarQualificadoraPenalCP129() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.FISICA), false, false, false, false, false);
        assertThat(service.avaliar(input).qualificadoraPenal()).contains("CP art. 129 §9");
    }

    @Test
    void semFisica_naoGeraQualificadoraPenal() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.MORAL), false, false, false, false, false);
        assertThat(service.avaliar(input).qualificadoraPenal()).isEmpty();
    }

    @Test
    void reincidencia_deveAdicionarObsPrisaoPreventiva() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.PSICOLOGICA), false, true, false, false, false);
        assertThat(service.avaliar(input).observacao()).contains("prisão preventiva");
    }

    @Test
    void apenasViolenciaMoral_deveBaixoRisco() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.MORAL), false, false, false, false, false);
        assertThat(service.avaliar(input).risco()).isEqualTo(SituacaoRisco.BAIXO);
    }

    @Test
    void multiplostipos_deveConterTodasMedidas() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.FISICA, TipoViolencia.PATRIMONIAL),
                false, false, true, true, true);
        List<TipoMedidaProtetiva> tipos = service.avaliar(input).medidasRecomendadas()
                .stream().map(MedidaProtetiva::tipo).toList();
        assertThat(tipos).contains(
                TipoMedidaProtetiva.PROIBICAO_CONTATO,
                TipoMedidaProtetiva.AFASTAMENTO_LAR,
                TipoMedidaProtetiva.PROIBICAO_APROXIMACAO,
                TipoMedidaProtetiva.SUSPENSAO_POSSE_ARMA,
                TipoMedidaProtetiva.PRESTACAO_ALIMENTOS_PROVISORIOS,
                TipoMedidaProtetiva.RESTRICAO_VISITAS_FILHOS);
    }

    @Test
    void proibicaoContato_semprePresente() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.MORAL), false, false, false, false, false);
        List<TipoMedidaProtetiva> tipos = service.avaliar(input).medidasRecomendadas()
                .stream().map(MedidaProtetiva::tipo).toList();
        assertThat(tipos).contains(TipoMedidaProtetiva.PROIBICAO_CONTATO);
    }

    @Test
    void prazoApreciacao_sempreMinimoSetentaEOitoHoras() {
        var input = new ViolenciaDomesticaInput(
                List.of(TipoViolencia.FISICA, TipoViolencia.PATRIMONIAL),
                true, true, true, true, true);
        service.avaliar(input).medidasRecomendadas()
                .forEach(m -> assertThat(m.prazoApreciacaoHoras()).isEqualTo(48));
    }
}
