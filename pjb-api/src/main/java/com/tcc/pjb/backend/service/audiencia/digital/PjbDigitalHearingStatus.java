package com.tcc.pjb.backend.service.audiencia.digital;

public enum PjbDigitalHearingStatus {
    READY,
    WAITING_ATTENDEES,
    WAITING_RECORDING,
    WAITING_ACCESSIBILITY,
    HUMAN_REVIEW_REQUIRED,
    BLOCKED
}
