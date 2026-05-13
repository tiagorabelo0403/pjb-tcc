package com.tcc.pjb.backend.core.comunicacao.institucional.access.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalRootAdministratorApproval;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalRootAdministratorApprovalStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_ROOT_ADMIN_APPROVAL";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalRootAdministratorApprovalStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalRootAdministratorApproval save(InstitutionalRootAdministratorApproval approval) {
        return stateStore.save(DOMAIN, approval.approvalId(), approval.affiliationId(), approval, null, null, null, approval.approved() ? "APROVADA" : approval.rejected() ? "REJEITADA" : "PENDENTE");
    }

    public Optional<InstitutionalRootAdministratorApproval> findLatestByAffiliationId(String affiliationId) {
        return findByAffiliationId(affiliationId).stream()
                .sorted(Comparator.comparing(InstitutionalRootAdministratorApproval::updatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst();
    }

    public List<InstitutionalRootAdministratorApproval> findByAffiliationId(String affiliationId) {
        return stateStore.findBySecondaryKey(DOMAIN, affiliationId, InstitutionalRootAdministratorApproval.class);
    }
}
