
package com.tcc.pjb.backend.service.infra;

import java.util.Collections;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.lgpd.PjbProcessoSigiloRlsEntryPointSupport;
import com.tcc.pjb.backend.service.outbox.OutboxGenericDispatchedEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class PjbProcessualReadModelProjector {

    private final PjbProcessualReadModelMeshService meshService;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<PjbProcessualReadModelPersistenceService> persistenceServiceProvider;
    private final PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport;

    public PjbProcessualReadModelProjector(PjbProcessualReadModelMeshService meshService,
                                           CacheManager cacheManager,
                                           ObjectMapper objectMapper,
                                           ObjectProvider<PjbProcessualReadModelPersistenceService> persistenceServiceProvider,
                                           PjbProcessoSigiloRlsEntryPointSupport processoSigiloRlsEntryPointSupport) {
        this.meshService = Objects.requireNonNull(meshService, "meshService");
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.persistenceServiceProvider = Objects.requireNonNull(persistenceServiceProvider, "persistenceServiceProvider");
        this.processoSigiloRlsEntryPointSupport = Objects.requireNonNull(processoSigiloRlsEntryPointSupport, "processoSigiloRlsEntryPointSupport");
    }

    @EventListener
    public void onOutboxDispatched(OutboxGenericDispatchedEvent event) {
        if (event == null || event.eventType() == null || event.eventType().isBlank()) {
            return;
        }
        ingest("OUTBOX", event.eventType(), event.aggregateType(), event.aggregateId(), parse(event.payloadJson()));
    }

    public void ingest(String source,
                       String eventType,
                       String aggregateType,
                       String aggregateId,
                       Map<String, Object> payload) {
        Long processoId = resolveProcessoId(payload, aggregateType, aggregateId);
        processoSigiloRlsEntryPointSupport.runWithProcessoContext(processoId, source, () -> ingestBound(source, eventType, aggregateType, aggregateId, payload));
    }

    private void ingestBound(String source,
                             String eventType,
                             String aggregateType,
                             String aggregateId,
                             Map<String, Object> payload) {
        List<PjbProcessualReadModelMeshService.ReadModelDefinition> domains = meshService.domainsForEventType(eventType);
        for (PjbProcessualReadModelMeshService.ReadModelDefinition domain : domains) {
            meshService.registerEvent(domain.domain(), eventType, aggregateType, aggregateId, source);
            invalidateDomainCaches(domain, payload);
            PjbProcessualReadModelPersistenceService persistenceService = persistenceServiceProvider.getIfAvailable();
            if (persistenceService != null) {
                persistenceService.materialize(domain.domain(), eventType, aggregateType, aggregateId, source, payload);
            }
        }
    }

    private void invalidateDomainCaches(PjbProcessualReadModelMeshService.ReadModelDefinition domain,
                                        Map<String, Object> payload) {
        for (String cacheName : domain.cacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache == null) {
                continue;
            }
            if ("public_timeline".equals(cacheName)) {
                String numero = firstNonBlank(string(payload, "numeroProcesso"), string(payload, "numero"), string(payload, "numeroUnificado"));
                if (numero != null) {
                    cache.evict(numero);
                    continue;
                }
            }
            Object nativeCache = cache.getNativeCache();
            if (nativeCache instanceof Map<?, ?> nativeMap) {
                nativeMap.clear();
                continue;
            }
            try {
                nativeCache.getClass().getMethod("clear").invoke(nativeCache);
            } catch (Exception ignored) {
                try {
                    cache.evict(domain.domain());
                } catch (Exception ignoredAgain) {
                }
            }
        }
    }

    private Map<String, Object> parse(String payloadJson) {
        try {
            if (payloadJson == null || payloadJson.isBlank()) {
                return Map.of();
            }
            Map<?, ?> parsed = objectMapper.readValue(payloadJson, Map.class);
            if (parsed == null || parsed.isEmpty()) {
                return Map.of();
            }
            java.util.LinkedHashMap<String, Object> out = new java.util.LinkedHashMap<>();
            parsed.forEach((key, value) -> {
                if (key != null) {
                    out.put(String.valueOf(key), value);
                }
            });
            return Collections.unmodifiableMap(out);
        } catch (Exception ex) {
            return Map.of();
        }
    }

    private Long resolveProcessoId(Map<String, Object> payload,
                                  String aggregateType,
                                  String aggregateId) {
        Long explicit = longValue(extractProcessId(payload));
        if (explicit != null) {
            return explicit;
        }
        if (aggregateType != null && aggregateType.equalsIgnoreCase("PROCESSO")) {
            return longValue(aggregateId);
        }
        return null;
    }

    private String extractProcessId(Map<String, Object> payload) {
        return firstNonBlank(
                extract(payload, "processoId", "processId", "processoLocalId"),
                null
        );
    }

    private Long longValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String string(Map<String, Object> payload, String key) {
        if (payload == null || key == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String extract(Map<String, Object> payload, String... keys) {
        if (payload == null || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null) continue;
            Object value = payload.get(key);
            if (value != null) {
                String text = String.valueOf(value).trim();
                if (!text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }
}
