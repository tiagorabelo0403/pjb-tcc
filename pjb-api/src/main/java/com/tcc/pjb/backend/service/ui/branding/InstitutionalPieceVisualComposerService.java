package com.tcc.pjb.backend.service.ui.branding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalPieceVisualComposerService {

    public Map<String, Object> compose(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        Map<String, Object> branding = safe.brandingProfile() == null ? Map.of() : safe.brandingProfile();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("pieceLabel", safe.pieceLabel());
        out.put("panelHeader", panelHeader(branding, safe));
        out.put("printHeader", printHeader(branding, safe));
        out.put("watermark", watermark(branding, safe));
        out.put("renderMode", "GOVERNED_INSTITUTIONAL_BRANDING");
        out.put("bannerWeight", "LIGHT_ASSET_REFERENCE_ONLY");
        out.put("dbBlobForbidden", branding.getOrDefault("databaseBlobForbidden", Boolean.TRUE));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> panelHeader(Map<String, Object> branding, ResolveRequest request) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("displayName", branding.getOrDefault("displayName", "Instituição vinculada ao PJB"));
        out.put("unitDisplayName", branding.getOrDefault("unitDisplayName", "Unidade institucional"));
        out.put("palette", branding.getOrDefault("palette", List.of()));
        Object assets = branding.get("assets");
        if (assets instanceof Map<?, ?> assetMap) {
            out.put("banner", nestedAsset(assetMap, "banner"));
            out.put("logo", nestedAsset(assetMap, "logo"));
        } else {
            out.put("banner", Map.of());
            out.put("logo", Map.of());
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> printHeader(Map<String, Object> branding, ResolveRequest request) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        String template = Objects.toString(branding.getOrDefault("headerTemplate", "PJB • {{institution}} • {{pieceLabel}}"));
        String institution = Objects.toString(branding.getOrDefault("displayName", "Instituição vinculada ao PJB"));
        String unit = Objects.toString(branding.getOrDefault("unitDisplayName", "Unidade institucional"));
        out.put("template", template);
        out.put("resolvedHeader", template.replace("{{institution}}", institution).replace("{{unit}}", unit).replace("{{pieceLabel}}", request.pieceLabel()));
        out.put("formalMode", "INSTITUTIONAL_SOLENE");
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> watermark(Map<String, Object> branding, ResolveRequest request) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", Boolean.TRUE);
        out.put("label", branding.getOrDefault("watermarkLabel", request.pieceLabel()));
        Object assets = branding.get("assets");
        if (assets instanceof Map<?, ?> assetMap) {
            out.put("seal", nestedAsset(assetMap, "seal"));
        } else {
            out.put("seal", Map.of());
        }
        return Collections.unmodifiableMap(out);
    }


    private static Map<String, Object> nestedAsset(Map<?, ?> assetMap, String key) {
        Object value = assetMap.get(key);
        if (value instanceof Map<?, ?> nested) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            nested.forEach((nestedKey, nestedValue) -> out.put(String.valueOf(nestedKey), nestedValue));
            return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
        }
        return Map.of();
    }

    public record ResolveRequest(
            String pieceLabel,
            Map<String, Object> brandingProfile
    ) {
        public static ResolveRequest empty() {
            return new ResolveRequest("Peça institucional", Map.of());
        }
    }
}
