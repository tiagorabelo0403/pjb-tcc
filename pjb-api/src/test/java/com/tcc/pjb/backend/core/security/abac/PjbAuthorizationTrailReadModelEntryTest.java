package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PjbAuthorizationTrailReadModelEntryTest {

    @Test
    void mustRoundTripSnapshotToPersistentReadModel() {
        PjbAuthorizationTrailSnapshot snapshot = new PjbAuthorizationTrailSnapshot(
                Instant.parse("2026-04-04T18:15:00Z"),
                "AUTHZ_REQUEST_INFOJUD_DENY",
                "REQUEST_INFOJUD",
                "INTEGRACAO_EXTERNA",
                "INFOJUD",
                false,
                "delegation_required",
                "abac-v1",
                "sha-policy",
                77L,
                "SERVIDOR",
                "req-7",
                "consulta motivada",
                "SIGILO_N2",
                PjbAuthorizationRiskLevel.CRITICO,
                88,
                "MFA",
                "STEP_UP_REQUIRED",
                true,
                false,
                "DELEGATION",
                "DELEGATION_REQUIRED",
                "INFOJUD",
                true,
                false,
                "INFOJUD",
                "NONE",
                "NONE",
                "NONE",
                "NONE",
                "payload-hash",
                "desc"
        );

        PjbAuthorizationTrailReadModelEntry entry = PjbAuthorizationTrailReadModelEntry.from(snapshot);
        PjbAuthorizationTrailSnapshot restored = entry.toSnapshot();

        assertEquals(snapshot.auditEventCode(), restored.auditEventCode());
        assertEquals(snapshot.resourceId(), restored.resourceId());
        assertEquals(snapshot.riskLevel(), restored.riskLevel());
        assertEquals(snapshot.governanceChannel(), restored.governanceChannel());
        assertEquals(snapshot.payloadHash(), restored.payloadHash());
    }
}
