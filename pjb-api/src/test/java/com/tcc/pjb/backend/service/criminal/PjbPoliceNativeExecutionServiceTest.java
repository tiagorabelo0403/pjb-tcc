package com.tcc.pjb.backend.service.criminal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.criminal.PoliceNativeCautelarDispatchRequest;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PjbPoliceNativeExecutionServiceTest {

    @Test
    void shouldExposeExecutableCatalogAndDispatch() {
        PjbPoliceNativeExecutionService service = new PjbPoliceNativeExecutionService(
                new PjbPoliceNativeToolbeltService(new ObjectMapper()),
                new PoliceTransactionalAdapterMeshService(new ObjectMapper()),
                new PoliceSovereignOperationalWorkbenchService(
                        new PoliceInvestigationSystemLandscapeService(new ObjectMapper()),
                        new PoliceInteroperabilityAdapterBlueprintService(new ObjectMapper()),
                        new PjbPoliceNativeToolbeltService(new ObjectMapper()),
                        new PoliceTransactionalAdapterMeshService(new ObjectMapper()),
                        new PoliceTraceableExecutionLedgerService(org.mockito.Mockito.mock(com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService.class))
                ),
                new PoliceTraceableExecutionLedgerService(org.mockito.Mockito.mock(com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService.class))
        );
        Map<String, Object> catalog = service.executableCatalog(TipoUsuario.DELEGADO_POLICIA);
        Assertions.assertEquals("PJB_POLICE_NATIVE_EXECUTABLE_CATALOG", catalog.get("mode"));
        Assertions.assertFalse(((List<?>) catalog.get("tools")).isEmpty());

        Map<String, Object> dispatch = service.dispatchCautelar(
                TipoUsuario.DELEGADO_POLICIA,
                new PoliceNativeCautelarDispatchRequest(1L, 2L, "Busca e apreensao", "Fundamento", List.of("EV-1"), Boolean.TRUE, "TJCE", Boolean.TRUE, Boolean.TRUE)
        );
        Assertions.assertEquals("REMESSA_CAUTELAR_PREPARADA", dispatch.get("status"));
        Assertions.assertNotNull(dispatch.get("route"));
    }
}
