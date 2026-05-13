package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRemoteCertificateAuthorization;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InstitutionalRemoteCertificateAuthorizationStateRepository {

    private static final String DOMAIN = "INSTITUTIONAL_REMOTE_CERTIFICATE_AUTHORIZATION";

    private final ComunicacaoJudicialStateStore stateStore;

    public InstitutionalRemoteCertificateAuthorizationStateRepository(ComunicacaoJudicialStateStore stateStore) {
        this.stateStore = Objects.requireNonNull(stateStore);
    }

    public InstitutionalRemoteCertificateAuthorization save(InstitutionalRemoteCertificateAuthorization authorization) {
        return stateStore.save(DOMAIN, authorization.authorizationId(), authorization.affiliationId(), authorization, null, null,
                String.valueOf(authorization.nominatedUserId()), authorization.status().name());
    }

    public Optional<InstitutionalRemoteCertificateAuthorization> findByAuthorizationId(String authorizationId) {
        return stateStore.find(DOMAIN, authorizationId, InstitutionalRemoteCertificateAuthorization.class);
    }

    public List<InstitutionalRemoteCertificateAuthorization> findByAffiliationId(String affiliationId) {
        return stateStore.findBySecondaryKey(DOMAIN, affiliationId, InstitutionalRemoteCertificateAuthorization.class);
    }

    public List<InstitutionalRemoteCertificateAuthorization> findAll() {
        return stateStore.findAll(DOMAIN, InstitutionalRemoteCertificateAuthorization.class);
    }
}
