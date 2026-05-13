package com.tcc.pjb.backend.core.security.magistratura.delegation;

import java.time.Instant;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public record DelegationCredential(
        String jti,
        Long magistrateId,
        TipoUsuario issuerTipo,
        Long delegateId,
        DelegationScope scope,
        Instant validUntil,
        String deviceBindingHash
) {
}
