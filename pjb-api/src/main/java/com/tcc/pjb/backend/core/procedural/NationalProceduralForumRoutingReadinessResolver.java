package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.core.preflight.ProceduralPreflightEngine;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralForumRoutingReadinessResolver {

    private final TribunalProtocolRoutingService tribunalProtocolRoutingService;
    private final ProceduralPreflightEngine proceduralPreflightEngine;
    private final NationalProceduralPreflightPayloadFactory preflightPayloadFactory;
    private final NationalProceduralForumAllocationMessages messages;

    public NationalProceduralForumRoutingReadinessResolver(TribunalProtocolRoutingService tribunalProtocolRoutingService,
                                                           ProceduralPreflightEngine proceduralPreflightEngine,
                                                           NationalProceduralPreflightPayloadFactory preflightPayloadFactory,
                                                           NationalProceduralForumAllocationMessages messages) {
        this.tribunalProtocolRoutingService = Objects.requireNonNull(tribunalProtocolRoutingService);
        this.proceduralPreflightEngine = Objects.requireNonNull(proceduralPreflightEngine);
        this.preflightPayloadFactory = Objects.requireNonNull(preflightPayloadFactory);
        this.messages = Objects.requireNonNull(messages);
    }

    NationalProceduralForumRoutingReadiness resolve(NationalProceduralForumAllocationContext context,
                                                    NationalProceduralForumAllocationSeed seed) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(seed);
        Map<String, Object> payload = context.payload() == null ? Map.of() : context.payload();

        TribunalProtocolRoutingService.RoutingDecision routingDecision = tribunalProtocolRoutingService.resolve(
                preflightPayloadFactory.buildProtocolRoutingPayload(payload, seed.classeTpu(), seed.tribunalCodigo(), seed.comarca(), seed.uf(), seed.unidadeCodigo(), seed.varaSugerida()),
                context.ritoSugerido(),
                NationalProceduralRoutingSupport.firstNonBlank(
                        context.canonical().ramoDireito(),
                        NationalProceduralRoutingSupport.text(payload.get("ramoDireito")),
                        NationalProceduralRoutingSupport.text(payload.get("materia"))
                ),
                context.tipoJustica().name(),
                NationalProceduralRoutingSupport.containsAny(context.corpus(), "APELACAO", "AGRAVO", "RECURSO", "EMBARGOS")
        );

        ProceduralPreflightEngine.PreflightResult preflight = proceduralPreflightEngine.evaluate(
                ProceduralPreflightEngine.PreflightContext.fromMap(
                        preflightPayloadFactory.buildPreflightPayload(payload, seed.classeTpu(), context.ritoSugerido(), context.canonical(), seed.tribunalCodigo(), seed.comarca(), seed.uf(), seed.unidadeCodigo(), seed.varaSugerida(), routingDecision)
                )
        );

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        LinkedHashSet<String> incompatibilities = new LinkedHashSet<>();
        JudicialSubmissionCapability capability = routingDecision == null ? null : routingDecision.capability();
        if (routingDecision != null) {
            addAll(warnings, routingDecision.warnings());
            if (capability == null || !capability.operational()) {
                warnings.add(messages.connectorNotHomologated());
            }
            if (capability != null && capability.requiresCertificate()) {
                reviewChecklist.add(messages.connectorCertificateChecklist());
            }
            if (capability != null && capability.requiresStepUpGovBr()) {
                reviewChecklist.add(messages.connectorGovBrChecklist());
            }
        }
        if (preflight != null) {
            List<ProceduralPreflightEngine.PreflightIssue> issues = preflight.issues() == null ? List.of() : preflight.issues();
            for (ProceduralPreflightEngine.PreflightIssue issue : issues) {
                if (issue == null) {
                    continue;
                }
                if (preflightPayloadFactory.isStructuralPreflightBlocker(issue)) {
                    incompatibilities.add(issue.message());
                    if (!NationalProceduralRoutingSupport.isBlank(issue.suggestedFix())) {
                        reviewChecklist.add(issue.suggestedFix());
                    }
                } else if (!NationalProceduralRoutingSupport.isBlank(issue.message())) {
                    warnings.add(issue.message());
                }
            }
        }
        boolean connectorOperational = capability != null && capability.operational();
        boolean preProtocoloApto = preflight != null && preflight.readyForSubmission();
        boolean protocoloRealDisponivel = connectorOperational && preProtocoloApto && incompatibilities.isEmpty();
        String preProtocoloStatus = preflight != null ? preProtocoloApto ? "READY_WITH_REVIEW" : "STRUCTURAL_REVIEW_REQUIRED" : "NOT_EVALUATED";

        return new NationalProceduralForumRoutingReadiness(
                routingDecision,
                capability,
                preflight,
                connectorOperational,
                protocoloRealDisponivel,
                preProtocoloApto,
                preProtocoloStatus,
                List.copyOf(incompatibilities),
                List.copyOf(warnings),
                List.copyOf(reviewChecklist)
        );
    }

    private static void addAll(Set<String> target, List<String> values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (!NationalProceduralRoutingSupport.isBlank(value)) {
                target.add(value.trim());
            }
        }
    }
}
