package com.tcc.pjb.backend.modules.laiane.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeMpDeadlineMonitorResponse;
import com.tcc.pjb.backend.modules.laiane.service.LaianeMpService;
import com.tcc.pjb.backend.modules.support.WebMvcTestSecurityConfig;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        controllers = LaianeMpController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, EquipeSwitchInterceptor.class}))
@Import(WebMvcTestSecurityConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=servlet")
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

    @Test
    void getOficio_403_quandoUsuarioAlheio() throws Exception {
        UUID trackingCode = UUID.fromString("01963c1a-7e3f-7000-8000-000000000001");
        when(service.getOficio(trackingCode)).thenThrow(new AccessDeniedException("Acesso negado ao ofício."));

        mockMvc.perform(get("/api/v1/laiane/mp/oficios/{trackingCode}", trackingCode))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_403_quandoUsuarioOrigem() throws Exception {
        UUID trackingCode = UUID.fromString("01963c1a-7e3f-7000-8000-000000000001");
        when(service.updateOficioStatus(Mockito.eq(trackingCode), Mockito.any()))
                .thenThrow(new AccessDeniedException("Operação negada para este ofício."));

        mockMvc.perform(put("/api/v1/laiane/mp/oficios/{trackingCode}/status", trackingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ENTREGUE\",\"justificativa\":\"Controle institucional\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_statusInvalido_retorna400() throws Exception {
        UUID trackingCode = UUID.fromString("01963c1a-7e3f-7000-8000-000000000002");
        when(service.updateOficioStatus(Mockito.eq(trackingCode), Mockito.any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status desconhecido: STATUS_INVALIDO"));

        mockMvc.perform(put("/api/v1/laiane/mp/oficios/{trackingCode}/status", trackingCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"STATUS_INVALIDO\",\"justificativa\":\"teste\"}"))
                .andExpect(status().isBadRequest());
    }
}
