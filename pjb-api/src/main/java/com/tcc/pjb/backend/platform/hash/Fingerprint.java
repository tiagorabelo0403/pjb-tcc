package com.tcc.pjb.backend.platform.hash;

import java.time.Instant;

public record Fingerprint(
        String sha256,
        int jsonBytes,
        int gzipBytes,
        Instant generatedAt
) {

    public String getSha256() {
        return sha256();
    }

    public int getJsonBytes() {
        return jsonBytes();
    }

    public int getJsonLength() {
        return jsonBytes();
    }

    public int getGzipBytes() {
        return gzipBytes();
    }

    public int getGzipLength() {
        return gzipBytes();
    }

    public int getGzLength() {
        return gzipBytes();
    }

    public Instant getGeneratedAt() {
        return generatedAt();
    }

    public Instant getCreatedAt() {
        return generatedAt();
    }

    public int jsonLength() {
        return jsonBytes();
    }

    public int gzLength() {
        return gzipBytes();
    }

    public Instant createdAt() {
        return generatedAt();
    }
}
