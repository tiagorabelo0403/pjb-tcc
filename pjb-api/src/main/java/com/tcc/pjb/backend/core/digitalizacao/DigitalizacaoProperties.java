package com.tcc.pjb.backend.core.digitalizacao;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.digitalizacao")
public class DigitalizacaoProperties {

    private boolean enabled = false;
    private double confiancaMinimaAuto = 70.0d;
    private String idiomaDefault = "por";
    private String tesseractExecutable;
    private int reviewBatchSize = 20;
    private long staleProcessingMinutes = 60L;
    private long governanceFixedRateMs = 300_000L;

    public DigitalizacaoProperties() {
    }

    public DigitalizacaoProperties(
            boolean enabled,
            double confiancaMinimaAuto,
            String idiomaDefault,
            int reviewBatchSize,
            long staleProcessingMinutes,
            long governanceFixedRateMs) {
        this.enabled = enabled;
        this.confiancaMinimaAuto = confiancaMinimaAuto;
        this.idiomaDefault = idiomaDefault == null || idiomaDefault.isBlank() ? "por" : idiomaDefault;
        this.reviewBatchSize = reviewBatchSize;
        this.staleProcessingMinutes = staleProcessingMinutes;
        this.governanceFixedRateMs = governanceFixedRateMs;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double confiancaMinimaAuto() {
        return confiancaMinimaAuto;
    }

    public double getConfiancaMinimaAuto() {
        return confiancaMinimaAuto;
    }

    public void setConfiancaMinimaAuto(double confiancaMinimaAuto) {
        this.confiancaMinimaAuto = confiancaMinimaAuto;
    }

    public String idiomaDefault() {
        return idiomaDefault;
    }

    public String getIdiomaDefault() {
        return idiomaDefault;
    }

    public void setIdiomaDefault(String idiomaDefault) {
        this.idiomaDefault = idiomaDefault;
    }

    public String tesseractExecutable() {
        return tesseractExecutable;
    }

    public String getTesseractExecutable() {
        return tesseractExecutable;
    }

    public void setTesseractExecutable(String tesseractExecutable) {
        this.tesseractExecutable = tesseractExecutable;
    }

    public int reviewBatchSize() {
        return reviewBatchSize;
    }

    public int getReviewBatchSize() {
        return reviewBatchSize;
    }

    public void setReviewBatchSize(int reviewBatchSize) {
        this.reviewBatchSize = reviewBatchSize;
    }

    public long staleProcessingMinutes() {
        return staleProcessingMinutes;
    }

    public long getStaleProcessingMinutes() {
        return staleProcessingMinutes;
    }

    public void setStaleProcessingMinutes(long staleProcessingMinutes) {
        this.staleProcessingMinutes = staleProcessingMinutes;
    }

    public long governanceFixedRateMs() {
        return governanceFixedRateMs;
    }

    public long getGovernanceFixedRateMs() {
        return governanceFixedRateMs;
    }

    public void setGovernanceFixedRateMs(long governanceFixedRateMs) {
        this.governanceFixedRateMs = governanceFixedRateMs;
    }

    public String idioma() {
        return idiomaDefault;
    }
}
