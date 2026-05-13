package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailQueryResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbAuthorizationTrailProjectionAssemblerTest {

    @Test
    void mustAggregateSummaryByActionResourceAndIntegration() {
        PjbAuthorizationTrailProjectionAssembler assembler = new PjbAuthorizationTrailProjectionAssembler();
        PjbAuthorizationTrailQueryResponse response = assembler.assemble(
                PjbAuthorizationTrailSourceMode.PERSISTED,
                12,
                3,
                List.of(
                        snapshot("READ_PROCESSO", "PROCESSO", "PROC-1", true, null, null, null),
                        snapshot("REQUEST_INFOJUD", "INTEGRACAO_EXTERNA", "INFOJUD", false, "INFOJUD", null, null),
                        snapshot("REQUEST_INFOJUD", "INTEGRACAO_EXTERNA", "INFOJUD", true, "INFOJUD", null, null),
                        snapshot("INSTITUTIONAL_BOX_CAPABILITY", "CAIXA_INSTITUCIONAL", "TJCE:INBOX:RECEBER:EXP-9", true, null, "RECEBER", "INBOX")
                )
        );

        assertEquals("PERSISTED", response.sourceMode());
        assertEquals(12, response.totalEntriesAvailable());
        assertEquals(12, response.totalEntriesPersisted());
        assertEquals(3, response.totalEntriesRuntime());
        assertEquals(4, response.summary().total());
        assertEquals(3, response.summary().allowed());
        assertEquals(1, response.summary().denied());
        assertEquals("REQUEST_INFOJUD", response.summary().byAction().getFirst().code());
        assertEquals("INTEGRACAO_EXTERNA", response.summary().byResourceType().getFirst().code());
        assertEquals("INFOJUD", response.summary().byIntegration().getFirst().code());
        assertEquals("RECEBER", response.summary().byInstitutionalCapability().getFirst().code());
        assertTrue(response.entries().stream().anyMatch(entry -> "TJCE".equals(entry.institutionalUnitCode())));
    }

    @Test
    void mustMergeRuntimeAndPersistedWithoutDuplicatingSameTrail() {
        PjbAuthorizationTrailProjectionAssembler assembler = new PjbAuthorizationTrailProjectionAssembler();
        PjbAuthorizationTrailSnapshot duplicated = snapshot("READ_PROCESSO", "PROCESSO", "PROC-9", true, null, null, null);
        List<PjbAuthorizationTrailSnapshot> merged = assembler.merge(
                List.of(duplicated, snapshot("REQUEST_INFOJUD", "INTEGRACAO_EXTERNA", "INFOJUD", false, "INFOJUD", null, null)),
                List.of(duplicated),
                10
        );
        assertEquals(2, merged.size());
        assertTrue(merged.stream().anyMatch(snapshot -> "REQUEST_INFOJUD".equals(snapshot.action())));
    }

    private PjbAuthorizationTrailSnapshot snapshot(String action,
                                                   String resourceType,
                                                   String resourceId,
                                                   boolean allowed,
                                                   String integrationCode,
                                                   String capability,
                                                   String caixa) {
        return new PjbAuthorizationTrailSnapshot(
                Instant.parse("2026-04-04T17:30:00Z"),
                "AUTHZ_" + action + '_' + (allowed ? "ALLOW" : "DENY"),
                action,
                resourceType,
                resourceId,
                allowed,
                allowed ? "granted" : "denied",
                "abac-v1",
                "sha256-policy",
                11L,
                "SERVIDOR",
                "req-1",
                "consulta",
                "PUBLICO",
                allowed ? PjbAuthorizationRiskLevel.MODERADO : PjbAuthorizationRiskLevel.ALTO,
                allowed ? 32 : 71,
                "NONE",
                "NONE",
                false,
                true,
                "NONE",
                "NONE",
                "NONE",
                false,
                true,
                integrationCode,
                resourceType.equals("CAIXA_INSTITUCIONAL") ? "TJCE" : null,
                caixa,
                capability,
                resourceType.equals("CAIXA_INSTITUCIONAL") ? "EXP-9" : null,
                action + '-' + resourceId + '-' + allowed,
                "desc"
        );
    }
}
