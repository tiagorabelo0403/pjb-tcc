package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalRemoteCertificateAuthorizationStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalRemoteCertificateAuthorization(
        String authorizationId,
        String affiliationId,
        Long nominatedUserId,
        Long issuedByUserId,
        String issuedByUserName,
        String reason,
        List<String> allowedNetworks,
        List<String> allowedDevices,
        Instant validFrom,
        Instant validUntil,
        InstitutionalRemoteCertificateAuthorizationStatus status,
        List<String> fundamentos,
        Instant createdAt,
        Instant updatedAt,
        String hashIntegridade
) {
    public InstitutionalRemoteCertificateAuthorization {
        Objects.requireNonNull(authorizationId);
        Objects.requireNonNull(affiliationId);
        Objects.requireNonNull(nominatedUserId);
        Objects.requireNonNull(status);
        allowedNetworks = allowedNetworks == null ? List.of() : List.copyOf(allowedNetworks);
        allowedDevices = allowedDevices == null ? List.of() : List.copyOf(allowedDevices);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(authorizationId, affiliationId, nominatedUserId, issuedByUserId, reason, allowedNetworks, allowedDevices,
                    validFrom, validUntil, status, fundamentos, updatedAt);
        }
    }

    public boolean ativaEm(Instant instant) {
        Instant ref = instant == null ? Instant.now() : instant;
        return status.isAtiva() && (validFrom == null || !validFrom.isAfter(ref)) && (validUntil == null || !validUntil.isBefore(ref));
    }

    public InstitutionalRemoteCertificateAuthorization revoke(List<String> extraFundamentos, Instant now) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(fundamentos);
        if (extraFundamentos != null) {
            out.addAll(extraFundamentos);
        }
        return new InstitutionalRemoteCertificateAuthorization(
                authorizationId,
                affiliationId,
                nominatedUserId,
                issuedByUserId,
                issuedByUserName,
                reason,
                allowedNetworks,
                allowedDevices,
                validFrom,
                validUntil,
                InstitutionalRemoteCertificateAuthorizationStatus.REVOGADA,
                List.copyOf(out.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList()),
                createdAt,
                now,
                null
        );
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_remote_certificate_authorization");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
