package com.tcc.pjb.backend.service.criminal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PoliceTransactionalAdapterMeshServiceTest {

    @Test
    void shouldExposeSovereignTransactionalMesh() {
        PoliceTransactionalAdapterMeshService service = new PoliceTransactionalAdapterMeshService(new ObjectMapper());
        Map<String, Object> mesh = service.sovereignMesh(TipoUsuario.DELEGADO_POLICIA_FEDERAL);
        Assertions.assertEquals("POLICE_TRANSACTIONAL_SOVEREIGN_MESH", mesh.get("mode"));
        Assertions.assertEquals(Boolean.TRUE, mesh.get("preferNativeBeforePartner"));
        Assertions.assertNotNull(mesh.get("transactionFamilies"));
    }
}
