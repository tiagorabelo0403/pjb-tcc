package com.tcc.pjb.backend.controller.mp;

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
import com.tcc.pjb.backend.service.mp.MinisterioPublicoPainelService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MinisterioPublicoPainelControllerTest {

    private InstitutionalPainelSurfaceFacadeService facadeService;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MinisterioPublicoPainelService service = mock(MinisterioPublicoPainelService.class);
        facadeService = mock(InstitutionalPainelSurfaceFacadeService.class);
        CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
        PainelSharedExperienceService sharedExperienceService = mock(PainelSharedExperienceService.class);
        objectMapper = new ObjectMapper();
        MinisterioPublicoPainelController controller = new MinisterioPublicoPainelController(
                service, facadeService, rateLimiter, sharedExperienceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void interporRecurso_expoeHeadersDeDepreciacaoDaSuperficieLegada() throws Exception {
        when(facadeService.ministerioPublicoInterporRecurso(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(new SurfaceActionResponse("mp.recurso", "interpor-recurso", 11L, "RECURSO_INTERPOSTO", null));

        String body = objectMapper.writeValueAsString(new InstitutionalRecursoRequest(
                "RESP", "razoes recursais", "fundamentacao", true, false, "obs"));

        mockMvc.perform(post("/api/v1/mp/recurso/{processoId}", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", "Tue, 28 Oct 2026 00:00:00 GMT"))
                .andExpect(header().string("Link", "</api/v1/recursal/processos/11/recurso>; rel=\"successor-version\""));
    }

    @Test
    void manifestacoesPendentes_naoGanhaHeadersDeDepreciacao() throws Exception {
        when(facadeService.ministerioPublicoManifestacoesPendentes()).thenReturn(new SurfaceCollectionResponse("mp.manifestacoes-pendentes", List.of()));

        mockMvc.perform(get("/api/v1/mp/manifestacoes/pendentes"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"));
    }
}
