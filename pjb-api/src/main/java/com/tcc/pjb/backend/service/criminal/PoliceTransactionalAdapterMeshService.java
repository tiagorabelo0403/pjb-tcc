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
public class PoliceTransactionalAdapterMeshService {

    private static final TypeReference<Map<String, Map<String, Object>>> CATALOG_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private volatile Map<String, Map<String, Object>> catalog;

    public PoliceTransactionalAdapterMeshService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public Map<String, Object> transactionalCatalog(TipoUsuario tipoUsuario) {
        String actorLane = actorLane(tipoUsuario);
        List<Map<String, Object>> adapters = adaptersForLane(actorLane);
        LinkedHashSet<String> transactions = new LinkedHashSet<>();
        LinkedHashSet<String> fallbacks = new LinkedHashSet<>();
        LinkedHashSet<String> guarantees = new LinkedHashSet<>();
        for (Map<String, Object> adapter : adapters) {
            transactions.addAll(strings(adapter.get("transactionFamilies")));
            fallbacks.addAll(strings(adapter.get("pjbNativeFallback")));
            guarantees.addAll(strings(adapter.get("operationalGuarantees")));
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "POLICE_TRANSACTIONAL_ADAPTER_CATALOG");
        out.put("actorLane", actorLane);
        out.put("adapters", adapters);
        out.put("transactionFamilies", List.copyOf(transactions));
        out.put("pjbNativeFallback", List.copyOf(fallbacks));
        out.put("operationalGuarantees", List.copyOf(guarantees));
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
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> sovereignMesh(TipoUsuario tipoUsuario) {
        String actorLane = actorLane(tipoUsuario);
        List<Map<String, Object>> adapters = adaptersForLane(actorLane);
        LinkedHashSet<String> transactions = new LinkedHashSet<>();
        LinkedHashSet<String> fallbacks = new LinkedHashSet<>();
        LinkedHashSet<String> guarantees = new LinkedHashSet<>();
        for (Map<String, Object> adapter : adapters) {
            transactions.addAll(strings(adapter.get("transactionFamilies")));
            fallbacks.addAll(strings(adapter.get("pjbNativeFallback")));
            guarantees.addAll(strings(adapter.get("operationalGuarantees")));
        }
        LinkedHashMap<String, Object> lanes = new LinkedHashMap<>();
        lanes.put("submission", List.of("PRECHECK", "ASSINATURA", "SUBMISSAO", "ACK"));
        lanes.put("sync", List.of("SNAPSHOT", "EVENTOS", "INTIMACOES", "RECONCILIACAO"));
        lanes.put("contingency", List.of("FILA_LOCAL", "RETRY_AUDITAVEL", "MIRROR_DE_PROTOCOLO"));
        lanes.put("nativeFirst", List.of("PJB_INTEROP_HUB", "PJB_CARTORIO_POLICIAL", "PJB_CAUTELAR_FLOW"));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "POLICE_TRANSACTIONAL_SOVEREIGN_MESH");
        out.put("actorLane", actorLane);
        out.put("preferNativeBeforePartner", Boolean.TRUE);
        out.put("adapters", adapters);
        out.put("transactionFamilies", List.copyOf(transactions));
        out.put("pjbNativeFallback", List.copyOf(fallbacks));
        out.put("operationalGuarantees", List.copyOf(guarantees));
        out.put("transactionLanes", Map.copyOf(lanes));
        out.put("mustBeImplemented", List.of(
                "precheck_de_remessa",
                "submissao_cautelar_e_representacao",
                "snapshot_processual_soberano",
                "sincronizacao_de_eventos_e_intimacoes",
                "fila_local_de_contingencia",
                "retentativa_auditavel"
        ));
        return Collections.unmodifiableMap(out);
    }

    private List<Map<String, Object>> adaptersForLane(String actorLane) {
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        addIfPresent(out, "PJB_PJE_MNI_TRANSACTIONAL");
        addIfPresent(out, "PJB_EPROC_TRANSACTIONAL");
        addIfPresent(out, "PJB_ESAJ_TRANSACTIONAL");
        addIfPresent(out, "PJB_SINESP_PPE_TRANSACTIONAL");
        if ("POLICIA_FEDERAL".equals(actorLane)) {
            addIfPresent(out, "PJB_EPOL_TRANSACTIONAL");
        }
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
        summary.put("partnerSystem", item.getOrDefault("partnerSystem", code));
        summary.put("scope", strings(item.get("scope")));
        summary.put("transactionFamilies", strings(item.get("transactionFamilies")));
        summary.put("pjbNativeFallback", strings(item.get("pjbNativeFallback")));
        summary.put("operationalGuarantees", strings(item.get("operationalGuarantees")));
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
            ClassPathResource resource = new ClassPathResource("catalog/police_transactional_adapters_2026.json");
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
