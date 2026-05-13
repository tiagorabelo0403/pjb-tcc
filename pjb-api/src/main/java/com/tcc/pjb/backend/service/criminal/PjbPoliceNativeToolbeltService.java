package com.tcc.pjb.backend.service.criminal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PjbPoliceNativeToolbeltService {

    private static final TypeReference<Map<String, Map<String, Object>>> CATALOG_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private volatile Map<String, Map<String, Object>> catalog;

    public PjbPoliceNativeToolbeltService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public Map<String, Object> toolCatalog(TipoUsuario tipoUsuario) {
        String actorLane = actorLane(tipoUsuario);
        List<Map<String, Object>> tools = toolsForLane(actorLane);
        LinkedHashSet<String> functions = new LinkedHashSet<>();
        LinkedHashSet<String> workflows = new LinkedHashSet<>();
        LinkedHashSet<String> security = new LinkedHashSet<>();
        LinkedHashSet<String> capabilities = new LinkedHashSet<>();
        for (Map<String, Object> tool : tools) {
            functions.addAll(strings(tool.get("requiredFunctions")));
            workflows.addAll(strings(tool.get("workflowBackbone")));
            security.addAll(strings(tool.get("securityBackbone")));
            capabilities.addAll(strings(tool.get("pjbSovereignCapabilities")));
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "PJB_POLICE_NATIVE_TOOL_CATALOG");
        out.put("actorLane", actorLane);
        out.put("nativeFirst", Boolean.TRUE);
        out.put("toolCount", tools.size());
        out.put("tools", tools);
        out.put("mandatoryFunctionFamilies", List.copyOf(functions));
        out.put("workflowBackbone", List.copyOf(workflows));
        out.put("securityBackbone", List.copyOf(security));
        out.put("pjbSovereignCapabilities", List.copyOf(capabilities));
        out.put("executionOrder", List.of("PJB_NATIVE_FIRST", "PARTNER_ADAPTER_SECOND", "MANUAL_CONTINGENCY_THIRD"));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> detail(String code) {
        String normalized = normalize(code);
        Map<String, Object> item = loadCatalog().get(normalized);
        if (item == null) {
            LinkedHashMap<String, Object> missing = new LinkedHashMap<>();
            missing.put("code", normalized);
            missing.put("found", false);
            missing.put("availableCodes", List.copyOf(loadCatalog().keySet()));
            return Map.copyOf(missing);
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("code", normalized);
        out.put("found", true);
        out.putAll(item);
        out.put("functionCount", strings(item.get("requiredFunctions")).size());
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> nativeWorkbench(TipoUsuario tipoUsuario) {
        String actorLane = actorLane(tipoUsuario);
        List<Map<String, Object>> tools = toolsForLane(actorLane);
        LinkedHashSet<String> functions = new LinkedHashSet<>();
        LinkedHashSet<String> workflows = new LinkedHashSet<>();
        LinkedHashSet<String> security = new LinkedHashSet<>();
        LinkedHashSet<String> evidenceSupport = new LinkedHashSet<>();
        LinkedHashSet<String> sovereign = new LinkedHashSet<>();
        for (Map<String, Object> tool : tools) {
            functions.addAll(strings(tool.get("requiredFunctions")));
            workflows.addAll(strings(tool.get("workflowBackbone")));
            security.addAll(strings(tool.get("securityBackbone")));
            evidenceSupport.addAll(strings(tool.get("evidenceSupport")));
            sovereign.addAll(strings(tool.get("pjbSovereignCapabilities")));
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "PJB_POLICE_NATIVE_WORKBENCH");
        out.put("actorLane", actorLane);
        out.put("nativeFirst", Boolean.TRUE);
        out.put("tools", tools);
        out.put("mandatoryFunctionFamilies", List.copyOf(functions));
        out.put("workflowBackbone", List.copyOf(workflows));
        out.put("securityBackbone", List.copyOf(security));
        out.put("evidenceSupport", List.copyOf(evidenceSupport));
        out.put("pjbSovereignCapabilities", List.copyOf(sovereign));
        out.put("mustBeImplemented", List.of(
                "editor_investigativo_nativo",
                "evidence_studio_nativo",
                "cadeia_custodia_publica_e_sigilosa",
                "cartorio_policial_nato_digital",
                "motor_nativo_de_cautelares",
                "hub_soberano_de_interoperabilidade"
        ));
        return Collections.unmodifiableMap(out);
    }

    private List<Map<String, Object>> toolsForLane(String actorLane) {
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        addIfPresent(out, "PJB_NARRATIVA_INVESTIGATIVA");
        addIfPresent(out, "PJB_EVIDENCE_STUDIO");
        addIfPresent(out, "PJB_CUSTODIA_GUARD");
        addIfPresent(out, "PJB_DILIGENCE_BOARD");
        addIfPresent(out, "PJB_CAUTELAR_FLOW");
        addIfPresent(out, "PJB_CARTORIO_POLICIAL");
        addIfPresent(out, "PJB_INTEROP_HUB");
        return List.copyOf(out);
    }

    private void addIfPresent(List<Map<String, Object>> out, String code) {
        Map<String, Object> item = loadCatalog().get(code);
        if (item == null) {
            return;
        }
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        summary.put("code", code);
        summary.put("displayName", item.getOrDefault("displayName", code));
        summary.put("toolLane", item.getOrDefault("toolLane", "GERAL"));
        summary.put("scope", strings(item.get("scope")));
        summary.put("requiredFunctions", strings(item.get("requiredFunctions")));
        summary.put("workflowBackbone", strings(item.get("workflowBackbone")));
        summary.put("securityBackbone", strings(item.get("securityBackbone")));
        summary.put("evidenceSupport", strings(item.get("evidenceSupport")));
        summary.put("pjbSovereignCapabilities", strings(item.get("pjbSovereignCapabilities")));
        out.add(Map.copyOf(summary));
    }

    private Map<String, Map<String, Object>> loadCatalog() {
        Map<String, Map<String, Object>> snapshot = catalog;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (this) {
            if (catalog != null) {
                return catalog;
            }
            catalog = loadCatalogInternal();
            return catalog;
        }
    }

    private Map<String, Map<String, Object>> loadCatalogInternal() {
        try {
            ClassPathResource resource = new ClassPathResource("catalog/pjb_police_native_tools_2026.json");
            try (InputStream in = resource.getInputStream()) {
                Map<String, Map<String, Object>> loaded = objectMapper.readValue(in, CATALOG_TYPE);
                return loaded == null ? Map.of() : Map.copyOf(loaded);
            }
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static String actorLane(TipoUsuario tipoUsuario) {
        return tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL ? "POLICIA_FEDERAL" : "POLICIA_CIVIL";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static List<String> strings(Object value) {
        if (value instanceof Iterable<?> iterable) {
            ArrayList<String> out = new ArrayList<>();
            for (Object item : iterable) {
                String text = string(item);
                if (text != null && !text.isBlank()) {
                    out.add(text);
                }
            }
            return List.copyOf(out);
        }
        String text = string(value);
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text);
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
