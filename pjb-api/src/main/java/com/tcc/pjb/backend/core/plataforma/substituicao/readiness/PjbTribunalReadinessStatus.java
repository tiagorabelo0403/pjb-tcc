package com.tcc.pjb.backend.core.plataforma.substituicao.readiness;

public enum PjbTribunalReadinessStatus {
    READY_FOR_PILOT,
    READY_FOR_PRODUCTION,
    BLOCKED_BY_GOVERNANCE,
    BLOCKED_BY_CONNECTOR,
    BLOCKED_BY_MIGRATION,
    BLOCKED_BY_OPERATIONAL_RISK
}
