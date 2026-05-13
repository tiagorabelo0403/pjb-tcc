package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralRoutingReportAssemblyContextFactoryTest {

    @Test
    void mustCreateAssemblyContextFromResolutionWithoutDirectFieldScatteringInFinalizer() {
        NationalProceduralRoutingReportAssemblyContextFactory factory = new NationalProceduralRoutingReportAssemblyContextFactory();
        NationalProceduralRoutingCoreResolution resolution = NationalProceduralRoutingTestFixtures.sampleResolution();
        ProceduralEconomicGateReport economicGate = NationalProceduralRoutingTestFixtures.sampleEconomicGate();
        Map<String, Object> metadata = Map.of("origem", "teste");

        NationalProceduralRoutingReportAssemblyContext context = factory.create(resolution, economicGate, metadata);

        assertEquals("ESTADUAL", context.tipoJustica());
        assertEquals("TJCE", context.tribunalCodigo());
        assertEquals("Tribunal de Justica do Ceara", context.tribunalNome());
        assertEquals("Foro de Fortaleza/CE", context.foroSugerido());
        assertEquals("1a Vara da Fazenda", context.varaSugerida());
        assertEquals("FAZENDA", context.tipoVaraSugerido());
        assertSame(resolution.actionProfile(), context.actionProfile());
        assertSame(resolution.juizadoDecision(), context.juizadoDecision());
        assertSame(resolution.reviewSynthesis(), context.reviewSynthesis());
        assertSame(economicGate, context.economicGate());
        assertSame(metadata, context.metadata());
    }
}
