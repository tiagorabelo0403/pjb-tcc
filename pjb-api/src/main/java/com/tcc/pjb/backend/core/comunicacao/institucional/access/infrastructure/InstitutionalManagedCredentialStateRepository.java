package com.tcc.pjb.backend.core.comunicacao.institucional.access.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.access.domain.InstitutionalManagedCredential;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalManagedCredentialStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_MANAGED_CREDENTIAL";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalManagedCredentialStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalManagedCredential save(InstitutionalManagedCredential credential) {
        return stateStore.save(DOMAIN, credential.credentialId(), credential.affiliationId(), credential, null, null, null, credential.status());
    }

    public Optional<InstitutionalManagedCredential> findByCredentialId(String credentialId) {
        return stateStore.find(DOMAIN, credentialId, InstitutionalManagedCredential.class);
    }

    public List<InstitutionalManagedCredential> findByAffiliationId(String affiliationId) {
        return stateStore.findBySecondaryKey(DOMAIN, affiliationId, InstitutionalManagedCredential.class);
    }

    public List<InstitutionalManagedCredential> findAll() {
        return stateStore.findAll(DOMAIN, InstitutionalManagedCredential.class);
    }
}
