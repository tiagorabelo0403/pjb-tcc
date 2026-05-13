package com.tcc.pjb.backend.core.kernel.recursal.model;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;

public record AppealFiledPayload(
        LegalAppealType appealType,
        String protocolNumber,
        String filingParty,
        InstanceLevel targetInstanceHint,
        String targetCourtHint,
        boolean autosApartadosLikely,
        String notes
) implements CanonicalFactPayload {

    public AppealFiledPayload {
        Objects.requireNonNull(appealType, "appealType é obrigatório");
        if (targetInstanceHint == null) targetInstanceHint = InstanceLevel.FIRST_INSTANCE;
        protocolNumber = Objects.toString(protocolNumber, "").trim();
        filingParty = Objects.toString(filingParty, "").trim();
        targetCourtHint = Objects.toString(targetCourtHint, "").trim();
        notes = Objects.toString(notes, "").trim();
    }
}
