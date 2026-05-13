package com.tcc.pjb.backend.governance.source;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SourceSwitchExhaustivenessGuardTest {

    @Test
    void switchesInferiveisEmEnumsDevemSerExaustivosOuTerDefault() {
        List<String> offenders = SourceGovernanceScanner.inferableEnumSwitchGaps();
        assertTrue(offenders.isEmpty(), "Switch em enum sem cobertura completa ou sem default: " + offenders);
    }
}
