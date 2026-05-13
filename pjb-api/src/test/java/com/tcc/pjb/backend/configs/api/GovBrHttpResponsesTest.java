package com.tcc.pjb.backend.configs.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GovBrHttpResponsesTest {

    @Test
    void deveRetornarRedirectEndurecido() {
        var response = GovBrHttpResponses.redirectOrNoContent("https://app.gov.br/callback/sucesso");
        assertEquals(HttpStatus.FOUND, response.getStatusCode());
        assertEquals("no-store, no-cache, max-age=0, must-revalidate", response.getHeaders().getCacheControl());
        assertEquals("no-referrer", response.getHeaders().getFirst("Referrer-Policy"));
    }

    @Test
    void deveRejeitarRedirectInvalido() {
        assertThrows(ResponseStatusException.class, () -> GovBrHttpResponses.redirectOrNoContent("javascript:alert(1)"));
    }
}
