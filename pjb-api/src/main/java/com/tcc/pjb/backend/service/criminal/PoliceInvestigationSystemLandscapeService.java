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
public class PoliceInvestigationSystemLandscapeService {

    private static final TypeReference<Map<String, Map<String, Object>>> CATALOG_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private volatile Map<String, Map<String, Object>> catalog;

    public PoliceInvestigationSystemLandscapeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public Map<String, Object> landscapeFor(TipoUsuario tipoUsuario) {
        String actorLane = actorLane(tipoUsuario);
        List<Map<String, Object>> systems = systemsForLane(actorLane);
        LinkedHashSet<String> imported = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> interoperabilities = new LinkedHashSet<>();
        for (Map<String, Object> system : systems) {
            imported.addAll(strings(system.get("pjbImportedCapabilities")));
            warnings.addAll(strings(system.get("hardeningWarnings")));
            interoperabilities.addAll(strings(system.get("integrations")));
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "POLICE_INVESTIGATION_SYSTEM_LANDSCAPE");
        out.put("actorLane", actorLane);
        out.put("systemsCount", systems.size());
        out.put("systems", systems);
        out.put("sharedPatterns", sharedPatterns(actorLane));
        out.put("pjbMustAbsorb", List.copyOf(imported));
        out.put("avoidFootguns", List.copyOf(warnings));
        out.put("interoperabilityBackbone", List.copyOf(interoperabilities));
        out.put("researchBackbone", researchBackbone(systems));
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
        out.put("researchBackbone", researchBackbone(List.of(Map.copyOf(item))));
        return Collections.unmodifiableMap(out);
    }

