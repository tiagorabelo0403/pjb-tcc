package com.tcc.pjb.backend.governance.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ControllerRouteGovernanceTest {

    @Test
    void naoDeveHaverAssinaturasDuplicadasDeRotaEntreControllers() {
        List<String> offenders = ControllerRouteGovernanceScanner.duplicateControllerRouteMappings();
        assertTrue(offenders.isEmpty(), "Rotas HTTP duplicadas detectadas entre controllers: " + offenders);
    }

    @Test
    void placeholdersDePathVariableDevemCorresponderAosParametrosDosControllers() {
        List<String> offenders = ControllerRouteGovernanceScanner.pathVariableBindingViolations();
        assertTrue(offenders.isEmpty(), "Bindings de @PathVariable inconsistentes detectados: " + offenders);
    }

    @Test
    void basesOperacionaisDevemSerUnicasENaoVazias() {
        List<String> offenders = ControllerRouteGovernanceScanner.duplicateOperationalBases();
        assertTrue(offenders.isEmpty(), "Bases operacionais inválidas ou duplicadas: " + offenders);
    }
}
