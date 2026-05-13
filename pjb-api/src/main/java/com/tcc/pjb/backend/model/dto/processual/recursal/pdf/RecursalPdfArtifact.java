package com.tcc.pjb.backend.model.dto.processual.recursal.pdf;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public record RecursalPdfArtifact(
        byte[] bytes,
        String filename,
        String mediaType,
        String sha256,
        int pageCount,
        Map<String, Object> metadata) {

    public RecursalPdfArtifact {
        bytes = bytes == null ? new byte[0] : bytes.clone();
        filename = normalize(filename);
        mediaType = normalize(mediaType);
        sha256 = normalize(sha256);
        metadata = sanitizeMap(metadata);
    }

    public static RecursalPdfArtifact unavailable() {
        return new RecursalPdfArtifact(new byte[0], null, null, null, 0, Map.of());
    }

    public boolean available() {
        return bytes.length > 0 && filename != null && mediaType != null && sha256 != null;
    }

    public long sizeBytes() {
        return bytes.length;
    }

    public String contentBase64() {
        return bytes.length == 0 ? null : Base64.getEncoder().encodeToString(bytes);
    }

    public RecursalPdfArtifact withMergedMetadata(Map<String, Object> extraMetadata) {
        if (extraMetadata == null || extraMetadata.isEmpty()) {
            return this;
        }
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>(metadata);
        merged.putAll(extraMetadata);
        return new RecursalPdfArtifact(bytes, filename, mediaType, sha256, pageCount, sanitizeMap(merged));
    }

    public Map<String, Object> toMap() {
        if (!available()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("filename", filename);
        out.put("mediaType", mediaType);
        out.put("sha256", sha256);
        out.put("pageCount", pageCount);
        out.put("sizeBytes", bytes.length);
        out.put("contentBase64", contentBase64());
        if (!metadata.isEmpty()) {
            out.put("metadata", metadata);
        }
        return Map.copyOf(out);
    }

    private static Map<String, Object> sanitizeMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        value.forEach((key, item) -> {
            if (key != null && item != null) {
                sanitized.put(key, item);
            }
        });
        return sanitized.isEmpty() ? Map.of() : Map.copyOf(sanitized);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
