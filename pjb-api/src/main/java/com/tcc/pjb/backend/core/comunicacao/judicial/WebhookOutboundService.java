package com.tcc.pjb.backend.core.comunicacao.judicial;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import jakarta.annotation.PostConstruct;
import com.tcc.pjb.backend.core.security.crypto.CryptoVaultService;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import com.tcc.pjb.backend.core.comunicacao.judicial.state.ComunicacaoJudicialStateStore;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionDescriptor;
import com.tcc.pjb.backend.platform.runtime.execution.PjbExecutionOrchestrator;

@Service
public class WebhookOutboundService {

    private static final Logger log = LoggerFactory.getLogger(WebhookOutboundService.class);
    private static final String RESOURCE_TYPE = "WEBHOOK_OUTBOUND";
    private static final String DOMAIN_ENDPOINT = "WEBHOOK_ENDPOINT";
    private static final String DOMAIN_DISPATCH = "WEBHOOK_DISPATCH";
    private static final int MAX_TENTATIVAS = 3;
    private static final int TIMEOUT_SEGUNDOS = 10;
    private static final int MAX_HISTORICO_CACHE = 500;
    private static final String HMAC_ALGORITMO = "HmacSHA256";
    private static final String HEADER_PJB_SIGNATURE = "X-PJB-Signature";
    private static final String HEADER_PJB_EVENT = "X-PJB-Event";
    private static final String HEADER_PJB_DELIVERY = "X-PJB-Delivery";

    public enum EventoWebhook {
        EXPEDICAO_EXPEDIDA,
        EXPEDICAO_ENTREGUE_CONFIRMADA,
        EXPEDICAO_LIDA_CONFIRMADA,
        EXPEDICAO_PRESUMIDA_ENTREGUE,
        EXPEDICAO_FRUSTRADA,
        EVASAO_DETECTADA,
        REVELIA_CONFIGURADA,
        CURADOR_NECESSARIO,
        HORA_CERTA_AGENDADA,
        CITACAO_EFETIVADA
    }

    public record WebhookEndpoint(
            String endpointId,
            Long proprietarioId,
            String url,
            String segredoHmac,
            List<EventoWebhook> eventosFiltro,
            boolean ativo,
            Instant cadastradoEm,
            int tentativasFalhaConsecutivas
    ) {
    }

    public record WebhookPayload(
            String payloadId,
            EventoWebhook evento,
            Long processoId,
            String numeroUnificado,
            String expedicaoUuid,
            String statusAtual,
            Instant timestamp,
            Map<String, String> metadados
    ) {
    }

    public record WebhookDispatch(
            String dispatchUuid,
            String endpointId,
            String url,
            EventoWebhook evento,
            int tentativa,
            int statusHttpResposta,
            boolean sucesso,
            long latenciaMs,
            Instant realizadoEm,
            String assinaturaHmac
    ) {
    }

    private static final Duration ENDPOINT_CACHE_TTL = Duration.ofHours(6);

    private final HttpClient httpClient;
    private final AuditLedgerService auditLedger;
    private final PjbExecutionOrchestrator executionOrchestrator;
    private final ComunicacaoJudicialStateStore stateStore;
    private final CryptoVaultService cryptoVaultService;
    private final Cache<String, WebhookEndpoint> endpoints = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(ENDPOINT_CACHE_TTL)
            .build();
    private final Cache<String, List<WebhookEndpoint>> endpointSnapshots = Caffeine.newBuilder()
            .maximumSize(4)
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();
    private final List<WebhookDispatch> historico = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong totalEnviados = new AtomicLong(0);
    private final AtomicLong totalFalhas = new AtomicLong(0);

