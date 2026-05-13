package com.tcc.pjb.backend.controller.intelligence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CompetenceControllerWebContractTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mockMvc;
    private CompetenceResolverService resolverService;

    @BeforeEach
    void setUp() {
        resolverService = mock(CompetenceResolverService.class);
        MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine = mock(MapaCompetenciaDinamicoEngine.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CompetenceController(resolverService, mapaCompetenciaDinamicoEngine)).build();
    }

    @Test
    void resolve_devePreservarShapeDoContratoHttp() throws Exception {
        when(resolverService.resolve(any())).thenReturn(new CompetenceResolveResponse(
                "req-123",
                Instant.parse("2026-04-11T12:00:00Z"),
                "ESTADUAL",
                "PROCEDIMENTO_COMUM",
                0.91,
                List.of("Contexto procedural consolidado"),
                List.of("CF/88, art. 109"),
                Map.of("source", "controller-test")
        ));

        CompetenceResolveRequest request = new CompetenceResolveRequest(
                "ação de obrigação de fazer com pedido de tutela",
                "saúde",
                "procedimento comum",
                "cível",
                "SP",
                "São Paulo",
                BigDecimal.valueOf(1000),
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        mockMvc.perform(post("/api/v1/intelligence/competencia/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("req-123"))
                .andExpect(jsonPath("$.generatedAt").value("2026-04-11T12:00:00Z"))
                .andExpect(jsonPath("$.tipoJusticaSugerida").value("ESTADUAL"))
                .andExpect(jsonPath("$.ritoSugerido").value("PROCEDIMENTO_COMUM"))
                .andExpect(jsonPath("$.confidence").value(0.91))
                .andExpect(jsonPath("$.reasons[0]").value("Contexto procedural consolidado"))
                .andExpect(jsonPath("$.legalBases[0]").value("CF/88, art. 109"))
                .andExpect(jsonPath("$.debug.source").value("controller-test"));
    }
}
