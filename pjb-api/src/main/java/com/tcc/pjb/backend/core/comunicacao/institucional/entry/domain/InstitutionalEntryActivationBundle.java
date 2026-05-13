package com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import java.util.Objects;

public record InstitutionalEntryActivationBundle(
        InstitutionalOperationalProfileProjection operationalProfile,
        InstitutionalEntryActivationDecision decision
) {
    public InstitutionalEntryActivationBundle {
        Objects.requireNonNull(decision);
    }
}
