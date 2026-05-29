package com.tcc.pjb.backend.modules.laiane.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.modules.laiane.dto.roles.mp.LaianeMpOficioResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class LaianeMpOpenApiSchemaTest {

    private final ObjectMapper objectMapper = Json.mapper();

    @Test
    void swaggerNaoDeveConterAdditionalPropNemIdNuloNemMapAberto() throws Exception {
        ResolvedSchema resolved = ModelConverters.getInstance()
                .readAllAsResolvedSchema(LaianeMpOficioResponse.class);

        var schemas = new LinkedHashMap<>(resolved.referencedSchemas);

        OpenAPI openApi = new OpenAPI()
                .components(new Components()
                        .schemas(schemas)
                        .addSchemas("LaianeMpOficioResponse", resolved.schema));

        JsonNode spec = objectMapper.readTree(objectMapper.writeValueAsString(openApi));

        assertFalse(hasFieldNamed(spec, "additionalProp1"), "additionalProp1 presente no schema");
        assertFalse(hasFieldNamed(spec, "additionalProp2"), "additionalProp2 presente no schema");
        assertFalse(hasFieldNamed(spec, "additionalProp3"), "additionalProp3 presente no schema");

        JsonNode trackingCode = spec
                .path("components")
                .path("schemas")
                .path("LaianeMpOficioResponse")
                .path("properties")
                .path("trackingCode");

        assertEquals("string", trackingCode.path("type").asText());
        assertEquals("uuid", trackingCode.path("format").asText());

        JsonNode props = spec
                .path("components")
                .path("schemas")
                .path("LaianeMpOficioResponse")
                .path("properties");

        assertTrue(props.isObject(), "schema publico deve expor propriedades estruturadas");
        assertFalse(props.has("id"), "propriedade 'id' exposta no schema público");
        assertFalse(props.has("origemId"), "propriedade 'origemId' exposta no schema público");
        assertFalse(props.has("destinoId"), "propriedade 'destinoId' exposta no schema público");
    }

    private boolean hasFieldNamed(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode()) {
            return false;
        }
        if (node.has(fieldName)) {
            return true;
        }
        if (node.isObject() || node.isArray()) {
            for (JsonNode child : node) {
                if (hasFieldNamed(child, fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
