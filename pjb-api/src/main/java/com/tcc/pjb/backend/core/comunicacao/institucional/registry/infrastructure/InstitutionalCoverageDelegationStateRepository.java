package com.tcc.pjb.backend.core.comunicacao.institucional.registry.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalCoverageDelegationSnapshot;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalCoverageDelegationStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_COVERAGE_DELEGATION";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalCoverageDelegationStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalCoverageDelegationSnapshot save(InstitutionalCoverageDelegationSnapshot snapshot) {
        return stateStore.save(DOMAIN, snapshot.snapshotId(), snapshot.affiliationId(), snapshot, null, null, null, snapshot.status());
    }

    public Optional<InstitutionalCoverageDelegationSnapshot> findLatestByAffiliationId(String affiliationId) {
        return findByAffiliationId(affiliationId).stream()
                .sorted(Comparator.comparing(InstitutionalCoverageDelegationSnapshot::generatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst();
    }

    public List<InstitutionalCoverageDelegationSnapshot> findByAffiliationId(String affiliationId) {
        return stateStore.findBySecondaryKey(DOMAIN, affiliationId, InstitutionalCoverageDelegationSnapshot.class);
    }
}
