package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbAuthorizationTrailAnalyticsMaterializationAssemblerTest {

    @Test
    void mustMaterializeOverviewAndDimensionBuckets() {
        PjbAuthorizationTrailAnalyticsMaterializationAssembler assembler = new PjbAuthorizationTrailAnalyticsMaterializationAssembler();
        List<PjbAuthorizationTrailAnalyticsEntry> entries = assembler.assemble(
                PjbAuthorizationTrailTemporalGranularity.HOUR,
                List.of(
                        snapshot("2026-04-04T10:15:00Z", "READ_PROCESSO", "PROCESSO", "NONE", "TJCE", "NONE"),
                        snapshot("2026-04-04T10:30:00Z", "REQUEST_INFOJUD", "INTEGRACAO_EXTERNA", "INFOJUD", "TJCE", "SIGILOSA"),
                        snapshot("2026-04-04T11:10:00Z", "INSTITUTIONAL_BOX_CAPABILITY", "CAIXA_INSTITUCIONAL", "NONE", "TJCE", "INBOX")
                ),
                Instant.parse("2026-04-04T12:00:00Z")
        );

        assertTrue(entries.stream().anyMatch(entry -> "OVERVIEW".equals(entry.getDimensionType())));
        assertTrue(entries.stream().anyMatch(entry -> "INFOJUD".equals(entry.getDimensionCode())));
        assertTrue(entries.stream().anyMatch(entry -> "TJCE".equals(entry.getDimensionCode())));
        assertEquals(2L, entries.stream().filter(entry -> "OVERVIEW".equals(entry.getDimensionType())).count());
    }

    private PjbAuthorizationTrailSnapshot snapshot(String occurredAt,
                                                   String action,
                                                   String resourceType,
                                                   String integrationCode,
                                                   String institutionalUnitCode,
                                                   String governanceScope) {
        return new PjbAuthorizationTrailSnapshot(
                Instant.parse(occurredAt),
                "AUTHZ_" + action + "_ALLOW",
                action,
                resourceType,
                action + "-ID",
                true,
                "granted",
                "abac-v1",
                "sha256-policy",
                1L,
                "SERVIDOR",
                "req-" + action,
                "just",
                "PUBLICO",
                PjbAuthorizationRiskLevel.MODERADO,
                35,
                "NONE",
                "NONE",
                false,
                true,
                "NONE",
                "NONE",
                governanceScope,
                false,
                true,
                integrationCode,
                institutionalUnitCode,
                "INBOX",
                resourceType.equals("CAIXA_INSTITUCIONAL") ? "RECEBER" : "NONE",
                "EXP-1",
                action + "-hash",
                "desc"
        );
    }
}
