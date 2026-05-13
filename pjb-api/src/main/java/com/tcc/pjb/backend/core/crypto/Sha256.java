package com.tcc.pjb.backend.core.crypto;

import com.tcc.pjb.backend.core.util.Hashes;
import java.nio.charset.StandardCharsets;

public final class Sha256 {

    private Sha256() {
    }

    public static String hex(String input) {
        return hex(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String hex(byte[] bytes) {
        return Hashes.sha256HexBytes(bytes);
    }
}
