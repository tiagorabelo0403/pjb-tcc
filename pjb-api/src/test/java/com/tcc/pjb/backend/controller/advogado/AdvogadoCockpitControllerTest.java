package com.tcc.pjb.backend.controller.advogado;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoClienteAnaliticoItemResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoCockpitSnapshotResponse;
import com.tcc.pjb.backend.model.dto.advogado.surface.AdvogadoOperacaoResponse;
import com.tcc.pjb.backend.model.dto.profile.operational.AdvogadoCienciaLoteRequest;
import com.tcc.pjb.backend.model.dto.profile.operational.AdvogadoPeticaoRequest;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.advogado.surface.AdvogadoSurfaceFacadeService;
import java.time.LocalDateTime;
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
    void analiticoPorCliente_retornaItensDoFacade() throws Exception {
        when(facadeService.analiticoPorCliente("12345678900")).thenReturn(List.of(
                new AdvogadoClienteAnaliticoItemResponse(9L, "0001-00.2026.8.06.0001", "instrucao", "COMUM_ORDINARIO", "TJCE")));

        mockMvc.perform(get("/api/v1/advogado/cockpit/clientes/analitico").param("clienteCpfCnpj", "12345678900"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].processoId").value(9))
                .andExpect(jsonPath("$[0].numero").value("0001-00.2026.8.06.0001"));
    }

    @Test
    void snapshot_retornaCockpitDoFacade() throws Exception {
        when(facadeService.cockpitSnapshot()).thenReturn(new AdvogadoCockpitSnapshotResponse(
                LocalDateTime.of(2026, 6, 1, 10, 0), "ADVOGADO", "Dr.", "Escritorio Teste", 12L,
                List.of(), List.of(), List.of(), List.of(), 0, 0, List.of(), null));

        mockMvc.perform(get("/api/v1/advogado/cockpit/snapshot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilAtivo").value("ADVOGADO"))
                .andExpect(jsonPath("$.carteiraTotalProcessos").value(12));
    }

    @Test
    void protocolizarPeticao_retorna201ComOperacaoDoFacade() throws Exception {
        when(facadeService.protocolizarPeticao(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(new AdvogadoOperacaoResponse("PETICAO_PROTOCOLIZADA", "PETICAO_SIMPLES", 7L, null, null, null, null, null, null));

        String body = objectMapper.writeValueAsString(new AdvogadoPeticaoRequest("PETICAO_SIMPLES", "conteudo da peticao", "fundamentacao"));

        mockMvc.perform(post("/api/v1/advogado/cockpit/processos/{processoId}/peticao", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PETICAO_PROTOCOLIZADA"));
    }

    @Test
    void protocolizarPeticao_camposEmBrancoRetorna400() throws Exception {
        String body = objectMapper.writeValueAsString(new AdvogadoPeticaoRequest("", "", ""));

        mockMvc.perform(post("/api/v1/advogado/cockpit/processos/{processoId}/peticao", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void darCienciaEmLote_retornaOperacaoDoFacade() throws Exception {
        when(facadeService.darCienciaEmLote(any())).thenReturn(new AdvogadoOperacaoResponse(
                "CIENCIA_REGISTRADA", null, null, null, null, 2, 2, List.of(1L, 2L), null));

        String body = objectMapper.writeValueAsString(new AdvogadoCienciaLoteRequest(List.of(1L, 2L)));

        mockMvc.perform(post("/api/v1/advogado/cockpit/intimacoes/ciencia-lote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CIENCIA_REGISTRADA"));
    }

    @Test
    void darCienciaEmLote_listaVaziaRetorna400() throws Exception {
        String body = objectMapper.writeValueAsString(new AdvogadoCienciaLoteRequest(List.of()));

        mockMvc.perform(post("/api/v1/advogado/cockpit/intimacoes/ciencia-lote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

}
