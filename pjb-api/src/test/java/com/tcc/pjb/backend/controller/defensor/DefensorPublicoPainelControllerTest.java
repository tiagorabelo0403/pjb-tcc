package com.tcc.pjb.backend.controller.defensor;

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
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.defensor.DefensorPublicoPainelService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DefensorPublicoPainelControllerTest {

    private InstitutionalPainelSurfaceFacadeService facadeService;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DefensorPublicoPainelService service = mock(DefensorPublicoPainelService.class);
        facadeService = mock(InstitutionalPainelSurfaceFacadeService.class);
        CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
        PainelSharedExperienceService sharedExperienceService = mock(PainelSharedExperienceService.class);
        objectMapper = new ObjectMapper();
        DefensorPublicoPainelController controller = new DefensorPublicoPainelController(
                service, facadeService, rateLimiter, sharedExperienceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void interporRecurso_expoeHeadersDeDepreciacaoDaSuperficieLegada() throws Exception {
        when(facadeService.defensorInterporRecurso(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(new SurfaceActionResponse("defensoria.recurso", "interpor-recurso", 9L, "RECURSO_INTERPOSTO", null));

        String body = objectMapper.writeValueAsString(new InstitutionalRecursoRequest(
                "APELACAO", "razoes recursais", "fundamentacao", true, false, "obs"));

        mockMvc.perform(post("/api/v1/defensor/recurso/{processoId}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", "Tue, 28 Oct 2026 00:00:00 GMT"))
                .andExpect(header().string("Link", "</api/v1/recursal/processos/9/recurso>; rel=\"successor-version\""));
    }

    @Test
    void assistidos_naoGanhaHeadersDeDepreciacao() throws Exception {
        when(facadeService.defensorAssistidos()).thenReturn(new SurfaceCollectionResponse("defensoria.assistidos", List.of()));

        mockMvc.perform(get("/api/v1/defensor/assistidos"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"));
    }
}
