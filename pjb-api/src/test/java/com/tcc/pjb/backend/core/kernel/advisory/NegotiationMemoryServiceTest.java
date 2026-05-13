package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NegotiationMemoryServiceTest {

    private final NegotiationMemoryService service = new NegotiationMemoryService();

    @Test
    void shouldExtractReusableNegotiationPatterns() {
        Processo processo = Processo.builder().id(9L).numeroUnificado("0009").faseAtual(FaseProcessual.CONHECIMENTO).build();
        PropostaAcordo proposta = PropostaAcordo.builder().id(4L).valorAcordo(BigDecimal.valueOf(800)).build();
        ChatMensagem m1 = ChatMensagem.builder().conteudo("Podemos aceitar parcelamento com multa por inadimplência").dataEnvio(LocalDateTime.now()).build();
        ChatMensagem m2 = ChatMensagem.builder().conteudo("Há prazo curto, mas existe chance de acordo").dataEnvio(LocalDateTime.now()).build();
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("READY", 0.82d, true, new NegotiationWindowReport("COOPERATIVO", 0.8d, BigDecimal.valueOf(600), BigDecimal.valueOf(800), BigDecimal.valueOf(1000), List.of(), List.of(), List.of()), List.of(), List.of("Executar cláusula de vencimento"), List.of("Avançar com proposta escrita"), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_STABLE", 0.8d, List.of(), List.of(), List.of(), List.of("Escalonar revisão final"), List.of("rito:COMUM"), Map.of());

        NegotiationMemoryReport report = service.analyzeProcess(processo, proposta, List.of(m1, m2), settlement, governance);

        assertEquals("NEGOTIATION_MEMORY_STABLE", report.status());
        assertFalse(report.learnedPatterns().isEmpty());
        assertFalse(report.reusablePlaybooks().isEmpty());
    }

    @Test
    void shouldExposeAttentionWithoutHistoryOrValue() {
        Processo processo = Processo.builder().id(11L).numeroUnificado("0011").faseAtual(FaseProcessual.EXECUCAO).build();
        PropostaAcordo proposta = PropostaAcordo.builder().id(7L).valorAcordo(BigDecimal.ZERO).build();
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("ATTENTION", 0.6d, false, new NegotiationWindowReport("TENSO", 0.5d, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of("Execução fraca"), List.of()), List.of(), List.of(), List.of(), Map.of());
        InstitutionalGovernanceContextReport governance = new InstitutionalGovernanceContextReport("NEGOTIATION", "REQUEST_GOVERNANCE_ATTENTION", 0.5d, List.of(), List.of("Atenção institucional"), List.of(), List.of(), List.of("rito:EXECUCAO"), Map.of());

        NegotiationMemoryReport report = service.analyzeProcess(processo, proposta, List.of(), settlement, governance);

        assertEquals("NEGOTIATION_MEMORY_ATTENTION", report.status());
        assertFalse(report.cautionPoints().isEmpty());
    }
}
