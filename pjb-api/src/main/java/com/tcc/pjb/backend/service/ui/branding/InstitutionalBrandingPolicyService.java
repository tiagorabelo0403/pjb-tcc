package com.tcc.pjb.backend.service.ui.branding;

import com.tcc.pjb.backend.configs.ui.InstitutionalBrandingProperties;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalBrandingPolicyService {

    private final InstitutionalBrandingProperties properties;

    public InstitutionalBrandingPolicyService(InstitutionalBrandingProperties properties) {
        this.properties = properties == null ? new InstitutionalBrandingProperties() : properties;
    }

    public Map<String, Object> governanceSummary() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", properties.isEnabled());
        out.put("objectStorageOnly", properties.isObjectStorageOnly());
        out.put("databaseBlobForbidden", properties.isDatabaseBlobForbidden());
        out.put("allowSvg", properties.isAllowSvg());
        out.put("allowAnimated", properties.isAllowAnimated());
        out.put("storagePrefix", properties.getStoragePrefix());
        out.put("deliveryBasePath", properties.getDeliveryBasePath());
        out.put("cacheTtlSeconds", properties.getCacheTtlSeconds());
        out.put("allowedMimeTypes", List.copyOf(properties.getAllowedMimeTypes()));
        out.put("banner", limits(properties.getBanner()));
        out.put("logo", limits(properties.getLogo()));
        out.put("seal", limits(properties.getSeal()));
        out.put("theme", themeSummary());
        return Collections.unmodifiableMap(out);
    }

    public SanitizedAssetReference sanitize(String assetKind, String storageKey, String mimeType, Long sizeBytes, Integer widthPx, Integer heightPx) {
        String kind = normalizeKind(assetKind);
        InstitutionalBrandingProperties.AssetLimits limits = limitsFor(kind);
        String normalizedMime = normalizeMime(mimeType);
        boolean mimeAllowed = normalizedMime != null && properties.getAllowedMimeTypes().stream().anyMatch(v -> v.equalsIgnoreCase(normalizedMime));
        long safeBytes = sizeBytes == null || sizeBytes < 0L ? 0L : sizeBytes;
        int safeWidth = widthPx == null || widthPx < 0 ? 0 : widthPx;
        int safeHeight = heightPx == null || heightPx < 0 ? 0 : heightPx;
        boolean sizeAllowed = safeBytes == 0L || safeBytes <= limits.getMaxBytes();
        boolean dimensionsAllowed = (safeWidth == 0 || safeWidth <= limits.getMaxWidthPx()) && (safeHeight == 0 || safeHeight <= limits.getMaxHeightPx());
        boolean keyAllowed = storageKey != null && !storageKey.isBlank() && normalizeStorageKey(storageKey).startsWith(properties.getStoragePrefix() + "/");
        boolean approved = properties.isEnabled() && mimeAllowed && sizeAllowed && dimensionsAllowed && keyAllowed;
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("mimeAllowed", mimeAllowed);
        diagnostics.put("sizeAllowed", sizeAllowed);
        diagnostics.put("dimensionsAllowed", dimensionsAllowed);
        diagnostics.put("keyAllowed", keyAllowed);
        diagnostics.put("maxBytes", limits.getMaxBytes());
        diagnostics.put("maxWidthPx", limits.getMaxWidthPx());
        diagnostics.put("maxHeightPx", limits.getMaxHeightPx());
        return new SanitizedAssetReference(
                kind,
                approved,
                approved ? normalizeStorageKey(storageKey) : null,
                approved ? normalizedMime : null,
                approved ? safeBytes : 0L,
                approved ? publishPath(normalizeStorageKey(storageKey)) : null,
                Map.copyOf(diagnostics)
        );
    }

    private Map<String, Object> themeSummary() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("maxPaletteEntries", properties.getTheme().getMaxPaletteEntries());
        out.put("nationalAccent", properties.getTheme().getNationalAccent());
        out.put("neutralSurface", properties.getTheme().getNeutralSurface());
        out.put("neutralInk", properties.getTheme().getNeutralInk());
        out.put("watermarkEnabled", properties.getTheme().isWatermarkEnabled());
        out.put("panelBannerEnabled", properties.getTheme().isPanelBannerEnabled());
        out.put("pieceHeaderBannerEnabled", properties.getTheme().isPieceHeaderBannerEnabled());
        out.put("printHeaderEnabled", properties.getTheme().isPrintHeaderEnabled());
        return Collections.unmodifiableMap(out);
    }

    private static Map<String, Object> limits(InstitutionalBrandingProperties.AssetLimits limits) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("maxBytes", limits.getMaxBytes());
        out.put("maxWidthPx", limits.getMaxWidthPx());
        out.put("maxHeightPx", limits.getMaxHeightPx());
        return Collections.unmodifiableMap(out);
    }

    private InstitutionalBrandingProperties.AssetLimits limitsFor(String kind) {
        return switch (kind) {
            case "LOGO" -> properties.getLogo();
            case "SEAL" -> properties.getSeal();
            default -> properties.getBanner();
        };
    }

    private String publishPath(String storageKey) {
        return properties.getDeliveryBasePath() + "?assetKey=" + storageKey;
    }

    private static String normalizeKind(String assetKind) {
        if (assetKind == null || assetKind.isBlank()) {
            return "BANNER";
        }
        String normalized = assetKind.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("LOGO")) {
            return "LOGO";
        }
        if (normalized.contains("SEAL") || normalized.contains("SELO")) {
            return "SEAL";
        }
        return "BANNER";
    }

    private static String normalizeMime(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return null;
        }
        return mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeStorageKey(String storageKey) {
        String normalized = storageKey == null ? "" : storageKey.trim();
        return normalized.replace("..", "").replace('\\', '/');
    }

    public record SanitizedAssetReference(
            String assetKind,
            boolean approved,
            String storageKey,
            String mimeType,
            long sizeBytes,
            String deliveryPath,
            Map<String, Object> diagnostics
    ) {
    }
}
