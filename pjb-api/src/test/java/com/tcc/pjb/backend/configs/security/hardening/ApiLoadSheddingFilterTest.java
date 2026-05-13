package com.tcc.pjb.backend.configs.security.hardening;

import com.tcc.pjb.backend.core.observability.systemhealth.PjbOperationalCrisisService;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiLoadSheddingFilterTest {

    @Test
    void shouldRejectContainedBulkRouteBeforeExecutingChain() throws Exception {
        ApiLoadSheddingProperties loadSheddingProperties = new ApiLoadSheddingProperties();
        ApiLoadSheddingProperties.Rule rule = new ApiLoadSheddingProperties.Rule();
        rule.setName("institutional-bulk");
        rule.setPrefixes(java.util.List.of("/api/v1/jobs"));
        rule.setMaxInFlight(96);
        loadSheddingProperties.getRules().add(rule);

        PjbOperationalCrisisProperties crisisProperties = new PjbOperationalCrisisProperties();
        crisisProperties.setEnabled(true);
        crisisProperties.setMode(PjbOperationalCrisisProperties.CrisisMode.CONTAINMENT);
        crisisProperties.getBlockedPrefixes().add("/api/v1/jobs");

        ApiLoadSheddingFilter filter = new ApiLoadSheddingFilter(loadSheddingProperties, new PjbOperationalCrisisService(crisisProperties));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/jobs/rebuild-index");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(503, response.getStatus());
        assertEquals("true", response.getHeader("X-PJB-Load-Shed"));
        assertEquals("institutional-bulk", response.getHeader("X-PJB-Load-Shed-Lane"));
        assertTrue(response.getContentAsString().contains("CRISIS_CONTAINMENT"));
    }

    @Test
    void shouldKeepCriticalRouteFlowingWithCrisisHeadersWhenRuleIsAdjusted() throws Exception {
        ApiLoadSheddingProperties loadSheddingProperties = new ApiLoadSheddingProperties();
        loadSheddingProperties.setEmitDebugHeaders(true);
        ApiLoadSheddingProperties.Rule rule = new ApiLoadSheddingProperties.Rule();
        rule.setName("frontdoor-critical");
        rule.setPrefixes(java.util.List.of("/api/v1/transito"));
        rule.setMaxInFlight(64);
        rule.setAcquireTimeout(java.time.Duration.ofMillis(10));
        loadSheddingProperties.getRules().add(rule);

        PjbOperationalCrisisProperties crisisProperties = new PjbOperationalCrisisProperties();
        crisisProperties.setEnabled(true);
        crisisProperties.setMode(PjbOperationalCrisisProperties.CrisisMode.CONTAINMENT);
        PjbOperationalCrisisProperties.LaneDirective directive = new PjbOperationalCrisisProperties.LaneDirective();
        directive.setName("frontdoor-critical");
        directive.setMaxInFlightOverride(80);
        crisisProperties.getLaneDirectives().add(directive);

        ApiLoadSheddingFilter filter = new ApiLoadSheddingFilter(loadSheddingProperties, new PjbOperationalCrisisService(crisisProperties));
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/transito/documentos");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("containment", response.getHeader("X-PJB-Crisis-Mode"));
        assertEquals("80", response.getHeader("X-PJB-Load-Shed-Lane-Limit"));
    }

    @Test
    void shouldReuseLaneBucketWhenLaneCapacityChanges() throws Exception {
        ApiLoadSheddingProperties loadSheddingProperties = new ApiLoadSheddingProperties();
        loadSheddingProperties.setEmitDebugHeaders(true);
        ApiLoadSheddingProperties.Rule rule = new ApiLoadSheddingProperties.Rule();
        rule.setName("lane-dynamic");
        rule.setPrefixes(java.util.List.of("/api/v1/dynamic"));
        rule.setMaxInFlight(32);
        loadSheddingProperties.getRules().add(rule);

        ApiLoadSheddingFilter filter = new ApiLoadSheddingFilter(loadSheddingProperties, new PjbOperationalCrisisService(new PjbOperationalCrisisProperties()));

        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/api/v1/dynamic/a");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, new MockFilterChain());

        rule.setMaxInFlight(48);

        MockHttpServletRequest secondRequest = new MockHttpServletRequest("GET", "/api/v1/dynamic/b");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(secondRequest, secondResponse, new MockFilterChain());

        Field lanePermitsField = ApiLoadSheddingFilter.class.getDeclaredField("lanePermits");
        lanePermitsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<String, ?> lanePermits = (ConcurrentHashMap<String, ?>) lanePermitsField.get(filter);

        assertEquals(1, lanePermits.size());
        assertEquals("48", secondResponse.getHeader("X-PJB-Load-Shed-Lane-Limit"));
    }
}
