package com.tcc.pjb.backend.core.kernel.governance;

import java.util.List;
import java.util.Objects;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.NegotiationRoundSnapshot;
import com.tcc.pjb.backend.model.entity.ProcessIntelligenceSnapshot;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.NegotiationRoundSnapshotRepository;
import com.tcc.pjb.backend.model.repository.ProcessIntelligenceSnapshotRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProcessIntelligenceSnapshotService {

    private final ProcessIntelligenceSnapshotRepository processSnapshotRepository;
    private final NegotiationRoundSnapshotRepository roundSnapshotRepository;

    @Transactional
    @PjbTransactionalBudget(operation = "process-intelligence.snapshot.persist", maxMillis = 1500)
    public ProcessIntelligenceSnapshot saveProcessSnapshot(Processo processo,
                                                           List<String> strategicFocus,
                                                           InstitutionalPolicySnapshotReport policy,
                                                           KernelRiskEscalationReport riskEscalation,
                                                           NegotiationMessageDecision decision) {
        Objects.requireNonNull(processo, "processo");
        ProcessIntelligenceSnapshot snapshot = ProcessIntelligenceSnapshot.builder()
                .processoId(processo.getId())
                .snapshotScope(decision != null ? decision.scope() : "PROCESS_INTELLIGENCE")
                .policyKey(policy != null ? policy.policyKey() : null)
                .policyTier(policy != null ? policy.policyTier() : null)
                .riskLevel(riskEscalation != null ? riskEscalation.escalationLevel() : null)
                .decisionCode(decision != null ? decision.decisionCode() : null)
                .strategicFocus(join(strategicFocus))
                .telemetry(join(policy != null ? policy.mandatoryDirectives() : List.of(), riskEscalation != null ? riskEscalation.containmentActions() : List.of()))
                .build();
        return processSnapshotRepository.save(snapshot);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "process-intelligence.negotiation-round.persist", maxMillis = 1500)
    public NegotiationRoundSnapshot saveNegotiationRound(Processo processo,
                                                         PropostaAcordo proposta,
                                                         Usuario actor,
                                                         NegotiationMessageDecision decision,
                                                         List<String> summary,
                                                         String suggestedMessage) {
        Objects.requireNonNull(processo, "processo");
        Objects.requireNonNull(decision, "decision");
        NegotiationRoundSnapshot snapshot = NegotiationRoundSnapshot.builder()
                .processoId(processo.getId())
                .propostaId(proposta != null ? proposta.getId() : null)
                .usuarioId(actor != null ? actor.getId() : null)
                .roundScope(decision.scope())
                .approvalBand(decision.approvalBand())
                .releaseMode(decision.releaseMode())
                .riskLevel(decision.riskLevel())
                .summary(join(summary))
                .suggestedMessage(suggestedMessage)
                .mandatoryActions(join(decision.mandatoryActions()))
                .build();
        return roundSnapshotRepository.save(snapshot);
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .reduce((a, b) -> a + "\n" + b)
                .orElse(null);
    }

    private String join(List<String> first, List<String> second) {
        return join(java.util.stream.Stream.concat(
                first == null ? java.util.stream.Stream.empty() : first.stream(),
                second == null ? java.util.stream.Stream.empty() : second.stream()
        ).filter(Objects::nonNull).toList());
    }
}
