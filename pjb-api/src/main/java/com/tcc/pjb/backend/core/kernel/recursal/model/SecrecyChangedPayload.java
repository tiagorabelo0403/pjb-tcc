package com.tcc.pjb.backend.core.kernel.recursal.model;

import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;

public record SecrecyChangedPayload(
        NivelSigilo newLevel,
        String reason
) implements CanonicalFactPayload {

    public SecrecyChangedPayload {
        Objects.requireNonNull(newLevel, "newLevel é obrigatório");
        reason = Objects.toString(reason, "").trim();
    }
}
