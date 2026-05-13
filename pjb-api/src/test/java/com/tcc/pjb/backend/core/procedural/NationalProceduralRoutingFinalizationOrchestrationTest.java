package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NationalProceduralRoutingFinalizationOrchestrationTest {

    @Test
    void mustOrchestrateEconomicGateMetadataAndReportAssemblyThroughDedicatedFactories() {
        NationalProceduralRoutingMetadataContextFactory metadataContextFactory = Mockito.mock(NationalProceduralRoutingMetadataContextFactory.class);
        NationalProceduralRoutingMetadataFactory metadataFactory = Mockito.mock(NationalProceduralRoutingMetadataFactory.class);
        NationalProceduralEconomicGateFactory economicGateFactory = Mockito.mock(NationalProceduralEconomicGateFactory.class);
        NationalProceduralRoutingReportAssemblyContextFactory reportAssemblyContextFactory = Mockito.mock(NationalProceduralRoutingReportAssemblyContextFactory.class);
        NationalProceduralRoutingReportAssembler reportAssembler = Mockito.mock(NationalProceduralRoutingReportAssembler.class);

        NationalProceduralRoutingFinalizationResolver resolver = new NationalProceduralRoutingFinalizationResolver(
                metadataContextFactory,
                metadataFactory,
                economicGateFactory,
                reportAssemblyContextFactory,
                reportAssembler
        );

        NationalProceduralRoutingCoreResolution resolution = NationalProceduralRoutingTestFixtures.sampleResolution();
        ProceduralEconomicGateReport economicGate = NationalProceduralRoutingTestFixtures.sampleEconomicGate();
        NationalProceduralRoutingMetadataContext metadataContext = new NationalProceduralRoutingMetadataContext(
                Map.of(),
                "context",
                "MATCHED",
                Map.of(),
                Map.of(),
                resolution.teto(),
                Map.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                economicGate,
                resolution.judicialPlacement().forumAllocation(),
                "INDENIZATORIA",
                "CIVEL_PATRIMONIAL",
                resolution.tipoJustica(),
                resolution.ritoSugerido(),
                resolution.complexityBand(),
                resolution.probatoryProfile(),
                0.84d,
                "MEDIUM",
                "abc"
        );
        NationalProceduralRoutingReportAssemblyContext assemblyContext = new NationalProceduralRoutingReportAssemblyContext(
                resolution.actionProfile(),
                resolution.proceduralRegime(),
                resolution.proceduralTrack(),
                resolution.tipoJustica().name(),
                resolution.ritoSugerido(),
                resolution.judicialPlacement().tribunalCodigo(),
                resolution.judicialPlacement().tribunalNome(),
                resolution.judicialPlacement().judicialSystem(),
                resolution.judicialPlacement().foroSugerido(),
                resolution.judicialPlacement().cidadeSugerida(),
                resolution.judicialPlacement().ufSugerida(),
                resolution.judicialPlacement().varaSugerida(),
                resolution.judicialPlacement().tipoVaraSugerido(),
                resolution.complexityBand(),
                resolution.probatoryProfile(),
                resolution.juizadoDecision(),
                resolution.reviewSynthesis(),
                economicGate,
                resolution.judicialPlacement().forumAllocation(),
                Map.of("origem", "teste")
        );
        ProceduralRoutingReport report = new NationalProceduralRoutingReportAssembler().assemble(assemblyContext);

        when(economicGateFactory.build(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), anyBoolean())).thenReturn(economicGate);
        when(metadataContextFactory.create(resolution, economicGate)).thenReturn(metadataContext);
        when(metadataFactory.build(metadataContext)).thenReturn(Map.of("origem", "teste"));
        when(reportAssemblyContextFactory.create(resolution, economicGate, Map.of("origem", "teste"))).thenReturn(assemblyContext);
        when(reportAssembler.assemble(assemblyContext)).thenReturn(report);

        ProceduralRoutingReport result = resolver.finalize(resolution);

        assertSame(report, result);
        verify(economicGateFactory).build(any(), any(), any(), any(), any(), any(), any(), anyBoolean(), any(), any(), any(), anyBoolean());
        verify(metadataContextFactory).create(resolution, economicGate);
        verify(metadataFactory).build(metadataContext);
        verify(reportAssemblyContextFactory).create(resolution, economicGate, Map.of("origem", "teste"));
        verify(reportAssembler).assemble(assemblyContext);
    }
}
