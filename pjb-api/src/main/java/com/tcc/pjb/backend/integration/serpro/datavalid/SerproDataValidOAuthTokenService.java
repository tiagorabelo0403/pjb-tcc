package com.tcc.pjb.backend.integration.serpro.datavalid;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerproDataValidOAuthTokenService {

    private static final Logger log = LoggerFactory.getLogger(SerproDataValidOAuthTokenService.class);
    private static final Pattern ACCESS_TOKEN_PATTERN = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern EXPIRES_IN_PATTERN = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");
    private static final int EXPIRY_BUFFER_SECONDS = 30;

    private final String tokenUrl;
    private final String credentials;
    private final HttpClient httpClient;

    private volatile String cachedToken;
    private volatile Instant tokenExpiry;

    SerproDataValidOAuthTokenService(String tokenUrl, String consumerKey, String consumerSecret, HttpClient httpClient) {
        this.tokenUrl = tokenUrl;
        this.credentials = Base64.getEncoder().encodeToString(
                (consumerKey + ":" + consumerSecret).getBytes(StandardCharsets.UTF_8));
        this.httpClient = httpClient;
    }

    synchronized String getAccessToken() throws IOException, InterruptedException {
        if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
            return cachedToken;
        }
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Falha ao obter token OAuth Serpro: HTTP " + response.statusCode());
        }
        String body = response.body();
        String token = extractJson(body, ACCESS_TOKEN_PATTERN);
        String expiresStr = extractJson(body, EXPIRES_IN_PATTERN);
        if (token == null || token.isBlank()) {
            throw new IOException("Token OAuth Serpro ausente na resposta.");
        }
        long expiresIn = expiresStr != null ? Long.parseLong(expiresStr) : 3600L;
        cachedToken = token;
        tokenExpiry = Instant.now().plusSeconds(Math.max(0, expiresIn - EXPIRY_BUFFER_SECONDS));
        log.debug("Token OAuth Serpro DataValid renovado, validade estimada: {}s", expiresIn);
        return cachedToken;
    }

    private static String extractJson(String body, Pattern pattern) {
        if (body == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(body);
        return matcher.find() ? matcher.group(1) : null;
    }
}
