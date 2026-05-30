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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
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

    @Test
    void contratoPublicoDeveManterCamposDoBaselineVersionado() throws Exception {
        Path baseline = Path.of("../docs/api/laiane-mp-openapi-v1.json");
        if (!Files.exists(baseline)) {
            return;
        }
        JsonNode baselineNode = new ObjectMapper().readTree(Files.readString(baseline));
        JsonNode fields = baselineNode.path("fields");

        ResolvedSchema resolved = ModelConverters.getInstance()
                .readAllAsResolvedSchema(LaianeMpOficioResponse.class);
        JsonNode props = objectMapper.readTree(objectMapper.writeValueAsString(resolved.schema))
                .path("properties");

        fields.forEach(field -> assertTrue(
                props.has(field.asText()),
                "Campo '" + field.asText() + "' removido do contrato sem versionamento — quebra backward compatibility"
        ));

        JsonNode prohibited = baselineNode.path("prohibited");
        prohibited.forEach(field -> assertFalse(
                props.has(field.asText()),
                "Campo proibido '" + field.asText() + "' exposto no schema público"
        ));
    }

    @Test
    @Tag("spec-export")
    @Disabled("Executar manualmente para atualizar docs/api/laiane-mp-openapi-v1.json: mvnw -Dtest=LaianeMpOpenApiSchemaTest#exportarBaselineVersionado -Dgroups=spec-export test")
    void exportarBaselineVersionado() throws Exception {
        ResolvedSchema resolved = ModelConverters.getInstance()
                .readAllAsResolvedSchema(LaianeMpOficioResponse.class);
        JsonNode schema = objectMapper.readTree(objectMapper.writeValueAsString(resolved.schema));
        JsonNode props = schema.path("properties");

        com.fasterxml.jackson.databind.node.ObjectNode baseline = objectMapper.createObjectNode();
        baseline.put("version", "v1");
        baseline.put("schema", "LaianeMpOficioResponse");
        baseline.put("generated", java.time.LocalDate.now().toString());
        com.fasterxml.jackson.databind.node.ArrayNode fieldArray = baseline.putArray("fields");
        props.fieldNames().forEachRemaining(fieldArray::add);
        com.fasterxml.jackson.databind.node.ObjectNode requiredFormat = baseline.putObject("required_format");
        if (props.has("trackingCode") && props.path("trackingCode").has("format")) {
            requiredFormat.put("trackingCode", props.path("trackingCode").path("format").asText());
        }
        com.fasterxml.jackson.databind.node.ArrayNode prohibited = baseline.putArray("prohibited");
        prohibited.add("id");
        prohibited.add("origemId");
        prohibited.add("destinoId");

        Path out = Path.of("../docs/api/laiane-mp-openapi-v1.json");
        Files.createDirectories(out.getParent());
        Files.writeString(out, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(baseline));
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
