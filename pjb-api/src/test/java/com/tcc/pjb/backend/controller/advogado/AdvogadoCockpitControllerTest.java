package com.tcc.pjb.backend.controller.advogado;

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
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoOperacaoResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.AdvogadoRecursoRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.advogado.surface.AdvogadoSurfaceFacadeService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdvogadoCockpitControllerTest {

    private AdvogadoSurfaceFacadeService facadeService;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        facadeService = mock(AdvogadoSurfaceFacadeService.class);
        CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
        objectMapper = new ObjectMapper();
        AdvogadoCockpitController controller = new AdvogadoCockpitController(facadeService, rateLimiter);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void interporRecurso_expoeHeadersDeDepreciacaoDaSuperficieLegada() throws Exception {
        when(facadeService.interporRecurso(anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(new AdvogadoOperacaoResponse("RECURSO_INTERPOSTO", "APELACAO", 7L, null, null, null, null, null, null));

        String body = objectMapper.writeValueAsString(new AdvogadoRecursoRequest(
                "APELACAO", "razoes recursais", "fundamentacao", true, false, "obs"));

        mockMvc.perform(post("/api/v1/advogado/cockpit/processos/{processoId}/recurso", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Sunset", "Tue, 28 Oct 2026 00:00:00 GMT"))
                .andExpect(header().string("Link", "</api/v1/recursal/processos/7/recurso>; rel=\"successor-version\""));
    }

    @Test
    void analiticoPorCliente_naoGanhaHeadersDeDepreciacao() throws Exception {
        when(facadeService.analiticoPorCliente(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/advogado/cockpit/clientes/analitico").param("clienteCpfCnpj", "12345678900"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"))
                .andExpect(header().doesNotExist("Sunset"))
                .andExpect(header().doesNotExist("Link"));
    }
}
