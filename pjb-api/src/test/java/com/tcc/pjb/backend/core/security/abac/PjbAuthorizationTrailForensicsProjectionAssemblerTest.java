package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailForensicsResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbAuthorizationTrailForensicsProjectionAssemblerTest {

    @Test
    void mustAggregateTimeSeriesAndDimensionBuckets() {
        PjbAuthorizationTrailForensicsProjectionAssembler assembler = new PjbAuthorizationTrailForensicsProjectionAssembler();
        PjbAuthorizationTrailForensicsResponse response = assembler.assemble(
                PjbAuthorizationTrailTemporalGranularity.HOUR,
                2000,
                List.of(
                        snapshot("2026-04-04T10:15:00Z", "READ_PROCESSO", "PROCESSO", "PROC-1", true, "NONE", "TJCE-1G-VARA1", "NONE"),
                        snapshot("2026-04-04T10:45:00Z", "REQUEST_INFOJUD", "INTEGRACAO_EXTERNA", "INFOJUD", false, "INFOJUD", "NONE", "INFOJUD"),
                        snapshot("2026-04-04T11:05:00Z", "INSTITUTIONAL_BOX_CAPABILITY", "CAIXA_INSTITUCIONAL", "TJCE:INBOX:RECEBER:EXP-9", true, "NONE", "TJCE", "INBOX")
                )
        );

        assertEquals(3, response.summary().total());
        assertEquals(2, response.timeSeries().size());
        assertEquals(2, response.timeSeries().getFirst().total());
        assertEquals(1, response.summary().denied());
        assertEquals("INFOJUD", response.byIntegration().getFirst().code());
        assertTrue(response.byInstitutionalUnit().stream().anyMatch(bucket -> "TJCE".equals(bucket.code())));
        assertEquals("CAIXA_INSTITUCIONAL", response.byResourceType().getFirst().code());
    }

    private PjbAuthorizationTrailSnapshot snapshot(String occurredAt,
                                                   String action,
                                                   String resourceType,
                                                   String resourceId,
                                                   boolean allowed,
                                                   String integrationCode,
                                                   String institutionalUnitCode,
                                                   String governanceScope) {
        return new PjbAuthorizationTrailSnapshot(
                Instant.parse(occurredAt),
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
                allowed ? PjbAuthorizationRiskLevel.MODERADO : PjbAuthorizationRiskLevel.CRITICO,
                allowed ? 32 : 87,
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
                resourceType.equals("CAIXA_INSTITUCIONAL") ? "INBOX" : "NONE",
                resourceType.equals("CAIXA_INSTITUCIONAL") ? "RECEBER" : "NONE",
                resourceType.equals("CAIXA_INSTITUCIONAL") ? "EXP-9" : "NONE",
                action + '-' + resourceId + '-' + allowed,
                "desc"
        );
    }
}
