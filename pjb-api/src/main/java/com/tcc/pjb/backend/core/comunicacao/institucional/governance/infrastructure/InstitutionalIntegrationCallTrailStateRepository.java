package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCallTrail;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalIntegrationCallTrailStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_INTEGRATION_CALL_TRAIL";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalIntegrationCallTrailStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalIntegrationCallTrail save(InstitutionalIntegrationCallTrail trail) {
        return stateStore.save(DOMAIN, trail.trailId(), trail.credentialId(), trail, null, null, null, trail.resultCode());
    }

    public List<InstitutionalIntegrationCallTrail> findByCredentialId(String credentialId) {
        return stateStore.findBySecondaryKey(DOMAIN, credentialId, InstitutionalIntegrationCallTrail.class);
    }
}
