package com.tcc.pjb.backend.core.procedural;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingFinalizationResolver {

    private final NationalProceduralRoutingMetadataContextFactory metadataContextFactory;
    private final NationalProceduralRoutingMetadataFactory metadataFactory;
    private final NationalProceduralEconomicGateFactory economicGateFactory;
    private final NationalProceduralRoutingReportAssemblyContextFactory reportAssemblyContextFactory;
    private final NationalProceduralRoutingReportAssembler routingReportAssembler;

    public NationalProceduralRoutingFinalizationResolver(NationalProceduralRoutingMetadataContextFactory metadataContextFactory,
                                                         NationalProceduralRoutingMetadataFactory metadataFactory,
                                                         NationalProceduralEconomicGateFactory economicGateFactory,
                                                         NationalProceduralRoutingReportAssemblyContextFactory reportAssemblyContextFactory,
                                                         NationalProceduralRoutingReportAssembler routingReportAssembler) {
        this.metadataContextFactory = Objects.requireNonNull(metadataContextFactory);
        this.metadataFactory = Objects.requireNonNull(metadataFactory);
        this.economicGateFactory = Objects.requireNonNull(economicGateFactory);
        this.reportAssemblyContextFactory = Objects.requireNonNull(reportAssemblyContextFactory);
        this.routingReportAssembler = Objects.requireNonNull(routingReportAssembler);
    }

    ProceduralRoutingReport finalize(NationalProceduralRoutingCoreResolution resolution) {
        Objects.requireNonNull(resolution);
        ProceduralEconomicGateReport economicGate = economicGateFactory.build(
                decimal(resolution.payload().get("valorCausa")),
                resolution.tipoJustica(),
                resolution.ritoSugerido(),
                resolution.proceduralTrack(),
                resolution.teto(),
                resolution.actionProfile().actionNature(),
                resolution.actionProfile().actionFamily(),
                resolution.juizadoDecision().admiteJuizado(),
                resolution.reviewSynthesis().missingInputs(),
                resolution.reviewSynthesis().reviewChecklist(),
                resolution.reviewSynthesis().blockingIssues(),
                requiresEconomicValue(resolution.actionProfile().actionNature())
        );

        Map<String, Object> metadata = metadataFactory.build(metadataContextFactory.create(resolution, economicGate));
        return routingReportAssembler.assemble(reportAssemblyContextFactory.create(resolution, economicGate, metadata));
    }

    private static boolean requiresEconomicValue(String actionNature) {
        return NationalProceduralRoutingSupport.containsAny(
                actionNature,
                "OBRIGACAO_DE_FAZER",
                "COBRANCA_REPETICAO",
                "INDENIZATORIA",
                "PREVIDENCIARIO",
                "RECLAMACAO_TRABALHISTA",
                "FAZENDA_PUBLICA",
                "EXECUCAO_FISCAL"
        );
    }

    private static BigDecimal decimal(Object value) {
        return NationalProceduralRoutingSupport.decimal(value);
    }
}
