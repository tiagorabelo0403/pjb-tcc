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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.profile.operational.InstitutionalRecursoRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.defensor.DefensorPublicoPainelService;
import com.tcc.pjb.backend.service.painel.shared.PainelSharedExperienceService;
import com.tcc.pjb.backend.service.painel.surface.InstitutionalPainelSurfaceFacadeService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DefensorPublicoPainelControllerTest {

    private DefensorPublicoPainelService service;
    private InstitutionalPainelSurfaceFacadeService facadeService;
    private PainelSharedExperienceService sharedExperienceService;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(DefensorPublicoPainelService.class);
        facadeService = mock(InstitutionalPainelSurfaceFacadeService.class);
        CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
        sharedExperienceService = mock(PainelSharedExperienceService.class);
        objectMapper = new ObjectMapper();
        DefensorPublicoPainelController controller = new DefensorPublicoPainelController(
                service, facadeService, rateLimiter, sharedExperienceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static PerfilDashboardPayload.DefensorPublicoPayload minimalPayload(String etag) {
        return new PerfilDashboardPayload.DefensorPublicoPayload(
                etag, LocalDateTime.now(), "DEFENSOR_PUBLICO", "Dr.", List.of(),
                List.of(), null, null, null, null,
                List.of(), null, "Nucleo Teste", false, 0,
                0, 0, 0, 0, List.of(),
                false, Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of(), Map.of());
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

    @Test
    void interporRecurso_camposEmBrancoRetorna400() throws Exception {
        String body = objectMapper.writeValueAsString(new InstitutionalRecursoRequest("", "", "", false, false, null));

        mockMvc.perform(post("/api/v1/defensor/recurso/{processoId}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void painel_retornaPayloadDoService() throws Exception {
        when(service.bootstrapPainel()).thenReturn(minimalPayload("etag-1"));

        mockMvc.perform(get("/api/v1/defensor/painel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilAtivo").value("DEFENSOR_PUBLICO"));
    }

    @Test
    void painel_ifNoneMatchIgualAoEtagRetorna304() throws Exception {
        when(service.bootstrapPainel()).thenReturn(minimalPayload("etag-1"));

        mockMvc.perform(get("/api/v1/defensor/painel").header("If-None-Match", "etag-1"))
                .andExpect(status().isNotModified());
    }

    @Test
    void processosAssistido_retornaSnapshotDoFacade() throws Exception {
        when(facadeService.defensorProcessosAssistido(anyLong()))
                .thenReturn(new SurfaceSnapshotResponse("defensoria.processos-assistido", List.of()));

        mockMvc.perform(get("/api/v1/defensor/assistidos/{assistidoId}/processos", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("defensoria.processos-assistido"));
    }

    @Test
    void registrarPeticao_retorna201ComActionDoFacade() throws Exception {
        when(facadeService.defensorRegistrarPeticao(anyLong(), any()))
                .thenReturn(new SurfaceActionResponse("defensoria.peticao", "registrar-peticao", 9L, "PETICAO_REGISTRADA", null));

        mockMvc.perform(post("/api/v1/defensor/peticao/{processoId}", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PETICAO_REGISTRADA"));
    }

    @Test
    void audienciasHoje_retornaColecaoDoFacade() throws Exception {
        when(facadeService.defensorAudienciasHoje()).thenReturn(new SurfaceCollectionResponse("defensoria.audiencias-hoje", List.of()));

        mockMvc.perform(get("/api/v1/defensor/audiencias/hoje"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("defensoria.audiencias-hoje"));
    }

    @Test
    void prazosCriticos_retornaColecaoDoFacade() throws Exception {
        when(facadeService.defensorPrazosCriticos()).thenReturn(new SurfaceCollectionResponse("defensoria.prazos-criticos", List.of()));

        mockMvc.perform(get("/api/v1/defensor/prazos/criticos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("defensoria.prazos-criticos"));
    }

    @Test
    void requerimentoGratuidade_retorna201ComActionDoFacade() throws Exception {
        when(facadeService.defensorRequerimentoGratuidade(anyLong(), any()))
                .thenReturn(new SurfaceActionResponse("defensoria.gratuidade", "registrar-requerimento-gratuidade", 9L, "REQUERIMENTO_REGISTRADO", null));

        mockMvc.perform(post("/api/v1/defensor/gratuidade/{processoId}/requerimento", 9L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REQUERIMENTO_REGISTRADO"));
    }

    @Test
    void experienciaCompartilhada_retornaSnapshotDoServico() throws Exception {
        when(sharedExperienceService.snapshot("DEFENSOR_PUBLICO")).thenReturn(Map.of("tema", "padrao"));

        mockMvc.perform(get("/api/v1/defensor/painel/experiencia-compartilhada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tema").value("padrao"));
    }
}
