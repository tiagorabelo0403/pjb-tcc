package com.tcc.pjb.backend.core.storage;

import java.net.URI;

public record ObjectWriteResult(
        String key,
        URI uri,
        long sizeBytes,
        String sha256,
        String sha384
) {
}
