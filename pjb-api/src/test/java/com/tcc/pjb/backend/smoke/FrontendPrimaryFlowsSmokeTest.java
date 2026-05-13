package com.tcc.pjb.backend.smoke;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcc.pjb.backend.controller.TimelineController;
import com.tcc.pjb.backend.controller.admin.AdminCustasController;
import com.tcc.pjb.backend.controller.admin.AdminDjeController;
import com.tcc.pjb.backend.controller.admin.AdminFrontendReadinessController;
import com.tcc.pjb.backend.controller.auth.PasskeyAuthController;
import com.tcc.pjb.backend.controller.processual.peticionamento.PeticionamentoController;
import com.tcc.pjb.backend.controller.publico.ConsultasPublicasController;
import com.tcc.pjb.backend.core.dje.DjeApplicationService;
import com.tcc.pjb.backend.core.financeiro.custas.CustasApplicationService;
import com.tcc.pjb.backend.core.frontend.readiness.application.PjbBackendReadyForFrontendApplicationService;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaSearchConfigDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaSearchResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceAccessibilityDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceDatasetDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceRoutesDto;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoResponse;
import com.tcc.pjb.backend.model.dto.security.WebAuthnAuthenticationResponse;
import com.tcc.pjb.backend.model.dto.security.WebAuthnChallengeResponse;
import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDecision;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import com.tcc.pjb.backend.service.auth.surface.WebAuthnSurfaceFacadeService;
import com.tcc.pjb.backend.service.consultapublica.ConsultaPublicaSearchService;
import com.tcc.pjb.backend.service.consultapublica.ConsultaPublicaWorkspaceService;
import com.tcc.pjb.backend.service.processual.peticionamento.journey.PeticionamentoJourneyIntelligenceService;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoSessaoFacadeService;
import com.tcc.pjb.backend.service.processual.peticionamento.journey.PeticionamentoSimpleProtocolWizardService;
import com.tcc.pjb.backend.service.processual.peticionamento.studio.PeticionamentoStudioWorkspaceService;
import com.tcc.pjb.backend.service.timeline.surface.TimelineSurfaceFacadeService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.math.BigDecimal;
import com.tcc.pjb.backend.core.frontend.readiness.domain.PjbBackendReadyForFrontendSummary;
import com.tcc.pjb.backend.core.dje.domain.DjePublicationMetricsView;
import com.tcc.pjb.backend.core.financeiro.custas.domain.CustaConsultaResult;

class FrontendPrimaryFlowsSmokeTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void deveExecutarFluxoPrimarioDeLoginPasskeyNaSurfaceHttp() throws Exception {
        WebAuthnSurfaceFacadeService facade = mock(WebAuthnSurfaceFacadeService.class);
        when(facade.startPasskey(any())).thenReturn(new WebAuthnChallengeResponse(11L, "{\"challenge\":\"abc\"}"));
        when(facade.finishPasskey(any(), any())).thenReturn(new WebAuthnAuthenticationResponse("token-1", LocalDateTime.of(2026, 4, 16, 15, 0), 7L));

        MockMvc mvc = standalone(new PasskeyAuthController(facade));

