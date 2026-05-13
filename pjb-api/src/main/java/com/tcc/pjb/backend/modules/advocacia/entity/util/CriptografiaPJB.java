package com.tcc.pjb.backend.modules.advocacia.entity.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public final class CriptografiaPJB {

    private static final Pattern REDACT_PATTERN = Pattern.compile("(?i)(porn|xxx|ofensa|palavrão|injuria)");
    private static final Pattern NON_DIGITS = Pattern.compile("\\D+");

    private CriptografiaPJB() {
    }

    public static String gerarHash(String input) {
        return sha256Hex(input);
    }

    public static String gerarHash512(String input) {
        return sha512Hex(input);
    }

    public static String sanitizarEntrada(String texto) {
        if (texto == null) return null;
        return REDACT_PATTERN.matcher(texto).replaceAll("[REDACTED]");
    }

    public static String normalizarDocumentoNumerico(String doc) {
        if (doc == null) return null;
        String trimmed = doc.trim();
        if (trimmed.isEmpty()) return null;
        String digits = NON_DIGITS.matcher(trimmed).replaceAll("");
        return digits.isEmpty() ? null : digits;
    }

    public static String hashCpfCnpj(String cpfCnpj) {
        String digits = normalizarDocumentoNumerico(cpfCnpj);
        return sha256Hex(digits);
    }

    public static List<String> candidateCpfHashes(String cpfCnpj) {
        if (cpfCnpj == null) return List.of();
        String raw = cpfCnpj.trim();
        if (raw.isEmpty()) return List.of();

        Set<String> hashes = new LinkedHashSet<>();

        String canonical = hashCpfCnpj(raw);
        if (canonical != null) hashes.add(canonical);

        String digits = normalizarDocumentoNumerico(raw);
        if (digits != null) {
            String legacy512Canonical = sha512Hex(digits);
            if (legacy512Canonical != null) hashes.add(legacy512Canonical);
        }

        String legacyRaw = sha256Hex(raw);
        if (legacyRaw != null) hashes.add(legacyRaw);

        String legacy512Raw = sha512Hex(raw);
        if (legacy512Raw != null) hashes.add(legacy512Raw);

        String noSpaces = raw.replace(" ", "");
        if (!Objects.equals(noSpaces, raw)) {
            String legacyNoSpaces = sha256Hex(noSpaces);
            if (legacyNoSpaces != null) hashes.add(legacyNoSpaces);

            String legacy512NoSpaces = sha512Hex(noSpaces);
            if (legacy512NoSpaces != null) hashes.add(legacy512NoSpaces);
        }

        return new ArrayList<>(hashes);
    }

    public static String sha256Hex(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash SHA-256", e);
        }
    }

    public static String sha512Hex(String input) {
        if (input == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash SHA-512", e);
        }
    }

    private static String toHex(byte[] hashBytes) {
        StringBuilder hex = new StringBuilder(hashBytes.length * 2);
        for (byte b : hashBytes) {
            String h = Integer.toHexString(0xff & b);
            if (h.length() == 1) hex.append('0');
            hex.append(h);
        }
        return hex.toString();
    }
}
