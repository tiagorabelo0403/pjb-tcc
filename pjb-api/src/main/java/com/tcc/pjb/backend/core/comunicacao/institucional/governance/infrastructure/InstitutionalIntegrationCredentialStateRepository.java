package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalIntegrationCredential;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalIntegrationCredentialStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_INTEGRATION_CREDENTIAL";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalIntegrationCredentialStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalIntegrationCredential save(InstitutionalIntegrationCredential credential) {
        return stateStore.save(DOMAIN, credential.credentialId(), credential.affiliationId(), credential, null, null, null, credential.status().name());
    }

    public Optional<InstitutionalIntegrationCredential> findByCredentialId(String credentialId) {
        return stateStore.find(DOMAIN, credentialId, InstitutionalIntegrationCredential.class);
    }

    public List<InstitutionalIntegrationCredential> findByAffiliationId(String affiliationId) {
        return stateStore.findBySecondaryKey(DOMAIN, affiliationId, InstitutionalIntegrationCredential.class);
    }

    public List<InstitutionalIntegrationCredential> findAll() {
        return stateStore.findAll(DOMAIN, InstitutionalIntegrationCredential.class);
    }
}
