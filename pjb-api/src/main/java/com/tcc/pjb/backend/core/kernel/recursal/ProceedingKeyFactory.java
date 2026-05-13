package com.tcc.pjb.backend.core.kernel.recursal;

import java.util.Objects;

public final class ProceedingKeyFactory {

    private ProceedingKeyFactory() {
    }

    public static String keyOf(Long caseFileId,
                               InstanceLevel instance,
                               String court,
                               String numeroUnificadoOrHint,
                               String discriminator) {
        Objects.requireNonNull(caseFileId, "caseFileId");
        Objects.requireNonNull(instance, "instance");
        String c = Objects.toString(court, "").trim();
        String n = Objects.toString(numeroUnificadoOrHint, "").trim();
        String d = Objects.toString(discriminator, "").trim();
        String canonical = caseFileId + "|" + instance.name() + "|" + c + "|" + n + "|" + d;
        return RecursalHash.sha256Hex(canonical);
    }

    public static String rootKey(Long caseFileId,
                                 InstanceLevel instance,
                                 String court,
                                 String numeroUnificado) {
        return keyOf(caseFileId, instance, court, numeroUnificado, "ROOT");
    }

    public static String shadowKey(Long caseFileId,
                                   InstanceLevel instance,
                                   String targetCourtHint,
                                   LegalAppealType appealType,
                                   String originNumeroUnificado) {
        String hint = "SHADOW:" + appealType.name() + ":" + Objects.toString(originNumeroUnificado, "").trim();
        return keyOf(caseFileId, instance, targetCourtHint, hint, "SHADOW");
    }

    public static String realKey(Long caseFileId,
                                 InstanceLevel instance,
                                 String court,
                                 String numeroUnificado) {
        return keyOf(caseFileId, instance, court, numeroUnificado, "REAL");
    }
}
