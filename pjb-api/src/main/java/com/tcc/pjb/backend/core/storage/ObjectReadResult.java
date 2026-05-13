package com.tcc.pjb.backend.core.storage;

import org.springframework.core.io.Resource;

public record ObjectReadResult(
        Resource resource,
        long contentLength,
        String contentType
) {
}
