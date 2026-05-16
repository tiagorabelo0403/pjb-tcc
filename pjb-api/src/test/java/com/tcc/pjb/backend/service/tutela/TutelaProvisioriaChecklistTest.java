package com.tcc.pjb.backend.service.tutela;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TutelaProvisioriaChecklistTest {

    private final TutelaProvisioriaChecklistService svc = new TutelaProvisioriaChecklistService();

    @Test
    void urgenciaAnticipadaViavelComRequisitosAtendidos() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                true, true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.tutelaViavel()).isTrue();
        assertThat(result.pendenciasIdentificadas()).isEmpty();
    }

    @Test
    void urgenciaAnticipadaInviavelSemFumus() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                false, true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.tutelaViavel()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("probabilidade") || p.contains("300"));
    }

    @Test
    void urgenciaAnticipadaInviavelSemPericulum() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                true, false, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.tutelaViavel()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("perigo") || p.contains("urgência"));
    }

    @Test
    void tutelaIrreversívelGeraPendencia() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                true, true, false, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("irreversív") || p.contains("300 §3"));
    }

    @Test
    void tutelaCautelarInformaFungibilidade() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_CAUTELAR,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                true, true, true, false, true, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("305") || o.contains("cautelar"));
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("ungibilidade") || o.contains("nominação"));
    }

    @Test
    void evidenciaViavelComFundamentoInformado() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.EVIDENCIA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                TutelaProvisioriaChecklistService.FundamentoEvidencia.PROVA_DOCUMENTAL_INEQUIVOCA,
                false, false, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.tutelaViavel()).isTrue();
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("311") || o.contains("inequívoca"));
    }

    @Test
    void evidenciaSemFundamentoGeraPendencia() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.EVIDENCIA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                false, false, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.tutelaViavel()).isFalse();
        assertThat(result.pendenciasIdentificadas()).anyMatch(p -> p.contains("311") || p.contains("fundamento"));
    }

    @Test
    void evidenciaAbusosDefesaInformaArt311I() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.EVIDENCIA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                TutelaProvisioriaChecklistService.FundamentoEvidencia.ABUSO_DEFESA_PROTELATORIA,
                false, false, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("311 I") || o.contains("protelatória") || o.contains("abuso"));
    }

    @Test
    void tutelaAntecedenteInformaEstabilizacaoArt304() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.ANTECEDENTE,
                null,
                true, true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("304") || o.contains("estabilização") || o.contains("stabiliz"));
    }

    @Test
    void liminarInicialInformaArt300Par2() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.LIMINAR_INICIAL,
                null,
                true, true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("300 §2") || o.contains("inicial") || o.contains("itar o réu"));
    }

    @Test
    void liminarSemContraditorioCabivelQuandoUrgente() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                true, true, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.liminarCabivelSemContraditorio()).isTrue();
    }

    @Test
    void liminarSemContraditoriaNaoCabivelQuandoSemPericulum() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                true, false, true, false, false, false);
        var result = svc.avaliar(input);
        assertThat(result.liminarCabivelSemContraditorio()).isFalse();
    }

    @Test
    void efetivacaoImediataInformaAstreintes() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                true, true, true, false, false, true);
        var result = svc.avaliar(input);
        assertThat(result.orientacoes()).anyMatch(o -> o.contains("297") || o.contains("astreintes") || o.contains("coerção"));
    }

    @Test
    void sinalizacaoSemPendencias() {
        var input = new TutelaProvisioriaChecklistService.TutelaProvisioriaInput(
                TutelaProvisioriaChecklistService.TipoTutela.URGENCIA_ANTECIPADA,
                TutelaProvisioriaChecklistService.MomentoRequerimento.INCIDENTAL,
                null,
                true, true, true, true, false, false);
        var result = svc.avaliar(input);
        assertThat(result.sinalizacao()).contains("advogado");
    }

    @Test
    void enumTipoTutelaTemTresTipos() {
        assertThat(TutelaProvisioriaChecklistService.TipoTutela.values()).hasSize(3);
    }

    @Test
    void enumFundamentoEvidenciaTemQuatroTipos() {
        assertThat(TutelaProvisioriaChecklistService.FundamentoEvidencia.values()).hasSize(4);
    }
}
