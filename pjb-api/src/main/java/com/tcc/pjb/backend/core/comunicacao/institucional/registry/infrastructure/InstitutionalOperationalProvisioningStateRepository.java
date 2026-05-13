package com.tcc.pjb.backend.core.comunicacao.institucional.registry.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.registry.domain.InstitutionalOperationalProvisioningSnapshot;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalOperationalProvisioningStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_OPERATIONAL_PROVISIONING";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalOperationalProvisioningStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalOperationalProvisioningSnapshot save(InstitutionalOperationalProvisioningSnapshot snapshot) {
        return stateStore.save(DOMAIN, snapshot.provisioningId(), snapshot.affiliationId(), snapshot, null, null, null, snapshot.status());
    }

    public Optional<InstitutionalOperationalProvisioningSnapshot> findLatestByAffiliationId(String affiliationId) {
        return findByAffiliationId(affiliationId).stream()
                .sorted(Comparator.comparing(InstitutionalOperationalProvisioningSnapshot::generatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst();
    }

    public List<InstitutionalOperationalProvisioningSnapshot> findByAffiliationId(String affiliationId) {
        return stateStore.findBySecondaryKey(DOMAIN, affiliationId, InstitutionalOperationalProvisioningSnapshot.class);
    }
}
