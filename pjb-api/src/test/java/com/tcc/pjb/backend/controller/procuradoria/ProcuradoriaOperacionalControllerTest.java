package com.tcc.pjb.backend.controller.procuradoria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.profile.operational.InstitutionalRecursoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.procuradoria.surface.ProcuradoriaOperationalSurfaceFacadeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProcuradoriaOperacionalControllerTest {

    private ProcuradoriaOperationalSurfaceFacadeService facadeService;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        facadeService = mock(ProcuradoriaOperationalSurfaceFacadeService.class);
        CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
        objectMapper = new ObjectMapper();
        ProcuradoriaOperacionalController controller = new ProcuradoriaOperacionalController(facadeService, rateLimiter);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void interporRecurso_expoeHeadersDeDepreciacaoDaSuperficieLegada() throws Exception {
        when(facadeService.interporRecurso(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(new SurfaceActionResponse("procuradoria.operacional", "interporRecurso", 15L, "RECURSO_INTERPOSTO", null));

        String body = objectMapper.writeValueAsString(new InstitutionalRecursoRequest(
                "EMBARGOS_DECLARACAO", "razoes recursais", "fundamentacao", true, false, "obs"));

        mockMvc.perform(post("/api/v1/procuradoria/operacional/processos/{processoId}/recurso", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", "Tue, 28 Oct 2026 00:00:00 GMT"))
                .andExpect(header().string("Link", "</api/v1/recursal/processos/15/recurso>; rel=\"successor-version\""));
    }

    @Test
    void snapshot_naoGanhaHeadersDeDepreciacao() throws Exception {
        when(facadeService.snapshot()).thenReturn(new SurfaceSnapshotResponse("procuradoria.operacional", List.of()));

        mockMvc.perform(get("/api/v1/procuradoria/operacional/snapshot"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"));
    }
}
