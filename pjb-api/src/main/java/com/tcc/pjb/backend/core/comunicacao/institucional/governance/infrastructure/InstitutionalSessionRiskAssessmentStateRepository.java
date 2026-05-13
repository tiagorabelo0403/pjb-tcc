package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalSessionRiskAssessment;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalSessionRiskAssessmentStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_SESSION_RISK_ASSESSMENT";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalSessionRiskAssessmentStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalSessionRiskAssessment save(InstitutionalSessionRiskAssessment assessment) {
        String secondaryKey = assessment.userId() == null ? "ANON" : String.valueOf(assessment.userId());
        return stateStore.save(DOMAIN, assessment.assessmentId(), secondaryKey, assessment, null, null, String.valueOf(assessment.userId()), assessment.riskLevel());
    }

    public List<InstitutionalSessionRiskAssessment> findByUserId(Long userId) {
        return stateStore.findBySecondaryKey(DOMAIN, String.valueOf(userId), InstitutionalSessionRiskAssessment.class);
    }
}
