package com.tcc.pjb.backend.service.tutela;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.tutela.TutelaProvisioriaChecklistService.FundamentoEvidencia;
import com.tcc.pjb.backend.service.tutela.TutelaProvisioriaChecklistService.MomentoRequerimento;
import com.tcc.pjb.backend.service.tutela.TutelaProvisioriaChecklistService.TipoTutela;
import com.tcc.pjb.backend.service.tutela.TutelaProvisioriaChecklistService.TutelaProvisioriaInput;
import com.tcc.pjb.backend.service.tutela.TutelaProvisioriaChecklistService.TutelaProvisioriaResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TutelaProvisioriaChecklistServiceTest {

    private final TutelaProvisioriaChecklistService service = new TutelaProvisioriaChecklistService();

    @Test
    void deveAvaliarViabilidadeUrgenciaAntecipadaQuandoFumusEPericulum() {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.URGENCIA_ANTECIPADA, MomentoRequerimento.INCIDENTAL,
                null, true, true, true, false, false, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.tutelaViavel()).isTrue();
        assertThat(result.pendenciasIdentificadas()).isEmpty();
    }

    @Test
    void deveMarcarInviavelQuandoFumusAusente() {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.URGENCIA_ANTECIPADA, MomentoRequerimento.INCIDENTAL,
                null, false, true, true, true, false, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.tutelaViavel()).isFalse();
        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("probabilidade do direito"));
    }

    @Test
    void deveMarcarInviavelQuandoPericulumAusente() {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.URGENCIA_ANTECIPADA, MomentoRequerimento.INCIDENTAL,
                null, true, false, true, true, false, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.tutelaViavel()).isFalse();
        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("perigo de dano"));
    }

    @Test
    void deveSinalizarLiminarSemContraditorioCabivelQuandoUrgenciaComPericulum() {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.URGENCIA_ANTECIPADA, MomentoRequerimento.LIMINAR_INICIAL,
                null, true, true, true, false, false, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.liminarCabivelSemContraditorio()).isTrue();
    }

    @Test
    void deveSinalizarLiminarNaoCabivelParaEvidencia() {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.EVIDENCIA, MomentoRequerimento.INCIDENTAL,
                FundamentoEvidencia.PROVA_DOCUMENTAL_INEQUIVOCA,
                false, false, false, false, false, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.liminarCabivelSemContraditorio()).isFalse();
    }

    @Test
    void deveMarcarInviavelEvidenciaSemFundamento() {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.EVIDENCIA, MomentoRequerimento.INCIDENTAL,
                null, false, false, false, false, false, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.tutelaViavel()).isFalse();
        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("fundamento específico"));
    }

    @Test
    void deveAvaliarViavelEvidenciaComFundamento() {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.EVIDENCIA, MomentoRequerimento.INCIDENTAL,
                FundamentoEvidencia.PROVA_DOCUMENTAL_INEQUIVOCA,
                false, false, false, false, false, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.tutelaViavel()).isTrue();
    }

    @Test
    void deveAdicionarOrientacaoFungibilidadeParaCautelarFungivel() {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.URGENCIA_CAUTELAR, MomentoRequerimento.INCIDENTAL,
                null, true, true, false, true, true, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.orientacoes())
                .anyMatch(o -> o.contains("Fungibilidade") || o.contains("ungibilidade"));
    }

    @Test
    void deveAdicionarPendenciaIrreversibilidadeParaAntecipadaIrreversivel() {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.URGENCIA_ANTECIPADA, MomentoRequerimento.INCIDENTAL,
                null, true, true, false, true, false, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.pendenciasIdentificadas())
                .anyMatch(p -> p.contains("irrevers"));
    }

    @ParameterizedTest(name = "fundamento={0} → viavel=true")
    @EnumSource(FundamentoEvidencia.class)
    void deveAvaliarViavelParaTodosFundamentosDeEvidencia(FundamentoEvidencia fundamento) {
        TutelaProvisioriaInput input = new TutelaProvisioriaInput(
                TipoTutela.EVIDENCIA, MomentoRequerimento.INCIDENTAL,
                fundamento, false, false, false, false, false, false);

        TutelaProvisioriaResult result = service.avaliar(input);

        assertThat(result.tutelaViavel()).isTrue();
    }
}
