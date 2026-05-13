package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.security.authz.PjbAuthorizationTrailAnalyticsResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbAuthorizationTrailAnalyticsProjectionAssemblerTest {

    @Test
    void mustAggregateOverviewAndTopDimensionsAcrossBuckets() {
        PjbAuthorizationTrailAnalyticsProjectionAssembler assembler = new PjbAuthorizationTrailAnalyticsProjectionAssembler();
        PjbAuthorizationTrailAnalyticsResponse response = assembler.assemble(
                PjbAuthorizationTrailTemporalGranularity.DAY,
                3,
                List.of(
                        entry("2026-04-03T00:00:00Z", "2026-04-04T00:00:00Z", PjbAuthorizationTrailAnalyticsDimensionType.OVERVIEW, "ALL", 10, 8, 2),
                        entry("2026-04-04T00:00:00Z", "2026-04-05T00:00:00Z", PjbAuthorizationTrailAnalyticsDimensionType.OVERVIEW, "ALL", 5, 3, 2),
                        entry("2026-04-03T00:00:00Z", "2026-04-04T00:00:00Z", PjbAuthorizationTrailAnalyticsDimensionType.INTEGRATION, "INFOJUD", 4, 2, 2),
                        entry("2026-04-04T00:00:00Z", "2026-04-05T00:00:00Z", PjbAuthorizationTrailAnalyticsDimensionType.INTEGRATION, "INFOJUD", 3, 3, 0),
                        entry("2026-04-03T00:00:00Z", "2026-04-04T00:00:00Z", PjbAuthorizationTrailAnalyticsDimensionType.RESOURCE_TYPE, "PROCESSO", 6, 5, 1)
                )
        );

        assertEquals(15, response.representedEvents());
        assertEquals(2, response.timeSeries().size());
        assertEquals("INTEGRATION", response.dimensions().getFirst().dimensionType());
        assertEquals("INFOJUD", response.dimensions().getFirst().buckets().getFirst().code());
        assertTrue(response.lastMaterializedAt() != null);
    }

    private PjbAuthorizationTrailAnalyticsEntry entry(String startedAt,
                                                      String endedAtExclusive,
                                                      PjbAuthorizationTrailAnalyticsDimensionType dimensionType,
                                                      String code,
                                                      long total,
                                                      long allowed,
                                                      long denied) {
        return PjbAuthorizationTrailAnalyticsEntry.of(
                PjbAuthorizationTrailTemporalGranularity.DAY,
                Instant.parse(startedAt),
                Instant.parse(endedAtExclusive),
                dimensionType,
                code,
                code,
                total,
                allowed,
                denied,
                denied,
                denied,
                denied,
                total,
                total,
                Instant.parse(startedAt),
                Instant.parse(startedAt),
                Instant.parse("2026-04-05T10:00:00Z"),
                total
        );
    }
}
