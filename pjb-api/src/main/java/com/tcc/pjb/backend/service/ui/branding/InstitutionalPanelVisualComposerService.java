package com.tcc.pjb.backend.service.ui.branding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalPanelVisualComposerService {

    public Map<String, Object> compose(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        Map<String, Object> branding = safe.brandingProfile() == null ? Map.of() : safe.brandingProfile();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("panelKind", safe.panelKind());
        out.put("actorLane", safe.actorLane());
        out.put("layoutMode", layoutMode(safe));
        out.put("headerDensity", headerDensity(safe));
        out.put("institutionStrip", institutionStrip(branding, safe));
        out.put("workspacePage", workspacePage(safe));
        out.put("evidenceRail", evidenceRail(safe));
        out.put("renderMode", "GOVERNED_PANEL_BRANDING");
        out.put("dbBlobForbidden", branding.getOrDefault("databaseBlobForbidden", Boolean.TRUE));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> institutionStrip(Map<String, Object> branding, ResolveRequest request) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("displayName", branding.getOrDefault("displayName", "Instituição vinculada ao PJB"));
        out.put("unitDisplayName", branding.getOrDefault("unitDisplayName", "Unidade institucional"));
        out.put("palette", branding.getOrDefault("palette", List.of()));
        Object renderingGuards = branding.get("renderingGuards");
        if (renderingGuards instanceof Map<?, ?> guards) {
            Object enabled = guards.containsKey("panelBannerEnabled") ? guards.get("panelBannerEnabled") : Boolean.TRUE;
            out.put("panelBannerEnabled", enabled);
        } else {
            out.put("panelBannerEnabled", Boolean.TRUE);
        }
        Object assets = branding.get("assets");
        if (assets instanceof Map<?, ?> assetMap) {
            out.put("banner", nestedAsset(assetMap, "banner"));
            out.put("logo", nestedAsset(assetMap, "logo"));
            out.put("seal", nestedAsset(assetMap, "seal"));
        } else {
            out.put("banner", Map.of());
            out.put("logo", Map.of());
            out.put("seal", Map.of());
        }
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> workspacePage(ResolveRequest request) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        boolean investigative = request.actorLane().contains("POLICIA") || request.panelKind().contains("INQUERITO");
        out.put("pageWidth", investigative ? "NARROW" : "STANDARD");
        out.put("narrativeMode", investigative ? "EVIDENCE_STREAM" : "PIECE_WORKSPACE");
        out.put("inlineMediaEnabled", Boolean.TRUE);
        out.put("inlineDocumentForbidden", Boolean.TRUE);
        out.put("postPieceAttachmentBlock", Boolean.TRUE);
        out.put("denseInspector", investigative);
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> evidenceRail(ResolveRequest request) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        boolean investigative = request.actorLane().contains("POLICIA") || request.panelKind().contains("INQUERITO");
        out.put("enabled", investigative);
        out.put("mode", investigative ? "RIGHT_DENSE_STRIP" : "BOTTOM_SUMMARY");
        out.put("supportsInlineImage", Boolean.TRUE);
        out.put("supportsInlineAudio", Boolean.TRUE);
        out.put("supportsInlineVideo", Boolean.TRUE);
        out.put("documentsAfterNarrative", Boolean.TRUE);
        return Collections.unmodifiableMap(out);
    }

    private String layoutMode(ResolveRequest request) {
        if (request.actorLane().contains("POLICIA") || request.panelKind().contains("INQUERITO")) {
            return "NARROW_EVIDENCE_STREAM";
        }
        if (request.actorLane().contains("PERICIA") || request.actorLane().contains("PSICOSSOCIAL")) {
            return "TECHNICAL_REVIEW_PANEL";
        }
        return "GOVERNED_INSTITUTIONAL_PANEL";
    }

    private String headerDensity(ResolveRequest request) {
        if (request.actorLane().contains("POLICIA") || request.panelKind().contains("INQUERITO")) {
            return "COMPACT_DENSE";
        }
        return "BALANCED";
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

    public record ResolveRequest(String actorLane, String panelKind, Map<String, Object> brandingProfile) {
        public ResolveRequest {
            actorLane = normalize(actorLane, "INSTITUCIONAL");
            panelKind = normalize(panelKind, "PAINEL_INSTITUCIONAL");
            brandingProfile = brandingProfile == null ? Map.of() : brandingProfile;
        }

        public static ResolveRequest empty() {
            return new ResolveRequest("INSTITUCIONAL", "PAINEL_INSTITUCIONAL", Map.of());
        }

        private static String normalize(String value, String fallback) {
            String text = value == null ? null : value.trim();
            if (text == null || text.isEmpty()) {
                return fallback;
            }
            return text.toUpperCase(Locale.ROOT);
        }
    }
}
