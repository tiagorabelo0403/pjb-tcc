package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingReportAssemblerTest {

    @Test
    void mustAssembleRoutingReportWithoutReintroducingLogicIntoService() {
        NationalProceduralRoutingReportAssembler assembler = new NationalProceduralRoutingReportAssembler();

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("fonte", "teste");
        metadata.put("transientNull", null);

        ProceduralRoutingReport report = assembler.assemble(
                new NationalProceduralRoutingReportAssemblyContext(
                        new NationalProceduralActionProfile("INDENIZATORIA", "CIVEL", false, "COMUM_ORDINARIO", "CIVEL", List.of("MARCADOR"), List.of(), List.of(), List.of(), List.of()),
                        "COMUM",
                        "FAZENDA_PUBLICA",
                        "ESTADUAL",
                        "COMUM_ORDINARIO",
                        "TJCE",
                        "Tribunal de Justica do Ceara",
                        "PJE",
                        "Foro de Fortaleza/CE",
                        "Fortaleza",
                        "CE",
                        "VARA-01",
                        "FAZENDA",
                        "ALTA",
                        "DOCUMENTAL",
                        new NationalProceduralJuizadoDecision(true, null, List.of(), List.of(), List.of(), List.of(), 0.87d, false),
                        new NationalProceduralReviewSynthesis(List.of("razao"), List.of("base"), List.of("alerta"), List.of("valorCausa"), List.of("MARCADOR"), List.of("checklist"), List.of("bloqueio"), 0.84d, true, "MEDIUM"),
                        new ProceduralEconomicGateReport(Instant.now(), "JU" , "JEF", "MEDIA", true, false, true, "FAZENDA_PUBLICA", null, null, 2026, "seguir", List.of(), List.of(), List.of(), Map.of()),
                        new ProceduralForumAllocationReport(Instant.now(), "7", "Procedimento Comum", "DOMICILIO_AUTOR", "Fortaleza", "CE", "fundamento", "NENHUM", "NENHUM", List.of(), "TJCE", "Tribunal de Justica do Ceara", "VARA-01", "1a Vara", "FAZENDA", false, true, 93.1d, "PJE", true, false, false, true, true, "APTO", List.of(), List.of(), List.of(), Map.of()),
                        metadata
                )
        );

        assertEquals("INDENIZATORIA", report.actionNature());
        assertEquals("ESTADUAL", report.tipoJusticaSugerida());
        assertEquals("TJCE", report.tribunalCodigo());
        assertEquals("Foro de Fortaleza/CE", report.foroSugerido());
        assertTrue(report.admiteJuizado());
        assertTrue(report.exigeRevisaoHumana());
        assertFalse(report.ritoEspecial());
        assertEquals(0.84d, report.confidence());
        assertEquals("teste", report.metadata().get("fonte"));
        assertFalse(report.metadata().containsKey("transientNull"));
    }
}