    public WebhookOutboundService(@Qualifier("hsmInterceptacaoHttpClient") HttpClient httpClient,
                                  AuditLedgerService auditLedger,
                                  PjbExecutionOrchestrator executionOrchestrator,
                                  ComunicacaoJudicialStateStore stateStore,
                                  CryptoVaultService cryptoVaultService) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger");
        this.executionOrchestrator = Objects.requireNonNull(executionOrchestrator, "executionOrchestrator");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.cryptoVaultService = Objects.requireNonNull(cryptoVaultService, "cryptoVaultService");
    }

    @PostConstruct
    void hydratePersistedCaches() {
        List<WebhookEndpoint> persistedEndpoints = stateStore.findAll(DOMAIN_ENDPOINT, WebhookEndpoint.class);
        for (WebhookEndpoint endpoint : persistedEndpoints) {
            endpoints.put(endpoint.endpointId(), endpoint);
        }
        synchronized (historico) {
            historico.clear();
        }
        endpointSnapshots.invalidateAll();
    }

    public WebhookEndpoint cadastrar(Long proprietarioId,
                                     String url,
                                     String segredoHmac,
                                     List<EventoWebhook> eventos) {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(segredoHmac, "segredoHmac");
        URI parsedUrl = validateCallbackUri(url);
        String id = UUID.randomUUID().toString();
        String segredoBlindado = cryptoVaultService.blindarDado(segredoHmac);
        WebhookEndpoint endpoint = new WebhookEndpoint(
                id,
                proprietarioId,
                parsedUrl.toString(),
                segredoBlindado,
                eventos != null ? List.copyOf(eventos) : List.of(EventoWebhook.values()),
                true,
                Instant.now(),
                0
        );
        persistirEndpoint(endpoint);
        auditLedger.appendSafely(
                "WEBHOOK_CADASTRADO",
                RESOURCE_TYPE,
                id,
                sha256Hex(id + url),
                "Webhook cadastrado. host=" + safeHost(parsedUrl.toString())
        );
        log.info("[Webhook] Endpoint cadastrado. id={} host={}", id, safeHost(parsedUrl.toString()));
        return endpoint;
    }

    public void desativar(String endpointId) {
        WebhookEndpoint ep = consultarEndpoint(endpointId).orElse(null);
        if (ep == null) {
            return;
        }
        persistirEndpoint(new WebhookEndpoint(
                ep.endpointId(),
                ep.proprietarioId(),
                ep.url(),
                ep.segredoHmac(),
                ep.eventosFiltro(),
                false,
                ep.cadastradoEm(),
                ep.tentativasFalhaConsecutivas()
        ));
        auditLedger.appendSafely(
                "WEBHOOK_DESATIVADO",
                RESOURCE_TYPE,
                endpointId,
                sha256Hex(endpointId),
                "Desativado."
        );
    }

    public void publicar(WebhookPayload payload) {
        Objects.requireNonNull(payload, "payload");
        endpointsAtivos().stream()
                .filter(ep -> ep.eventosFiltro().isEmpty() || ep.eventosFiltro().contains(payload.evento()))
                .filter(ep -> ep.tentativasFalhaConsecutivas() < 10)
                .forEach(ep -> executionOrchestrator.run(PjbExecutionDescriptor.externalIo("webhook-outbound.publicar", Duration.ofSeconds(TIMEOUT_SEGUNDOS + 2L)), () -> despacharComRetry(ep, payload)));
    }

    public void publicarEventoExpedicao(ExpedicaoJudicial expedicao,
                                        EventoWebhook evento,
                                        Map<String, String> metadados) {
        if (expedicao == null || evento == null) {
            return;
        }
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("modalidade", expedicao.getModalidade() != null ? expedicao.getModalidade().name() : "DESCONHECIDA");
        dados.put("tipoComunicacao", expedicao.getTipoComunicacao() != null ? expedicao.getTipoComunicacao().name() : "DESCONHECIDA");
        dados.put("destinatario", mascararDocumento(expedicao.getDestinatarioDocumento()));
        if (metadados != null && !metadados.isEmpty()) {
            dados.putAll(metadados);
        }
        publicar(new WebhookPayload(
                UUID.randomUUID().toString(),
                evento,
                expedicao.getProcessoId(),
                expedicao.getNumeroUnificado(),
                expedicao.getExpedicaoUuid(),
                expedicao.getStatus() != null ? expedicao.getStatus().name() : null,
                Instant.now(),
                Map.copyOf(dados)
        ));
    }

    public List<WebhookDispatch> historico(String endpointId, int limite) {
        Map<String, WebhookDispatch> consolidados = new LinkedHashMap<>();
        synchronized (historico) {
            historico.stream()
                    .filter(d -> d.endpointId().equals(endpointId))
                    .forEach(dispatch -> consolidados.putIfAbsent(dispatch.dispatchUuid(), dispatch));
        }
        stateStore.findBySecondaryKey(DOMAIN_DISPATCH, endpointId, WebhookDispatch.class).stream()
                .filter(d -> d.endpointId().equals(endpointId))
                .forEach(dispatch -> consolidados.putIfAbsent(dispatch.dispatchUuid(), dispatch));
        return consolidados.values().stream()
                .sorted(Comparator.comparing(WebhookDispatch::realizadoEm, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(Math.max(1, limite))
                .toList();
    }

    public Map<String, Object> metricas() {
        long enviados = totalEnviados.get();
        long falhas = totalFalhas.get();
        return Map.of(
                "totalEndpoints", todosEndpoints().size(),
                "totalAtivos", endpointsAtivos().size(),
                "totalEnviados", enviados,
                "totalFalhas", falhas,
                "taxaSucesso", enviados == 0 ? 1.0 : (double) (enviados - falhas) / enviados
        );
    }



    private List<WebhookEndpoint> todosEndpoints() {
        return endpointSnapshots.get("all", key -> carregarEndpoints(null));
    }

    private List<WebhookEndpoint> endpointsAtivos() {
        return endpointSnapshots.get("active", key -> carregarEndpoints("ATIVO"));
    }

    private List<WebhookEndpoint> carregarEndpoints(String statusCode) {
        List<WebhookEndpoint> persisted = statusCode == null
                ? stateStore.findAll(DOMAIN_ENDPOINT, WebhookEndpoint.class)
                : stateStore.findByStatusCode(DOMAIN_ENDPOINT, statusCode, WebhookEndpoint.class);
        LinkedHashSet<String> vistos = new LinkedHashSet<>();
        List<WebhookEndpoint> consolidados = new ArrayList<>(persisted.size() + endpoints.asMap().size());
        for (WebhookEndpoint endpoint : persisted) {
            if (vistos.add(endpoint.endpointId())) {
                endpoints.put(endpoint.endpointId(), endpoint);
                consolidados.add(endpoint);
            }
        }
        for (WebhookEndpoint endpoint : endpoints.asMap().values()) {
            if ((statusCode == null || endpoint.ativo()) && vistos.add(endpoint.endpointId())) {
                consolidados.add(endpoint);
            }
        }
        return List.copyOf(consolidados);
    }

    private Optional<WebhookEndpoint> consultarEndpoint(String endpointId) {
        WebhookEndpoint cache = endpoints.getIfPresent(endpointId);
        if (cache != null) {
            return Optional.of(cache);
        }
        Optional<WebhookEndpoint> persisted = stateStore.find(DOMAIN_ENDPOINT, endpointId, WebhookEndpoint.class);
        persisted.ifPresent(endpoint -> endpoints.put(endpointId, endpoint));
        return persisted;
    }

    private void persistirEndpoint(WebhookEndpoint endpoint) {
        endpoints.put(endpoint.endpointId(), endpoint);
        endpointSnapshots.invalidateAll();
        stateStore.save(DOMAIN_ENDPOINT, endpoint.endpointId(), String.valueOf(endpoint.proprietarioId()), endpoint, null, null, null, endpoint.ativo() ? "ATIVO" : "INATIVO");
    }

    private void despacharComRetry(WebhookEndpoint endpoint, WebhookPayload payload) {
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            WebhookDispatch dispatch = enviar(endpoint, payload, tentativa);
            appendHistorico(dispatch);
            stateStore.save(DOMAIN_DISPATCH, dispatch.dispatchUuid(), dispatch.endpointId(), dispatch, null, payload.expedicaoUuid(), null, dispatch.sucesso() ? "SUCESSO" : "FALHA");
            totalEnviados.incrementAndGet();
            if (dispatch.sucesso()) {
                auditLedger.appendSafely(
                        "WEBHOOK_ENTREGUE",
                        RESOURCE_TYPE,
                        endpoint.endpointId(),
                        dispatch.assinaturaHmac(),
                        "Evento %s entregue. Status=%d Latencia=%dms".formatted(
                                payload.evento(),
                                dispatch.statusHttpResposta(),
                                dispatch.latenciaMs()
                        )
                );
                persistirEndpoint(new WebhookEndpoint(
                        endpoint.endpointId(),
                        endpoint.proprietarioId(),
                        endpoint.url(),
                        endpoint.segredoHmac(),
                        endpoint.eventosFiltro(),
                        endpoint.ativo(),
                        endpoint.cadastradoEm(),
                        0
                ));
                return;
            }
            totalFalhas.incrementAndGet();
            if (tentativa < MAX_TENTATIVAS) {
                sleepBackoff(tentativa);
            }
        }
        WebhookEndpoint endpointAtualizado = consultarEndpoint(endpoint.endpointId()).orElse(endpoint);
        persistirEndpoint(new WebhookEndpoint(
                endpointAtualizado.endpointId(),
                endpointAtualizado.proprietarioId(),
                endpointAtualizado.url(),
                endpointAtualizado.segredoHmac(),
                endpointAtualizado.eventosFiltro(),
                endpointAtualizado.ativo(),
                endpointAtualizado.cadastradoEm(),
                endpointAtualizado.tentativasFalhaConsecutivas() + 1
        ));
        auditLedger.appendSafely(
                "WEBHOOK_FALHA_TOTAL",
                RESOURCE_TYPE,
                endpoint.endpointId(),
                sha256Hex(endpoint.endpointId() + payload.payloadId()),
                "Evento %s falhou em %d tentativas.".formatted(payload.evento(), MAX_TENTATIVAS)
        );
        log.warn("[Webhook] Falha total. endpoint={} host={} evento={}", endpoint.endpointId(), safeHost(endpoint.url()), payload.evento());
    }

    private void appendHistorico(WebhookDispatch dispatch) {
        synchronized (historico) {
            historico.add(dispatch);
            if (historico.size() > MAX_HISTORICO_CACHE) {
                historico.remove(0);
            }
        }
    }

    private WebhookDispatch enviar(WebhookEndpoint endpoint, WebhookPayload payload, int tentativa) {
        String corpo = serializarPayload(payload);
        String segredoHmac = cryptoVaultService.lerDadoBlindado(endpoint.segredoHmac());
        if (segredoHmac == null || segredoHmac.isBlank()) {
            return new WebhookDispatch(
                    UUID.randomUUID().toString(),
                    endpoint.endpointId(),
                    endpoint.url(),
                    payload.evento(),
                    tentativa,
                    0,
                    false,
                    0L,
                    Instant.now(),
                    sha256Hex(endpoint.endpointId() + ":secret_unavailable")
            );
        }
        String assinatura = gerarHmac(segredoHmac, corpo);
        long inicio = System.currentTimeMillis();
        int statusHttp = 0;
        boolean sucesso = false;
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(validateCallbackUri(endpoint.url()))
                    .header("Content-Type", "application/json")
                    .header(HEADER_PJB_SIGNATURE, "sha256=" + assinatura)
                    .header(HEADER_PJB_EVENT, payload.evento().name())
                    .header(HEADER_PJB_DELIVERY, payload.payloadId())
                    .POST(HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8))
                    .timeout(Duration.ofSeconds(TIMEOUT_SEGUNDOS))
                    .build();
            HttpResponse<Void> resp = httpClient.send(req, HttpResponse.BodyHandlers.discarding());
            statusHttp = resp.statusCode();
            sucesso = statusHttp >= 200 && statusHttp < 300;
        } catch (Exception e) {
            log.debug("[Webhook] Falha tentativa={} endpoint={} erro={}", tentativa, endpoint.endpointId(), e.getMessage());
        }
        return new WebhookDispatch(
                UUID.randomUUID().toString(),
                endpoint.endpointId(),
                endpoint.url(),
                payload.evento(),
                tentativa,
                statusHttp,
                sucesso,
                System.currentTimeMillis() - inicio,
                Instant.now(),
                assinatura
        );
    }

    private URI validateCallbackUri(String callbackUrl) {
        Objects.requireNonNull(callbackUrl, "callbackUrl");
        URI uri = URI.create(callbackUrl.trim()).normalize();
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().trim().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().trim().toLowerCase(Locale.ROOT);
        boolean loopback = "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        if (!("https".equals(scheme) || ("http".equals(scheme) && loopback))) {
            throw new IllegalArgumentException("Webhook callback deve utilizar HTTPS, exceto em loopback controlado.");
        }
        if (host.isBlank() || uri.getUserInfo() != null || uri.getFragment() != null || isUnsafeRemoteHost(host, loopback)) {
            throw new IllegalArgumentException("Webhook callback aponta para destino não permitido.");
        }
        return uri;
    }

    private boolean isUnsafeRemoteHost(String host, boolean loopback) {
        if (loopback) {
            return false;
        }
        String normalized = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || normalized.endsWith(".local") || normalized.endsWith(".internal") || "0.0.0.0".equals(normalized)) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(normalized);
            return address.isAnyLocalAddress()
                    || address.isLoopbackAddress()
                    || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress()
                    || address.isMulticastAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private String safeHost(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl.trim());
            return uri.getHost() == null ? "desconhecido" : uri.getHost().trim().toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "desconhecido";
        }
    }

    private String serializarPayload(WebhookPayload p) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        appendJsonField(sb, "payloadId", p.payloadId(), true);
        appendJsonField(sb, "evento", p.evento() != null ? p.evento().name() : null, true);
        if (p.processoId() != null) {
            sb.append("\"processoId\":").append(p.processoId()).append(',');
        } else {
            sb.append("\"processoId\":null,");
        }
        appendJsonField(sb, "numeroUnificado", p.numeroUnificado(), true);
        appendJsonField(sb, "expedicaoUuid", p.expedicaoUuid(), true);
        appendJsonField(sb, "statusAtual", p.statusAtual(), true);
        appendJsonField(sb, "timestamp", p.timestamp() != null ? p.timestamp().toString() : null, true);
        sb.append("\"metadados\":");
        appendJsonMap(sb, p.metadados());
        sb.append('}');
        return sb.toString();
    }

    private static void appendJsonField(StringBuilder sb, String key, String value, boolean trailingComma) {
        sb.append('"').append(escapeJson(key)).append("\":");
        if (value == null) {
            sb.append("null");
        } else {
            sb.append('"').append(escapeJson(value)).append('"');
        }
        if (trailingComma) {
            sb.append(',');
        }
    }

    private static void appendJsonMap(StringBuilder sb, Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            sb.append("{}");
            return;
        }
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escapeJson(entry.getKey())).append("\":");
            if (entry.getValue() == null) {
                sb.append("null");
            } else {
                sb.append('"').append(escapeJson(entry.getValue())).append('"');
            }
        }
        sb.append('}');
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 32) {
                        sb.append("\\u").append(String.format("%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        return sb.toString();
    }

    private static String gerarHmac(String segredo, String corpo) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITMO);
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITMO));
            byte[] result = mac.doFinal(corpo.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (Exception e) {
            return sha256Hex(segredo + corpo);
        }
    }

    private static void sleepBackoff(int tentativa) {
        try {
            Thread.sleep(Duration.ofSeconds((long) Math.pow(2, tentativa)).toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String sha256Hex(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return Integer.toHexString(Objects.hashCode(raw));
        }
    }

    private static String mascararDocumento(String doc) {
        if (doc == null || doc.length() < 4) {
            return "***";
        }
        if (doc.length() == 11) {
            return doc.substring(0, 3) + ".***.***-" + doc.substring(9);
        }
        if (doc.length() == 14) {
            return doc.substring(0, 2) + ".***.***/****-" + doc.substring(12);
        }
        return doc.substring(0, 2) + "***" + doc.substring(Math.max(2, doc.length() - 2));
    }
}
