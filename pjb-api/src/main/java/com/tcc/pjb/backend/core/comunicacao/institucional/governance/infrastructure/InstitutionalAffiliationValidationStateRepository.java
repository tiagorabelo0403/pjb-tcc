package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationValidationReport;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalAffiliationValidationStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_AFFILIATION_VALIDATION";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalAffiliationValidationStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalAffiliationValidationReport save(InstitutionalAffiliationValidationReport report) {
        return stateStore.save(DOMAIN, report.validationId(), report.requestId(), report, null, null, null,
                report.aptaParaHomologacao() ? "APTA" : "BLOQUEADA");
    }

    public Optional<InstitutionalAffiliationValidationReport> findLatestByRequestId(String requestId) {
        return findByRequestId(requestId).stream().max(Comparator.comparing(InstitutionalAffiliationValidationReport::validatedAt));
    }

    public List<InstitutionalAffiliationValidationReport> findByRequestId(String requestId) {
        return stateStore.findBySecondaryKey(DOMAIN, requestId, InstitutionalAffiliationValidationReport.class);
    }
}
