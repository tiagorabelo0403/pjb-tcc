package com.tcc.pjb.backend.controller.processual;

import com.tcc.pjb.backend.BackendApplication;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.profiles.active=test",
                "spring.task.scheduling.enabled=false",
                "spring.main.lazy-initialization=true"
        }
)
@AutoConfigureMockMvc
class ProcessoCompletudeArquiteturalControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "quality@test.local", authorities = "ROLE_ADMIN")
    void sanidadeCodigoExposeCoverageQualityAndLegacyIsolationSignals() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/processual/unificado/sanidade-codigo"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("disponivel").asBoolean());
        assertFalse(hasIssue(json, "quality.coverage.absent"));
        assertFalse(hasIssue(json, "quality.checkstyle.absent"));
        assertFalse(hasIssue(json, "quality.checkstyle.config.absent"));
        assertFalse(hasIssue(json, "legacy.judge.import"));
        assertFalse(hasIssue(json, "legacy.judge.package.expansion"));
    }

    @Test
    @WithMockUser(username = "quality@test.local", authorities = "ROLE_ADMIN")
    void sanidadeCodigoComRefreshExplicitoDeveResponderNaMalhaIntegrada() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/processual/unificado/sanidade-codigo").param("refresh", "true"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("disponivel").asBoolean());
        assertTrue(json.get("issues").isArray());
    }

    @Test
    @WithMockUser(username = "quality@test.local", authorities = "ROLE_ADMIN")
    void sanidadeAprendizadoDeveExporHotspotsETrilhasDeExtracao() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/processual/unificado/sanidade-aprendizado"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("disponivel").asBoolean());
        assertTrue(json.get("testesIntegracao").asInt() > 0);
        assertTrue(json.get("hotspots").isArray());
        assertTrue(json.get("hotspots").size() > 0);
        assertTrue(json.get("hotspots").get(0).get("trilhasExtracao").isArray());
        assertTrue(json.get("blueprintsExtracao").isArray());
        assertTrue(json.get("blueprintsExtracao").size() > 0);
        assertTrue(json.get("fluxosCriticos").isArray());
        assertTrue(json.get("fluxosCriticos").size() > 0);
    }

    @Test
    @WithMockUser(username = "quality@test.local", authorities = "ROLE_ADMIN")
    void sanidadeAprendizadoComRefreshExplicitoDeveResponderNaMalhaIntegrada() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/processual/unificado/sanidade-aprendizado").param("refresh", "true"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("disponivel").asBoolean());
        assertTrue(json.get("fluxosCriticos").isArray());
    }

    @Test
    @WithMockUser(username = "quality@test.local", authorities = "ROLE_ADMIN")
    void sanidadeApiDeveResponderNaMalhaIntegrada() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/processual/unificado/sanidade-api"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(json.get("raizEncontrada").asBoolean());
    }

    private boolean hasIssue(JsonNode root, String code) {
        JsonNode issues = root.get("issues");
        if (issues == null || !issues.isArray()) {
            return false;
        }
        for (JsonNode issue : issues) {
            if (code.equals(issue.path("codigo").asText())) {
                return true;
            }
        }
        return false;
    }
}
