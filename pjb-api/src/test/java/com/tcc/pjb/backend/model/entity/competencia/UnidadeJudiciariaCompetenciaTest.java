package com.tcc.pjb.backend.model.entity.competencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UnidadeJudiciariaCompetenciaTest {

    private UnidadeJudiciariaCompetencia unidade() {
        Tribunal tribunal = new Tribunal("TJCE", "Tribunal de Justiça do Ceará", TipoJustica.ESTADUAL, GrauJurisdicao.SEGUNDO_GRAU, "CE");
        Comarca comarca = mock(Comarca.class);
        return new UnidadeJudiciariaCompetencia("VARA-CIVEL-01", tribunal, comarca, "CE", TipoJustica.ESTADUAL,
                RamoDireito.CIVIL, TipoVaraDistribuicao.CIVEL_GERAL);
    }

    @Test
    void naoPermiteMarcarTurmaRecursalAntesDeDefinirGrauSegundoGrau() {
        UnidadeJudiciariaCompetencia unidade = unidade();

        assertThatThrownBy(() -> unidade.setTipoTurmaRecursal(TipoTurmaRecursal.CIVEL))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void permiteMarcarTurmaRecursalDepoisDeElevarOGrau() {
        UnidadeJudiciariaCompetencia unidade = unidade();

        unidade.setGrau(GrauJurisdicao.SEGUNDO_GRAU);
        unidade.setTipoTurmaRecursal(TipoTurmaRecursal.CIVEL);

        assertThat(unidade.getGrau()).isEqualTo(GrauJurisdicao.SEGUNDO_GRAU);
        assertThat(unidade.getTipoTurmaRecursal()).isEqualTo(TipoTurmaRecursal.CIVEL);
    }

    @Test
    void naoPermiteRebaixarGrauEnquantoMarcadaComoTurmaRecursal() {
        UnidadeJudiciariaCompetencia unidade = unidade();
        unidade.setGrau(GrauJurisdicao.SEGUNDO_GRAU);
        unidade.setTipoTurmaRecursal(TipoTurmaRecursal.CRIMINAL);

        assertThatThrownBy(() -> unidade.setGrau(GrauJurisdicao.PRIMEIRO_GRAU))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void permiteAlterarGrauLivrementeSemMarcacaoDeTurmaRecursal() {
        UnidadeJudiciariaCompetencia unidade = unidade();

        unidade.setGrau(GrauJurisdicao.PRIMEIRO_GRAU);

        assertThat(unidade.getGrau()).isEqualTo(GrauJurisdicao.PRIMEIRO_GRAU);
        assertThat(unidade.getTipoTurmaRecursal()).isNull();
    }

    @Test
    void varaLotadaMasAtivaContinuaAptaParaDistribuicaoMasSemFolgaParaReceber() {
        UnidadeJudiciariaCompetencia unidade = unidade();
        unidade.setAceitaDistribuicao(true);
        unidade.setIndiceCongestionamento(BigDecimal.ONE);

        assertThat(unidade.estaAptaParaDistribuicao()).isTrue();
        assertThat(unidade.temDisponibilidadeParaReceber()).isFalse();
    }

    @Test
    void varaComFolgaEstaAptaETemDisponibilidade() {
        UnidadeJudiciariaCompetencia unidade = unidade();
        unidade.setAceitaDistribuicao(true);
        unidade.setIndiceCongestionamento(new BigDecimal("0.10"));

        assertThat(unidade.estaAptaParaDistribuicao()).isTrue();
        assertThat(unidade.temDisponibilidadeParaReceber()).isTrue();
    }

    @Test
    void varaQueNaoAceitaDistribuicaoNaoEhAptaNemTemFolga() {
        UnidadeJudiciariaCompetencia unidade = unidade();
        unidade.setAceitaDistribuicao(false);
        unidade.setIndiceCongestionamento(new BigDecimal("0.10"));

        assertThat(unidade.estaAptaParaDistribuicao()).isFalse();
        assertThat(unidade.temDisponibilidadeParaReceber()).isFalse();
    }
}
