package com.tcc.pjb.backend.controller.ui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tcc.pjb.backend.core.frontend.delivery.application.PjbFrontendDeliveryApplicationService;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendBootstrapView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliveryBlockerView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDeliverySummary;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendDomainView;
import com.tcc.pjb.backend.core.frontend.delivery.domain.PjbFrontendRouteView;
import com.tcc.pjb.backend.service.api.ApiResponseFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class FrontendDeliveryControllerTest {

    private PjbFrontendDeliveryApplicationService applicationService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        applicationService = mock(PjbFrontendDeliveryApplicationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new FrontendDeliveryController(applicationService, new ApiResponseFactory())).build();
    }

    @Test
    void summary_deveExporResumoParaFrontend() throws Exception {
        when(applicationService.summary()).thenReturn(new PjbFrontendDeliverySummary(
                false,
                true,
                true,
                false,
                120,
                40,
                20,
                80,
                50,
                18,
                4,
                Instant.parse("2026-04-12T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/frontend/delivery/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRoutes").value(120))
                .andExpect(jsonPath("$.data.readyForFrontend").value(false));
    }

    @Test
    void routes_deveExporCatalogoDeRotas() throws Exception {
        when(applicationService.routes()).thenReturn(List.of(
                new PjbFrontendRouteView("GET", "/api/v1/ui/alpha/catalog", "UiAlphaController", "com.tcc.pjb.backend.controller.ui", "ui", false, true)
        ));

        mockMvc.perform(get("/api/v1/frontend/delivery/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].path").value("/api/v1/ui/alpha/catalog"));
    }

    @Test
    void blockers_deveExporBloqueadores() throws Exception {
        when(applicationService.blockers()).thenReturn(List.of(
                new PjbFrontendDeliveryBlockerView("roadmap", "ALTO", "roadmap.pending", "macroblocos ainda parciais")
        ));

        mockMvc.perform(get("/api/v1/frontend/delivery/blockers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].scope").value("roadmap"));
    }

    @Test
    void bootstrap_deveExporPacoteInicialParaConsumoDoFrontend() throws Exception {
        when(applicationService.bootstrap()).thenReturn(new PjbFrontendBootstrapView(
                new PjbFrontendDeliverySummary(false, true, true, false, 120, 40, 20, 80, 50, 18, 4, Instant.parse("2026-04-12T10:00:00Z")),
                List.of(new PjbFrontendDomainView("ui", 10, 3, false, List.of("/api/v1/ui/alpha/catalog"))),
                List.of(new PjbFrontendRouteView("GET", "/api/v1/ui/alpha/catalog", "UiAlphaController", "com.tcc.pjb.backend.controller.ui", "ui", false, true)),
                List.of(new PjbFrontendDeliveryBlockerView("roadmap", "ALTO", "roadmap.pending", "macroblocos ainda parciais")),
                List.of("Consumir summary"),
                Instant.parse("2026-04-12T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/frontend/delivery/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.domains[0].domain").value("ui"))
                .andExpect(jsonPath("$.data.priorityRoutes[0].path").value("/api/v1/ui/alpha/catalog"));
    }
}
