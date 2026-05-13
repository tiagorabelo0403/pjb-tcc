package com.tcc.pjb.backend.core.kernel.recursal.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kind")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AppealFiledPayload.class, name = "APPEAL_FILED"),
        @JsonSubTypes.Type(value = AdmissibilityPayload.class, name = "ADMISSIBILITY"),
        @JsonSubTypes.Type(value = AutuationPayload.class, name = "AUTUATION"),
        @JsonSubTypes.Type(value = SecrecyChangedPayload.class, name = "CONFIDENTIALITY_CHANGED"),
        @JsonSubTypes.Type(value = JudgmentPublishedPayload.class, name = "JUDGMENT_PUBLISHED"),
        @JsonSubTypes.Type(value = MovementRecordedPayload.class, name = "MOVEMENT_RECORDED")
})
public sealed interface CanonicalFactPayload
        permits AppealFiledPayload, AdmissibilityPayload, AutuationPayload, SecrecyChangedPayload,
        JudgmentPublishedPayload, MovementRecordedPayload {
}
