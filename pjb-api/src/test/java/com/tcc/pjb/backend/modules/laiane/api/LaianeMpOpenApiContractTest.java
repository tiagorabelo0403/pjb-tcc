package com.tcc.pjb.backend.modules.laiane.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.modules.laiane.service.LaianeMpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.tcc.pjb.backend.configs.EquipeSwitchInterceptor;
import com.tcc.pjb.backend.configs.api.ApiExceptionHandler;
import com.tcc.pjb.backend.configs.api.OpenApiConfig;
import com.tcc.pjb.backend.configs.api.PdfWatermarkAdvice;
import org.springframework.context.annotation.Import;

@Import({
        org.springdoc.core.configuration.SpringDocConfiguration.class,
        org.springdoc.core.properties.SpringDocConfigProperties.class,
        org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration.class,
        OpenApiConfig.class
})
@WebMvcTest(
        controllers = LaianeMpController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        ApiExceptionHandler.class,
                        PdfWatermarkAdvice.class,
                        EquipeSwitchInterceptor.class
                }
        ),
        properties = {
                "spring.task.scheduling.enabled=false"
        }
)
@AutoConfigureMockMvc(addFilters = false)
class LaianeMpOpenApiContractTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    LaianeMpService laianeMpService;

    @Test
    void schemaPublicoNaoExpoePkNemAdditionalProp() throws Exception {
        MvcResult res = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode spec = objectMapper.readTree(res.getResponse().getContentAsString());

        JsonNode schemas = spec.path("components").path("schemas");

        String schemaName = null;
        var it = schemas.fieldNames();
        while (it.hasNext()) {
            String name = it.next();
            if (name.contains("LaianeMpOficioResponse")) {
                schemaName = name;
                break;
            }
        }

        assertTrue(schemaName != null,
                "LaianeMpOficioResponse não encontrado. Schemas: " + iteratorToString(schemas.fieldNames()));

        JsonNode props = schemas.path(schemaName).path("properties");

        assertTrue(props.has("trackingCode"), "trackingCode ausente no schema");
        assertEquals("uuid", props.path("trackingCode").path("format").asText(), "trackingCode sem format uuid");

        assertFalse(props.has("id"), "id exposto no schema público");
        assertFalse(props.has("origemId"), "origemId exposto no schema público");
        assertFalse(props.has("destinoId"), "destinoId exposto no schema público");

        assertFalse(hasAnyKey(spec, "additionalProp1", "additionalProp2", "additionalProp3"),
                "additionalProp encontrado no schema");
    }

    private boolean hasAnyKey(JsonNode root, String... keys) {
        if (root == null || root.isMissingNode()) return false;
        for (String key : keys) {
            if (root.has(key)) return true;
        }
        if (root.isObject() || root.isArray()) {
            for (JsonNode child : root) {
                if (hasAnyKey(child, keys)) return true;
            }
        }
        return false;
    }

    private String iteratorToString(java.util.Iterator<String> it) {
        StringBuilder sb = new StringBuilder("[");
        while (it.hasNext()) {
            if (sb.length() > 1) sb.append(", ");
            sb.append(it.next());
        }
        return sb.append("]").toString();
    }
}
