package com.tcc.pjb.backend.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.configs.api.OpenApiConfig;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;

class OpenApiContractIT {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void openapi_exposes_basic_security_and_standard_headers() {
        OpenAPI openApi = config.pjbOpenAPI();

        SecurityScheme basicAuth = openApi.getComponents().getSecuritySchemes().get("basicAuth");
        assertTrue(basicAuth != null, "Deve expor security scheme basicAuth");
        assertEquals(SecurityScheme.Type.HTTP, basicAuth.getType());
        assertEquals("basic", basicAuth.getScheme());

        Operation operation = new Operation();
        PathItem pathItem = new PathItem().get(operation);
        Paths paths = new Paths();
        paths.addPathItem("/api/v1/exemplo", pathItem);
        openApi.setPaths(paths);

        OpenApiCustomizer headersCustomizer = config.pjbStandardHeadersCustomizer();
        headersCustomizer.customise(openApi);

        Set<String> headerNames = operation.getParameters().stream()
                .filter(p -> "header".equalsIgnoreCase(p.getIn()))
                .map(Parameter::getName)
                .collect(Collectors.toSet());

        assertTrue(headerNames.contains("X-Request-Id"), "Header X-Request-Id deve existir no contrato");
        assertTrue(headerNames.contains("X-PJB-Justificativa"), "Header X-PJB-Justificativa deve existir no contrato");
    }
}
