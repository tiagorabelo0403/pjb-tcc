package com.tcc.pjb.backend.core.kernel.recursal;

import com.tcc.pjb.backend.core.crypto.Sha256;

public final class RecursalHash {

    private RecursalHash() {
    }

    public static String sha256Hex(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Valor para hash recursal não informado");
        }
        return Sha256.hex(value.trim());
    }
}
