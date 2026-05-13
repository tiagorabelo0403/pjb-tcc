package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralJurisdictionIntakeResolverTest {

    @Test
    void mustProjectElectoralIntakeWithoutFreeChoiceOfUnit() {
        NationalProceduralJurisdictionIntakeResolver resolver = new NationalProceduralJurisdictionIntakeResolver(new NationalProceduralJurisdictionIntakeMessages());
        NationalProceduralRoutingMetadataContext context = new NationalProceduralRoutingMetadataContext(
                Map.of(
                        "classe", "AIJE",
                        "pedidoPrincipal", "cassação",
                        "tribunalCodigo", "TRECE"
                ),
                "context",
                "MATCHED",
                Map.of(),
                Map.of(),
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                new ProceduralForumAllocationReport(
                        java.time.Instant.now(),
                        "1",
                        "AIJE",
                        "ELEITORAL",
                        "Morada Nova",
                        "CE",
                        "fundamento",
                        null,
                        null,
                        java.util.List.of(),
                        "TRECE",
                        "TRE do Ceará",
                        "ZE-47",
                        "47ª Zona Eleitoral",
                        "ZONA_ELEITORAL",
                        true,
                        true,
                        0.9d,
                        "PJe",
                        true,
                        false,
                        false,
                        true,
                        true,
                        "READY",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        Map.of()
                ),
                "AIJE",
                "ELEITORAL",
                TipoJustica.ELEITORAL,
                "ELEITORAL_AIJE",
                "ALTA",
                "DOCUMENTAL_DIGITAL",
                0.91d,
                "MEDIO",
                "hash"
        );

        ProceduralJurisdictionIntakeReport report = resolver.resolve(context);

        assertEquals("ELEITORAL", report.branchProfile());
        assertEquals("ZONA_ELEITORAL_PADRAO", report.filingTier());
        assertFalse(report.userMayChooseJudicialUnit());
        assertTrue(report.mayStartAtTribunal());
        assertTrue(report.technicalSelectionOptional());
        assertTrue(report.noviceSafe());
        assertEquals("FATOS_PRIMARIOS_COM_TRIAGEM_ASSISTIDA", report.intakeMode());
        assertFalse(report.guidedQuestions().isEmpty());
        assertTrue(report.guidedQuestions().stream().anyMatch(item -> "ELEITORAL_PLEITO".equals(item.get("code"))));
        assertTrue(report.warnings().stream().anyMatch(item -> item.contains("o sistema continua guiando pelos fatos") || item.contains("peticionante não precisa saber previamente")));
    }


    @Test
    void mustPreferFederalFactsInsteadOfSectionSelection() {
        NationalProceduralJurisdictionIntakeResolver resolver = new NationalProceduralJurisdictionIntakeResolver(new NationalProceduralJurisdictionIntakeMessages());
        NationalProceduralRoutingMetadataContext context = new NationalProceduralRoutingMetadataContext(
                Map.of(
                        "classe", "ACAO PREVIDENCIARIA",
                        "pedidoPrincipal", "concessão de benefício",
                        "tribunalCodigo", "TRF5"
                ),
                "context",
                "MATCHED",
                Map.of(),
                Map.of(),
                null,
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null,
                new ProceduralForumAllocationReport(
                        java.time.Instant.now(),
                        "2",
                        "ACAO PREVIDENCIARIA",
                        "FEDERAL",
                        "Fortaleza",
                        "CE",
                        "fundamento",
                        null,
                        null,
                        java.util.List.of(),
                        "TRF5",
                        "TRF da 5a Regiao",
                        "JFCE-FOR",
                        "Unidade Federal",
                        "VARA_FEDERAL",
                        true,
                        true,
                        0.9d,
                        "PJe",
                        true,
                        false,
                        false,
                        true,
                        true,
                        "READY",
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        Map.of()
                ),
                "ACAO PREVIDENCIARIA",
                "FEDERAL",
                TipoJustica.FEDERAL,
                "PREVIDENCIARIO_COMUM",
                "ALTA",
                "DOCUMENTAL_DIGITAL",
                0.91d,
                "MEDIO",
                "hash"
        );

        ProceduralJurisdictionIntakeReport report = resolver.resolve(context);

        assertTrue(report.mandatorySignals().contains("vinculoTerritorialFederal"));
        assertFalse(report.mandatorySignals().contains("secaoOuSubsecaoJudiciaria"));
        assertTrue(report.guidedQuestions().stream().anyMatch(item -> "FEDERAL_TERRITORIO".equals(item.get("code"))
                && String.valueOf(item.get("question")).contains("onde ocorreu o fato")));
        assertTrue(report.warnings().stream().anyMatch(item -> item.contains("se isso não existir, o sistema continua guiando pelos fatos")));
    }

}
