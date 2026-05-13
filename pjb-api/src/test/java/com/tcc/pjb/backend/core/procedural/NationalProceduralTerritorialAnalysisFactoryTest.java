package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class NationalProceduralTerritorialAnalysisFactoryTest {

    private final NationalProceduralForumAllocationMessages messages = new NationalProceduralForumAllocationMessages();
    private final NationalProceduralTerritorialAnalysisFactory factory =
            new NationalProceduralTerritorialAnalysisFactory(messages);
    private final NationalProceduralLinkageAnalysisFactory linkageFactory =
            new NationalProceduralLinkageAnalysisFactory(messages);

    @Test
    void prefersExplicitForumAsTerritorialAnchor() {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("foro", "Fortaleza");
        payload.put("uf", "CE");

        NationalProceduralTerritorialAnchor anchor = factory.resolveTerritorialAnchor(
                payload,
                "pedido com foro expresso",
                TipoJustica.ESTADUAL,
                "ACAO_CIVEL_GERAL",
                "CIVIL_GERAL",
                null,
                null
        );

        assertEquals("FORO_EXPRESSO", anchor.mode());
        assertEquals("Fortaleza", anchor.comarca());
        assertEquals("CE", anchor.uf());
    }

    @Test
    void marksRelatedProcessesWhenCnjNumbersAppearInCorpus() {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();

        NationalProceduralLinkageAnalysis linkage = linkageFactory.resolve(
                payload,
                "há dependência com o feito 1234567-89.2024.8.06.0001 para prevenção"
        );

        assertEquals("PREVENCAO", linkage.linkageMode());
        assertTrue(linkage.relatedProcessNumbers().contains("1234567-89.2024.8.06.0001"));
        assertTrue(linkage.reasons().stream().anyMatch(value -> value.contains("Números CNJ relacionados detectados")));
    }
}
