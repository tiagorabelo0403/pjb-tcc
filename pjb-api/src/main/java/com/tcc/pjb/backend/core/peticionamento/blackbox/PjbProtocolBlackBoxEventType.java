package com.tcc.pjb.backend.core.peticionamento.blackbox;

public enum PjbProtocolBlackBoxEventType {
    REQUEST_ACCEPTED,
    DOCUMENT_HASHED,
    SIGNATURE_ATTACHED,
    CONNECTOR_ATTEMPTED,
    CONNECTOR_REJECTED,
    OUTAGE_DETECTED,
    PROTOCOL_CONFIRMED,
    PROTOCOL_FAILED
}
