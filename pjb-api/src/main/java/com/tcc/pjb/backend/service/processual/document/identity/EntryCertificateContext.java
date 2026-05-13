package com.tcc.pjb.backend.service.processual.document.identity;

import java.time.Instant;
import java.util.List;
import java.util.Map;


    public record EntryCertificateContext(
            boolean presente,
            String source,
            String fingerprintSha256,
            String subjectRfc2253,
            String issuerRfc2253,
            String serialNumber,
            Map<String, String> subjectAttributes,
            List<String> roleHints,
            List<String> jurisdictionHints,
            Map<String, Object> payload
    ) {
    }
