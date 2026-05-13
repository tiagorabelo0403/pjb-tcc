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

class NegotiationApprovalMatrixServiceTest {

    private final NegotiationChatDigestService chatDigestService = new NegotiationChatDigestService();
    private final NegotiationApprovalMatrixService service = new NegotiationApprovalMatrixService();

    @Test
    void shouldRequireExecutiveEscalationWhenChatDependsOnApproval() {
        Processo processo = Processo.builder().id(70L).numeroUnificado("0070").faseAtual(FaseProcessual.CONHECIMENTO).build();
        PropostaAcordo proposta = PropostaAcordo.builder()
                .id(9L)
                .status(StatusAcordo.EM_NEGOCIACAO)
                .valorAcordo(BigDecimal.valueOf(3200))
                .build();
        ChatMensagem m1 = ChatMensagem.builder().conteudo("Preciso de aprovação da diretoria e do cliente antes de aceitar a proposta").dataEnvio(LocalDateTime.now()).build();
        ChatMensagem m2 = ChatMensagem.builder().conteudo("O prazo é hoje e precisamos revisar a multa de inadimplemento").dataEnvio(LocalDateTime.now()).build();
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("ATTENTION", 0.62d, true, new NegotiationWindowReport("MISTO", 0.61d, BigDecimal.valueOf(3000), BigDecimal.valueOf(3200), BigDecimal.valueOf(3500), List.of(), List.of("Prazo curto"), List.of()), List.of(), List.of("Conferir multa e cronograma"), List.of("Solicitar aceite formal"), Map.of());
        NegotiationMemoryReport memory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_ATTENTION", 0.63d, List.of("Chat pede validação hierárquica"), List.of("Rodada para em diretoria"), List.of("Antecipar trilha de aprovação"), List.of("Sigilo de números"), List.of(), Map.of());
        NegotiationExplainabilityReport explainability = new NegotiationExplainabilityReport("NEGOTIATION", "NEGOTIATION_EXPLAINABILITY_ATTENTION", 0.59d, List.of(), List.of("Quem autoriza o fechamento econômico?"), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_ATTENTION", 0.58d, List.of("Governança contratual"), List.of("Necessidade de aprovação externa"), List.of("Controlar compartilhamento sensível"), List.of("Escalar para patrocinador executivo"), List.of(), Map.of());
        KernelOperationalGovernanceReport kernel = new KernelOperationalGovernanceReport("KERNEL", "KERNEL_GOVERNANCE_ATTENTION", 0.57d, List.of(), List.of("controle:alçada"), List.of("registrar responsável de liberação"), List.of("vigiar urgência"), Map.of());

        NegotiationChatDigestReport digest = chatDigestService.analyzeProcess(processo, proposta, List.of(m1, m2), settlement, memory, explainability, governance, kernel);
        NegotiationApprovalMatrixReport report = service.analyzeProcess(processo, proposta, List.of(m1, m2), governance, kernel, memory, explainability, digest);

        assertEquals("EXECUTIVE_ESCALATION", report.approvalBand());
        assertEquals("BLOCKED_RELEASE", report.releaseMode());
        assertFalse(report.approvalGates().isEmpty());
        assertFalse(report.escalationLanes().isEmpty());
        assertFalse(report.internalControls().isEmpty());
    }

    @Test
    void shouldAllowControlledReleaseWhenGovernanceIsReady() {
        Processo processo = Processo.builder().id(71L).numeroUnificado("0071").faseAtual(FaseProcessual.EXECUCAO).build();
        PropostaAcordo proposta = PropostaAcordo.builder()
                .id(10L)
                .status(StatusAcordo.EM_NEGOCIACAO)
                .valorAcordo(BigDecimal.valueOf(1800))
                .aprovadoPor(22L)
                .dataAprovacao(LocalDateTime.now())
                .build();
        ChatMensagem m1 = ChatMensagem.builder().conteudo("Aceitamos fechar com cronograma e garantia já alinhados").dataEnvio(LocalDateTime.now()).build();
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("READY", 0.86d, true, new NegotiationWindowReport("COOPERATIVO", 0.81d, BigDecimal.valueOf(1500), BigDecimal.valueOf(1800), BigDecimal.valueOf(2000), List.of(), List.of(), List.of()), List.of(), List.of("Blindar cronograma"), List.of("Encaminhar aceite final"), Map.of());
        NegotiationMemoryReport memory = new NegotiationMemoryReport("NEGOTIATION", "NEGOTIATION_MEMORY_STABLE", 0.8d, List.of("Fechamento com enforcement"), List.of(), List.of("Confirmar aceite final"), List.of(), List.of(), Map.of());
        NegotiationExplainabilityReport explainability = new NegotiationExplainabilityReport("NEGOTIATION", "NEGOTIATION_EXPLAINABILITY_STABLE", 0.79d, List.of(), List.of(), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_STABLE", 0.82d, List.of("Governança estável"), List.of(), List.of("Preservar enforcement"), List.of(), List.of(), Map.of());
        KernelOperationalGovernanceReport kernel = new KernelOperationalGovernanceReport("KERNEL", "KERNEL_GOVERNANCE_STABLE", 0.81d, List.of(), List.of(), List.of("Emitir confirmação final"), List.of(), Map.of());

        NegotiationChatDigestReport digest = chatDigestService.analyzeProcess(processo, proposta, List.of(m1), settlement, memory, explainability, governance, kernel);
        NegotiationApprovalMatrixReport report = service.analyzeProcess(processo, proposta, List.of(m1), governance, kernel, memory, explainability, digest);

        assertEquals("READY_FOR_RELEASE", report.approvalBand());
        assertEquals("CLOSEOUT_RELEASE", report.releaseMode());
        assertTrue(report.approvalGates().isEmpty());
        assertFalse(report.releaseChecklist().isEmpty());
    }
}
