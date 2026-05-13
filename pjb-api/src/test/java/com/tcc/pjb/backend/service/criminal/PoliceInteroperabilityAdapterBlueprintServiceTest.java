package com.tcc.pjb.backend.service.criminal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PoliceInteroperabilityAdapterBlueprintServiceTest {

    @Test
    void shouldExposeOperationalMeshForCivilPolice() {
        PoliceInteroperabilityAdapterBlueprintService service = new PoliceInteroperabilityAdapterBlueprintService(new ObjectMapper());
        Object mode = service.operationalMesh(TipoUsuario.DELEGADO_POLICIA).get("mode");
        Object functions = service.operationalMesh(TipoUsuario.DELEGADO_POLICIA).get("mandatoryFunctionFamilies");
        Assertions.assertEquals("POLICE_INTEROPERABILITY_OPERATIONAL_MESH", mode);
        Assertions.assertNotNull(functions);
    }
}
