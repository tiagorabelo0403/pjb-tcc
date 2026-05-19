package com.tcc.pjb.backend.core.processo.stf.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.OrgaoJulgadorSTF;
import com.tcc.pjb.backend.model.entity.enums.ResultadoVotacaoSTF;
import org.junit.jupiter.api.Test;

class VotacaoPlenarioEngineTest {

    private final VotacaoPlenarioEngine engine = new VotacaoPlenarioEngine();

    @Test
    void maioriaPlenariaAprovado() {
        ResultadoVotacaoSTF r = engine.calcular(OrgaoJulgadorSTF.PLENARIO, 6, 5, 0, 0, false);
        assertEquals(ResultadoVotacaoSTF.APROVADO, r);
    }

    @Test
    void maioriaContrarioRejeitado() {
        ResultadoVotacaoSTF r = engine.calcular(OrgaoJulgadorSTF.PLENARIO, 5, 6, 0, 0, false);
        assertEquals(ResultadoVotacaoSTF.REJEITADO, r);
    }

    @Test
    void empateNoPlenarioSemImpedimentoSolucaoContraria() {
        ResultadoVotacaoSTF r = engine.calcular(OrgaoJulgadorSTF.PLENARIO, 5, 5, 0, 0, false);
        assertEquals(ResultadoVotacaoSTF.EMPATE_SOLUCAO_CONTRARIA, r);
    }

    @Test
    void empateComImpedimentoEPresidenteFavoravelVotoQualidade() {
        ResultadoVotacaoSTF r = engine.calcular(OrgaoJulgadorSTF.PLENARIO, 5, 5, 0, 1, true);
        assertEquals(ResultadoVotacaoSTF.EMPATE_VOTO_QUALIDADE, r);
    }

    @Test
    void votosVistaSuspendePorPedidoVista() {
        ResultadoVotacaoSTF r = engine.calcular(OrgaoJulgadorSTF.PLENARIO, 6, 4, 1, 0, false);
        assertEquals(ResultadoVotacaoSTF.SUSPENSO_PEDIDO_VISTA, r);
    }

    @Test
    void presentesAbaixoQuorumAguardaQuorum() {
        ResultadoVotacaoSTF r = engine.calcular(OrgaoJulgadorSTF.PLENARIO, 2, 2, 0, 6, false);
        assertEquals(ResultadoVotacaoSTF.AGUARDANDO_QUORUM, r);
    }

    @Test
    void turmaAprovado3a2() {
        ResultadoVotacaoSTF r = engine.calcular(OrgaoJulgadorSTF.PRIMEIRA_TURMA, 3, 2, 0, 0, false);
        assertEquals(ResultadoVotacaoSTF.APROVADO, r);
    }

    @Test
    void turmaEmpate2a2SemImpedimentoSolucaoContraria() {
        ResultadoVotacaoSTF r = engine.calcular(OrgaoJulgadorSTF.PRIMEIRA_TURMA, 2, 2, 0, 0, false);
        assertEquals(ResultadoVotacaoSTF.EMPATE_SOLUCAO_CONTRARIA, r);
    }

    @Test
    void atingeReservaPlenario6de11True() {
        assertTrue(engine.atingeReservaPlenario(6, 11));
    }

    @Test
    void naoAtingeReservaPlenario5de11False() {
        assertFalse(engine.atingeReservaPlenario(5, 11));
    }

    @Test
    void plenarioAtingeQuorumCom6() {
        assertTrue(OrgaoJulgadorSTF.PLENARIO.atingeQuorum(6));
    }

    @Test
    void plenarioNaoAtingeQuorumCom5() {
        assertFalse(OrgaoJulgadorSTF.PLENARIO.atingeQuorum(5));
    }

    @Test
    void primeiraTurmaAtingeQuorumCom3() {
        assertTrue(OrgaoJulgadorSTF.PRIMEIRA_TURMA.atingeQuorum(3));
    }

    @Test
    void primeiraTurmaQuorumMaioriaAbsoluta3() {
        assertEquals(3, OrgaoJulgadorSTF.PRIMEIRA_TURMA.quorumMaioriaAbsoluta());
    }

    @Test
    void plenarioQuorumMaioriaAbsoluta6() {
        assertEquals(6, OrgaoJulgadorSTF.PLENARIO.quorumMaioriaAbsoluta());
    }
}
