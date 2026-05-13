package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralRoutingMetadataFactoryOrchestrationTest {

    @Test
    void mustComposeSeedMetadataWithIntelligencePipeline() {
        NationalProceduralRoutingMetadataSeedFactory seedFactory = Mockito.mock(NationalProceduralRoutingMetadataSeedFactory.class);
        NationalProceduralRoutingIntelligenceResolver intelligenceResolver = Mockito.mock(NationalProceduralRoutingIntelligenceResolver.class);
        NationalProceduralRoutingMetadataFactory factory = new NationalProceduralRoutingMetadataFactory(seedFactory, intelligenceResolver);

        NationalProceduralRoutingMetadataContext context = new NationalProceduralRoutingMetadataContext(
                Map.of("classe", "indenizacao"),
                "context",
                "MATCHED",
                Map.of("classeCanonical", "INDENIZATORIA"),
                Map.of("resolver", "competence"),
                null,
                Map.of("modo", "DINAMICO"),
                Map.of("hasPessoaPublica", true),
                Map.of("actionNature", "INDENIZATORIA"),
                Map.of("admiteJuizado", true),
                null,
                null,
                "INDENIZATORIA",
                "CIVEL_PATRIMONIAL",
                TipoJustica.ESTADUAL,
                "COMUM_ORDINARIO",
                "MEDIA",
                "DOCUMENTAL",
                0.89d,
                "BAIXO",
                "abc123"
        );

        NationalProceduralRoutingIntelligenceBundle bundle = new NationalProceduralRoutingIntelligenceBundle(
                new ProceduralIntelligenceAdvisoryReport(Instant.now(), null, null, TipoJustica.ESTADUAL, null, null, null, null, 0.88d, "BAIXA", "coerente", null, null, null, null, false, false, false, Map.of()),
                new ProceduralDecisionQualityReport(Instant.now(), 0.80d, 0.77d, 0.10d, 0.83d, true, "SAFE_AUTOMATION", null, null, null, null, null, Map.of()),
                new ProceduralAutomationPolicyReport(Instant.now(), null, null, false, true, false, false, false, null, null, null, null, null, Map.of()),
                new ProceduralExecutiveExplainabilityReport("Resumo executivo", "Quadro de ação", null, null, null, Map.of()),
                new ProceduralAccelerationReport(Instant.now(), null, null, "PADRAO", 30, 120, 60, true, false, false, false, false, false, false, true, "DECISAO_PRIORITARIA", "Prioridade controlada", null, null, null, null, null, Map.of())
        );

        when(seedFactory.build(context)).thenReturn(Map.of("source", "context", "corpusFingerprint", "abc123"));
        when(intelligenceResolver.analyze(any())).thenReturn(bundle);

        Map<String, Object> metadata = factory.build(context);

        assertEquals("context", metadata.get("source"));
        assertEquals("abc123", metadata.get("corpusFingerprint"));
        assertEquals("SAFE_AUTOMATION", metadata.get("operatingModeHint"));
        assertEquals(Boolean.TRUE, metadata.get("safeAutomationEligible"));
        assertEquals(Boolean.TRUE, metadata.get("queueBypassEligible"));
        assertTrue(metadata.containsKey("executiveExplainability"));
        assertTrue(metadata.containsKey("acceleration"));

        verify(seedFactory).build(context);
        verify(intelligenceResolver).analyze(any(NationalProceduralRoutingIntelligenceContext.class));
    }
}
