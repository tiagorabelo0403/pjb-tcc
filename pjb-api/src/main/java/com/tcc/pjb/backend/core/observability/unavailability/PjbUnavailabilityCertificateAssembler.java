package com.tcc.pjb.backend.core.observability.unavailability;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class PjbUnavailabilityCertificateAssembler {

    public PjbUnavailabilityCertificate assemble(PjbSystemUnavailabilityEvent event, PjbDeadlineImpactResult impact, Instant issuedAt) {
        Objects.requireNonNull(event, "event");
        PjbDeadlineImpactResult result = impact == null
                ? new PjbDeadlineImpactResult(false, null, event.duration(), List.of("impact assessment missing"))
                : impact;
        List<String> services = event.affectedServices().stream().map(Enum::name).sorted().toList();
        String material = event.tribunalCode() + event.startedAt() + event.endedAt() + services + result.extendsDeadline() + result.creditedOutage();
        return new PjbUnavailabilityCertificate(
                event.tribunalCode(),
                issuedAt,
                result.extendsDeadline(),
                result.creditedOutage().toString(),
                services,
                result.reasons(),
                sha256(material)
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Objects.toString(value, "").getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
