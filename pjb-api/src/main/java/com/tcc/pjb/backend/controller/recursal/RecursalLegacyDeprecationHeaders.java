package com.tcc.pjb.backend.controller.recursal;

import org.springframework.http.HttpHeaders;

public final class RecursalLegacyDeprecationHeaders {

    private static final String SUNSET = "Tue, 28 Oct 2026 00:00:00 GMT";

    private RecursalLegacyDeprecationHeaders() {
    }

    public static HttpHeaders forProcesso(Long processoId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Deprecation", "true");
        headers.add("Link", "</api/v1/recursal/processos/" + processoId + "/recurso>; rel=\"successor-version\"");
        headers.add("Sunset", SUNSET);
        return headers;
    }
}
