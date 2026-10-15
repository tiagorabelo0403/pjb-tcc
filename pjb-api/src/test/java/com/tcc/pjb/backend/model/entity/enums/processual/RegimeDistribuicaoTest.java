package com.tcc.pjb.backend.model.entity.enums.processual;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RegimeDistribuicaoTest {

    @Test
    void cadaRegimeCarregaFundamentoLegal() {
        assertEquals("CPC, art. 285", RegimeDistribuicao.ALEATORIO_ALTERNADO.fundamentoLegal());
        assertEquals("CPC, art. 930", RegimeDistribuicao.SORTEIO_RELATORIA.fundamentoLegal());
        assertEquals("CPC, art. 286", RegimeDistribuicao.PREVENCAO.fundamentoLegal());
        assertEquals("CPC, art. 971, parágrafo único", RegimeDistribuicao.EXCLUSAO_LEGAL.fundamentoLegal());
        assertEquals("CPC, arts. 1.036 e 1.037", RegimeDistribuicao.AFETACAO_SOBRESTAMENTO.fundamentoLegal());
        assertEquals("CF, art. 100", RegimeDistribuicao.CRONOLOGICO.fundamentoLegal());
        assertEquals("Código Eleitoral", RegimeDistribuicao.DESIGNACAO_LEGAL.fundamentoLegal());
        assertEquals("Organização judiciária local", RegimeDistribuicao.UNIDADE_UNICA.fundamentoLegal());
    }

    @Test
    void cadaRegimeTemDescricaoNaoVazia() {
        for (RegimeDistribuicao regime : RegimeDistribuicao.values()) {
            assertFalse(regime.descricao() == null || regime.descricao().isBlank(),
                    "descrição ausente em " + regime.name());
        }
    }

    @Test
    void sorteioApenasNosRegimesComEscolhaAleatoria() {
        assertTrue(RegimeDistribuicao.ALEATORIO_ALTERNADO.implicaSorteio());
        assertTrue(RegimeDistribuicao.SORTEIO_RELATORIA.implicaSorteio());
        assertTrue(RegimeDistribuicao.EXCLUSAO_LEGAL.implicaSorteio());

        assertFalse(RegimeDistribuicao.PREVENCAO.implicaSorteio());
        assertFalse(RegimeDistribuicao.AFETACAO_SOBRESTAMENTO.implicaSorteio());
        assertFalse(RegimeDistribuicao.CRONOLOGICO.implicaSorteio());
        assertFalse(RegimeDistribuicao.DESIGNACAO_LEGAL.implicaSorteio());
        assertFalse(RegimeDistribuicao.UNIDADE_UNICA.implicaSorteio());
    }

    @Test
    void dispensaSorteioEhOInversoDeImplicaSorteio() {
        for (RegimeDistribuicao regime : RegimeDistribuicao.values()) {
            assertEquals(!regime.implicaSorteio(), regime.dispensaSorteio(), regime.name());
        }
    }
}
