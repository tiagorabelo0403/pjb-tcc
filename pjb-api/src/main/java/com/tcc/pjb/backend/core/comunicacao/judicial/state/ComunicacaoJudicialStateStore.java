package com.tcc.pjb.backend.core.comunicacao.judicial.state;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;

@Service
public class ComunicacaoJudicialStateStore {

    private static final String CACHE_BY_KEY = "comJudStateByKey";
    private static final String CACHE_BY_DOMAIN = "comJudStateByDomain";
    private static final String CACHE_BY_SECONDARY = "comJudStateBySecondary";
    private static final String CACHE_BY_PROCESSO = "comJudStateByProcesso";
    private static final String CACHE_BY_STATUS = "comJudStateByStatus";
    private static final String NULL_JSON = "\u0000";

    private final ComunicacaoJudicialStateRepository repository;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    public ComunicacaoJudicialStateStore(ComunicacaoJudicialStateRepository repository,
                                         ObjectMapper objectMapper,
                                         CacheManager cacheManager) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
    }

    @Transactional
    public <T> T save(String domain,
                      String stateKey,
                      String secondaryKey,
                      T payload,
                      Long processoId,
                      String expedicaoUuid,
                      String mandadoId,
                      String statusCode) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(stateKey, "stateKey");
        Objects.requireNonNull(payload, "payload");
        ComunicacaoJudicialStateEntry entry = repository.findByDomainNameAndStateKey(domain, stateKey)
                .orElseGet(ComunicacaoJudicialStateEntry::new);
        String payloadJson = toJson(payload);
        entry.setDomainName(domain);
        entry.setStateKey(stateKey);
        entry.setSecondaryKey(secondaryKey);
        entry.setProcessoId(processoId);
        entry.setExpedicaoUuid(expedicaoUuid);
        entry.setMandadoId(mandadoId);
        entry.setStatusCode(statusCode);
        entry.setPayloadJson(payloadJson);
        entry.setHashIntegridade(sha256Hex(domain + "|" + stateKey + "|" + payloadJson));
        repository.save(entry);
        putString(CACHE_BY_KEY, cacheKey(domain, stateKey), payloadJson);
        evictListCaches(domain, secondaryKey, processoId);
        return payload;
    }

    @Transactional(readOnly = true)
    public <T> Optional<T> find(String domain, String stateKey, Class<T> type) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(stateKey, "stateKey");
        Objects.requireNonNull(type, "type");
        String payloadJson = getString(
                CACHE_BY_KEY,
                cacheKey(domain, stateKey),
                () -> repository.findByDomainNameAndStateKey(domain, stateKey).map(ComunicacaoJudicialStateEntry::getPayloadJson).orElse(NULL_JSON)
        );
        if (payloadJson == null || NULL_JSON.equals(payloadJson)) {
            return Optional.empty();
        }
        return Optional.of(fromJson(payloadJson, type));
    }

    @PjbTransactionalBudget(operation = "comunicacao-judicial.state-store.find-all", maxMillis = 3000)
    @Transactional(readOnly = true)
    public <T> List<T> findAll(String domain, Class<T> type) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(type, "type");
        return getJsonList(
                CACHE_BY_DOMAIN,
                domain,
                () -> repository.findByDomainNameOrderByUpdatedAtDesc(domain).stream()
                        .map(ComunicacaoJudicialStateEntry::getPayloadJson)
                        .toList(),
                type
        );
    }

    @Transactional(readOnly = true)
    public <T> List<T> findBySecondaryKey(String domain, String secondaryKey, Class<T> type) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(secondaryKey, "secondaryKey");
        Objects.requireNonNull(type, "type");
        return getJsonList(
                CACHE_BY_SECONDARY,
                domain + '|' + secondaryKey,
                () -> repository.findByDomainNameAndSecondaryKeyOrderByUpdatedAtDesc(domain, secondaryKey).stream()
                        .map(ComunicacaoJudicialStateEntry::getPayloadJson)
                        .toList(),
                type
        );
    }

    @Transactional(readOnly = true)
    public <T> List<T> findByProcessoId(String domain, Long processoId, Class<T> type) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(processoId, "processoId");
        Objects.requireNonNull(type, "type");
        return getJsonList(
                CACHE_BY_PROCESSO,
                domain + '|' + processoId,
                () -> repository.findByDomainNameAndProcessoIdOrderByUpdatedAtDesc(domain, processoId).stream()
                        .map(ComunicacaoJudicialStateEntry::getPayloadJson)
                        .toList(),
                type
        );
    }

    @Transactional(readOnly = true)
    public <T> List<T> findByStatusCode(String domain, String statusCode, Class<T> type) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(statusCode, "statusCode");
        Objects.requireNonNull(type, "type");
        return getJsonList(
                CACHE_BY_STATUS,
                domain + '|' + statusCode,
                () -> repository.findByDomainNameAndStatusCodeOrderByUpdatedAtDesc(domain, statusCode).stream()
                        .map(ComunicacaoJudicialStateEntry::getPayloadJson)
                        .toList(),
                type
        );
    }

    @Transactional(readOnly = true)
    public <T> List<T> findByStatusCodes(String domain, Collection<String> statusCodes, Class<T> type) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(statusCodes, "statusCodes");
        Objects.requireNonNull(type, "type");
        List<String> normalized = statusCodes.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .distinct()
                .sorted()
                .toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        return getJsonList(
                CACHE_BY_STATUS,
                domain + '|' + String.join(",", normalized),
                () -> repository.findByDomainNameAndStatusCodeInOrderByUpdatedAtDesc(domain, normalized).stream()
                        .map(ComunicacaoJudicialStateEntry::getPayloadJson)
                        .toList(),
                type
        );
    }

    @Transactional
    public void delete(String domain, String stateKey) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(stateKey, "stateKey");
        repository.deleteByDomainNameAndStateKey(domain, stateKey);
        putString(CACHE_BY_KEY, cacheKey(domain, stateKey), NULL_JSON);
        clear(CACHE_BY_DOMAIN);
        clear(CACHE_BY_SECONDARY);
        clear(CACHE_BY_PROCESSO);
        clear(CACHE_BY_STATUS);
    }

    @Transactional(readOnly = true)
    public boolean exists(String domain, String stateKey) {
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(stateKey, "stateKey");
        String payloadJson = getString(
                CACHE_BY_KEY,
                cacheKey(domain, stateKey),
                () -> repository.findByDomainNameAndStateKey(domain, stateKey)
                        .map(ComunicacaoJudicialStateEntry::getPayloadJson)
                        .orElse(NULL_JSON)
        );
        return payloadJson != null && !NULL_JSON.equals(payloadJson);
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar estado de comunicação judicial.", e);
        }
    }

    private <T> T fromJson(String payloadJson, Class<T> type) {
        try {
            return objectMapper.readValue(payloadJson, type);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao desserializar estado de comunicação judicial para " + type.getSimpleName(), e);
        }
    }

    private String cacheKey(String domain, String stateKey) {
        return domain + '|' + stateKey;
    }

    private void evictListCaches(String domain, String secondaryKey, Long processoId) {
        clear(CACHE_BY_DOMAIN);
        if (secondaryKey != null && !secondaryKey.isBlank()) {
            evict(CACHE_BY_SECONDARY, domain + '|' + secondaryKey);
        } else {
            clear(CACHE_BY_SECONDARY);
        }
        if (processoId != null) {
            evict(CACHE_BY_PROCESSO, domain + '|' + processoId);
        } else {
            clear(CACHE_BY_PROCESSO);
        }
        clear(CACHE_BY_STATUS);
    }

    private String getString(String cacheName, String key, java.util.function.Supplier<String> loader) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return loader.get();
        }
        Cache.ValueWrapper cached = cache.get(key);
        if (cached != null) {
            Object value = cached.get();
            return value instanceof String s ? s : null;
        }
        String value = loader.get();
        cache.put(key, value);
        return value;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getJsonList(String cacheName,
                                    String key,
                                    java.util.function.Supplier<List<String>> loader,
                                    Class<T> type) {
        Cache cache = cacheManager.getCache(cacheName);
        List<String> payloads;
        if (cache == null) {
            payloads = loader.get();
        } else {
            Cache.ValueWrapper cached = cache.get(key);
            if (cached != null && cached.get() instanceof List<?> list) {
                payloads = (List<String>) list;
            } else {
                payloads = loader.get();
                cache.put(key, payloads);
            }
        }
        List<T> result = new ArrayList<>(payloads.size());
        for (String payload : payloads) {
            result.add(fromJson(payload, type));
        }
        return List.copyOf(result);
    }

    private void putString(String cacheName, String key, String value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.put(key, value);
        }
    }

    private void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private static String sha256Hex(String raw) {
        return Hashes.sha256Hex(raw);
    }
}
