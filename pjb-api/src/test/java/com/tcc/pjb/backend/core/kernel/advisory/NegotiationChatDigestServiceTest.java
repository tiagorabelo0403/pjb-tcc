package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NegotiationChatDigestServiceTest {

    private final NegotiationChatDigestService service = new NegotiationChatDigestService();

    @Test
    void shouldBuildConvergingDigest() {
        Processo processo = Processo.builder().id(41L).numeroUnificado("0041").faseAtual(FaseProcessual.CONHECIMENTO).build();
        PropostaAcordo proposta = PropostaAcordo.builder()
                .id(6L)
                .status(StatusAcordo.EM_NEGOCIACAO)
                .valorAcordo(BigDecimal.valueOf(1200))
                .aprovadoPor(9L)
                .dataAprovacao(LocalDateTime.now())
                .build();
        ChatMensagem m1 = ChatMensagem.builder().conteudo("Aceitamos proposta com parcelamento e garantia").dataEnvio(LocalDateTime.now()).build();
        ChatMensagem m2 = ChatMensagem.builder().conteudo("Podemos fechar hoje se a multa de inadimplência constar").dataEnvio(LocalDateTime.now()).build();
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("READY", 0.84d, true, new NegotiationWindowReport("COOPERATIVO", 0.8d, BigDecimal.valueOf(900), BigDecimal.valueOf(1200), BigDecimal.valueOf(1500), List.of(), List.of(), List.of()), List.of(), List.of("Blindar multa e cronograma"), List.of("Encaminhar minuta final"), Map.of());
        NegotiationMemoryReport negotiationMemory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_STABLE", 0.82d, List.of("A conversa converge com enforcement"), List.of(), List.of("Usar âncora já aceita"), List.of(), List.of(), Map.of());
        NegotiationExplainabilityReport explainability = new NegotiationExplainabilityReport("NEGOTIATION", "NEGOTIATION_EXPLAINABILITY_STABLE", 0.8d, List.of(), List.of(), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_STABLE", 0.8d, List.of("Âncora institucional"), List.of(), List.of("Submeter versão final homologável"), List.of(), List.of(), Map.of());
        KernelOperationalGovernanceReport kernel = new KernelOperationalGovernanceReport("KERNEL", "KERNEL_GOVERNANCE_STABLE", 0.8d, List.of("controle:mensagem"), List.of(), List.of("confirmar aceite final"), List.of(), Map.of());

        NegotiationChatDigestReport report = service.analyzeProcess(processo, proposta, List.of(m1, m2), settlement, negotiationMemory, explainability, governance, kernel);

        assertEquals("CONVERGING", report.conversationStage());
        assertEquals("NEGOTIATION_CHAT_STABLE", report.status());
        assertEquals("CLOSEOUT_RELEASE", report.sendMode());
        assertFalse(report.suggestedNextMessage().isBlank());
        assertFalse(report.nextTurnObjectives().isEmpty());
        assertFalse(report.internalActions().isEmpty());
        assertFalse(report.messageBlueprints().isEmpty());
    }

    @Test
    void shouldExposeImpasseAttention() {
        Processo processo = Processo.builder().id(42L).numeroUnificado("0042").faseAtual(FaseProcessual.EXECUCAO).build();
        ChatMensagem m1 = ChatMensagem.builder().conteudo("Sem acordo, proposta inaceitável e prazo urgente").dataEnvio(LocalDateTime.now()).build();
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("ATTENTION", 0.48d, false, new NegotiationWindowReport("TENSO", 0.4d, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of("Impasse forte"), List.of()), List.of(), List.of(), List.of(), Map.of());
        NegotiationMemoryReport negotiationMemory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_ATTENTION", 0.5d, List.of(), List.of("Histórico travado"), List.of(), List.of("Revisão de sigilo"), List.of(), Map.of());
        NegotiationExplainabilityReport explainability = new NegotiationExplainabilityReport("NEGOTIATION", "NEGOTIATION_EXPLAINABILITY_ATTENTION", 0.5d, List.of(), List.of("Há impasse aberto"), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_ATTENTION", 0.5d, List.of(), List.of("Escalada interna"), List.of(), List.of(), List.of(), Map.of());
        KernelOperationalGovernanceReport kernel = new KernelOperationalGovernanceReport("KERNEL", "KERNEL_GOVERNANCE_ATTENTION", 0.45d, List.of(), List.of("vigiar ruído"), List.of(), List.of(), Map.of());

        NegotiationChatDigestReport report = service.analyzeProcess(processo, null, List.of(m1), settlement, negotiationMemory, explainability, governance, kernel);

        assertEquals("IMPASSE", report.conversationStage());
        assertEquals("NEGOTIATION_CHAT_ATTENTION", report.status());
        assertTrue(report.posture().contains("DEESCALATION"));
        assertEquals("GUIDED_RELEASE", report.sendMode());
        assertFalse(report.escalationSignals().isEmpty());
        assertFalse(report.forbiddenMoves().isEmpty());
    }
}
