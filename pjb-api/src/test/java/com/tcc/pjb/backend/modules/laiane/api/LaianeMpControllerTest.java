package com.tcc.pjb.backend.modules.laiane.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.configs.SecurityConfig;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.recursal.governance.RecursalFactsIngressProperties;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.DeviceSecurityProperties;
import com.tcc.pjb.backend.core.security.device.download.DownloadBudgetService;
import com.tcc.pjb.backend.core.security.device.download.DownloadEventService;
import com.tcc.pjb.backend.core.security.device.download.PdfWatermarkService;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficePersonalScopeService;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeMpDeadlineMonitorResponse;
import com.tcc.pjb.backend.modules.laiane.service.LaianeMpService;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = LaianeMpController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@Import(WebMvcTestSecurityConfig.class)
@WithMockUser(roles = "MEMBRO_MINISTERIO_PUBLICO")
class LaianeMpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LaianeMpService service;

    @MockitoBean
    private MembroEquipeRepository membroEquipeRepository;

    @MockitoBean
    private EntityManager entityManager;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private OfficePersonalScopeService officePersonalScopeService;

    @MockitoBean
    private DeviceSecurityProperties deviceSecurityProperties;

    @MockitoBean
    private PdfWatermarkService pdfWatermarkService;

    @MockitoBean
    private DownloadEventService downloadEventService;

    @MockitoBean
    private DownloadBudgetService downloadBudgetService;

    @MockitoBean
    private AuditLedgerService auditLedgerService;

    @MockitoBean
    private RecursalFactsIngressProperties recursalFactsIngressProperties;

    @Test
    void monitorDeadlines_shouldReturnConfiguredPayloadAndUseDefaultLimit() throws Exception {
        var resp = LaianeMpDeadlineMonitorResponse.builder()
                .generatedAt(Instant.now())
                .horizonDays(30)
                .total(0)
                .overdue(0)
                .items(java.util.List.of())
                .build();

        when(service.monitorDeadlines(30, 50)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/laiane/mp/deadlines/monitor").param("horizonDays", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horizonDays").value(30))
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        verify(service).monitorDeadlines(30, 50);
    }
}
