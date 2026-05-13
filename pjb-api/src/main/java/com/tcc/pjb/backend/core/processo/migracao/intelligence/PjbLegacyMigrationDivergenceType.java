package com.tcc.pjb.backend.core.processo.migracao.intelligence;

public enum PjbLegacyMigrationDivergenceType {
    PARTY_DUPLICATION,
    DOCUMENT_ORPHAN,
    MOVEMENT_UNMAPPED,
    SECRECY_MISMATCH,
    PROTOCOL_GAP,
    CLASS_SUBJECT_INCONSISTENCY,
    HISTORICAL_EVENT_LOSS
}
