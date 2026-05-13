package com.tcc.pjb.backend.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hashes {

    private Hashes() {
    }

    public static String sha256Hex(String value) {
        return hex("SHA-256", value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] value) {
        return hex("SHA-256", value == null ? new byte[0] : value);
    }

    public static byte[] sha256(byte[] value) {
        return digest("SHA-256", value == null ? new byte[0] : value);
    }

    public static byte[] sha256(String value) {
        return sha256(value == null ? null : value.getBytes(StandardCharsets.UTF_8));
    }


    public static String sha256HexPrefix(String value, int length) {
        String hash = sha256Hex(value);
        int safeLength = Math.max(1, Math.min(hash.length(), length));
        return hash.substring(0, safeLength);
    }

    public static String sha384Hex(String value) {
        return hex("SHA-384", value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256HexBytes(byte[] value) {
        return sha256Hex(value);
    }

    private static String hex(String algorithm, byte[] bytes) {
        byte[] hashed = digest(algorithm, bytes);
                StringBuilder builder = new StringBuilder(hashed.length * 2);
        for (byte item : hashed) {
            builder.append(String.format("%02x", item));
        }
        return builder.toString();
    }

    private static byte[] digest(String algorithm, byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return digest.digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("algoritmo de hash indisponivel", e);
        }
    }
}
