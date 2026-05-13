package com.tcc.pjb.backend.it;

import com.tcc.pjb.backend.BackendApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(
        classes = BackendApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.profiles.active=test",
                "spring.task.scheduling.enabled=false",
                "spring.main.lazy-initialization=true"
        }
)
class OpenApiContractIT {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    ObjectMapper mapper;

    @Test
    void openapi_exposes_basic_security_and_standard_headers() throws Exception {
        JsonNode root = fetchOpenApi();

        JsonNode schemes = root.path("components").path("securitySchemes");
        assertTrue(schemes.has("basicAuth"), "Deve expor security scheme basicAuth");
        assertEquals("http", schemes.path("basicAuth").path("type").asText());

        Set<String> headers = collectHeaderParameters(root);
        assertTrue(headers.contains("X-Request-Id"), "Header X-Request-Id deve existir no contrato");
        assertTrue(headers.contains("X-PJB-Justificativa"), "Header X-PJB-Justificativa deve existir no contrato");

        maybeUpdateOrEnforceSnapshot(root);
    }

    private JsonNode fetchOpenApi() throws Exception {
        ResponseEntity<String> resp = rest.getForEntity("http://localhost:" + port + "/v3/api-docs", String.class);
        assertEquals(200, resp.getStatusCode().value(), "/v3/api-docs deve responder 200");
        assertNotNull(resp.getBody(), "Body OpenAPI não pode ser nulo");
        return mapper.readTree(resp.getBody());
    }

    private Set<String> collectHeaderParameters(JsonNode root) {
        Set<String> headers = new HashSet<>();
        JsonNode paths = root.path("paths");
        Iterator<String> pathNames = paths.fieldNames();
        while (pathNames.hasNext()) {
            String p = pathNames.next();
            JsonNode pathItem = paths.path(p);
            Iterator<String> methods = pathItem.fieldNames();
            while (methods.hasNext()) {
                String m = methods.next();
                JsonNode op = pathItem.path(m);
                if (!op.isObject()) continue;
                JsonNode params = op.path("parameters");
                if (!params.isArray()) continue;
                for (JsonNode param : params) {
                    if (!"header".equalsIgnoreCase(param.path("in").asText())) continue;
                    String name = param.path("name").asText();
                    if (name != null && !name.isBlank()) headers.add(name);
                }
            }
        }
        return headers;
    }

    private void maybeUpdateOrEnforceSnapshot(JsonNode root) throws Exception {
        boolean update = Boolean.getBoolean("openapi.snapshot.update");
        boolean enforce = Boolean.getBoolean("openapi.snapshot.enforce");

        Path snapshot = Paths.get("src/test/resources/openapi/openapi-snapshot.json");
        Files.createDirectories(snapshot.getParent());

        
        ObjectMapper canonical = mapper.copy();
        canonical.configure(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        String canonicalJson = canonical.writeValueAsString(root);

        if (update) {
            Files.writeString(snapshot, canonicalJson);
            return;
        }

        if (!enforce) {
            return; 
        }

        if (!Files.exists(snapshot)) {
            fail("Snapshot OpenAPI ausente. Rode: mvn -Pit test -Dopenapi.snapshot.update=true e commite o arquivo.");
        }

        String expected = Files.readString(snapshot);
        assertEquals(expected.trim(), canonicalJson.trim(), "Drift detectado no contrato OpenAPI (snapshot)");
    }
}
