package com.tcc.pjb.backend.integration.judicial.security;

import com.tcc.pjb.backend.integration.judicial.JudicialIntegrationProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class JudicialConnectorSecureTransport implements JudicialConnectorTransport {

    private static final int MAX_CLIENTS = 512;
    private static final int TARGET_CLIENTS = 384;
    private static final Duration CLIENT_IDLE_TTL = Duration.ofHours(2);

    private final JudicialConnectorCryptographicContextService cryptographicContextService;
    private final JudicialConnectorLowLevelSecurityAuditService auditService;
    private final JudicialConnectorSecurityTelemetryService telemetryService;
    private final JudicialConnectorSecuritySessionService sessionService;
    private final Executor executorService;
    private final ConcurrentMap<ClientKey, ClientHolder> clientCache = new ConcurrentHashMap<>();
    private final AtomicLong cleanupClock = new AtomicLong();

    public JudicialConnectorSecureTransport(JudicialConnectorCryptographicContextService cryptographicContextService,
                                            JudicialConnectorLowLevelSecurityAuditService auditService,
                                            JudicialConnectorSecurityTelemetryService telemetryService,
                                            JudicialConnectorSecuritySessionService sessionService,
                                            @Qualifier("judicialConnectorSecurityExecutor") Executor executorService) {
        this.cryptographicContextService = Objects.requireNonNull(cryptographicContextService);
        this.auditService = Objects.requireNonNull(auditService);
        this.telemetryService = Objects.requireNonNull(telemetryService);
        this.sessionService = Objects.requireNonNull(sessionService);
        this.executorService = Objects.requireNonNull(executorService);
    }

    @Override
    public JudicialSecureHttpResponse exchange(JudicialSystem system,
                                               String tribunalCodigo,
                                               URI targetUri,
                                               JudicialIntegrationProperties.Connector connectorConfig,
                                               JudicialSecureHttpRequest request) {
        Objects.requireNonNull(targetUri);
        Objects.requireNonNull(request);
        Instant startedAt = Instant.now();
        JudicialConnectorCryptographicContext context = cryptographicContextService.resolve(system, tribunalCodigo, targetUri, connectorConfig, request.metadata());
        try {
            HttpClient client = resolveClient(targetUri, context, effectiveConnectTimeout(request, context));
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(targetUri)
                    .timeout(effectiveReadTimeout(request, context));
            request.headers().forEach((name, values) -> values.forEach(value -> requestBuilder.header(name, value)));
            String method = request.method() == null || request.method().isBlank() ? "GET" : request.method().trim().toUpperCase();
            byte[] body = request.body();
            if (body == null || body.length == 0) {
                requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                requestBuilder.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
            }
            HttpResponse<byte[]> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
            Duration duration = Duration.between(startedAt, Instant.now());
            telemetryService.recordHandshake(system, tribunalCodigo, response.statusCode() < 500 ? "SUCCESS" : "REMOTE_FAILURE", duration);
            JudicialSecureHttpResponse secureHttpResponse = new JudicialSecureHttpResponse(response.statusCode(), response.headers().map(), response.body(), Instant.now());
            sessionService.recordSuccess(system, tribunalCodigo, targetUri, context, request, secureHttpResponse, duration);
            return secureHttpResponse;
        } catch (SSLHandshakeException ex) {
            Duration duration = Duration.between(startedAt, Instant.now());
            telemetryService.recordHandshake(system, tribunalCodigo, "HANDSHAKE_FAILURE", duration);
            sessionService.recordFailure(system, tribunalCodigo, targetUri, context, request, duration, ex);
            auditService.recordTransportFailure(system, tribunalCodigo, targetUri, request.operationName(), context.binding(), ex, request.correlationId(), request.metadata());
            throw new JudicialConnectorCryptographicException("TLS handshake failed for judicial connector " + targetUri.getHost() + '.', ex);
        } catch (SSLException ex) {
            Duration duration = Duration.between(startedAt, Instant.now());
            telemetryService.recordHandshake(system, tribunalCodigo, "TLS_FAILURE", duration);
            sessionService.recordFailure(system, tribunalCodigo, targetUri, context, request, duration, ex);
            auditService.recordTransportFailure(system, tribunalCodigo, targetUri, request.operationName(), context.binding(), ex, request.correlationId(), request.metadata());
            throw new JudicialConnectorCryptographicException("Secure TLS failure for judicial connector " + targetUri.getHost() + '.', ex);
        } catch (IOException ex) {
            Duration duration = Duration.between(startedAt, Instant.now());
            telemetryService.recordHandshake(system, tribunalCodigo, "IO_FAILURE", duration);
            sessionService.recordFailure(system, tribunalCodigo, targetUri, context, request, duration, ex);
            auditService.recordTransportFailure(system, tribunalCodigo, targetUri, request.operationName(), context.binding(), ex, request.correlationId(), request.metadata());
            throw new JudicialConnectorCryptographicException("Secure transport I/O failure for judicial connector " + targetUri.getHost() + '.', ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            Duration duration = Duration.between(startedAt, Instant.now());
            telemetryService.recordHandshake(system, tribunalCodigo, "INTERRUPTED", duration);
            sessionService.recordFailure(system, tribunalCodigo, targetUri, context, request, duration, ex);
            auditService.recordTransportFailure(system, tribunalCodigo, targetUri, request.operationName(), context.binding(), ex, request.correlationId(), request.metadata());
            throw new JudicialConnectorCryptographicException("Secure transport execution was interrupted for judicial connector " + targetUri.getHost() + '.', ex);
        }
    }

    private HttpClient resolveClient(URI targetUri, JudicialConnectorCryptographicContext context, Duration connectTimeout) {
        maybeCleanup();
        JudicialResolvedSecurityBinding binding = context.binding();
        String bindingId = binding != null && binding.bindingId() != null ? binding.bindingId() : "NO_BINDING";
        ClientKey key = new ClientKey(
                bindingId,
                targetUri != null ? targetUri.getHost() : null,
                connectTimeout,
                context.transportSecurityEnabled(),
                context.sslContext() != null ? System.identityHashCode(context.sslContext()) : 0,
                context.sslParameters() != null ? System.identityHashCode(context.sslParameters()) : 0
        );
        ClientHolder holder = clientCache.computeIfAbsent(key, ignored -> new ClientHolder(buildClient(context, connectTimeout)));
        holder.touch();
        if (clientCache.size() > MAX_CLIENTS) {
            trimOverflow();
        }
        return holder.client();
    }

    private HttpClient buildClient(JudicialConnectorCryptographicContext context, Duration connectTimeout) {
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .executor(executorService)
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(connectTimeout);
        if (context.transportSecurityEnabled()) {
            clientBuilder.sslContext(context.sslContext());
            clientBuilder.sslParameters(context.sslParameters());
        }
        return clientBuilder.build();
    }

    private Duration effectiveConnectTimeout(JudicialSecureHttpRequest request, JudicialConnectorCryptographicContext context) {
        Duration fallback = context.binding() != null ? context.binding().connectTimeout() : null;
        return firstNonNull(request.requestTimeout(), fallback);
    }

    private Duration effectiveReadTimeout(JudicialSecureHttpRequest request, JudicialConnectorCryptographicContext context) {
        Duration fallback = context.binding() != null ? context.binding().readTimeout() : null;
        return firstNonNull(request.requestTimeout(), fallback);
    }

    private Duration firstNonNull(Duration first, Duration fallback) {
        return first != null ? first : fallback != null ? fallback : Duration.ofSeconds(20);
    }

    private void maybeCleanup() {
        long tick = cleanupClock.incrementAndGet();
        if ((tick & 255L) != 0L && clientCache.size() <= MAX_CLIENTS) {
            return;
        }
        Instant now = Instant.now();
        clientCache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
        if (clientCache.size() > MAX_CLIENTS) {
            trimOverflow();
        }
    }

    private void trimOverflow() {
        int overflow = clientCache.size() - TARGET_CLIENTS;
        if (overflow <= 0) {
            return;
        }
        clientCache.entrySet().stream()
                .sorted(Comparator.comparingLong(entry -> entry.getValue().lastTouchedAt()))
                .limit(overflow)
                .map(java.util.Map.Entry::getKey)
                .toList()
                .forEach(clientCache::remove);
    }

    private record ClientKey(String bindingId,
                             String host,
                             Duration connectTimeout,
                             boolean transportSecurityEnabled,
                             int sslContextIdentity,
                             int sslParametersIdentity) {
    }

    private static final class ClientHolder {

        private final HttpClient client;
        private final AtomicLong lastTouchedAt = new AtomicLong(System.currentTimeMillis());

        private ClientHolder(HttpClient client) {
            this.client = Objects.requireNonNull(client);
        }

        private HttpClient client() {
            return client;
        }

        private void touch() {
            lastTouchedAt.set(System.currentTimeMillis());
        }

        private long lastTouchedAt() {
            return lastTouchedAt.get();
        }

        private boolean isExpired(Instant now) {
            return Instant.ofEpochMilli(lastTouchedAt()).plus(CLIENT_IDLE_TTL).isBefore(now);
        }
    }
}
