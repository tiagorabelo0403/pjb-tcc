package com.tcc.pjb.backend.core.kernel.recursal.model;

import java.util.Objects;

public record AdmissibilityPayload(
        boolean granted,
        String authority,
        String reason
) implements CanonicalFactPayload {

    public AdmissibilityPayload {
        authority = Objects.toString(authority, "").trim();
        reason = Objects.toString(reason, "").trim();
    }
}
