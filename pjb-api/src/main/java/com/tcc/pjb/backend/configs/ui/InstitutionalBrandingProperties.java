package com.tcc.pjb.backend.configs.ui;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.ui.branding")
public class InstitutionalBrandingProperties {

    private boolean enabled = true;
    private boolean objectStorageOnly = true;
    private boolean databaseBlobForbidden = true;
    private boolean allowSvg = false;
    private boolean allowAnimated = false;
    private String storagePrefix = "ui/institutional-branding";
    private String deliveryBasePath = "/api/v1/ui/branding/assets";
    private long cacheTtlSeconds = 86_400L;
    private AssetLimits banner = new AssetLimits(2_097_152L, 1_920, 320);
    private AssetLimits logo = new AssetLimits(786_432L, 512, 512);
    private AssetLimits seal = new AssetLimits(786_432L, 512, 512);
    private Theme theme = new Theme();
    private List<String> allowedMimeTypes = new ArrayList<>(List.of("image/png", "image/jpeg", "image/webp"));

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isObjectStorageOnly() {
        return objectStorageOnly;
    }

    public void setObjectStorageOnly(boolean objectStorageOnly) {
        this.objectStorageOnly = objectStorageOnly;
    }

    public boolean isDatabaseBlobForbidden() {
        return databaseBlobForbidden;
    }

    public void setDatabaseBlobForbidden(boolean databaseBlobForbidden) {
        this.databaseBlobForbidden = databaseBlobForbidden;
    }

    public boolean isAllowSvg() {
        return allowSvg;
    }

    public void setAllowSvg(boolean allowSvg) {
        this.allowSvg = allowSvg;
    }

    public boolean isAllowAnimated() {
        return allowAnimated;
    }

    public void setAllowAnimated(boolean allowAnimated) {
        this.allowAnimated = allowAnimated;
    }

    public String getStoragePrefix() {
        return storagePrefix;
    }

    public void setStoragePrefix(String storagePrefix) {
        this.storagePrefix = storagePrefix;
    }

    public String getDeliveryBasePath() {
        return deliveryBasePath;
    }

    public void setDeliveryBasePath(String deliveryBasePath) {
        this.deliveryBasePath = deliveryBasePath;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public AssetLimits getBanner() {
        return banner;
    }

    public void setBanner(AssetLimits banner) {
        this.banner = banner;
    }

    public AssetLimits getLogo() {
        return logo;
    }

    public void setLogo(AssetLimits logo) {
        this.logo = logo;
    }

    public AssetLimits getSeal() {
        return seal;
    }

    public void setSeal(AssetLimits seal) {
        this.seal = seal;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    public void setAllowedMimeTypes(List<String> allowedMimeTypes) {
        this.allowedMimeTypes = allowedMimeTypes == null ? new ArrayList<>() : new ArrayList<>(allowedMimeTypes);
    }

    public static class AssetLimits {
        private long maxBytes;
        private int maxWidthPx;
        private int maxHeightPx;

        public AssetLimits() {
        }

        public AssetLimits(long maxBytes, int maxWidthPx, int maxHeightPx) {
            this.maxBytes = maxBytes;
            this.maxWidthPx = maxWidthPx;
            this.maxHeightPx = maxHeightPx;
        }

        public long getMaxBytes() {
            return maxBytes;
        }

        public void setMaxBytes(long maxBytes) {
            this.maxBytes = maxBytes;
        }

        public int getMaxWidthPx() {
            return maxWidthPx;
        }

        public void setMaxWidthPx(int maxWidthPx) {
            this.maxWidthPx = maxWidthPx;
        }

        public int getMaxHeightPx() {
            return maxHeightPx;
        }

        public void setMaxHeightPx(int maxHeightPx) {
            this.maxHeightPx = maxHeightPx;
        }
    }

    public static class Theme {
        private int maxPaletteEntries = 3;
        private String nationalAccent = "#0B3A75";
        private String neutralSurface = "#F7F9FC";
        private String neutralInk = "#132238";
        private boolean watermarkEnabled = true;
        private boolean panelBannerEnabled = true;
        private boolean pieceHeaderBannerEnabled = true;
        private boolean printHeaderEnabled = true;

        public int getMaxPaletteEntries() {
            return maxPaletteEntries;
        }

        public void setMaxPaletteEntries(int maxPaletteEntries) {
            this.maxPaletteEntries = maxPaletteEntries;
        }

        public String getNationalAccent() {
            return nationalAccent;
        }

        public void setNationalAccent(String nationalAccent) {
            this.nationalAccent = nationalAccent;
        }

        public String getNeutralSurface() {
            return neutralSurface;
        }

        public void setNeutralSurface(String neutralSurface) {
            this.neutralSurface = neutralSurface;
        }

        public String getNeutralInk() {
            return neutralInk;
        }

        public void setNeutralInk(String neutralInk) {
            this.neutralInk = neutralInk;
        }

        public boolean isWatermarkEnabled() {
            return watermarkEnabled;
        }

        public void setWatermarkEnabled(boolean watermarkEnabled) {
            this.watermarkEnabled = watermarkEnabled;
        }

        public boolean isPanelBannerEnabled() {
            return panelBannerEnabled;
        }

        public void setPanelBannerEnabled(boolean panelBannerEnabled) {
            this.panelBannerEnabled = panelBannerEnabled;
        }

        public boolean isPieceHeaderBannerEnabled() {
            return pieceHeaderBannerEnabled;
        }

        public void setPieceHeaderBannerEnabled(boolean pieceHeaderBannerEnabled) {
            this.pieceHeaderBannerEnabled = pieceHeaderBannerEnabled;
        }

        public boolean isPrintHeaderEnabled() {
            return printHeaderEnabled;
        }

        public void setPrintHeaderEnabled(boolean printHeaderEnabled) {
            this.printHeaderEnabled = printHeaderEnabled;
        }
    }
}
