package com.tcc.pjb.backend.service.institutional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.GrauSigilo;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstituicaoVisibilidadePolicyTest {

    private static final UUID INSTITUICAO_ID = UUID.randomUUID();
    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final LocalDate HOJE = LocalDate.now();

    private InstituicaoJudicial varaUnica() {
        return new InstituicaoJudicial(INSTITUICAO_ID, "Vara Única de Teste", "VU-CE",
                new InstituicaoJudicialTipo.VaraUnica(), "COMARCA_CE",
                UUID.randomUUID(), "CE", Set.of(), InstituicaoConfiguracao.padraoPrimeiroGrau());
    }

    private LotacaoInstitucional lotacaoAtiva(PapelInstitucional papel) {
        return LotacaoInstitucional.principal(USUARIO_ID, INSTITUICAO_ID, papel,
                HOJE.minusDays(30));
    }

    @Test
    void magistradoVeProcessoPublico() {
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.magistrado()), varaUnica(),
                GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.decisao()).isEqualTo(InstituicaoVisibilidadePolicy.Decisao.PODE_ASSINAR);
        assertThat(resultado.stepUpNecessario()).isFalse();
    }

    @Test
    void servidorVeProcessoPublico() {
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.servidorJudiciario()), varaUnica(),
                GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.decisao()).isIn(
                InstituicaoVisibilidadePolicy.Decisao.PODE_MOVIMENTAR,
                InstituicaoVisibilidadePolicy.Decisao.PODE_ASSINAR);
    }

    @Test
    void servidorRequerStepUpParaProcessoSigiloso() {
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.servidorJudiciario()), varaUnica(),
                GrauSigilo.SIGILOSO, HOJE);
        assertThat(resultado.decisao()).isEqualTo(InstituicaoVisibilidadePolicy.Decisao.REQUER_STEP_UP);
        assertThat(resultado.stepUpNecessario()).isTrue();
    }

    @Test
    void servidorNegadoParaProcessoUltrassecreto() {
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.servidorJudiciario()), varaUnica(),
                GrauSigilo.ULTRASSECRETO, HOJE);
        assertThat(resultado.decisao()).isEqualTo(InstituicaoVisibilidadePolicy.Decisao.NEGADO);
    }

    @Test
    void magistradoAcessaProcessoSigilosoComBreakGlass() {
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.magistrado()), varaUnica(),
                GrauSigilo.SIGILOSO, HOJE);
        assertThat(resultado.decisao()).isEqualTo(InstituicaoVisibilidadePolicy.Decisao.PODE_ASSINAR);
        assertThat(resultado.motivos()).anyMatch(m -> m.contains("break-glass"));
    }

    @Test
    void lotacaoInativaResultaEmNegado() {
        var lotacaoExpirada = new LotacaoInstitucional(USUARIO_ID, INSTITUICAO_ID,
                PapelInstitucional.magistrado(),
                HOJE.minusDays(60), HOJE.minusDays(1), true, false, null);
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoExpirada, varaUnica(), GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.decisao()).isEqualTo(InstituicaoVisibilidadePolicy.Decisao.NEGADO);
    }

    @Test
    void substituicaoTemporariaPermiteAcesso() {
        UUID substituindoId = UUID.randomUUID();
        var cobertura = LotacaoInstitucional.cobertura(USUARIO_ID, INSTITUICAO_ID,
                PapelInstitucional.substituto(), HOJE.minusDays(5), HOJE.plusDays(5), substituindoId);
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                cobertura, varaUnica(), GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.decisao()).isNotEqualTo(InstituicaoVisibilidadePolicy.Decisao.NEGADO);
    }

    @Test
    void assessorSoPodeVer() {
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.assessor()), varaUnica(),
                GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.decisao()).isIn(
                InstituicaoVisibilidadePolicy.Decisao.PODE_VER,
                InstituicaoVisibilidadePolicy.Decisao.PODE_MOVIMENTAR);
    }

    @Test
    void plantaoApenasParaUrgencias() {
        var plantao = new InstituicaoJudicial(INSTITUICAO_ID, "Plantão", "PLANTAO-CE",
                new InstituicaoJudicialTipo.Plantao("PLANTAO-2026-01"),
                "COMARCA_CE", UUID.randomUUID(), "CE", Set.of(),
                InstituicaoConfiguracao.padraoPrimeiroGrau());
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.magistrado()), plantao,
                GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.decisao()).isEqualTo(InstituicaoVisibilidadePolicy.Decisao.PODE_ASSINAR);
    }

    @Test
    void crossInstitucionalRegistraRisco() {
        UUID outraInstituicaoId = UUID.randomUUID();
        var lotacaoOutra = LotacaoInstitucional.principal(USUARIO_ID, outraInstituicaoId,
                PapelInstitucional.magistrado(), HOJE.minusDays(10));
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoOutra, varaUnica(), GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.riscos()).anyMatch(r -> r.contains("cross-institucional"));
    }

    @Test
    void explicacaoEstaPreenchida() {
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.magistrado()), varaUnica(),
                GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.explicacaoDetalhada()).isNotBlank();
    }

    @Test
    void motivosContemPapelELotacao() {
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.servidorJudiciario()), varaUnica(),
                GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.motivos()).anyMatch(m -> m.contains("SERVIDOR_JUDICIARIO"));
        assertThat(resultado.motivos()).anyMatch(m -> m.contains("Lotação"));
    }

    @Test
    void jecItineranteAceitaAcessoBasico() {
        var jec = new InstituicaoJudicial(INSTITUICAO_ID, "JEC Itinerante", "JEC-IT-CE",
                new InstituicaoJudicialTipo.JecItinerante("Quixadá"),
                "COMARCA_CE", UUID.randomUUID(), "CE", Set.of(),
                InstituicaoConfiguracao.padraoPrimeiroGrau());
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.magistrado()), jec,
                GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.decisao()).isEqualTo(InstituicaoVisibilidadePolicy.Decisao.PODE_ASSINAR);
    }

    @Test
    void comarcaPequenaComMagistradoSubstituto() {
        UUID substituidoId = UUID.randomUUID();
        var cobertura = LotacaoInstitucional.cobertura(USUARIO_ID, INSTITUICAO_ID,
                PapelInstitucional.substituto(), HOJE.minusDays(1), HOJE.plusDays(10), substituidoId);
        assertThat(cobertura.substituindo()).isPresent();
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                cobertura, varaUnica(), GrauSigilo.RESTRITO, HOJE);
        assertThat(resultado.decisao()).isNotEqualTo(InstituicaoVisibilidadePolicy.Decisao.NEGADO);
    }

    @Test
    void processoNaoSigilosoNaoExigeStepUp() {
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(PapelInstitucional.servidorJudiciario()), varaUnica(),
                GrauSigilo.PUBLICO, HOJE);
        assertThat(resultado.stepUpNecessario()).isFalse();
    }

    @Test
    void magistradoNegadoParaUltrassecreto() {
        var magistradoSemBreakGlass = new PapelInstitucional("MAG_RESTRITO", "Magistrado Restrito",
                Set.of("DECIDIR", "ASSINAR"), 10, true, true, true, false, false);
        var resultado = InstituicaoVisibilidadePolicy.avaliar(
                lotacaoAtiva(magistradoSemBreakGlass), varaUnica(),
                GrauSigilo.ULTRASSECRETO, HOJE);
        assertThat(resultado.decisao()).isEqualTo(InstituicaoVisibilidadePolicy.Decisao.NEGADO);
    }
}
