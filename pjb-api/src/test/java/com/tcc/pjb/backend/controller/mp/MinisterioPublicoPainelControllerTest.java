package com.tcc.pjb.backend.controller.mp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.dashboard.PerfilDashboardPayload;
import com.tcc.pjb.backend.model.dto.profile.operational.MinisterioPublicoParecerRequest;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceActionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceCollectionResponse;
import com.tcc.pjb.backend.model.dto.surface.common.SurfaceSnapshotResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.mp.MinisterioPublicoPainelService;
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

class MinisterioPublicoPainelControllerTest {

    private MinisterioPublicoPainelService service;
    private InstitutionalPainelSurfaceFacadeService facadeService;
    private PainelSharedExperienceService sharedExperienceService;
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        service = mock(MinisterioPublicoPainelService.class);
        facadeService = mock(InstitutionalPainelSurfaceFacadeService.class);
        CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
        sharedExperienceService = mock(PainelSharedExperienceService.class);
        objectMapper = new ObjectMapper();
        MinisterioPublicoPainelController controller = new MinisterioPublicoPainelController(
                service, facadeService, rateLimiter, sharedExperienceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private static PerfilDashboardPayload.MinisterioPublicoPayload minimalPayload(String etag) {
        return new PerfilDashboardPayload.MinisterioPublicoPayload(
                etag, LocalDateTime.now(), "MEMBRO_MINISTERIO_PUBLICO", "Dr.", List.of(),
                List.of(), null, null, null, null,
                List.of(), null, "Promotoria Teste", "civel", 0,
                0, 0, List.of(), List.of(), false,
                Map.of(), Map.of(), Map.of(), Map.of(), Map.of(),
                Map.of(), Map.of(), Map.of());
    }

    @Test
    void manifestacoesPendentes_retornaColecaoDoFacade() throws Exception {
        when(facadeService.ministerioPublicoManifestacoesPendentes()).thenReturn(new SurfaceCollectionResponse("mp.manifestacoes-pendentes", List.of()));

        mockMvc.perform(get("/api/v1/mp/manifestacoes/pendentes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("mp.manifestacoes-pendentes"));
    }

    @Test
    void painel_retornaPayloadDoService() throws Exception {
        when(service.bootstrapPainel()).thenReturn(minimalPayload("etag-mp-1"));

        mockMvc.perform(get("/api/v1/mp/painel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfilAtivo").value("MEMBRO_MINISTERIO_PUBLICO"));
    }

    @Test
    void painel_ifNoneMatchIgualAoEtagRetorna304() throws Exception {
        when(service.bootstrapPainel()).thenReturn(minimalPayload("etag-mp-1"));

        mockMvc.perform(get("/api/v1/mp/painel").header("If-None-Match", "etag-mp-1"))
                .andExpect(status().isNotModified());
    }

    @Test
    void malhaProcesso_retornaSnapshotDoFacade() throws Exception {
        when(facadeService.ministerioPublicoMalhaProcesso(anyLong()))
                .thenReturn(new SurfaceSnapshotResponse("mp.malha-processo", List.of()));

        mockMvc.perform(get("/api/v1/mp/processos/{processoId}/malha", 11L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("mp.malha-processo"));
    }

    @Test
    void registrarManifestacao_retorna201ComActionDoFacade() throws Exception {
        when(facadeService.ministerioPublicoRegistrarManifestacao(anyLong(), any()))
                .thenReturn(new SurfaceActionResponse("mp.manifestacao", "registrar-manifestacao", 11L, "MANIFESTACAO_REGISTRADA", null));

        mockMvc.perform(post("/api/v1/mp/manifestacao/{processoId}", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("MANIFESTACAO_REGISTRADA"));
    }

    @Test
    void registrarParecer_retorna201ComActionDoFacade() throws Exception {
        when(facadeService.ministerioPublicoRegistrarParecer(anyLong(), any()))
                .thenReturn(new SurfaceActionResponse("mp.parecer", "registrar-parecer", 11L, "PARECER_REGISTRADO", null));

        String body = objectMapper.writeValueAsString(new MinisterioPublicoParecerRequest(
                "parecer favorável", "fundamentacao", null, null, null, null, null, null, null));

        mockMvc.perform(post("/api/v1/mp/parecer/{processoId}", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARECER_REGISTRADO"));
    }

    @Test
    void registrarParecer_camposEmBrancoRetorna400() throws Exception {
        String body = objectMapper.writeValueAsString(new MinisterioPublicoParecerRequest(
                "", "", null, null, null, null, null, null, null));

        mockMvc.perform(post("/api/v1/mp/parecer/{processoId}", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inqueritosAcompanhamento_retornaColecaoDoFacade() throws Exception {
        when(facadeService.ministerioPublicoInqueritosAcompanhamento())
                .thenReturn(new SurfaceCollectionResponse("mp.inqueritos-acompanhamento", List.of()));

        mockMvc.perform(get("/api/v1/mp/inqueritos/acompanhamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("mp.inqueritos-acompanhamento"));
    }

    @Test
    void requisitarDiligencia_retorna201ComActionDoFacade() throws Exception {
        when(facadeService.ministerioPublicoRequisitarDiligencia(anyLong(), any()))
                .thenReturn(new SurfaceActionResponse("mp.diligencia", "requisitar-diligencia", 11L, "DILIGENCIA_REQUISITADA", null));

        mockMvc.perform(post("/api/v1/mp/requisicao/diligencia/{processoId}", 11L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DILIGENCIA_REQUISITADA"));
    }

    @Test
    void prazosCriticos_retornaColecaoDoFacade() throws Exception {
        when(facadeService.ministerioPublicoPrazosCriticos()).thenReturn(new SurfaceCollectionResponse("mp.prazos-criticos", List.of()));

        mockMvc.perform(get("/api/v1/mp/prazos/criticos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scope").value("mp.prazos-criticos"));
    }

    @Test
    void experienciaCompartilhada_retornaSnapshotDoServico() throws Exception {
        when(sharedExperienceService.snapshot("MINISTERIO_PUBLICO")).thenReturn(Map.of("tema", "padrao"));

        mockMvc.perform(get("/api/v1/mp/painel/experiencia-compartilhada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tema").value("padrao"));
    }
}
