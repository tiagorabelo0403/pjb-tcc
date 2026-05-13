package com.tcc.pjb.backend.model.dto.security.operational;

public record OperationalCredentialDirectorProvisionRequest(
        Long targetUserId,
        String functionCode,
        String justicaAxis,
        String tribunalCodigo,
        String forumCode,
        String unitCode,
        String varaLabel,
        String reason,
        boolean forceReset
) {
}
