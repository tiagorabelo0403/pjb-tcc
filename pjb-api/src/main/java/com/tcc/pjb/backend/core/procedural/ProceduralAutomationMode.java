package com.tcc.pjb.backend.core.procedural;

public enum ProceduralAutomationMode {
    ADVISORY_ONLY,
    ASSISTED_DECISION,
    HUMAN_GATE_REQUIRED,
    AUTOMATE_SAFE;

    public boolean permitsAutomation() {
        return this == AUTOMATE_SAFE;
    }

    public boolean requiresHumanGate() {
        return this == HUMAN_GATE_REQUIRED || this == ADVISORY_ONLY;
    }
}
