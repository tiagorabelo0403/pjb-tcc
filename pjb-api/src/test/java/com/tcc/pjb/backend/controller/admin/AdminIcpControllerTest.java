package com.tcc.pjb.backend.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.icp.IcpBrasilApplicationService;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilOcspHealthSnapshot;
import com.tcc.pjb.backend.core.icp.domain.IcpBrasilSignaturePolicySnapshot;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminIcpControllerTest {

    private IcpBrasilApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(IcpBrasilApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminIcpController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void policy_deveExporSnapshot() throws Exception {
        when(applicationService.policy()).thenReturn(new IcpBrasilSignaturePolicySnapshot(true, false, "LTA"));

        mockMvc.perform(get("/api/v1/admin/icp/policy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.profileCandidate").value("LTA"));
    }

    @Test
    void ocspHealth_deveExporConfig() throws Exception {
        when(applicationService.ocspHealth()).thenReturn(new IcpBrasilOcspHealthSnapshot(true, 3600L, "pjb:icp:ocsp:"));

        mockMvc.perform(get("/api/v1/admin/icp/ocsp/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.ttlSeconds").value(3600));
    }
}
