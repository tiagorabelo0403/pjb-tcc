package com.tcc.pjb.backend.service.criminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PoliceInvestigationSystemLandscapeServiceTest {

    private final PoliceInvestigationSystemLandscapeService service = new PoliceInvestigationSystemLandscapeService(new ObjectMapper());

    @Test
    void deveRetornarLandscapeCivilComSistemasEstaduaisENacionais() {
        Map<String, Object> landscape = service.landscapeFor(TipoUsuario.DELEGADO_POLICIA);
        assertEquals("POLICIA_CIVIL", landscape.get("actorLane"));
        assertTrue(((Number) landscape.get("systemsCount")).intValue() >= 5);
        List<?> systems = (List<?>) landscape.get("systems");
        assertFalse(systems.isEmpty());
    }

    @Test
    void deveRetornarBlueprintFederalComValidador() {
        Map<String, Object> blueprint = service.workstationBlueprint(TipoUsuario.DELEGADO_POLICIA_FEDERAL);
        assertEquals("POLICIA_FEDERAL", blueprint.get("actorLane"));
        Map<?, ?> signature = (Map<?, ?>) blueprint.get("signatureAndAuthenticity");
        assertEquals(Boolean.TRUE, signature.get("validatorRequired"));
        assertEquals(Boolean.TRUE, signature.get("publicAuthenticityVerifier"));
    }

    @Test
    void deveRetornarDetalheEncontrado() {
        Map<String, Object> detail = service.detail("EPOL_PF");
        assertEquals(Boolean.TRUE, detail.get("found"));
        assertEquals("EPOL_PF", detail.get("code"));
    }
}
