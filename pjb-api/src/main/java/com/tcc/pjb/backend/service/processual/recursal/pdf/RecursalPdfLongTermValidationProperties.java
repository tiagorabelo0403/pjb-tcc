package com.tcc.pjb.backend.service.processual.recursal.pdf;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.recursal.long-term")
public record RecursalPdfLongTermValidationProperties(
        Boolean enabled,
        Boolean embedDocumentTimestamp,
        Boolean issueArchiveTimestamp,
        String profileRequested,
        String trustStoreRef,
        Boolean materializeDss,
        Boolean materializeVri,
        Boolean requireRevocationMaterialization,
        Boolean requireExternalActForLta
) {
    public RecursalPdfLongTermValidationProperties {
        enabled = enabled == null ? Boolean.TRUE : enabled;
        embedDocumentTimestamp = embedDocumentTimestamp == null ? Boolean.TRUE : embedDocumentTimestamp;
        issueArchiveTimestamp = issueArchiveTimestamp == null ? Boolean.TRUE : issueArchiveTimestamp;
        profileRequested = profileRequested == null || profileRequested.isBlank() ? "PADES_LTA_EVIDENCE_BUNDLE" : profileRequested.trim();
        trustStoreRef = trustStoreRef == null || trustStoreRef.isBlank() ? null : trustStoreRef.trim();
        materializeDss = materializeDss == null ? Boolean.TRUE : materializeDss;
        materializeVri = materializeVri == null ? Boolean.TRUE : materializeVri;
        requireRevocationMaterialization = requireRevocationMaterialization == null ? Boolean.TRUE : requireRevocationMaterialization;
        requireExternalActForLta = requireExternalActForLta == null ? Boolean.FALSE : requireExternalActForLta;
    }
}
