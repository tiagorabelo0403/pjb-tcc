package com.tcc.pjb.backend.core.icp;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record RecursalIcpBrasilMetadata(
        boolean enabled,
        boolean certificateAvailable,
        Boolean validationOk,
        String failureReason,
        String profileCandidate,
        String profileAchieved,
        String certType,
        String acSigla,
        String cpfTitular,
        String cnpjTitular,
        String serialHex,
        Long signerUserId,
        String mode) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", enabled);
        out.put("certificateAvailable", certificateAvailable);
        put(out, "validationOk", validationOk);
        put(out, "failureReason", failureReason);
        put(out, "profileCandidate", profileCandidate);
        put(out, "profileAchieved", profileAchieved);
        put(out, "certType", certType);
        put(out, "acSigla", acSigla);
        put(out, "cpfTitular", cpfTitular);
        put(out, "cnpjTitular", cnpjTitular);
        put(out, "serialHex", serialHex);
        put(out, "signerUserId", signerUserId);
        put(out, "mode", mode);
        return Collections.unmodifiableMap(out);
    }

    private static void put(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
