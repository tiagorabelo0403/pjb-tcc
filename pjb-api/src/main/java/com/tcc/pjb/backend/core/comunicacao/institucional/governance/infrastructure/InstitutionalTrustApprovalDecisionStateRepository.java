package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalTrustApprovalDecision;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalTrustApprovalDecisionStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_TRUST_APPROVAL_DECISION";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalTrustApprovalDecisionStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalTrustApprovalDecision save(InstitutionalTrustApprovalDecision decision) {
        return stateStore.save(
                DOMAIN,
                decision.decisionId(),
                decision.profileKey(),
                decision,
                null,
                null,
                decision.nominatedUserId() == null ? null : String.valueOf(decision.nominatedUserId()),
                decision.approvalKind().name());
    }

    public List<InstitutionalTrustApprovalDecision> findByProfileKey(String profileKey) {
        return stateStore.findBySecondaryKey(DOMAIN, profileKey, InstitutionalTrustApprovalDecision.class).stream()
                .sorted(Comparator.comparing(InstitutionalTrustApprovalDecision::decidedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Optional<InstitutionalTrustApprovalDecision> findLatestByProfileAndKind(String profileKey, String approvalKind) {
        return findByProfileKey(profileKey).stream()
                .filter(item -> item.approvalKind().name().equalsIgnoreCase(approvalKind))
                .max(Comparator.comparing(InstitutionalTrustApprovalDecision::decidedAt, Comparator.nullsLast(Comparator.naturalOrder())));
    }
}
