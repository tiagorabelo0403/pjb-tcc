package com.tcc.pjb.backend.service.offline.domain;

public record OfflineBundleTimelineHealthView(
        String reference,
        String status,
        String summary
) {

    public String getReference() {
        return reference();
    }

    public String getReferencia() {
        return reference();
    }

    public String getStatus() {
        return status();
    }

    public String getSummary() {
        return summary();
    }

    public String referencia() {
        return reference();
    }
}
