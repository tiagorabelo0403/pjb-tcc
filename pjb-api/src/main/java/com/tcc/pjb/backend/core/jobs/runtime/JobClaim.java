package com.tcc.pjb.backend.core.jobs.runtime;

import java.util.UUID;

public record JobClaim(UUID id, String type, String ownerUserId, String inboxKey, String uf, String orgao) {

    public static JobClaim of(UUID id, String type, String ownerUserId, String inboxKey) {
        String uf = null;
        String orgao = null;
        if (inboxKey != null && !inboxKey.isBlank()) {
            String k = inboxKey.trim();
            int p1 = k.indexOf(':');
            if (p1 > 0) {
                uf = k.substring(0, p1);
                int p2 = k.indexOf(':', p1 + 1);
                if (p2 > p1 + 1) {
                    orgao = k.substring(p1 + 1, p2);
                }
            }
        }
        return new JobClaim(id, type, ownerUserId, inboxKey, uf, orgao);
    }
}
