package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class NationalProceduralRoutingMetadataContextFactoryTest {

    @Test
    void mustCreateMetadataContextFromCoreResolutionWithoutLeakingAssemblyLogicIntoFinalizer() {
        NationalProceduralRoutingMetadataContextFactory factory = new NationalProceduralRoutingMetadataContextFactory();

        NationalProceduralRoutingMetadataContext context = factory.create(
                NationalProceduralRoutingTestFixtures.sampleResolution(),
                NationalProceduralRoutingTestFixtures.sampleEconomicGate()
        );

        assertEquals("context", context.sourceLabel());
        assertEquals("CANONICAL_RITO_RESOLVED", context.canonicalStatus());
        assertEquals("ESTADUAL", context.tipoJustica().name());
        assertEquals("COMUM_ORDINARIO", context.ritoSugerido());
        assertEquals("INDENIZATORIA", context.actionNature());
        assertEquals("CIVEL_PATRIMONIAL", context.actionFamily());
        assertEquals("VARA-01", context.distributionMetadata().get("unidadeCodigo"));
        assertEquals("capital", context.forumAllocation().metadata().get("regionalizacao"));
        assertNotNull(context.economicGate());
        assertNotNull(context.corpusFingerprint());
    }
}
