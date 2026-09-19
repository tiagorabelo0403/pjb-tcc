package com.tcc.pjb.backend.core.security.crypto.quantum;

import java.util.Arrays;
import java.util.Optional;

public enum PqcSignatureAlgorithm {

    ML_DSA_44("ML-DSA-44"),
    ML_DSA_65("ML-DSA-65"),
    ML_DSA_87("ML-DSA-87");

    private final String jcaName;

    PqcSignatureAlgorithm(String jcaName) {
        this.jcaName = jcaName;
    }

    public String jcaName() {
        return jcaName;
    }

    public static Optional<PqcSignatureAlgorithm> fromJcaName(String jcaName) {
        return Arrays.stream(values())
                .filter(algorithm -> algorithm.jcaName.equals(jcaName))
                .findFirst();
    }
}
