package com.tcc.pjb.backend.core.db.credentials;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

@SuppressWarnings({"HttpHeaderInspection", "UastIncorrectHttpHeaderInspection"})
public final class VaultDbCredentialsProvider implements DbCredentialsProvider {

    private static final String VAULT_TOKEN_HEADER = "X-Vault-Token";

    private final URI endpoint;
    private final String tokenEnv;
    private final Duration timeout;
    private final ObjectMapper mapper;
    private final HttpClient client;

    public VaultDbCredentialsProvider(String vaultUrl, String vaultPath, String tokenEnv, Duration timeout, ObjectMapper mapper, HttpClient client) {
        this.endpoint = URI.create(vaultUrl.endsWith("/") ? vaultUrl + "v1/" + vaultPath : vaultUrl + "/v1/" + vaultPath);
        this.tokenEnv = tokenEnv;
        this.timeout = timeout;
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    @SuppressWarnings({"HttpHeaderInspection", "UastIncorrectHttpHeaderInspection"})
    public DbCredentials fetch() {
        String token = System.getenv(tokenEnv);
        if (token == null || token.isBlank()) throw new IllegalStateException("missing vault token env: " + tokenEnv);
        try {
            HttpRequest req = HttpRequest.newBuilder(endpoint)
                    .timeout(timeout)
                    .header(VAULT_TOKEN_HEADER, token.trim())
                    .GET()
                    .build();
            HttpResponse<byte[]> resp = client.send(req, HttpResponse.BodyHandlers.ofByteArray());
            if (resp.statusCode() < 200 || resp.statusCode() > 299) throw new IllegalStateException("vault http " + resp.statusCode());
            JsonNode root = mapper.readTree(resp.body());
            JsonNode data = root.path("data");
            String username = data.path("username").asText(null);
            String password = data.path("password").asText(null);
            long ttl = data.path("ttl").asLong(0L);
            if (username == null || username.isBlank()) throw new IllegalStateException("vault missing username");
            if (password == null || password.isBlank()) throw new IllegalStateException("vault missing password");
            return new DbCredentials(username, password.toCharArray(), ttl);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
