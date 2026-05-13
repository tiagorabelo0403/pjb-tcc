package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NegotiationExplainabilityServiceTest {

    private final NegotiationExplainabilityService service = new NegotiationExplainabilityService();

    @Test
    void shouldComposeNegotiationTrail() {
        Processo processo = Processo.builder().id(25L).numeroUnificado("0025").faseAtual(FaseProcessual.CONHECIMENTO).build();
        PropostaAcordo proposta = PropostaAcordo.builder().id(3L).valorAcordo(BigDecimal.valueOf(900)).build();
        ChatMensagem m1 = ChatMensagem.builder().conteudo("Aceitamos parcelamento com garantia").dataEnvio(LocalDateTime.now()).build();
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("READY", 0.8d, true, new NegotiationWindowReport("COOPERATIVO", 0.78d, BigDecimal.valueOf(700), BigDecimal.valueOf(900), BigDecimal.valueOf(1100), List.of(), List.of(), List.of()), List.of(), List.of("Executar multa contratual"), List.of("Fechar minuta"), Map.of());
        NegotiationMemoryReport negotiationMemory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_STABLE", 0.8d, List.of("Há padrão de enforcement"), List.of(), List.of("Usar âncora anterior"), List.of(), List.of(), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_STABLE", 0.8d, List.of(), List.of(), List.of("Revisão final por trilha institucional"), List.of(), List.of("rito:COMUM"), Map.of());

        NegotiationExplainabilityReport report = service.compose(processo, proposta, List.of(m1), settlement, negotiationMemory, governance);

        assertEquals("NEGOTIATION_EXPLAINABILITY_STABLE", report.status());
        assertEquals(5, report.nodes().size());
        assertTrue(report.openQuestions().isEmpty());
    }

    @Test
    void shouldExposeOpenQuestionsUnderAttention() {
        Processo processo = Processo.builder().id(26L).numeroUnificado("0026").faseAtual(FaseProcessual.EXECUCAO).build();
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("ATTENTION", 0.5d, false, new NegotiationWindowReport("TENSO", 0.4d, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of("Impasse"), List.of()), List.of(), List.of("Blindar garantias"), List.of(), Map.of());
        NegotiationMemoryReport negotiationMemory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_ATTENTION", 0.5d, List.of(), List.of("Histórico travado"), List.of(), List.of(), List.of(), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_ATTENTION", 0.5d, List.of(), List.of("Atenção institucional"), List.of(), List.of(), List.of(), Map.of());

        NegotiationExplainabilityReport report = service.compose(processo, null, List.of(), settlement, negotiationMemory, governance);

        assertEquals("NEGOTIATION_EXPLAINABILITY_ATTENTION", report.status());
        assertFalse(report.openQuestions().isEmpty());
    }
}
