package com.tcc.pjb.backend.service.criminal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PjbPoliceNativeToolbeltServiceTest {

    @Test
    void shouldExposeNativeWorkbench() {
        PjbPoliceNativeToolbeltService service = new PjbPoliceNativeToolbeltService(new ObjectMapper());
        Map<String, Object> workbench = service.nativeWorkbench(TipoUsuario.DELEGADO_POLICIA);
        Assertions.assertEquals("PJB_POLICE_NATIVE_WORKBENCH", workbench.get("mode"));
        Assertions.assertEquals(Boolean.TRUE, workbench.get("nativeFirst"));
        Assertions.assertNotNull(workbench.get("mandatoryFunctionFamilies"));
    }
}
