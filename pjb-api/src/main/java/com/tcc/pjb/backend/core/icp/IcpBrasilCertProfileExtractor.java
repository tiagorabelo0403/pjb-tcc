package com.tcc.pjb.backend.core.icp;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IcpBrasilCertProfileExtractor {

    private static final Pattern CPF_PATTERN = Pattern.compile("(?:CPF|2\\.16\\.76\\.1\\.3\\.1)=?([0-9]{11})", Pattern.CASE_INSENSITIVE);
    private static final Pattern CNPJ_PATTERN = Pattern.compile("(?:CNPJ|2\\.16\\.76\\.1\\.3\\.3)=?([0-9]{14})", Pattern.CASE_INSENSITIVE);

    private IcpBrasilCertProfileExtractor() {
    }

    public static IcpBrasilCertProfile extract(X509Certificate cert) {
        String subject = cert.getSubjectX500Principal().getName();
        String issuer = cert.getIssuerX500Principal().getName();
        return new IcpBrasilCertProfile(
                subject,
                issuer,
                toSerialHex(cert.getSerialNumber()),
                extract(subject, CPF_PATTERN),
                extract(subject, CNPJ_PATTERN),
                extractCn(subject, "CN="),
                inferType(subject),
                extractCn(issuer, "CN="),
                toInstant(cert.getNotBefore()),
                toInstant(cert.getNotAfter())
        );
    }

    private static Instant toInstant(java.util.Date value) {
        return value == null ? null : value.toInstant();
    }

    private static String extract(String value, Pattern pattern) {
        Matcher matcher = pattern.matcher(value == null ? "" : value);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String extractCn(String dn, String marker) {
        if (dn == null || dn.isBlank()) {
            return null;
        }
        String upper = dn.toUpperCase(Locale.ROOT);
        int start = upper.indexOf(marker);
        if (start < 0) {
            return dn;
        }
        int end = dn.indexOf(',', start);
        return (end < 0 ? dn.substring(start + marker.length()) : dn.substring(start + marker.length(), end)).trim();
    }

    private static String inferType(String subject) {
        String normalized = normalize(subject);
        if (normalized.contains("A4")) {
            return "A4";
        }
        if (normalized.contains("A3")) {
            return "A3";
        }
        return "A1";
    }

    private static String toSerialHex(BigInteger serial) {
        return serial == null ? null : serial.toString(16).toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
