package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingIntelligenceResolverTest {

    @Test
    void mustProduceAdvisoryQualityAutomationExplainabilityAndAccelerationBundle() {
        NationalProceduralRoutingIntelligenceResolver resolver = new NationalProceduralRoutingIntelligenceResolver();
        NationalProceduralRoutingIntelligenceBundle bundle = resolver.analyze(
                new NationalProceduralRoutingIntelligenceContext(
                        Map.of(
                                "classe", "acao de indenizacao por danos morais",
                                "assunto", "consumo",
                                "tipoJustica", "ESTADUAL",
                                "valorCausa", 12000,
                                "urgencia", true
                        ),
                        Map.of(
                                "classeCanonical", "INDENIZATORIA",
                                "tipoJustica", "ESTADUAL",
                                "rito", "COMUM_ORDINARIO"
                        ),
                        "INDENIZATORIA",
                        "CIVEL_PATRIMONIAL",
                        TipoJustica.ESTADUAL,
                        "COMUM_ORDINARIO",
                        null,
                        null,
                        "MEDIA",
                        "DOCUMENTAL",
                        0.82d,
                        "BAIXO"
                )
        );

        assertNotNull(bundle.advisoryIntelligence());
        assertNotNull(bundle.decisionQuality());
        assertNotNull(bundle.automationPolicy());
        assertNotNull(bundle.executiveExplainability());
        assertNotNull(bundle.acceleration());
        assertTrue(bundle.toMetadataEntries().containsKey("advisoryIntelligence"));
        assertTrue(bundle.toMetadataEntries().containsKey("decisionQuality"));
        assertTrue(bundle.toMetadataEntries().containsKey("automationPolicy"));
        assertTrue(bundle.toMetadataEntries().containsKey("executiveExplainability"));
        assertTrue(bundle.toMetadataEntries().containsKey("acceleration"));
    }
}
