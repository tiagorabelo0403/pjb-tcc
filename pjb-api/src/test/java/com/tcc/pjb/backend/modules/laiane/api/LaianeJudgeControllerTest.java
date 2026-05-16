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
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeQueueBucketDto;
import com.tcc.pjb.backend.modules.laiane.dto.roles.judge.LaianeJudgeQueuePanelResponse;
import com.tcc.pjb.backend.modules.laiane.service.LaianeJudgeRadarJurisprudenciaService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeJudgeService;
import com.tcc.pjb.backend.modules.laiane.service.LaianeSentencaService;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = LaianeJudgeController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class))
@Import(WebMvcTestSecurityConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
@WithMockUser(roles = "MAGISTRADO")
class LaianeJudgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LaianeJudgeService service;

    @MockitoBean
    private LaianeSentencaService sentencaService;

    @MockitoBean
    private LaianeJudgeRadarJurisprudenciaService radarJurisprudenciaService;

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
    void queuePanel_shouldReturnConfiguredPanelAndUseDefaultLimit() throws Exception {
        var resp = LaianeJudgeQueuePanelResponse.builder()
                .generatedAt(Instant.now())
                .uf("CE")
                .comarca("Morada Nova")
                .total(1)
                .buckets(List.of(LaianeJudgeQueueBucketDto.builder().nome("HOJE").count(1).items(List.of()).build()))
                .build();

        when(service.queuePanel(30)).thenReturn(resp);

        mockMvc.perform(get("/api/v1/laiane/judge/queues/panel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uf").value("CE"))
                .andExpect(jsonPath("$.comarca").value("Morada Nova"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.buckets[0].nome").value("HOJE"));

        verify(service).queuePanel(30);
    }
}
