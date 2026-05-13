package com.tcc.pjb.backend.service.ui.branding;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.ui.InstitutionalBrandingProperties;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalBrandingResolverService {

    private static final TypeReference<Map<String, Map<String, Object>>> CATALOG_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;
    private final InstitutionalBrandingProperties properties;
    private final InstitutionalBrandingPolicyService policyService;
    private volatile Map<String, Map<String, Object>> catalog;

    public InstitutionalBrandingResolverService(ObjectMapper objectMapper,
                                                InstitutionalBrandingProperties properties,
                                                InstitutionalBrandingPolicyService policyService) {
        this.objectMapper = Objects.requireNonNullElseGet(objectMapper, ObjectMapper::new);
        this.properties = properties == null ? new InstitutionalBrandingProperties() : properties;
        this.policyService = Objects.requireNonNullElseGet(policyService, () -> new InstitutionalBrandingPolicyService(this.properties));
    }

    public Map<String, Object> resolveProfile(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        String profileCode = resolveProfileCode(safe);
        Map<String, Object> base = loadCatalog().getOrDefault(profileCode, loadCatalog().getOrDefault("DEFAULT", Map.of()));
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("mode", "INSTITUTIONAL_GOVERNED");
        out.put("profileCode", profileCode);
        out.put("actorLane", safe.actorLane());
        out.put("pieceKind", safe.pieceKind());
        out.put("displayName", resolveDisplayName(base, safe));
        out.put("unitDisplayName", resolveUnitDisplayName(safe));
        out.put("palette", resolvePalette(base));
        out.put("headerTemplate", resolveString(safe.requestMap(), List.of("formalHeaderTemplate", "headerTemplate"), stringValue(base.get("headerTemplate"))));
        out.put("watermarkLabel", resolveString(safe.requestMap(), List.of("watermarkLabel", "siglaInstitucional"), stringValue(base.get("watermarkLabel"))));
        out.put("cacheTtlSeconds", properties.getCacheTtlSeconds());
        out.put("objectStorageOnly", properties.isObjectStorageOnly());
        out.put("databaseBlobForbidden", properties.isDatabaseBlobForbidden());
        out.put("assets", resolveAssets(base, safe));
        out.put("renderingGuards", renderingGuards());
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> resolveAssets(Map<String, Object> base, ResolveRequest request) {
        LinkedHashMap<String, Object> assets = new LinkedHashMap<>();
        assets.put("banner", asset("banner", request, base));
        assets.put("logo", asset("logo", request, base));
        assets.put("seal", asset("seal", request, base));
        return Map.copyOf(assets);
    }

    private Map<String, Object> asset(String kind, ResolveRequest request, Map<String, Object> base) {
        String capitalized = kind.substring(0, 1).toUpperCase(Locale.ROOT) + kind.substring(1);
        String baseKey = stringValue(base.get(kind + "StorageKey"));
        String baseMime = stringValue(base.get(kind + "MimeType"));
        Long baseSize = longValue(base.get(kind + "Bytes"));
        String storageKey = resolveString(request.requestMap(), List.of("branding" + capitalized + "Key", kind + "StorageKey"), baseKey);
        String mimeType = resolveString(request.requestMap(), List.of("branding" + capitalized + "MimeType", kind + "MimeType"), baseMime);
        Long sizeBytes = longValue(request.requestMap().getOrDefault("branding" + capitalized + "Bytes", baseSize));
        InstitutionalBrandingPolicyService.SanitizedAssetReference ref = policyService.sanitize(kind, storageKey, mimeType, sizeBytes, null, null);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("approved", ref.approved());
        out.put("storageKey", ref.storageKey());
        out.put("deliveryPath", ref.deliveryPath());
        out.put("mimeType", ref.mimeType());
        out.put("sizeBytes", ref.sizeBytes());
        out.put("diagnostics", ref.diagnostics());
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> renderingGuards() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("allowSvg", properties.isAllowSvg());
        out.put("allowAnimated", properties.isAllowAnimated());
        out.put("panelBannerEnabled", properties.getTheme().isPanelBannerEnabled());
        out.put("pieceHeaderBannerEnabled", properties.getTheme().isPieceHeaderBannerEnabled());
        out.put("printHeaderEnabled", properties.getTheme().isPrintHeaderEnabled());
        out.put("watermarkEnabled", properties.getTheme().isWatermarkEnabled());
        return Collections.unmodifiableMap(out);
    }

    private List<String> resolvePalette(Map<String, Object> base) {
        List<String> palette = new ArrayList<>();
        Object raw = base.get("palette");
        if (raw instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                String value = stringValue(item);
                if (value != null && !palette.contains(value)) {
                    palette.add(value);
                }
            }
        }
        if (palette.isEmpty()) {
            palette.add(properties.getTheme().getNationalAccent());
            palette.add(properties.getTheme().getNeutralSurface());
            palette.add(properties.getTheme().getNeutralInk());
        }
        int maxEntries = Math.max(1, properties.getTheme().getMaxPaletteEntries());
        return palette.size() <= maxEntries ? List.copyOf(palette) : List.copyOf(palette.subList(0, maxEntries));
    }

    private String resolveProfileCode(ResolveRequest request) {
        String requested = resolveString(request.requestMap(), List.of("brandingProfileCode"), null);
        if (requested != null) {
            String normalized = requested.trim().toUpperCase(Locale.ROOT);
            if (loadCatalog().containsKey(normalized)) {
                return normalized;
            }
        }
        String lane = normalize(request.actorLane());
        if (lane.contains("MINISTERIO_PUBLICO") || lane.contains("MP")) {
            return "MINISTERIO_PUBLICO";
        }
        if (lane.contains("DEFENSORIA") || request.tipoUsuario() == TipoUsuario.DEFENSOR_PUBLICO) {
            return "DEFENSORIA";
        }
        if (lane.contains("PROCURADORIA") || lane.contains("PROCURADOR")) {
            return "PROCURADORIA";
        }
        if (lane.contains("PERICIA") || lane.contains("PERITO")) {
            return "PERICIA";
        }
        if (lane.contains("PSICOSSOCIAL") || lane.contains("PSICO") || lane.contains("ASSISTENTE_SOCIAL")) {
            return "PSICOSSOCIAL";
        }
        if (lane.contains("POLICIA_FEDERAL") || request.tipoUsuario() == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            return "POLICIA_FEDERAL";
        }
        if (lane.contains("POLICIA_CIVIL") || lane.contains("DELEGADO") || lane.contains("POLICIA") || request.tipoUsuario() == TipoUsuario.DELEGADO_POLICIA) {
            return "POLICIA_CIVIL";
        }
        if (lane.contains("OFICIAL")) {
            return "OFICIAL_JUSTICA";
        }
        return "DEFAULT";
    }

    private String resolveDisplayName(Map<String, Object> base, ResolveRequest request) {
        String requestName = resolveString(request.requestMap(), List.of("institutionDisplayName", "orgaoDisplayName", "orgao"), null);
        if (requestName != null) {
            return requestName;
        }
        return stringValue(base.get("displayName")) != null ? stringValue(base.get("displayName")) : "Instituição vinculada ao PJB";
    }

    private String resolveUnitDisplayName(ResolveRequest request) {
        return resolveString(request.requestMap(), List.of("unitDisplayName", "unidadeNome", "lotacaoNome"), "Unidade institucional");
    }

    private String resolveString(Map<String, Object> map, List<String> keys, String fallback) {
        for (String key : keys) {
            String value = stringValue(map.get(key));
            if (value != null) {
                return value;
            }
        }
        return fallback;
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
            ClassPathResource resource = new ClassPathResource("catalog/institutional_branding_profiles_2026.json");
            try (InputStream in = resource.getInputStream()) {
                Map<String, Map<String, Object>> loaded = objectMapper.readValue(in, CATALOG_TYPE);
                return loaded == null || loaded.isEmpty() ? fallbackCatalog() : loaded;
            }
        } catch (Exception ignored) {
            return fallbackCatalog();
        }
    }

    private static Map<String, Map<String, Object>> fallbackCatalog() {
        LinkedHashMap<String, Map<String, Object>> out = new LinkedHashMap<>();
        LinkedHashMap<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("displayName", "Instituição do Sistema de Justiça");
        defaults.put("palette", List.of("#0B3A75", "#F7F9FC", "#132238"));
        defaults.put("headerTemplate", "PJB • {{institution}} • {{pieceLabel}}");
        defaults.put("watermarkLabel", "PJB");
        defaults.put("bannerStorageKey", "ui/institutional-branding/default/banner-default.webp");
        defaults.put("bannerMimeType", "image/webp");
        defaults.put("bannerBytes", 128000);
        defaults.put("logoStorageKey", "ui/institutional-branding/default/logo-default.webp");
        defaults.put("logoMimeType", "image/webp");
        defaults.put("logoBytes", 64000);
        defaults.put("sealStorageKey", "ui/institutional-branding/default/seal-default.webp");
        defaults.put("sealMimeType", "image/webp");
        defaults.put("sealBytes", 64000);
        out.put("DEFAULT", Map.copyOf(defaults));
        return Collections.unmodifiableMap(out);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = String.valueOf(value).trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    public record ResolveRequest(
            String actorLane,
            String pieceKind,
            TipoUsuario tipoUsuario,
            Map<String, Object> requestMap
    ) {
        public static ResolveRequest empty() {
            return new ResolveRequest("INSTITUCIONAL", "PECA_INSTITUCIONAL", null, Map.of());
        }
    }
}
