package com.tcc.pjb.backend.service.infra;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelMaterializationTrail;
import com.tcc.pjb.backend.model.entity.infra.ProcessualReadModelProjection;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelMaterializationTrailRepository;
import com.tcc.pjb.backend.model.repository.ProcessualReadModelProjectionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbProcessualReadModelPersistenceService {

    private final ProcessualReadModelProjectionRepository projectionRepository;
    private final ProcessualReadModelMaterializationTrailRepository trailRepository;
    private final PjbDataSourceRoutingProperties properties;
    private final ObjectMapper objectMapper;

    public PjbProcessualReadModelPersistenceService(ProcessualReadModelProjectionRepository projectionRepository,
                                                    ProcessualReadModelMaterializationTrailRepository trailRepository,
                                                    PjbDataSourceRoutingProperties properties,
                                                    ObjectMapper objectMapper) {
        this.projectionRepository = Objects.requireNonNull(projectionRepository, "projectionRepository");
        this.trailRepository = Objects.requireNonNull(trailRepository, "trailRepository");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Transactional
    public MaterializationResult materialize(String domain,
                                             String eventType,
                                             String aggregateType,
                                             String aggregateId,
                                             String source,
                                             Map<String, Object> payload) {
        if (!properties.getProcessualReadModels().isPersistenceEnabled()) {
            return new MaterializationResult(normalizeDomain(domain), null, 0L, null, null, "DISABLED");
        }
        Map<String, Object> safePayload = sanitizePayload(payload);
        Instant now = Instant.now();
        String domainKey = normalizeDomain(domain);
        String eventTypeKey = normalize(eventType);
        String aggregateTypeKey = normalize(aggregateType);
        String aggregateIdKey = trimToNull(aggregateId);
        String tribunalCode = firstNonBlank(normalize(extract(safePayload, "tribunalCode", "tribunal", "tribunalSigla", "orgaoTribunal")), null);
        String ramoCode = firstNonBlank(normalize(extract(safePayload, "ramoCode", "ramo", "ramoDireito", "ramoSigla")), null);
        String scopeKey = buildScopeKey(safePayload, tribunalCode, ramoCode, null);
        String materializationKey = buildMaterializationKey(domainKey, aggregateTypeKey, aggregateIdKey, safePayload, scopeKey);
        String payloadJson = toJson(safePayload);
        String payloadHash = sha256(domainKey + '|' + materializationKey + '|' + payloadJson);
        ProcessualReadModelProjection entity = projectionRepository.findByDomainIgnoreCaseAndMaterializationKeyIgnoreCase(domainKey, materializationKey)
                .orElseGet(ProcessualReadModelProjection::new);
        long previousVersion = entity.getProjectionVersion() == null ? 0L : Math.max(0L, entity.getProjectionVersion());
        long newVersion = previousVersion + 1L;
        entity.setDomain(domainKey);
        entity.setMaterializationKey(materializationKey);
        entity.setAggregateType(firstNonBlank(aggregateTypeKey, entity.getAggregateType()));
        entity.setAggregateId(firstNonBlank(aggregateIdKey, fallbackAggregateId(safePayload), entity.getAggregateId()));
        entity.setTribunalCode(firstNonBlank(tribunalCode, entity.getTribunalCode()));
        entity.setRamoCode(firstNonBlank(ramoCode, entity.getRamoCode()));
        entity.setScopeKey(buildScopeKey(safePayload, entity.getTribunalCode(), entity.getRamoCode(), entity.getScopeKey()));
        entity.setProjectionVersion(newVersion);
        entity.setLastEventType(firstNonBlank(eventTypeKey, entity.getLastEventType()));
        entity.setSource(firstNonBlank(normalize(source), entity.getSource()));
        entity.setPayloadHash(payloadHash);
        entity.setPayloadJson(payloadJson);
        entity.setStatus("MATERIALIZED");
        entity.setFreshnessAt(now);
        entity.setLastMaterializedAt(now);
        ProcessualReadModelProjection saved = projectionRepository.save(entity);
        trailRepository.save(buildTrail(saved, previousVersion, eventTypeKey, payloadHash, payloadJson, "MATERIALIZED", aggregateTypeKey, aggregateIdKey, source, null, now));
        return new MaterializationResult(saved.getDomain(), saved.getMaterializationKey(), saved.getProjectionVersion(), saved.getTribunalCode(), saved.getRamoCode(), saved.getStatus());
    }

    @Transactional
    public RecompositionResult recompose(String domain,
                                         String tribunalCode,
                                         String ramoCode,
                                         String scopeKey,
                                         String source,
                                         String reason) {
        if (!properties.getProcessualReadModels().isPersistenceEnabled()) {
            return new RecompositionResult(normalizeDomain(domain), 0, "DISABLED");
        }
        String domainKey = normalizeDomain(domain);
        String tribunalKey = normalizeNullable(tribunalCode);
        String ramoKey = normalizeNullable(ramoCode);
        String scopeKeyNormalized = normalizeNullable(scopeKey);
        List<ProcessualReadModelProjection> projections = projectionRepository.findForRecomposition(
                domainKey,
                tribunalKey == null ? "" : tribunalKey,
                ramoKey == null ? "" : ramoKey,
                scopeKeyNormalized == null ? "" : scopeKeyNormalized,
                PageRequest.of(0, Math.max(1, properties.getProcessualReadModels().getRecompositionBatchSize()))
        );
        Instant now = Instant.now();
        int affected = 0;
        for (ProcessualReadModelProjection projection : projections) {
            if (projection == null) {
                continue;
            }
            long previousVersion = projection.getProjectionVersion() == null ? 0L : Math.max(0L, projection.getProjectionVersion());
            projection.setProjectionVersion(previousVersion + 1L);
            projection.setStatus("RECOMPOSED");
            projection.setFreshnessAt(now);
            projection.setLastMaterializedAt(now);
            projection.setLastRecompositionRequestedAt(now);
            ProcessualReadModelProjection saved = projectionRepository.save(projection);
            trailRepository.save(buildTrail(saved, previousVersion, "RECOMPOSITION", saved.getPayloadHash(), saved.getPayloadJson(), "RECOMPOSED",
                    saved.getAggregateType(), saved.getAggregateId(), source, reason, now));
            affected++;
        }
        return new RecompositionResult(domainKey, affected, affected == 0 ? "NO_TARGETS" : "RECOMPOSED");
    }

    private ProcessualReadModelMaterializationTrail buildTrail(ProcessualReadModelProjection projection,
                                                               long previousVersion,
                                                               String eventType,
                                                               String payloadHash,
                                                               String payloadJson,
                                                               String status,
                                                               String aggregateType,
                                                               String aggregateId,
                                                               String source,
                                                               String notes,
                                                               Instant occurredAt) {
        ProcessualReadModelMaterializationTrail trail = new ProcessualReadModelMaterializationTrail();
        trail.setProjectionDomain(projection.getDomain());
        trail.setProjectionKey(projection.getMaterializationKey());
        trail.setProjectionVersion(projection.getProjectionVersion());
        trail.setPreviousVersion(previousVersion <= 0L ? null : previousVersion);
        trail.setEventType(eventType);
        trail.setAggregateType(aggregateType);
        trail.setAggregateId(aggregateId);
        trail.setTribunalCode(projection.getTribunalCode());
        trail.setRamoCode(projection.getRamoCode());
        trail.setSource(normalize(source));
        trail.setMaterializationHash(payloadHash);
        trail.setMaterializationStatus(status);
        trail.setPayloadSnapshotJson(payloadJson);
        trail.setNotes(trimToNull(notes));
        trail.setOccurredAt(occurredAt == null ? Instant.now() : occurredAt);
        return trail;
    }

    private Map<String, Object> sanitizePayload(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        payload.forEach((key, value) -> {
            if (key != null) {
                sanitized.put(String.valueOf(key), value);
            }
        });
        return sanitized.isEmpty() ? Map.of() : sanitized;
    }

    private String buildMaterializationKey(String domain,
                                           String aggregateType,
                                           String aggregateId,
                                           Map<String, Object> payload,
                                           String scopeKey) {
        String specific = switch (domain) {
            case "PROCESSO_TIMELINE_HOT" -> firstNonBlank(extract(payload, "numeroProcesso", "numero", "numeroUnificado", "processoId"), aggregateId);
            case "AUDIENCIA_AGENDA" -> firstNonBlank(extract(payload, "audienciaId", "sessaoId", "pautaId"), aggregateId);
            case "PETICIONAMENTO_WORKSPACE" -> firstNonBlank(extract(payload, "workspaceId", "protocoloId", "peticionamentoId"), aggregateId);
            default -> firstNonBlank(aggregateId, extract(payload, "id", "numero", "codigo"));
        };
        String normalizedSpecific = normalizeNullable(specific);
        if (normalizedSpecific != null) {
            return domain + "|" + normalizedSpecific;
        }
        String aggregatePart = firstNonBlank(aggregateType, "GENERIC");
        String scopePart = firstNonBlank(scopeKey, "GLOBAL");
        return domain + "|" + aggregatePart + "|" + scopePart + "|" + sha256(toJson(payload));
    }

    private String buildScopeKey(Map<String, Object> payload,
                                 String tribunalCode,
                                 String ramoCode,
                                 String fallback) {
        String tribunal = firstNonBlank(normalize(extract(payload, "tribunalCode", "tribunal", "tribunalSigla")), normalizeNullable(tribunalCode));
        String ramo = firstNonBlank(normalize(extract(payload, "ramoCode", "ramo", "ramoDireito")), normalizeNullable(ramoCode));
        String processo = trimToNull(extract(payload, "numeroProcesso", "numero", "numeroUnificado"));
        String workspace = trimToNull(extract(payload, "workspaceId", "protocoloId"));
        String scope = firstNonBlank(joinNonBlank("|", tribunal, ramo, processo), joinNonBlank("|", tribunal, ramo, workspace), normalizeNullable(fallback));
        return scope == null ? null : scope;
    }

    private String fallbackAggregateId(Map<String, Object> payload) {
        return firstNonBlank(extract(payload, "aggregateId", "processoId", "workspaceId", "audienciaId", "id"), null);
    }

    private String extract(Map<String, Object> payload, String... keys) {
        if (payload == null || payload.isEmpty() || keys == null) {
            return null;
        }
        for (String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return null;
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload == null ? Map.of() : payload);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                out.append(Character.forDigit((current >> 4) & 0xF, 16));
                out.append(Character.forDigit(current & 0xF, 16));
            }
            return out.toString().toUpperCase(Locale.ROOT);
        } catch (Exception ex) {
            return Integer.toHexString(Objects.hashCode(value)).toUpperCase(Locale.ROOT);
        }
    }

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ROOT);
    }

    private String normalizeDomain(String value) {
        String normalized = normalize(value);
        return normalized == null ? "UNKNOWN_DOMAIN" : normalized.replace('-', '_').replace(' ', '_');
    }

    private String normalizeNullable(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.replace('-', '_').replace(' ', '_');
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String joinNonBlank(String delimiter, String... values) {
        if (values == null || values.length == 0) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(delimiter);
            }
            out.append(value);
        }
        return out.isEmpty() ? null : out.toString();
    }

    public record MaterializationResult(
            String domain,
            String materializationKey,
            long version,
            String tribunalCode,
            String ramoCode,
            String status
    ) {
    }

    public record RecompositionResult(
            String domain,
            int affectedProjections,
            String status
    ) {
    }
}
