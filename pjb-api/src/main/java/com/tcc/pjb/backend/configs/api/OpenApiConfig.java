package com.tcc.pjb.backend.configs.api;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pjbOpenAPI() {
        final String schemeName = "basicAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("PJB Backend")
						.description("API do projeto PJB (Padrão Estado: RBAC/ABAC, auditoria imutável, observabilidade e contratos)")
                        .version("v8.1"))
                .components(new Components()
                        .addSecuritySchemes(schemeName,
                                new SecurityScheme()
                                        .name(schemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName));
    }

    
    @Bean
    public OpenApiCustomizer pjbStandardHeadersCustomizer() {
        Parameter requestId = new Parameter()
                .in("header")
                .name("X-Request-Id")
                .required(false)
                .description("ID de correlação (se ausente, o backend gera)")
                .schema(new StringSchema());

        Parameter justificativa = new Parameter()
                .in("header")
                .name("X-PJB-Justificativa")
                .required(false)
                .description("Justificativa do operador. Obrigatória em endpoints sensíveis (ex.: processos com sigilo).")
                .schema(new StringSchema());

        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().values().forEach(pathItem -> {
                pathItem.readOperations().forEach(op -> {
                    op.addParametersItem(requestId);
                    op.addParametersItem(justificativa);
                });
            });
        };
    }

    @Bean
    public GroupedOpenApi laianeApi() {
        return GroupedOpenApi.builder()
                .group("laiane")
                .pathsToMatch("/api/v1/laiane/**")
                .build();
    }
}
