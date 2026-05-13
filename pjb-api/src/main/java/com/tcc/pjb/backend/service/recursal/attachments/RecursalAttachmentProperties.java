package com.tcc.pjb.backend.service.recursal.attachments;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.recursal.attachments")
public class RecursalAttachmentProperties {

    private boolean enabled = true;

    
    private String localPath = "./data/recursal-attachments";

    
    private long maxUploadBytes = 268_435_456L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        if (localPath != null && !localPath.isBlank()) {
            this.localPath = localPath.trim();
        }
    }

    public long getMaxUploadBytes() {
        return maxUploadBytes;
    }

    public void setMaxUploadBytes(long maxUploadBytes) {
        if (maxUploadBytes > 0) {
            this.maxUploadBytes = maxUploadBytes;
        }
    }
}
