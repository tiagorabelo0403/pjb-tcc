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
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class PoliceInteroperabilityAdapterBlueprintService {

    private static final TypeReference<Map<String, Map<String, Object>>> CATALOG_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private volatile Map<String, Map<String, Object>> catalog;

    public PoliceInteroperabilityAdapterBlueprintService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public Map<String, Object> adapterCatalog(TipoUsuario tipoUsuario) {
        String actorLane = actorLane(tipoUsuario);
        List<Map<String, Object>> adapters = adaptersForLane(actorLane);
        LinkedHashSet<String> requiredFunctions = new LinkedHashSet<>();
        LinkedHashSet<String> workflows = new LinkedHashSet<>();
        LinkedHashSet<String> support = new LinkedHashSet<>();
        for (Map<String, Object> adapter : adapters) {
            requiredFunctions.addAll(strings(adapter.get("requiredFunctions")));
            workflows.addAll(strings(adapter.get("workflowBackbone")));
            support.addAll(strings(adapter.get("evidenceSupport")));
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "POLICE_INTEROPERABILITY_ADAPTER_CATALOG");
        out.put("actorLane", actorLane);
        out.put("adaptersCount", adapters.size());
        out.put("adapters", adapters);
        out.put("mandatoryFunctionFamilies", List.copyOf(requiredFunctions));
        out.put("workflowBackbone", List.copyOf(workflows));
        out.put("evidenceSupport", List.copyOf(support));
        out.put("allFunctionsNecessary", Boolean.TRUE);
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

    public Map<String, Object> operationalMesh(TipoUsuario tipoUsuario) {
        String actorLane = actorLane(tipoUsuario);
        List<Map<String, Object>> adapters = adaptersForLane(actorLane);
        LinkedHashSet<String> mandatoryFunctions = new LinkedHashSet<>();
        LinkedHashSet<String> workflows = new LinkedHashSet<>();
        LinkedHashSet<String> evidenceSupport = new LinkedHashSet<>();
        LinkedHashSet<String> pjbAbsorptionPlan = new LinkedHashSet<>();
        LinkedHashSet<String> securityProfile = new LinkedHashSet<>();
        for (Map<String, Object> adapter : adapters) {
            mandatoryFunctions.addAll(strings(adapter.get("requiredFunctions")));
            workflows.addAll(strings(adapter.get("workflowBackbone")));
            evidenceSupport.addAll(strings(adapter.get("evidenceSupport")));
            pjbAbsorptionPlan.addAll(strings(adapter.get("pjbAbsorptionPlan")));
            securityProfile.addAll(strings(adapter.get("securityProfile")));
        }
        LinkedHashMap<String, Object> lanes = new LinkedHashMap<>();
        lanes.put("origin", List.of("REGISTRO_BO_INTEGRADO", "AUTUACAO_INQUERITO_ELETRONICO", "LAVRATURA_TCO_BOC_IP_APF_AIAI_AAFAI"));
        lanes.put("cartorio", List.of("DESPACHO_HOMOLOGATORIO", "GESTAO_CARTORARIA", "PECAS_PRE_PREENCHIDAS", "GESTAO_DILIGENCIAS"));
        lanes.put("evidencia", List.of("ANEXOS_EVIDENCIAS", "CADEIA_CUSTODIA", "TRANSCRICAO_MIDIA", "KEYFRAMES_HASH", "GESTAO_ANEXOS"));
        lanes.put("medidas", List.of("COMUNICACAO_FLAGRANTE", "PEDIDO_MEDIDA_CAUTELAR", "MEDIDA_PROTETIVA", "REPRESENTACOES_POLICIAIS"));
        lanes.put("judiciario", List.of("REMESSA_JUDICIARIO", "REMESSA_MP", "CONSULTA_PROCESSO", "EVENTOS_PROCESSUAIS", "INTIMACOES_COMUNICACOES"));
        lanes.put("analytics", List.of("ANALYTICS_INQUERITOS", "PAINEL_ALERTAS", "PRAZOS", "GEOREFERENCIAMENTO", "CONSULTA_BASES_EXTERNAS"));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "POLICE_INTEROPERABILITY_OPERATIONAL_MESH");
        out.put("actorLane", actorLane);
        out.put("adapters", adapters);
        out.put("mandatoryFunctionFamilies", List.copyOf(mandatoryFunctions));
        out.put("workflowBackbone", List.copyOf(workflows));
        out.put("evidenceSupport", List.copyOf(evidenceSupport));
        out.put("pjbAbsorptionPlan", List.copyOf(pjbAbsorptionPlan));
        out.put("securityBackbone", List.copyOf(securityProfile));
        out.put("operationalLanes", Map.copyOf(lanes));
        out.put("mustBeImplemented", List.of(
                "registro_origem_integrado",
                "cartorio_investigativo_com_despacho",
                "evidencia_multimidia_inline_e_anexos_pos_narrativa",
                "medidas_cautelares_e_protetivas",
                "remessa_mp_judiciario",
                "sync_eventos_intimacoes_e_snapshot",
                "autenticidade_assinatura_e_auditoria"
        ));
        return Collections.unmodifiableMap(out);
    }

    private List<Map<String, Object>> adaptersForLane(String actorLane) {
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        if ("POLICIA_FEDERAL".equals(actorLane)) {
            addIfPresent(out, "EPOL_FEDERAL_OPERACIONAL");
            addIfPresent(out, "PJE_MNI_OPERACIONAL");
            addIfPresent(out, "EPROC_OPERACIONAL");
            addIfPresent(out, "ESAJ_OPERACIONAL");
            addIfPresent(out, "SINESP_PPE_OPERACIONAL");
        } else {
            addIfPresent(out, "SINESP_PPE_OPERACIONAL");
            addIfPresent(out, "PJE_MNI_OPERACIONAL");
            addIfPresent(out, "EPROC_OPERACIONAL");
            addIfPresent(out, "ESAJ_OPERACIONAL");
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
        summary.put("partnerLane", item.getOrDefault("partnerLane", "GENERICO"));
        summary.put("scope", strings(item.get("scope")));
        summary.put("requiredFunctions", strings(item.get("requiredFunctions")));
        summary.put("workflowBackbone", strings(item.get("workflowBackbone")));
        summary.put("evidenceSupport", strings(item.get("evidenceSupport")));
        summary.put("pjbAbsorptionPlan", strings(item.get("pjbAbsorptionPlan")));
        summary.put("securityProfile", strings(item.get("securityProfile")));
        summary.put("officialSignals", item.getOrDefault("officialSignals", List.of()));
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
            ClassPathResource resource = new ClassPathResource("catalog/police_interoperability_adapters_2026.json");
            try (InputStream in = resource.getInputStream()) {
                Map<String, Map<String, Object>> loaded = objectMapper.readValue(in, CATALOG_TYPE);
                return loaded == null ? Map.of() : Map.copyOf(loaded);
            }
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private static String actorLane(TipoUsuario tipoUsuario) {
        if (tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            return "POLICIA_FEDERAL";
        }
        return "POLICIA_CIVIL";
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
