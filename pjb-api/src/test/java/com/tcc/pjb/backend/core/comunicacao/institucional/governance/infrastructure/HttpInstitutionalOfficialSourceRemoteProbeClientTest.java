package com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import org.junit.jupiter.api.Test;

class HttpInstitutionalOfficialSourceRemoteProbeClientTest {

    @Test
    void shouldProbeDataJudWithApiKeyAndParsePayload() {
        InstitutionalOfficialSourceConnectorProperties properties = new InstitutionalOfficialSourceConnectorProperties();
        InstitutionalOfficialSourceConnectorProperties.SourceConfig config = new InstitutionalOfficialSourceConnectorProperties.SourceConfig();
        config.setApiKey("public-test-key");
        properties.getSources().put("CNJ_DATAJUD", config);
        RecordingHttpClient httpClient = new RecordingHttpClient(200, "{\"timed_out\":false,\"_shards\":{\"failed\":0},\"hits\":{\"total\":{\"value\":0}}}");
        HttpInstitutionalOfficialSourceRemoteProbeClient client = new HttpInstitutionalOfficialSourceRemoteProbeClient(httpClient, new ObjectMapper(), properties);

        var result = client.probe("CNJ_DATAJUD", URI.create("https://api-publica.datajud.cnj.jus.br/api_publica_trf1/_search"), Duration.ofSeconds(3));

        assertTrue(result.reachable());
        assertEquals("APIKey public-test-key", httpClient.lastAuthorizationHeader);
        assertEquals("POST", httpClient.lastMethod);
        assertTrue(result.signals().stream().anyMatch(value -> value.startsWith("datajud_hits_total=")));
    }

    @Test
    void shouldMarkDataJudAsBlockedWhenApiKeyIsMissing() {
        HttpInstitutionalOfficialSourceRemoteProbeClient client = new HttpInstitutionalOfficialSourceRemoteProbeClient(new RecordingHttpClient(200, "{}"), new ObjectMapper(), new InstitutionalOfficialSourceConnectorProperties());

        var result = client.probe("CNJ_DATAJUD", URI.create("https://api-publica.datajud.cnj.jus.br/api_publica_trf1/_search"), Duration.ofSeconds(3));

        assertFalse(result.reachable());
        assertTrue(result.blockers().contains("datajud_api_key_missing"));
    }

    @Test
    void shouldProbeIbgeAndParseMunicipalityPayload() {
        String payload = "{\"id\":2304400,\"nome\":\"Fortaleza\"}";
        HttpInstitutionalOfficialSourceRemoteProbeClient client = new HttpInstitutionalOfficialSourceRemoteProbeClient(new RecordingHttpClient(200, payload), new ObjectMapper(), new InstitutionalOfficialSourceConnectorProperties());

        var result = client.probe("IBGE_OU_TOPOLOGIA_CNJ", URI.create("https://servicodados.ibge.gov.br/api/v1/localidades/municipios/2304400"), Duration.ofSeconds(3));

        assertTrue(result.reachable());
        assertTrue(result.signals().contains("ibge_id=2304400"));
        assertTrue(result.signals().contains("ibge_nome=Fortaleza"));
    }

    private static final class RecordingHttpClient extends HttpClient {
        private final int statusCode;
        private final String body;
        private String lastAuthorizationHeader;
        private String lastMethod;

        private RecordingHttpClient(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
            this.lastMethod = request.method();
            this.lastAuthorizationHeader = request.headers().firstValue("Authorization").orElse(null);
            Object responseBody = responseBodyHandler == HttpResponse.BodyHandlers.discarding() ? null : body;
            return new SimpleHttpResponse<>(request, statusCode, (T) responseBody);
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler, HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NORMAL;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return new SSLParameters();
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<java.util.concurrent.Executor> executor() {
            return Optional.empty();
        }
    }

    private record SimpleHttpResponse<T>(HttpRequest request, int statusCode, T body) implements HttpResponse<T> {
        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(java.util.Map.of(), (a, b) -> true);
        }

        @Override
        public URI uri() {
            return request.uri();
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}
