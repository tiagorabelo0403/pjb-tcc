package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InstitutionalGovernanceContextServiceTest {

    private final InstitutionalGovernanceContextService service = new InstitutionalGovernanceContextService();

    @Test
    void shouldGenerateStableGovernanceForStructuredRequest() {
        LaianePeticaoAssistRequest request = LaianePeticaoAssistRequest.builder()
                .ramoDireito("CIVEL")
                .classeTpu("7")
                .cpfCnpjAutor("111")
                .cpfCnpjReu("222")
                .build();
        CanonicalContext canonical = new CanonicalContext(
                Instant.now(),
                RitoProcessual.COMUM_ORDINARIO,
                "CIVEL",
                "7",
                "Procedimento comum",
                "ESTADUAL",
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "PJE",
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );
        LegalCoherenceReport coherence = new LegalCoherenceReport(0.85d, false, List.of(), List.of("Narrativa estável"), List.of());
        InstitutionalMemoryReport memory = new InstitutionalMemoryReport("PETITION_ASSIST", "READY", 0.8d, List.of("Playbook aproveitável"), List.of(), List.of("Revisar urgência e mérito em faixas separadas"), List.of(), List.of("rito:COMUM"), Map.of());
        ContextualPrecedentAdvisoryReport precedents = new ContextualPrecedentAdvisoryReport("PETITION_ASSIST", "READY", 0.83d, List.of("dimensão:consumidor"), List.of(), List.of("perfil:magistrado-objetivo"), List.of(), List.of(), Map.of());

        InstitutionalGovernanceContextReport report = service.analyzeRequest(request, canonical, "COMUM_ORDINARIO", coherence, memory, precedents);

        assertEquals("REQUEST_GOVERNANCE_STABLE", report.status());
        assertFalse(report.policyGuards().isEmpty());
        assertTrue(report.anchorDimensions().stream().anyMatch(v -> v.contains("TJCE") || v.contains("rito:")));
    }

    @Test
    void shouldGenerateAttentionForProcessUnderSensitiveConditions() {
        Processo processo = Processo.builder()
                .id(41L)
                .numeroUnificado("000041")
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .valorCausa(BigDecimal.valueOf(1200))
                .build();
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("ATTENTION", 0.6d, false, new NegotiationWindowReport("TENSO", 0.45d, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of("Execução frágil"), List.of()), List.of("Condição suspensiva"), List.of("Blindar vencimento antecipado"), List.of(), Map.of());
        InstitutionalMemoryReport memory = new InstitutionalMemoryReport("PROCESS_TWIN", "REVIEW", 0.5d, List.of(), List.of("Falha recorrente de governança"), List.of(), List.of("Alerta institucional"), List.of(), Map.of());
        ContextualPrecedentAdvisoryReport precedents = new ContextualPrecedentAdvisoryReport("PROCESS_TWIN", "ATTENTION", 0.4d, List.of(), List.of(), List.of(), List.of(), List.of("Cautela com aderência fática"), Map.of());

        InstitutionalGovernanceContextReport report = service.analyzeProcess(processo, "COMUM_ORDINARIO", settlement, memory, precedents);

        assertEquals("PROCESS_GOVERNANCE_ATTENTION", report.status());
        assertFalse(report.governanceAlerts().isEmpty());
    }
}
