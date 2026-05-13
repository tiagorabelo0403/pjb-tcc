package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class NegotiationLanguageHeuristicsTest {

    @Test
    void shouldTreatSemAcordoAsFrictionAndNotPositive() {
        String text = "Sem acordo nesta rodada; a proposta segue inviável.";
        assertTrue(NegotiationLanguageHeuristics.containsSettlementFriction(text));
        assertFalse(NegotiationLanguageHeuristics.containsPositiveSettlementSignal(text));
    }

    @Test
    void shouldTreatNaoAceitamosAsFrictionAndNotPositive() {
        String text = "Não aceitamos a minuta e não há acordo neste momento.";
        assertTrue(NegotiationLanguageHeuristics.containsSettlementFriction(text));
        assertFalse(NegotiationLanguageHeuristics.containsPositiveSettlementSignal(text));
    }

    @Test
    void shouldTreatExplicitAcceptanceAsPositiveSignal() {
        String text = "Aceitamos o acordo e podemos fechar a minuta final.";
        assertFalse(NegotiationLanguageHeuristics.containsSettlementFriction(text));
        assertTrue(NegotiationLanguageHeuristics.containsPositiveSettlementSignal(text));
    }
}
