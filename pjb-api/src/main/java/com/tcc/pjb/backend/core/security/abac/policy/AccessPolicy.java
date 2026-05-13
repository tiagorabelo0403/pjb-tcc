package com.tcc.pjb.backend.core.security.abac.policy;

import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;

public interface AccessPolicy {

    
    String version();

    
    AuthzDecision canReadProcesso(Usuario usuario, Processo processo, String justificativa);

    



    default AuthzDecision canReadVotosColegiados(Usuario usuario, Processo processo, String justificativa) {
        return AuthzDecision.deny("votos_nao_suportado", version());
    }
}