        mvc.perform(post("/api/v1/auth/passkey/options")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("email", "tiago@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value(11L))
                .andExpect(jsonPath("$.optionsJson").value("{\"challenge\":\"abc\"}"));

        mvc.perform(post("/api/v1/auth/passkey/finish")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("sessionId", 11L, "credentialJson", "{}"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-1"))
                .andExpect(jsonPath("$.deviceId").value(7L));
    }

    @Test
    void deveExecutarFluxoPrimarioDeConsultaPublicaNaSurfaceHttp() throws Exception {
        ConsultaPublicaSearchService searchService = mock(ConsultaPublicaSearchService.class);
        ConsultaPublicaWorkspaceService workspaceService = mock(ConsultaPublicaWorkspaceService.class);
        CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
        when(rateLimiter.enforce(any(), any(), anyString(), any())).thenReturn(new CapabilityRateLimitDecision(true, 100L, 99L, 0L, 60, 1));
        when(workspaceService.workspace()).thenReturn(new ConsultaPublicaWorkspaceResponse(
                "etag-workspace",
                LocalDateTime.of(2026, 4, 16, 15, 0),
                "PUBLIC",
                "Consulta pública",
                "Workspace cidadão",
                new ConsultaPublicaSearchConfigDto(3, 20, 50, List.of(), List.of(), List.of("q"), "buscar processo", "relevancia"),
                new ConsultaPublicaWorkspaceRoutesDto(
                        "/api/v1/public/consultas-publicas/workspace",
                        "/api/v1/public/consultas-publicas/search",
                        "/api/v1/public/consultas-publicas/processos/{numero}",
                        null, null, null, null,
                        "/api/v1/public/consultas-publicas/pages/{pageId}",
                        null, null, null, null, null, null, null, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, null, null, null, null, null),
                new ConsultaPublicaWorkspaceAccessibilityDto("WCAG 2.2", List.of("perceptivel"), List.of("contraste"), null, null, null),
                new ConsultaPublicaWorkspaceDatasetDto(true, 2, 15, 25, 30, false, false, true, false, true, "JUDICIAL_PUBLIC_DATA"),
                List.of(),
                List.of(),
                List.of(),
                null,
                List.of(),
                List.of("autos sigilosos continuam restritos")
        ));
        when(searchService.searchPublic(anyString(), any(), any(), anyInt(), anyInt()))
                .thenReturn(ConsultaPublicaSearchResponse.builder().query("123").page(0).size(20).total(1).hits(List.of()).build());

        MockMvc mvc = standalone(new ConsultasPublicasController(searchService, workspaceService, rateLimiter));

        mvc.perform(get("/api/v1/public/consultas-publicas/workspace"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", "Authorization"))
                .andExpect(header().string("ETag", "\"etag-workspace\""))
                .andExpect(jsonPath("$.etag").value("etag-workspace"))
                .andExpect(jsonPath("$.headline").value("Consulta pública"));

        mvc.perform(get("/api/v1/public/consultas-publicas/search").param("q", "123"))
                .andExpect(status().isOk())
                .andExpect(header().string("Vary", "Authorization"))
                .andExpect(jsonPath("$.query").value("123"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void deveExecutarFluxoPrimarioDeTimelineNaSurfaceHttp() throws Exception {
        TimelineSurfaceFacadeService timelineSurfaceFacadeService = mock(TimelineSurfaceFacadeService.class);
        when(timelineSurfaceFacadeService.timeline(99L)).thenReturn(List.of(
                new TimelineItemResponse(1L, Instant.parse("2026-04-16T18:00:00Z"), "DISTRIBUICAO", "CONCLUSAO", "movimento", 7L, "Servidor", false, false, 0, 0, "NO_PRAZO", 0, null, null, false, null)
        ));

        MockMvc mvc = standalone(new TimelineController(timelineSurfaceFacadeService));

        mvc.perform(get("/api/v1/timeline/processo/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].faseDe").value("DISTRIBUICAO"));
    }

    @Test
    void deveExecutarFluxoPrimarioDePeticionamentoNaSurfaceHttp() throws Exception {
        PeticionamentoSessaoFacadeService facadeService = mock(PeticionamentoSessaoFacadeService.class);
        LaianePeticaoInicialDraftService draftService = mock(LaianePeticaoInicialDraftService.class);
        PeticionamentoStudioWorkspaceService studioWorkspaceService = mock(PeticionamentoStudioWorkspaceService.class);
        PeticionamentoSimpleProtocolWizardService simpleProtocolWizardService = mock(PeticionamentoSimpleProtocolWizardService.class);
        PeticionamentoJourneyIntelligenceService journeyIntelligenceService = mock(PeticionamentoJourneyIntelligenceService.class);
        CapabilityRateLimiter rateLimiter = mock(CapabilityRateLimiter.class);
        when(rateLimiter.enforce(any(), any(), anyString(), any())).thenReturn(new CapabilityRateLimitDecision(true, 100L, 99L, 0L, 60, 1));
        when(facadeService.abrirSessaoInicial(any())).thenReturn(PeticionamentoSessaoResponse.builder()
                .modoSolicitado("ASSISTIDO")
                .modoResolvido("ASSISTIDO")
                .status("ABERTA")
                .sessionKey("sess-001")
                .workspace(new LinkedHashMap<>())
                .build());

        MockMvc mvc = standalone(new PeticionamentoController(
                facadeService,
                draftService,
                studioWorkspaceService,
                simpleProtocolWizardService,
                journeyIntelligenceService,
                rateLimiter
        ));

        mvc.perform(post("/api/v1/peticionamento/inicial/sessao")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("modo", "ASSISTIDO", "tituloCaso", "Caso de teste", "parteAutora", "Autor", "parteRe", "Réu"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.sessionKey").value("sess-001"));
    }

    @Test
    void deveExecutarFluxoPrimarioDeCustasNaSurfaceHttp() throws Exception {
        CustasApplicationService applicationService = mock(CustasApplicationService.class);
        when(applicationService.consulta(10L)).thenReturn(new CustaConsultaResult(10L, "JUDICIAL", BigDecimal.TEN, "EM_ABERTO", LocalDate.now().plusDays(5), null, null));

        MockMvc mvc = standalone(new AdminCustasController(applicationService, new ApiResponseFactory()));

        mvc.perform(get("/api/v1/admin/custas/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.status").value("EM_ABERTO"));
    }

    @Test
    void deveExecutarFluxoPrimarioDeDjeNaSurfaceHttp() throws Exception {
        DjeApplicationService applicationService = mock(DjeApplicationService.class);
        when(applicationService.metrics()).thenReturn(new DjePublicationMetricsView(3, 4, 12, 0));

        MockMvc mvc = standalone(new AdminDjeController(applicationService, new ApiResponseFactory()));

        mvc.perform(get("/api/v1/admin/dje/metrics/publication"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.published").value(12))
                .andExpect(jsonPath("$.data.failed").value(0));
    }

    @Test
    void deveExecutarFluxoPrimarioDeAdminReadinessNaSurfaceHttp() throws Exception {
        PjbBackendReadyForFrontendApplicationService applicationService = mock(PjbBackendReadyForFrontendApplicationService.class);
        when(applicationService.summary()).thenReturn(new PjbBackendReadyForFrontendSummary(true, true, true, true, true, true, true, true, 10, 5, 3, 0, Instant.now()));

        MockMvc mvc = standalone(new AdminFrontendReadinessController(applicationService, new ApiResponseFactory()));

        mvc.perform(get("/api/v1/admin/frontend-readiness/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.readyForFrontend").value(true))
                .andExpect(jsonPath("$.data.apiSurfaceClean").value(true));
    }

    private MockMvc standalone(Object controller) {
        return MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }
}
