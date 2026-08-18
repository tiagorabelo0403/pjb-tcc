package com.tcc.pjb.backend.controller.procuradoria;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaContestacaoRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaExecucaoFiscalRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.ProcuradoriaParecerRequest;
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
    void snapshot_retornaSnapshotDoFacade() throws Exception {
        when(facadeService.snapshot()).thenReturn(new SurfaceSnapshotResponse("procuradoria.operacional", List.of()));

        mockMvc.perform(get("/api/v1/procuradoria/operacional/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("procuradoria.operacional"));
    }

    @Test
    void malhaProcesso_retornaSnapshotDoFacade() throws Exception {
        when(facadeService.malhaProcesso(anyLong())).thenReturn(new SurfaceSnapshotResponse("procuradoria.operacional.malha", List.of()));

        mockMvc.perform(get("/api/v1/procuradoria/operacional/processos/{processoId}/malha", 15L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("procuradoria.operacional.malha"));
    }

    @Test
    void apresentarContestacao_retorna201ComActionDoFacade() throws Exception {
        when(facadeService.apresentarContestacao(anyLong(), any()))
                .thenReturn(new SurfaceActionResponse("procuradoria.contestacao", "apresentar-contestacao", 15L, "CONTESTACAO_APRESENTADA", null));

        String body = objectMapper.writeValueAsString(new ProcuradoriaContestacaoRequest(
                "texto da contestacao", "fundamentacao", null, null, null, null, null, null, null));

        mockMvc.perform(post("/api/v1/procuradoria/operacional/processos/{processoId}/contestacao", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONTESTACAO_APRESENTADA"));
    }

    @Test
    void apresentarContestacao_camposEmBrancoRetorna400() throws Exception {
        String body = objectMapper.writeValueAsString(new ProcuradoriaContestacaoRequest(
                "", "", null, null, null, null, null, null, null));

        mockMvc.perform(post("/api/v1/procuradoria/operacional/processos/{processoId}/contestacao", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ajuizarExecucaoFiscal_retorna201ComActionDoFacade() throws Exception {
        when(facadeService.ajuizarExecucaoFiscal(anyString(), anyDouble(), anyString(), anyString()))
                .thenReturn(new SurfaceActionResponse("procuradoria.execucao-fiscal", "ajuizar-execucao-fiscal", null, "EXECUCAO_AJUIZADA", null));

        String body = objectMapper.writeValueAsString(new ProcuradoriaExecucaoFiscalRequest(
                "12345678900", 1000.0, "IPTU em atraso", "Fortaleza"));

        mockMvc.perform(post("/api/v1/procuradoria/operacional/execucao-fiscal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("EXECUCAO_AJUIZADA"));
    }

    @Test
    void ajuizarExecucaoFiscal_camposEmBrancoRetorna400() throws Exception {
        String body = objectMapper.writeValueAsString(new ProcuradoriaExecucaoFiscalRequest("", -1.0, "", ""));

        mockMvc.perform(post("/api/v1/procuradoria/operacional/execucao-fiscal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emitirParecer_retorna201ComActionDoFacade() throws Exception {
        when(facadeService.emitirParecer(anyLong(), any()))
                .thenReturn(new SurfaceActionResponse("procuradoria.parecer", "emitir-parecer", 15L, "PARECER_EMITIDO", null));

        String body = objectMapper.writeValueAsString(new ProcuradoriaParecerRequest(
                "parecer tecnico", "fundamentacao", null, null, null, null, null, null, null));

        mockMvc.perform(post("/api/v1/procuradoria/operacional/processos/{processoId}/parecer", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARECER_EMITIDO"));
    }

    @Test
    void emitirParecer_camposEmBrancoRetorna400() throws Exception {
        String body = objectMapper.writeValueAsString(new ProcuradoriaParecerRequest(
                "", "", null, null, null, null, null, null, null));

        mockMvc.perform(post("/api/v1/procuradoria/operacional/processos/{processoId}/parecer", 15L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
