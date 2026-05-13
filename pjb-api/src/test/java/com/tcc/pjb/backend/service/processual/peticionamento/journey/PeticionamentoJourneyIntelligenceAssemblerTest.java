package com.tcc.pjb.backend.service.processual.peticionamento.journey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookRow;
import com.tcc.pjb.backend.core.procedural.NationalProceduralOperationalPlaybookService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRightsCoverageService;
import com.tcc.pjb.backend.core.procedural.NationalProceduralTribunalVariationRow;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.journey.PeticionamentoJourneyIntelligenceResponse;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PeticionamentoJourneyIntelligenceAssemblerTest {

    private final NationalProceduralOperationalPlaybookService playbookService = new NationalProceduralOperationalPlaybookService(new NationalProceduralRightsCoverageService());

    @Test
    void assembleProducesCompactGuidanceWithoutBackgroundTracking() {
        PeticionamentoSessaoRequest request = PeticionamentoSessaoRequest.builder()
                .tituloCaso("Obrigação de fazer")
                .parteAutora("Autor")
                .parteRe("Réu")
                .tipoJustica("ESTADUAL")
                .ritoProcessual("COMUM_ORDINARIO")
                .classeProcessual("Procedimento Comum Cível")
                .pedidos(List.of("Condenação"))
                .provasIndicadas(List.of("Contrato"))
                .valorCausa(new BigDecimal("15000.00"))
                .build();
        ProceduralSubmissionBlueprintReport blueprint = new ProceduralSubmissionBlueprintReport(
                Instant.now(),
                "req-1",
                "ASSISTIDO",
                true,
                false,
                true,
                JudicialSystem.PJE,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "123",
                "Procedimento Comum Cível",
                "VARA_CIVEL_1",
                "1ª Vara Cível",
                "COMUM_ORDINARIO",
                "PETICAO_INICIAL",
                "TERRITORIAL",
                "NENHUMA",
                "NENHUMA",
                "SEM_VINCULO",
                List.of(),
                true,
                false,
                false,
                "ACCEPTED",
                "dry-1",
                List.of(),
                List.of("Conferir valor da causa"),
                List.of(),
                Map.of(),
                Map.of()
        );
        NationalProceduralOperationalPlaybookRow playbook = playbookService.describe("COMUM_ORDINARIO");
        NationalProceduralTribunalVariationRow variation = new NationalProceduralTribunalVariationRow(
                "TJCE",
                "VARA_CIVEL_1",
                "COMUM_ORDINARIO",
                "CIVIL",
                "ESTADUAL",
                "PJE",
                true,
                false,
                false,
                List.of("PJE"),
                List.of("VARA_COMPETENTE"),
                List.of("VALIDAR_DISTRIBUICAO_LOCAL"),
                List.of(),
                Map.of()
        );

        PeticionamentoJourneyIntelligenceResponse response = PeticionamentoJourneyIntelligenceAssembler.assemble(request, blueprint, playbook, variation, null);

        assertEquals("PROTOCOLO_ASSISTIDO", response.operationalPulse());
        assertTrue(response.lowOverheadMode());
        assertTrue(response.passiveObservation());
        assertTrue(response.observedSignals().contains("PARTES_QUALIFICADAS"));
        assertFalse(response.nextActions().isEmpty());
        assertEquals("NONE", response.compactMetrics().get("sessionRetention"));
    }

    @Test
    void assembleFlagsMissingDomainsWhenPackageIsStillRaw() {
        PeticionamentoSessaoRequest request = PeticionamentoSessaoRequest.builder()
                .tituloCaso("Caso bruto")
                .build();
        ProceduralSubmissionBlueprintReport blueprint = new ProceduralSubmissionBlueprintReport(
                Instant.now(),
                "req-2",
                "BLOQUEADO",
                false,
                false,
                false,
                JudicialSystem.OUTRO,
                "PJB_PADRAO",
                "PJB",
                null,
                null,
                null,
                null,
                "COMUM_ORDINARIO",
                "PETICAO_INICIAL",
                null,
                null,
                null,
                "SEM_VINCULO",
                List.of(),
                false,
                true,
                true,
                null,
                null,
                List.of("Competência indefinida"),
                List.of(),
                List.of("Conector indisponível"),
                Map.of(),
                Map.of()
        );
        NationalProceduralOperationalPlaybookRow playbook = playbookService.describe("COMUM_ORDINARIO");
        NationalProceduralTribunalVariationRow variation = new NationalProceduralTribunalVariationRow(
                "PJB_PADRAO",
                "UNIDADE_A_DEFINIR",
                "COMUM_ORDINARIO",
                "CIVIL",
                "ESTADUAL",
                "OUTRO",
                false,
                true,
                true,
                List.of("PJB"),
                List.of(),
                List.of(),
                List.of(),
                Map.of()
        );

        PeticionamentoJourneyIntelligenceResponse response = PeticionamentoJourneyIntelligenceAssembler.assemble(request, blueprint, playbook, variation, null);

        assertEquals("SANEAMENTO", response.operationalPulse());
        assertTrue(response.missingDomains().contains("PARTES_E_REPRESENTACAO"));
        assertTrue(response.missingDomains().contains("PROVA_E_DOCUMENTOS"));
        assertTrue(response.nextActions().stream().anyMatch(action -> "EXECUTAR_STEP_UP".equals(action.code())));
    }
}
