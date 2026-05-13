package com.tcc.pjb.backend.core.comunicacao.institucional.registry.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalUnitGovernanceSnapshot;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalUnitGovernanceStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_UNIT_GOVERNANCE";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalUnitGovernanceStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalUnitGovernanceSnapshot save(InstitutionalUnitGovernanceSnapshot snapshot) {
        return stateStore.save(DOMAIN, snapshot.snapshotId(), snapshot.affiliationId(), snapshot, null, null, null, snapshot.status());
    }

    public Optional<InstitutionalUnitGovernanceSnapshot> findLatestByAffiliationId(String affiliationId) {
        return findByAffiliationId(affiliationId).stream()
                .sorted(Comparator.comparing(InstitutionalUnitGovernanceSnapshot::generatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst();
    }

    public List<InstitutionalUnitGovernanceSnapshot> findByAffiliationId(String affiliationId) {
        return stateStore.findBySecondaryKey(DOMAIN, affiliationId, InstitutionalUnitGovernanceSnapshot.class);
    }
}
