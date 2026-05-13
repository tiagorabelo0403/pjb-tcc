package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbAuthorizationTrailCsvExporterTest {

    @Test
    void mustExportHeaderAndEscapedValues() {
        PjbAuthorizationTrailCsvExporter exporter = new PjbAuthorizationTrailCsvExporter();
        String csv = exporter.export(List.of(new PjbAuthorizationTrailSnapshot(
                Instant.parse("2026-04-04T12:00:00Z"),
                "AUTHZ_REQUEST_INFOJUD_DENY",
                "REQUEST_INFOJUD",
                "INTEGRACAO_EXTERNA",
                "INFOJUD",
                false,
                "motivo \"\"formal\"\"",
                "abac-v1",
                "sha256-policy",
                77L,
                "MAGISTRADO",
                "req-77",
                "consulta",
                "SIGILO_N2",
                PjbAuthorizationRiskLevel.CRITICO,
                95,
                "MFA",
                "STEP_UP",
                true,
                false,
                "FORMAL_CONTEXT",
                "FORMAL_CONTEXT_REQUIRED",
                "INFOJUD",
                true,
                false,
                "INFOJUD",
                "NONE",
                "NONE",
                "NONE",
                "NONE",
                "payload",
                "desc"
        )));

        assertTrue(csv.startsWith("occurredAt,auditEventCode"));
        assertTrue(csv.contains("\"REQUEST_INFOJUD\""));
        assertTrue(csv.contains("motivo \"\"\"\"formal\"\"\"\""));
    }
}
