package com.tcc.pjb.backend.core.procedural;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveRequest;
import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

final class CompetenceResolverContractClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final URI baseUri;

    CompetenceResolverContractClient(String baseUrl) {
        this(HttpClient.newHttpClient(), baseUrl, new ObjectMapper().registerModule(new JavaTimeModule()));
    }

    CompetenceResolverContractClient(HttpClient httpClient, String baseUrl, ObjectMapper objectMapper) {
        this.httpClient = Objects.requireNonNull(httpClient);
        this.baseUri = URI.create(Objects.requireNonNull(baseUrl));
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    private String contractPayload(CompetenceResolveRequest request) {
        Objects.requireNonNull(request);
        return "{"
                + "\"textoCaso\":" + json(request.textoCaso()) + ","
                + "\"assunto\":" + json(request.assunto()) + ","
                + "\"classeProcessual\":" + json(request.classeProcessual()) + ","
                + "\"materia\":" + json(request.materia()) + ","
                + "\"uf\":" + json(request.uf()) + ","
                + "\"comarca\":" + json(request.comarca()) + ","
                + "\"valorCausa\":" + valorCausa(request) + ","
                + "\"envolveUniao\":" + request.envolveUniao() + ","
                + "\"envolveAutarquiaFederal\":" + request.envolveAutarquiaFederal() + ","
                + "\"envolveEmpresaPublicaFederal\":" + request.envolveEmpresaPublicaFederal() + ","
                + "\"envolveEstado\":" + request.envolveEstado() + ","
                + "\"envolveMunicipio\":" + request.envolveMunicipio() + ","
                + "\"envolveRelacaoTrabalho\":" + request.envolveRelacaoTrabalho() + ","
                + "\"envolveEleitoral\":" + request.envolveEleitoral() + ","
                + "\"envolveMilitar\":" + request.envolveMilitar()
                + "}";
    }

    private String valorCausa(CompetenceResolveRequest request) {
        return request.valorCausa() == null ? "0.0" : request.valorCausa().stripTrailingZeros().toPlainString() + (request.valorCausa().scale() <= 0 ? ".0" : "");
    }

    private String json(String value) {
        if (value == null) {
            return "\"\"";
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    CompetenceResolveResponse resolve(CompetenceResolveRequest request) {
        try {
            HttpRequest httpRequest = HttpRequest.newBuilder(baseUri.resolve("/api/v1/intelligence/competencia/resolve"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(contractPayload(request), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Competence resolver contract returned status=" + response.statusCode());
            }
            return objectMapper.readValue(response.body(), CompetenceResolveResponse.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while invoking competence contract", e);
        }
    }
}
