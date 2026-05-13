package com.tcc.pjb.backend.core.kernel.advisory;

import static org.junit.jupiter.api.Assertions.*;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExplainableDecisionTrailServiceTest {

    private final ExplainableDecisionTrailService service = new ExplainableDecisionTrailService();

    @Test
    void shouldComposeExplainableTrailForRequest() {
        LaianePeticaoAssistRequest request = LaianePeticaoAssistRequest.builder()
                .classeTpu("7")
                .ramoDireito("CIVEL")
                .materiaPrincipal("Consumidor")
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
        DynamicCompetenceDistributionResponse competencia = new DynamicCompetenceDistributionResponse("REQ-1", Instant.now(), "NUP-1", "UNID-1", "TJCE", "FORTALEZA", "CE", "VARA_CIVEL", 0.9d, true, "ok", List.of(), List.of(), List.of(), null);
        LegalCoherenceReport coherence = new LegalCoherenceReport(0.8d, false, List.of(), List.of("Coerência estável"), List.of("Refinar pedido subsidiário"));
        ProtocolDryRunReport dryRun = new ProtocolDryRunReport("READY", true, List.of(), List.of("Assinar e protocolar"), Map.of());
        ProcessIntegrityRadarReport radar = new ProcessIntegrityRadarReport("OK", 0.82d, false, List.of(), List.of(), List.of("Rito e tribunal aderentes"), Map.of());
        StrategicCopilotReport copilot = new StrategicCopilotReport("PETITION_ASSIST", "INICIAL", 0.78d, List.of(), List.of(), List.of(), List.of(), List.of(), List.of("Classe TPU consolidada"), Map.of());
        InstitutionalMemoryReport memory = new InstitutionalMemoryReport("PETITION_ASSIST", "READY", 0.8d, List.of("Playbook válido"), List.of(), List.of("Usar bloco de urgência"), List.of(), List.of("rito:COMUM_ORDINARIO"), Map.of());
        ContextualPrecedentAdvisoryReport precedents = new ContextualPrecedentAdvisoryReport("PETITION_ASSIST", "READY", 0.81d, List.of("Rito efetivo"), List.of("consumidor comum ordinario"), List.of(), List.of(), List.of(), Map.of());

        ExplainableDecisionTrailReport report = service.composeRequest(request, canonical, "COMUM_ORDINARIO", competencia, coherence, dryRun, radar, copilot, memory, precedents);

        assertEquals("EXPLAINABILITY_STABLE", report.status());
        assertEquals(5, report.nodes().size());
        assertTrue(report.openQuestions().isEmpty());
    }

    @Test
    void shouldExposeOpenQuestionsForProcessUnderAttention() {
        Processo processo = Processo.builder()
                .id(77L)
                .numeroUnificado("00077")
                .faseAtual(FaseProcessual.RECURSAL)
                .valorCausa(BigDecimal.valueOf(2000))
                .build();
        var ritoPlan = new com.tcc.pjb.backend.service.rito.dto.RitoPlanDto();
        ritoPlan.setBlockingOpen(List.of(com.tcc.pjb.backend.model.dto.workitem.WorkItemDto.builder().titulo("PREPARO").blocking(true).build()));
        LegalCoherenceReport coherence = new LegalCoherenceReport(0.4d, true, List.of(new LegalCoherenceReport.Issue("A", "Incoerência", "desc", "HIGH", true, List.of())), List.of(), List.of());
        ProtocolDryRunReport dryRun = new ProtocolDryRunReport("REVIEW", false, List.of(), List.of("Revalidar preparo"), Map.of());
        ProcessIntegrityRadarReport radar = new ProcessIntegrityRadarReport("BLOCKING", 0.4d, true, List.of(new ProcessIntegrityRadarReport.Finding("A", "RECURSAL", "Risco", "HIGH", true, "Preparação recursal insuficiente", List.of())), List.of("Conferir preparo e tempestividade"), List.of(), Map.of());
        StrategicCopilotReport copilot = new StrategicCopilotReport("PROCESS_TWIN", "RECURSAL", 0.5d, List.of(), List.of(), List.of(), List.of(), List.of(new StrategicCopilotReport.Action("N", "Negociação", "LOW", "racional", List.of())), List.of(), Map.of());
        InstitutionalMemoryReport memory = new InstitutionalMemoryReport("PROCESS_TWIN", "REVIEW", 0.5d, List.of(), List.of("Pendência recursal crítica"), List.of(), List.of(), List.of(), Map.of());
        ContextualPrecedentAdvisoryReport precedents = new ContextualPrecedentAdvisoryReport("PROCESS_TWIN", "ATTENTION", 0.4d, List.of(), List.of(), List.of(), List.of(), List.of("Não usar precedente como substituto do preparo"), Map.of());
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport("ATTENTION", 0.55d, false, new NegotiationWindowReport("TENSO", 0.4d, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of("Cláusula inexequível"), List.of()), List.of(), List.of("Blindar multa e vencimento"), List.of(), Map.of());

        ExplainableDecisionTrailReport report = service.composeProcess(processo, "RECURSAL_APELACAO", ritoPlan, coherence, dryRun, radar, copilot, memory, precedents, settlement);

        assertEquals("PROCESS_EXPLAINABILITY_ATTENTION", report.status());
        assertFalse(report.openQuestions().isEmpty());
    }
}
