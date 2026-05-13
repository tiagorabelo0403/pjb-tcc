package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationApprovalTrail;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalAffiliationApprovalTrailStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_AFFILIATION_APPROVAL_TRAIL";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalAffiliationApprovalTrailStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalAffiliationApprovalTrail save(InstitutionalAffiliationApprovalTrail trail) {
        return stateStore.save(DOMAIN, trail.trailId(), trail.requestId(), trail, null, null, trail.representativeUserId() == null ? null : String.valueOf(trail.representativeUserId()), trail.currentStatus());
    }

    public Optional<InstitutionalAffiliationApprovalTrail> findLatestByRequestId(String requestId) {
        return stateStore.findBySecondaryKey(DOMAIN, requestId, InstitutionalAffiliationApprovalTrail.class).stream()
                .max(Comparator.comparing(InstitutionalAffiliationApprovalTrail::updatedAt));
    }

    public List<InstitutionalAffiliationApprovalTrail> findByRequestId(String requestId) {
        return stateStore.findBySecondaryKey(DOMAIN, requestId, InstitutionalAffiliationApprovalTrail.class);
    }
}
