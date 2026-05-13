package com.tcc.pjb.backend.core.comunicacao.institucional.access.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InstitutionalManagedCredential(
        String credentialId,
        String affiliationId,
        String nominationId,
        Long nominatedUserId,
        String nominatedUserName,
        String managedUsername,
        String displayName,
        String laneCode,
        boolean signerOrSensitive,
        boolean allowsInstitutionManagedLogin,
        boolean govBrBindingRequired,
        boolean govBrBindingConfirmed,
        String status,
        int rotationWindowDays,
        List<String> allowedNetworks,
        List<String> findings,
        List<String> fundamentos,
        Instant createdAt,
        Instant updatedAt,
        String hashIntegridade
) {
    public InstitutionalManagedCredential {
        Objects.requireNonNull(credentialId);
        Objects.requireNonNull(affiliationId);
        Objects.requireNonNull(managedUsername);
        status = sanitize(status, "PENDENTE");
        allowedNetworks = allowedNetworks == null ? List.of() : allowedNetworks.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
        findings = findings == null ? List.of() : List.copyOf(findings);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (rotationWindowDays <= 0) {
            rotationWindowDays = 90;
        }
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(credentialId, affiliationId, nominationId, nominatedUserId, managedUsername, laneCode, signerOrSensitive, allowsInstitutionManagedLogin, govBrBindingRequired, govBrBindingConfirmed, status, rotationWindowDays, allowedNetworks, findings, fundamentos);
        }
    }

    public boolean ativa() {
        return "ATIVA".equalsIgnoreCase(status);
    }

    public InstitutionalManagedCredential withStatus(String newStatus,
                                                     List<String> newFindings,
                                                     List<String> extraFundamentos,
                                                     Instant when) {
        return new InstitutionalManagedCredential(
                credentialId,
                affiliationId,
                nominationId,
                nominatedUserId,
                nominatedUserName,
                managedUsername,
                displayName,
                laneCode,
                signerOrSensitive,
                allowsInstitutionManagedLogin,
                govBrBindingRequired,
                govBrBindingConfirmed,
                sanitize(newStatus, status),
                rotationWindowDays,
                allowedNetworks,
                newFindings == null ? findings : List.copyOf(newFindings),
                merge(extraFundamentos),
                createdAt,
                when == null ? Instant.now() : when,
                null
        );
    }

    private List<String> merge(List<String> extras) {
        if (extras == null || extras.isEmpty()) {
            return fundamentos;
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<>(fundamentos);
        out.addAll(extras);
        return List.copyOf(out);
    }

    private static String sanitize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_managed_credential");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
