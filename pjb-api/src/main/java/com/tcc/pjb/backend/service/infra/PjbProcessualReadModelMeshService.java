package com.tcc.pjb.backend.service.infra;

import com.tcc.pjb.backend.configs.datasource.PjbDataSourceRoutingProperties;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PjbProcessualReadModelMeshService {

    private final PjbDataSourceRoutingProperties properties;
    private final ConcurrentHashMap<String, ProjectionFreshness> freshness = new ConcurrentHashMap<>();

    public PjbProcessualReadModelMeshService(PjbDataSourceRoutingProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public ProcessualReadModelBlueprint blueprint() {
        List<ReadModelDefinition> defaults = defaults();
        return new ProcessualReadModelBlueprint(
                properties.getProcessualReadModels().isEnabled(),
                properties.getProcessualReadModels().getKafkaTopic(),
                defaults.stream()
                        .map(domain -> new DomainBlueprint(
                                domain.domain(),
                                domain.consistencyMode(),
                                domain.sovereignScope(),
                                domain.hotPath(),
                                domain.routePrefixes(),
                                domain.cacheNames(),
                                domain.eventFragments(),
                                freshness.get(normalizeDomain(domain.domain()))
                        ))
                        .toList()
        );
    }

    public List<ReadModelDefinition> defaults() {
        return List.of(
                new ReadModelDefinition(
                        "PROCESSO_TIMELINE_HOT",
                        "MATERIALIZED_CACHE",
                        "PROCESSO",
                        true,
                        List.of("/api/v1/timeline", "/api/v1/processos/resumo", "/api/v1/publico/processos"),
                        List.of("timeline_processo", "public_timeline", "processo_timeline_hot"),
                        List.of("MOVIMENT", "PROTOCOLO", "PETICAO", "PROCESSO", "DOCUMENTO", "SENTENCA", "ACORDAO")
                ),
                new ReadModelDefinition(
                        "AUDIENCIA_AGENDA",
                        "MATERIALIZED_CACHE",
                        "AGENDA",
                        true,
                        List.of("/api/v1/agenda", "/api/v1/calendar", "/api/v1/audiencias"),
                        List.of("audiencia_agenda"),
                        List.of("AUDIENCIA", "SESSAO", "PAUTA", "CUSTODIA")
                ),
                new ReadModelDefinition(
                        "PETICIONAMENTO_WORKSPACE",
                        "MATERIALIZED_CACHE",
                        "PETICIONAMENTO",
                        true,
                        List.of("/api/v1/peticionamento", "/api/v1/processos/protocolo"),
                        List.of("peticionamento_workspace"),
                        List.of("PETICION", "ANEXO", "ASSINAT", "PROTOCOLO", "WORKSPACE")
                )
        );
    }

    public List<ReadModelDefinition> domainsForEventType(String eventType) {
        String normalized = normalize(eventType);
        if (normalized == null) {
            return List.of();
        }
        return defaults().stream()
                .filter(domain -> domain.eventFragments().stream().anyMatch(normalized::contains))
                .toList();
    }

    public ProjectionFreshness registerEvent(String domain,
                                             String eventType,
                                             String aggregateType,
                                             String aggregateId,
                                             String source) {
        ProjectionFreshness updated = new ProjectionFreshness(Instant.now(), normalize(eventType), normalize(aggregateType), aggregateId, normalize(source));
        String normalizedDomain = normalizeDomain(domain);
        if (!managedDomains().contains(normalizedDomain)) {
            return updated;
        }
        freshness.put(normalizedDomain, updated);
        return updated;
    }

    private Set<String> managedDomains() {
        return defaults().stream()
                .map(ReadModelDefinition::domain)
                .map(this::normalizeDomain)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDomain(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    public record ProcessualReadModelBlueprint(
            boolean enabled,
            String kafkaTopic,
            List<DomainBlueprint> domains
    ) {
    }

    public record DomainBlueprint(
            String domain,
            String consistencyMode,
            String sovereignScope,
            boolean hotPath,
            List<String> routePrefixes,
            List<String> cacheNames,
            List<String> eventFragments,
            ProjectionFreshness freshness
    ) {
    }

    public record ProjectionFreshness(
            Instant updatedAt,
            String lastEventType,
            String lastAggregateType,
            String lastAggregateId,
            String source
    ) {
    }

    public record ReadModelDefinition(
            String domain,
            String consistencyMode,
            String sovereignScope,
            boolean hotPath,
            List<String> routePrefixes,
            List<String> cacheNames,
            List<String> eventFragments
    ) {
    }
}