    public Map<String, Object> workstationBlueprint(TipoUsuario tipoUsuario) {
        String actorLane = actorLane(tipoUsuario);
        List<Map<String, Object>> systems = systemsForLane(actorLane);
        LinkedHashSet<String> coreProcedures = new LinkedHashSet<>();
        LinkedHashSet<String> interoperabilityTargets = new LinkedHashSet<>();
        LinkedHashSet<String> imported = new LinkedHashSet<>();
        for (Map<String, Object> system : systems) {
            coreProcedures.addAll(strings(system.get("coreProcedures")));
            interoperabilityTargets.addAll(strings(system.get("integrations")));
            imported.addAll(strings(system.get("pjbImportedCapabilities")));
        }
        LinkedHashMap<String, Object> signature = new LinkedHashMap<>();
        signature.put("model", "ICP_BRASIL_E_HASH_DE_CUSTODIA");
        signature.put("validatorRequired", true);
        signature.put("printedCopyNotCanonical", true);
        signature.put("publicAuthenticityVerifier", true);
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("inlineNarrativeMedia", List.of("IMAGEM", "AUDIO", "VIDEO"));
        evidence.put("documentsAfterNarrative", true);
        evidence.put("transcriptionBackbone", List.of("AUDIO", "VIDEO"));
        evidence.put("periciaNative", List.of("LAUDO_ELETRONICO", "TRANSCRICAO_SEGURA", "KEYFRAMES_HASH"));
        LinkedHashMap<String, Object> lanes = new LinkedHashMap<>();
        lanes.put("origin", List.of("BO", "NOTICIA_CRIME", "DENUNCIA", "FLAGRANTE", "COMUNICACAO_DIGITAL"));
        lanes.put("cartorio", List.of("AUTUACAO", "PORTARIA", "DESPACHO_HOMOLOGATORIO", "DILIGENCIAS", "COTAS", "DILACAO_PRAZO"));
        lanes.put("justice", List.of("REMESSA_MP", "REMESSA_JUDICIARIO", "MNI", "PJE", "EPROC", "SAJ"));
        lanes.put("analytics", List.of("BI_INQUERITOS", "PRODUTIVIDADE", "MAPA_CRIMINAL", "PRAZOS"));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "INVESTIGATIVE_WORKSTATION_BLUEPRINT");
        out.put("actorLane", actorLane);
        out.put("layoutMode", "NARROW_EVIDENCE_STREAM");
        out.put("headerDensity", "COMPACT_DENSE");
        out.put("documentModel", "NATO_DIGITAL_FIRST");
        out.put("mandatoryStations", List.of(
                "registro-origem",
                "cartorio-investigativo",
                "evidencia-multimidia-inline",
                "anexos-pos-narrativa",
                "pericia-e-laudo",
                "tramitação-mp-judiciario",
                "autenticidade-e-assinatura",
                "analytics-produto-prazo"
        ));
        out.put("coreProcedures", List.copyOf(coreProcedures));
        out.put("interoperabilityTargets", List.copyOf(interoperabilityTargets));
        out.put("signatureAndAuthenticity", Map.copyOf(signature));
        out.put("evidenceHandling", Map.copyOf(evidence));
        out.put("operationalLanes", Map.copyOf(lanes));
        out.put("absorbedCapabilities", List.copyOf(imported));
        return Collections.unmodifiableMap(out);
    }

    private List<Map<String, Object>> systemsForLane(String actorLane) {
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        if ("POLICIA_FEDERAL".equals(actorLane)) {
            addIfPresent(out, "EPOL_PF");
            addIfPresent(out, "SINESP_PPE");
            addIfPresent(out, "PCDF_PJE_MNI_ELAUDO");
            addIfPresent(out, "PCSC_IP_DIGITAL");
        } else {
            addIfPresent(out, "SINESP_PPE");
            addIfPresent(out, "PCMG_PCNET_PPJE");
            addIfPresent(out, "PJCMT_GEIA_CARTORIUM_PJE");
            addIfPresent(out, "PCSC_IP_DIGITAL");
            addIfPresent(out, "PCDF_PJE_MNI_ELAUDO");
            addIfPresent(out, "PCSP_IPE_SAJ");
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
        summary.put("institution", item.getOrDefault("institution", "Instituição policial"));
        summary.put("sphere", item.getOrDefault("sphere", "FEDERATIVO"));
        summary.put("status", item.getOrDefault("status", "MAPEADO"));
        summary.put("documentModel", item.getOrDefault("documentModel", "DIGITAL"));
        summary.put("coreProcedures", strings(item.get("coreProcedures")));
        summary.put("integrations", strings(item.get("integrations")));
        summary.put("pjbImportedCapabilities", strings(item.get("pjbImportedCapabilities")));
        summary.put("hardeningWarnings", strings(item.get("hardeningWarnings")));
        summary.put("officialSignals", item.getOrDefault("officialSignals", List.of()));
        out.add(Map.copyOf(summary));
    }

    private List<Map<String, Object>> researchBackbone(List<Map<String, Object>> systems) {
        ArrayList<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> system : systems) {
            String displayName = string(system.get("displayName"));
            Object raw = system.get("officialSignals");
            if (raw instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    if (!(item instanceof Map<?, ?> map)) {
                        continue;
                    }
                    LinkedHashMap<String, Object> signal = new LinkedHashMap<>();
                    signal.put("system", displayName);
                    signal.put("title", string(map.get("title")));
                    signal.put("year", map.get("year"));
                    signal.put("sourceType", string(map.get("sourceType")));
                    out.add(Map.copyOf(signal));
                }
            }
        }
        return List.copyOf(out);
    }

    private List<String> sharedPatterns(String actorLane) {
        ArrayList<String> out = new ArrayList<>();
        out.add("nato-digital-first");
        out.add("documentos-separados-da-narrativa");
        out.add("audio-video-imagem-como-evidencia-inline");
        out.add("tramitação-direta-para-mp-e-judiciario");
        out.add("assinatura-digital-e-autenticidade-verificavel");
        out.add("gestao-cartoraria-e-prazo");
        out.add("analytics-de-produtividade-e-fluxo");
        if ("POLICIA_FEDERAL".equals(actorLane)) {
            out.add("federacao-com-integracao-plataformas-nacionais");
        } else {
            out.add("interoperabilidade-estadual-com-tribunal-parceiro");
        }
        return List.copyOf(out);
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
            ClassPathResource resource = new ClassPathResource("catalog/police_investigation_systems_2026.json");
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

    private static String string(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static List<String> strings(Object raw) {
        if (!(raw instanceof Iterable<?> iterable)) {
            return List.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (Object item : iterable) {
            String value = string(item);
            if (value != null) {
                out.add(value);
            }
        }
        return List.copyOf(out);
    }
}
