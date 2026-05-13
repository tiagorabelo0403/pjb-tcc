package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class PjbInstitutionalRoutingAugmentedRequestTest {

    @Test
    void mustMergeHeadersWithoutLosingOriginalOnes() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-PJB-UF", "CE");
        request.addHeader("X-Trace", "abc");

        PjbInstitutionalRoutingAugmentedRequest wrapped = new PjbInstitutionalRoutingAugmentedRequest(request, Map.of(
                "X-PJB-Tribunal", "TJCE",
                "X-PJB-Read-Replica", "read-ce"
        ));

        assertThat(wrapped.getHeader("X-PJB-UF")).isEqualTo("CE");
        assertThat(wrapped.getHeader("x-pjb-tribunal")).isEqualTo("TJCE");
        assertThat(wrapped.getHeader("X-PJB-Read-Replica")).isEqualTo("read-ce");
        assertThat(Collections.list(wrapped.getHeaderNames()))
                .contains("X-PJB-UF", "X-Trace", "X-PJB-Tribunal", "X-PJB-Read-Replica");
    }
}
