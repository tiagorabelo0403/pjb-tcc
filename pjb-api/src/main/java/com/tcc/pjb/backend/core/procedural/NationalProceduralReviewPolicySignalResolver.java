package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralReviewPolicySignalResolver {

    private final NationalProceduralReviewMessages messages;

    public NationalProceduralReviewPolicySignalResolver(NationalProceduralReviewMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    NationalProceduralReviewSignalSet collect(NationalProceduralReviewSynthesisContext context) {
        Objects.requireNonNull(context);
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        LinkedHashSet<String> blockingIssues = new LinkedHashSet<>();

        addAll(alerts, context.juizadoDecision().alerts());
        addAll(alerts, context.actionProfile().alerts());
        addAll(reviewChecklist, context.actionProfile().reviewChecklist());
        addAll(reviewChecklist, context.juizadoDecision().reviewChecklist());
        addAll(alerts, context.forumAllocation().warnings());
        addAll(reviewChecklist, context.forumAllocation().reviewChecklist());
        addAll(blockingIssues, context.forumAllocation().incompatibilities());

        if (!context.forumAllocation().preProtocoloApto()) {
            alerts.add(messages.preProtocolSanitizationRequired());
        }
        if (!context.forumAllocation().distribuicaoAutomatica()) {
            alerts.add(messages.territorialMeshNeedsHumanValidation());
        }
        if (!"NENHUM_SINAL".equals(context.forumAllocation().linkageMode()) || !context.forumAllocation().relatedProcessNumbers().isEmpty()) {
            reviewChecklist.add(messages.linkageReviewChecklist());
        }
        if (context.selectedRito().heuristicUsed()) {
            alerts.add(messages.heuristicCompatibilityAlert());
            reviewChecklist.add(messages.heuristicCompatibilityChecklist());
        }
        if (context.selectedRito().fallbackApplied()) {
            alerts.add(messages.residualRitoAlert());
            blockingIssues.add(messages.residualRitoBlocking());
        }
        if (context.selectedRito().sanityGate() != null && context.selectedRito().sanityGate().hasBlockingIssues()) {
            alerts.add(messages.sanityGateAlert(context.selectedRito().sanityGate().statusCodes()));
            reviewChecklist.add(messages.sanityGateChecklist());
            blockingIssues.add(messages.sanityGateBlocking());
        }
        if (context.teto().bloqueante()) {
            alerts.add(messages.tetoBlockingAlert(context.teto().sugestaoOperacional()));
            reviewChecklist.add(messages.tetoBlockingChecklist());
            blockingIssues.add(messages.tetoBlockingReason(context.teto().fundamentoLegal()));
        } else if (context.teto().alerta()) {
            alerts.add(messages.tetoWarningAlert());
            reviewChecklist.add(messages.tetoWarningChecklist());
        }
        if (context.distribution() == null && !NationalProceduralRoutingSupport.isBlank(context.cidadeSugerida()) && !NationalProceduralRoutingSupport.isBlank(context.ufSugerida())) {
            alerts.add(messages.distributionFallbackAlert());
        }
        if (context.partyProfile().publicParty()
                && context.tipoJustica() == TipoJustica.ESTADUAL
                && !context.juizadoDecision().admiteJuizado()
                && context.actionProfile().specialProcedure()) {
            reviewChecklist.add(messages.publicPartySpecializedChecklist());
        }
        return new NationalProceduralReviewSignalSet(List.copyOf(alerts), List.copyOf(reviewChecklist), List.copyOf(blockingIssues));
    }

    private static void addAll(LinkedHashSet<String> target, List<String> values) {
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
