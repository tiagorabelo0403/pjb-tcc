package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.security.abac.policy.PolicyRegistry;
import com.tcc.pjb.backend.core.security.access.ProcessoVinculoResolver;
import com.tcc.pjb.backend.model.entity.Usuario;

final class PjbAuthorizationInstitutionalMalhaAccessFacade {

    private final PolicyRegistry policyRegistry;
    private final ProcessoVinculoResolver vinculoResolver;

    PjbAuthorizationInstitutionalMalhaAccessFacade(PolicyRegistry policyRegistry,
                                                   ProcessoVinculoResolver vinculoResolver) {
        this.policyRegistry = policyRegistry;
        this.vinculoResolver = vinculoResolver;
    }

    AuthzDecision evaluate(Usuario usuario, Long processoId) {
        String policyVersion = policyRegistry.active().policy().version();
        if (usuario == null || usuario.getId() == null || usuario.getTipoUsuario() == null || processoId == null) {
            return AuthzDecision.deny("malha_institucional_contexto_invalido", policyVersion);
        }

        if (usuario.getTipoUsuario().isAdmin()) {
            return AuthzDecision.allow("malha_institucional_admin", policyVersion);
        }

        return vinculoResolver.vinculoInstitucional(usuario, processoId)
                .map(vinculo -> AuthzDecision.allow("malha_institucional_" + vinculo.motivo(), policyVersion))
                .orElseGet(() -> AuthzDecision.deny("malha_institucional_sem_vinculo_no_processo", policyVersion));
    }
}
