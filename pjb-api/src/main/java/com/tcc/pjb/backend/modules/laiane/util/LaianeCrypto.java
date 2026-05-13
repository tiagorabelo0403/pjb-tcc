package com.tcc.pjb.backend.modules.laiane.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class LaianeCrypto {

    private LaianeCrypto() {
    }

    public static String sha256Hex(String input) {
        if (input == null) input = "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao calcular SHA-256", e);
        }
    }
}
