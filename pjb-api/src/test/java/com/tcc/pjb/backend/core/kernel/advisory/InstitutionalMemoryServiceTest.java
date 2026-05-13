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

class InstitutionalMemoryServiceTest {

    private final InstitutionalMemoryService service = new InstitutionalMemoryService();

    @Test
    void shouldProduceReusablePlaybooksForStructuredRequest() {
        LaianePeticaoAssistRequest request = LaianePeticaoAssistRequest.builder()
                .classeTpu("7")
                .ramoDireito("CIVEL")
                .requerJuizadoEspecial(true)
                .requerLiminar(true)
                .cpfCnpjAutor("111")
                .cpfCnpjReu("222")
                .materiaPrincipal("Consumidor")
                .build();
        CanonicalContext canonical = new CanonicalContext(
                Instant.now(),
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL,
                "CIVEL",
                "7",
                "Procedimento do Juizado",
                "ESTADUAL",
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "PJE",
                List.of("AUTOR", "REU"),
                List.of("INICIAL", "PROVA"),
                List.of("JUIZADO"),
                Map.of()
        );
        DynamicCompetenceDistributionResponse competencia = new DynamicCompetenceDistributionResponse(
                "REQ-1",
                Instant.now(),
                "NUP-1",
                "UNID-1",
                "TJCE",
                "FORTALEZA",
                "CE",
                "VARA_CIVEL",
                0.91d,
                true,
                "Distribuição automática disponível",
                List.of(),
                List.of(),
                List.of(),
                null
        );
        LegalCoherenceReport coherence = new LegalCoherenceReport(0.81d, false, List.of(), List.of("Narrativa estável"), List.of("Separar urgência e mérito"));
        ProcessIntegrityRadarReport radar = new ProcessIntegrityRadarReport("OK", 0.84d, false, List.of(), List.of("Conferir integridade final"), List.of("Rito estável"), Map.of());
        StrategicCopilotReport copilot = new StrategicCopilotReport("PETITION_ASSIST", "INICIAL", 0.8d, List.of(), List.of(), List.of(), List.of(), List.of(), List.of("Classe TPU consolidada"), Map.of());

        InstitutionalMemoryReport report = service.analyzeRequest(request, canonical, "JUIZADO_ESPECIAL_CIVEL", competencia, coherence, radar, copilot);

        assertEquals("INSTITUTIONAL_MEMORY_READY", report.status());
        assertFalse(report.reusablePlaybooks().isEmpty());
        assertTrue(report.memoryKeys().stream().anyMatch(k -> k.startsWith("rito:")));
    }

    @Test
    void shouldDowngradeProcessMemoryWhenWorkflowHasBlockers() {
        Processo processo = Processo.builder()
                .id(19L)
                .numeroUnificado("00019")
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .valorCausa(BigDecimal.valueOf(1500))
                .build();
        ProcessIntegrityRadarReport radar = new ProcessIntegrityRadarReport("BLOCKING", 0.41d, true, List.of(), List.of("Revisar nulidade"), List.of(), Map.of());
        SettlementAdvisoryReport settlement = new SettlementAdvisoryReport(
                "ATTENTION",
                0.7d,
                true,
                new NegotiationWindowReport("COOPERATIVO", 0.77d, BigDecimal.valueOf(500), BigDecimal.valueOf(800), BigDecimal.valueOf(1000), List.of(), List.of(), List.of()),
                List.of(),
                List.of("Formalizar cláusula de vencimento antecipado"),
                List.of(),
                Map.of()
        );
        StrategicCopilotReport copilot = new StrategicCopilotReport("PROCESS_TWIN", "CONHECIMENTO", 0.7d, List.of(), List.of(), List.of(new StrategicCopilotReport.Action("A", "Saneamento", "HIGH", "Pendência", List.of())), List.of(), List.of(), List.of(), Map.of());
        var ritoPlan = new com.tcc.pjb.backend.service.rito.dto.RitoPlanDto();
        ritoPlan.setBlockingOpen(List.of(com.tcc.pjb.backend.model.dto.workitem.WorkItemDto.builder().titulo("EMENDA").blocking(true).build()));

        InstitutionalMemoryReport report = service.analyzeProcess(processo, "COMUM_ORDINARIO", ritoPlan, radar, copilot, settlement);

        assertEquals("PROCESS_MEMORY_REVIEW", report.status());
        assertFalse(report.repeatedFailureModes().isEmpty());
    }
}
