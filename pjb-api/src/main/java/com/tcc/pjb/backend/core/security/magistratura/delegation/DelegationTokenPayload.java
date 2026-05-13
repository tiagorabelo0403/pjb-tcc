package com.tcc.pjb.backend.core.security.magistratura.delegation;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record DelegationTokenPayload(
        String jti,
        Long magistrateId,
        Long delegateId,
        String uf,
        String comarca,
        String deviceBindingHash,
        long iat,
        long exp,
        String scope
) {

    @JsonIgnore
    public DelegationScope scopeEnum() {
        return DelegationScope.parseLenient(scope);
    }

    @JsonIgnore
    public boolean isExpired(long nowEpochSec) {
        return exp <= nowEpochSec;
    }
}
