package com.tcc.pjb.backend.service.criminal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PoliceSovereignOperationalWorkbenchServiceTest {

    @Test
    void shouldComposeNativeFirstWorkbench() {
        ObjectMapper mapper = new ObjectMapper();
        PoliceInvestigationSystemLandscapeService landscapeService = new PoliceInvestigationSystemLandscapeService(mapper);
        PoliceInteroperabilityAdapterBlueprintService adapterService = new PoliceInteroperabilityAdapterBlueprintService(mapper);
        PjbPoliceNativeToolbeltService nativeService = new PjbPoliceNativeToolbeltService(mapper);
        PoliceTransactionalAdapterMeshService transactionalService = new PoliceTransactionalAdapterMeshService(mapper);
        PoliceTraceableExecutionLedgerService ledgerService = new PoliceTraceableExecutionLedgerService(org.mockito.Mockito.mock(com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService.class));
        PoliceSovereignOperationalWorkbenchService service = new PoliceSovereignOperationalWorkbenchService(landscapeService, adapterService, nativeService, transactionalService, ledgerService);
        Map<String, Object> mesh = service.compose(TipoUsuario.DELEGADO_POLICIA);
        Assertions.assertEquals("POLICE_SOVEREIGN_OPERATIONAL_WORKBENCH", mesh.get("mode"));
        Assertions.assertEquals(Boolean.TRUE, mesh.get("nativeFirst"));
        Assertions.assertNotNull(mesh.get("nativeToolbelt"));
        Assertions.assertNotNull(mesh.get("transactionalAdapterMesh"));
    }
}
