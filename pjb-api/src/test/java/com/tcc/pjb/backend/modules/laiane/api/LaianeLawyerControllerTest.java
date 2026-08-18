package com.tcc.pjb.backend.modules.laiane.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
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
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.LaianeLawyerAttachmentValidationRequest;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.LaianeLawyerAttachmentValidationResponse;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeLawyerService;
import com.tcc.pjb.backend.modules.laiane.service.LaianePeticaoAssistService;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = LaianeLawyerController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import(WebMvcTestSecurityConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
@WithMockUser(authorities = "ROLE_ADVOGADO")
class LaianeLawyerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LaianeLawyerService service;

    @MockitoBean
    private LaianePeticaoAssistService assistService;

    @MockitoBean
    private com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter capabilityRateLimiter;

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

    @MockitoBean
    private PjbTimeService timeService;

    @Test
    void validateAttachments_shouldReturn200AndCallService() throws Exception {
        var resp = LaianeLawyerAttachmentValidationResponse.builder()
                .rito("CPC_COMUM")
                .ok(true)
                .required(List.of("PETICAO_INICIAL"))
                .present(List.of("PETICAO_INICIAL"))
                .missing(List.of())
                .build();

        when(service.validateAttachments(any(LaianeLawyerAttachmentValidationRequest.class))).thenReturn(resp);

        String json = "{\"rito\":\"CPC_COMUM\",\"anexos\":[\"PETICAO_INICIAL\"]}";

        mockMvc.perform(post("/api/v1/laiane/lawyer/attachments/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.required[0]").value("PETICAO_INICIAL"))
                .andExpect(jsonPath("$.missing").isEmpty());

        verify(service).validateAttachments(any(LaianeLawyerAttachmentValidationRequest.class));
    }
}
