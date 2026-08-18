package com.tcc.pjb.backend.service.procedural;

import com.tcc.pjb.backend.core.catalog.TpuClasseCnj;
import com.tcc.pjb.backend.integration.cnj.CnjTpuSyncService;
import com.tcc.pjb.backend.integration.cnj.CnjTpuSyncService.DivergenceReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class ProceduralArchitectureSanityService {

    public record SanityReport(
            Instant generatedAt,
            boolean healthy,
            int totalRitos,
            int totalClassesTpu,
            int totalTribunais,
            int totalConnectorsResolved,
            List<String> issues,
            Map<String, Object> catalogCoverage,
            Map<String, Object> cnjHealth,
            Map<String, Object> cnjDivergence,
            Map<String, Object> routingCoverage
    ) {
        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
            out.put("healthy", healthy);
            out.put("totalRitos", totalRitos);
            out.put("totalClassesTpu", totalClassesTpu);
            out.put("totalTribunais", totalTribunais);
            out.put("totalConnectorsResolved", totalConnectorsResolved);
            out.put("issues", issues);
            out.put("catalogCoverage", catalogCoverage);
            out.put("cnjHealth", cnjHealth);
            out.put("cnjDivergence", cnjDivergence);
            out.put("routingCoverage", routingCoverage);
            return copyNonNull(out);
        }
    }

    private final ProceduralCatalogService proceduralCatalogService;
    private final RitoPackService ritoPackService;
    private final CnjTpuSyncService cnjTpuSyncService;
    private final JudicialConnectorRegistry connectorRegistry;
    private final ProceduralBootstrapGovernanceProperties bootstrapProperties;
    private final Environment environment;

    public ProceduralArchitectureSanityService(ProceduralCatalogService proceduralCatalogService,
                                               RitoPackService ritoPackService,
                                               CnjTpuSyncService cnjTpuSyncService,
                                               JudicialConnectorRegistry connectorRegistry,
                                               ProceduralBootstrapGovernanceProperties bootstrapProperties,
                                               Environment environment) {
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
        this.ritoPackService = Objects.requireNonNull(ritoPackService);
        this.cnjTpuSyncService = Objects.requireNonNull(cnjTpuSyncService);
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.bootstrapProperties = Objects.requireNonNull(bootstrapProperties);
        this.environment = Objects.requireNonNull(environment);
    }

    public SanityReport report() {
        Map<String, Object> coverage = proceduralCatalogService.coverage();
        Map<String, Object> health = cnjTpuSyncService.health();
        DivergenceReport divergence = cnjTpuSyncService.checkDivergence();
        LinkedHashSet<String> issues = new LinkedHashSet<>();

        int totalRitos = asNumber(coverage.get("totalRitos")).intValue();
        int withStages = asNumber(coverage.get("withStages")).intValue();
        int withRequiredParties = asNumber(coverage.get("withRequiredParties")).intValue();
        int withRequiredDocuments = asNumber(coverage.get("withRequiredDocuments")).intValue();
        int withExternalActor = asNumber(coverage.get("withExternalActor")).intValue();

        if (withStages < totalRitos) {
            issues.add("Existem ritos sem stage gerado pelo catálogo procedural.");
        }
        if (withRequiredParties < totalRitos) {
            issues.add("Existem ritos sem esquema mínimo de partes obrigatório.");
        }
        if (withRequiredDocuments < totalRitos) {
            issues.add("Existem ritos sem documentação mínima obrigatória catalogada.");
        }
        if (withExternalActor < totalRitos) {
            issues.add("Existem ritos sem participação externa resolvida pelo catálogo procedural.");
        }
        if (!Boolean.TRUE.equals(health.get("snapshotFresh"))) {
            issues.add("Snapshot CNJ/TPU está desatualizado ou indisponível.");
        }
        if (!divergence.integridadeOk()) {
            issues.add("Catálogo local diverge do snapshot CNJ/TPU disponível.");
        }
        long coveredCanonical = proceduralCatalogService.catalogDrivenRitos().stream()
                .map(Enum::name)
                .filter(ritoPackService.definitions()::containsKey)
                .count();
        if (coveredCanonical < totalRitos) {
            issues.add("Rito pack carregado não cobre integralmente o catálogo procedural consolidado.");
        }

        List<String> routingCoverage = new ArrayList<>();
        int connectorsResolved = 0;
        boolean strictConnectorRegistry = isStrictConnectorRegistry();
        for (var tribunal : proceduralCatalogService.listNationalTribunals()) {
            Object pref = tribunal.get("connectorPreferido");
            if (pref == null) {
                routingCoverage.add("Conector preferido não registrado: " + tribunal.get("codigo") + " -> AUSENTE");
                continue;
            }
            try {
                Optional<JudicialProcessConnector> connector = connectorRegistry.find(JudicialSystem.valueOf(pref.toString()));
                if (connector.isPresent()) {
                    connectorsResolved++;
                } else {
                    routingCoverage.add("Conector preferido não registrado: " + tribunal.get("codigo") + " -> " + pref);
                }
            } catch (Exception ex) {
                routingCoverage.add("Conector preferido inválido: " + tribunal.get("codigo") + " -> " + pref);
            }
        }
        if (strictConnectorRegistry && !routingCoverage.isEmpty()) {
            issues.add("Conector preferido ausente, inválido ou sem registro no registry judicial para tribunais catalogados.");
        }

        Map<String, Object> routing = new LinkedHashMap<>();
        routing.put("missingPreferredConnectors", List.copyOf(routingCoverage));
        routing.put("resolvedPreferredConnectors", connectorsResolved);
        routing.put("totalTribunais", proceduralCatalogService.listNationalTribunals().size());
        routing.put("strictConnectorRegistry", strictConnectorRegistry);

        Map<String, Object> divergenceMap = new LinkedHashMap<>();
        divergenceMap.put("integridadeOk", divergence.integridadeOk());
        divergenceMap.put("classesLocais", divergence.classesLocais());
        divergenceMap.put("classesCnj", divergence.classesCnj());
        divergenceMap.put("classesNoLocalNaoCnj", divergence.classesNoLocalNaoCnj());
        divergenceMap.put("classesCnjNaoLocal", divergence.classesCnjNaoLocal());
        divergenceMap.put("descricoesDesatualizadas", divergence.descricoesDesatualizadas());
        divergenceMap.put("totalTpuLocal", TpuClasseCnj.values().length);

        return new SanityReport(
                Instant.now(),
                issues.isEmpty(),
                totalRitos,
                TpuClasseCnj.values().length,
                proceduralCatalogService.listNationalTribunals().size(),
                connectorsResolved,
                List.copyOf(issues),
                copyNonNull(coverage),
                copyNonNull(health),
                copyNonNull(divergenceMap),
                copyNonNull(routing)
        );
    }

    private static Map<String, Object> copyNonNull(Map<String, Object> values) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (values != null) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    out.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return Collections.unmodifiableMap(out);
    }

    private Number asNumber(Object value) {
        return value instanceof Number number ? number : 0;
    }

    private boolean isStrictConnectorRegistry() {
        if (bootstrapProperties.isStrictConnectorRegistry()) {
            return true;
        }
        List<String> actives = List.of(environment.getActiveProfiles());
        if (actives.isEmpty()) {
            return false;
        }
        for (String active : actives) {
            String normalized = active == null ? "" : active.trim().toLowerCase(Locale.ROOT);
            for (String profile : bootstrapProperties.getStrictConnectorRegistryProfiles()) {
                if (normalized.equals(profile.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }
}
