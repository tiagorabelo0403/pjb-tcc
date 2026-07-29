package com.tcc.pjb.backend.controller.recursal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.profile.operational.InstitutionalRecursoRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDecision;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import com.tcc.pjb.backend.service.processual.recursal.RecursalPeticionamentoPerfilRouter;
import com.tcc.pjb.backend.service.surface.common.SurfaceProjectionSupport;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class RecursalPeticionamentoControllerTest {

    private RecursalPeticionamentoPerfilRouter router;
    private CapabilityRateLimiter rateLimiter;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        router = mock(RecursalPeticionamentoPerfilRouter.class);
        rateLimiter = mock(CapabilityRateLimiter.class);
        objectMapper = new ObjectMapper();
        SurfaceProjectionSupport projectionSupport = new SurfaceProjectionSupport();
        RecursalPeticionamentoController controller = new RecursalPeticionamentoController(router, projectionSupport, rateLimiter);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(rateLimiter.enforce(any(CapabilityRateLimitDomain.class), any(), anyString(), any(ApiVersion.class)))
                .thenReturn(new CapabilityRateLimitDecision(true, 100L, 99L, 0L, 60, 1));
    }

    @Test
    void interporRecurso_advogado_delegaRouterAplicaRateLimitLawyerEEnvelopa() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.ADVOCACIA);
        LinkedHashMap<String, Object> engineResponse = new LinkedHashMap<>();
        engineResponse.put("status", "RECURSO_INTERPOSTO");
        engineResponse.put("tipo", "APELACAO");
        engineResponse.put("perfil", "ADVOCACIA");
        engineResponse.put("workItemId", 42);
        when(router.interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.ADVOCACIA),
                eq(5L), eq("APELACAO"), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(engineResponse);

        String body = objectMapper.writeValueAsString(new InstitutionalRecursoRequest(
                "APELACAO", "razoes recursais", "CPC art. 1009", true, false, "obs"));

        mockMvc.perform(post("/api/v1/recursal/processos/{processoId}/recurso", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.advocacia"))
                .andExpect(jsonPath("$.action").value("interpor-recurso"))
                .andExpect(jsonPath("$.processoId").value(5))
                .andExpect(jsonPath("$.status").value("RECURSO_INTERPOSTO"));

        verify(rateLimiter).enforce(eq(CapabilityRateLimitDomain.LAWYER), any(), eq("recursal_peticionamento_recurso"), eq(ApiVersion.V1));
    }

    @Test
    void interporRecurso_defensor_usaRateLimitInstitucionalEScopeDefensoria() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.DEFENSORIA);
        when(router.interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.DEFENSORIA),
                anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(Map.of("status", "RECURSO_INTERPOSTO"));

        String body = objectMapper.writeValueAsString(new InstitutionalRecursoRequest(
                "APELACAO", "razoes", "fundamentacao", false, true, null));

        mockMvc.perform(post("/api/v1/recursal/processos/{processoId}/recurso", 8L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.defensoria"));

        verify(rateLimiter).enforce(eq(CapabilityRateLimitDomain.INSTITUCIONAL), any(), eq("recursal_peticionamento_recurso"), eq(ApiVersion.V1));
    }

    @Test
    void interporRecurso_ministerioPublico_usaScopeMinisterioPublico() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO);
        when(router.interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.MINISTERIO_PUBLICO),
                anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(Map.of("status", "RECURSO_INTERPOSTO"));

        String body = objectMapper.writeValueAsString(new InstitutionalRecursoRequest(
                "RESP", "razoes", "fundamentacao", true, true, "obs"));

        mockMvc.perform(post("/api/v1/recursal/processos/{processoId}/recurso", 12L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.ministerio-publico"));
    }

    @Test
    void interporRecurso_procuradoria_usaScopeProcuradoria() throws Exception {
        when(router.resolverPerfilAtivo()).thenReturn(RecursalPeticionamentoPerfilRouter.Perfil.PROCURADORIA);
        when(router.interporRecurso(eq(RecursalPeticionamentoPerfilRouter.Perfil.PROCURADORIA),
                anyLong(), anyString(), anyString(), anyString(), anyBoolean(), anyBoolean(), any()))
                .thenReturn(Map.of("status", "RECURSO_INTERPOSTO"));

        String body = objectMapper.writeValueAsString(new InstitutionalRecursoRequest(
                "EMBARGOS_DECLARACAO", "razoes", "fundamentacao", false, false, null));

        mockMvc.perform(post("/api/v1/recursal/processos/{processoId}/recurso", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("recursal.peticionamento.procuradoria"));
    }
}
