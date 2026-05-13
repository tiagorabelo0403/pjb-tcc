package com.tcc.pjb.backend.governance.source;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class EnumCollectionGovernanceTest {

    @Test
    void naoDeveHaverFactoriesDeColecaoComEnumValuesNoFormatoErrado() {
        List<String> offenders = SourceGovernanceScanner.suspiciousEnumFactoryCalls();
        assertTrue(offenders.isEmpty(), "Uso frágil de Set.of/EnumSet.of com values() detectado: " + offenders);
    }
}
