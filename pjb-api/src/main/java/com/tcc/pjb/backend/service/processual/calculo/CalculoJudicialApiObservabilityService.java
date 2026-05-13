package com.tcc.pjb.backend.service.processual.calculo;

import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.observability.RequestCorrelationFilter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class CalculoJudicialApiObservabilityService {

    private final CalculoJudicialFrontendContractService frontendContractService;
    private final MeterRegistry meterRegistry;

    public CalculoJudicialApiObservabilityService(CalculoJudicialFrontendContractService frontendContractService,
                                                  MeterRegistry meterRegistry) {
        this.frontendContractService = frontendContractService;
        this.meterRegistry = meterRegistry;
    }

    public CalculoJudicialApiRouteContext canonical(String operation, String dominio) {
        String domain = dominio == null || dominio.isBlank() ? null : CalculoJudicialDomainSupport.requireSupported(dominio);
        return new CalculoJudicialApiRouteContext("calculos", "canonical", normalizeOperation(operation), domain, true, false);
    }

    public CalculoJudicialApiRouteContext legacyTrabalhista() {
        return new CalculoJudicialApiRouteContext("trabalhista_legacy", "compatibility", "legacy_verbas_rescisorias", "TRABALHISTA_CLT", false, true);
    }

    public CalculoJudicialApiRouteContext fromRequest(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            return null;
        }
        if (path.startsWith(CalculoJudicialDomainSupport.basePath())) {
            String operation = operationFromPath(path, request.getMethod());
            String domain = CalculoJudicialDomainSupport.fromPath(path);
            return canonical(operation, domain);
        }
        if (CalculoJudicialDomainSupport.legacyTrabalhistaVerbasRescisoriasRoute().equals(path)) {
            return legacyTrabalhista();
        }
        return null;
    }

    public void apply(HttpHeaders headers, CalculoJudicialApiRouteContext context) {
        if (headers == null || context == null) {
            return;
        }
        headers.set("X-PJB-Api-Family", context.apiFamily());
        headers.set("X-PJB-Api-Route-Status", context.routeStatus());
        headers.set("X-PJB-Api-Operation", context.operation());
        headers.set("X-PJB-Frontend-Ready", Boolean.toString(context.frontendReady()));
        headers.set("X-PJB-Contract-Version", frontendContractService.version());
        headers.set("X-PJB-Contract-Fingerprint", frontendContractService.fingerprint());
        if (context.domain() != null) {
            headers.set("X-PJB-Calculation-Domain", context.domain());
        }
        RequestContext.getRequestId().ifPresent(id -> headers.set(RequestCorrelationFilter.HEADER_REQUEST_ID, id));
        if (context.legacy()) {
            headers.set("Deprecation", "true");
            headers.set("X-PJB-Legacy-Route", "true");
            headers.set("X-PJB-Preferred-Route", CalculoJudicialDomainSupport.jsonRoute("TRABALHISTA_CLT"));
            headers.set("X-PJB-Preferred-Workspace-Route", CalculoJudicialDomainSupport.workspaceRoute("TRABALHISTA_CLT"));
            headers.set(HttpHeaders.LINK, "<" + CalculoJudicialDomainSupport.jsonRoute("TRABALHISTA_CLT") + ">; rel=\"successor-version\"");
        }
        mergeExposeHeaders(headers, context);
    }

    public HttpHeaders problemHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        apply(headers, fromRequest(request));
        return headers;
    }

    public void record(CalculoJudicialApiRouteContext context, String method, int statusCode) {
        if (context == null) {
            return;
        }
        Counter.builder("pjb.calculo.api.requests")
                .tag("family", safe(context.apiFamily()))
                .tag("route_status", safe(context.routeStatus()))
                .tag("operation", safe(context.operation()))
                .tag("domain", safe(context.domain()))
                .tag("method", safe(method))
                .tag("status", Integer.toString(statusCode))
                .register(meterRegistry)
                .increment();
        if (statusCode >= 400) {
            Counter.builder("pjb.calculo.api.errors")
                    .tag("family", safe(context.apiFamily()))
                    .tag("route_status", safe(context.routeStatus()))
                    .tag("operation", safe(context.operation()))
                    .tag("domain", safe(context.domain()))
                    .tag("method", safe(method))
                    .tag("status", Integer.toString(statusCode))
                    .register(meterRegistry)
                    .increment();
        }
    }

    private String operationFromPath(String path, String method) {
        String normalized = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (normalized.endsWith("/tabelas/oficiais")) {
            return "tabelas_oficiais";
        }
        if (normalized.contains("/tabelas/oficiais/")) {
            return "tabelas_oficiais_dominio";
        }
        if (normalized.endsWith("/referencias/economicas")) {
            return "referencias_economicas";
        }
        if (normalized.endsWith("/experiencia/preferencia")) {
            return "experience_preference";
        }
        if (normalized.contains("/catalogo/") && normalized.endsWith("/bootstrap")) {
            return "bootstrap";
        }
        if (normalized.endsWith("/catalogo")) {
            return "catalogo";
        }
        if (normalized.contains("/catalogo/")) {
            return "catalogo_dominio";
        }
        if (normalized.endsWith("/workspace")) {
            return "workspace";
        }
        if (normalized.contains("/workspace/") && normalized.endsWith("/ajuda")) {
            return "workspace_ajuda";
        }
        if (normalized.contains("/workspace/")) {
            return "workspace_dominio";
        }
        if (normalized.contains("/ia/financeira/sinalizar-ajuizamento")) {
            return "ia_financeira_live_signal";
        }
        if (normalized.contains("/ia/financeira/")) {
            return "ia_financeira";
        }
        if (normalized.contains("/assistente/")) {
            return "assistente";
        }
        if (normalized.endsWith("/pdf")) {
            return "pdf";
        }
        if ("POST".equalsIgnoreCase(method)) {
            return "json";
        }
        return "unknown";
    }

    private String normalizeOperation(String operation) {
        if (operation == null || operation.isBlank()) {
            return "unknown";
        }
        return operation.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private void mergeExposeHeaders(HttpHeaders headers, CalculoJudicialApiRouteContext context) {
        Set<String> values = new LinkedHashSet<>();
        headers.getOrEmpty(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS).forEach(raw -> splitExpose(values, raw));
        values.add(RequestCorrelationFilter.HEADER_REQUEST_ID);
        values.add("X-PJB-Api-Family");
        values.add("X-PJB-Api-Route-Status");
        values.add("X-PJB-Api-Operation");
        values.add("X-PJB-Frontend-Ready");
        values.add("X-PJB-Contract-Version");
        values.add("X-PJB-Contract-Fingerprint");
        if (context.domain() != null) {
            values.add("X-PJB-Calculation-Domain");
        }
        if (headers.containsKey("X-PJB-Calculation-File-Name")) {
            values.add("X-PJB-Calculation-File-Name");
        }
        if (headers.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
            values.add(HttpHeaders.CONTENT_DISPOSITION);
        }
        if (headers.containsKey(HttpHeaders.RETRY_AFTER)) {
            values.add(HttpHeaders.RETRY_AFTER);
        }
        if (context.legacy()) {
            values.add("Deprecation");
            values.add("X-PJB-Legacy-Route");
            values.add("X-PJB-Preferred-Route");
            values.add("X-PJB-Preferred-Workspace-Route");
            values.add(HttpHeaders.LINK);
        }
        headers.set(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, String.join(", ", values));
    }

    private void splitExpose(Set<String> values, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        for (String item : raw.split(",")) {
            String normalized = item.trim();
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}
