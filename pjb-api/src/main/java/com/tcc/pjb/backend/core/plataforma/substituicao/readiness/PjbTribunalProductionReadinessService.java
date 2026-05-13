package com.tcc.pjb.backend.core.plataforma.substituicao.readiness;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoCapacidadeNacional;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoCapacidadeStatus;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalCapabilityCatalog;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PjbTribunalProductionReadinessService {

    public PjbTribunalReadinessSnapshot evaluate(String tribunalCode,
                                                 Set<String> homologatedCapabilityCodes,
                                                 Set<String> blockedCapabilityCodes) {
        Set<String> homologadas = normalized(homologatedCapabilityCodes);
        Set<String> bloqueadas = normalized(blockedCapabilityCodes);
        List<PjbTribunalReadinessCapability> capabilities = PjbSubstituicaoNacionalCapabilityCatalog.capacidades().stream()
                .map(capacidade -> toCapability(capacidade, homologadas, bloqueadas))
                .toList();
        return snapshot(tribunalCode, capabilities, Clock.systemUTC());
    }

    public PjbTribunalReadinessSnapshot snapshot(String tribunalCode,
                                                 List<PjbTribunalReadinessCapability> capabilities,
                                                 Clock clock) {
        List<PjbTribunalReadinessCapability> safeCapabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        int total = safeCapabilities.size();
        int ready = (int) safeCapabilities.stream().filter(PjbTribunalReadinessCapability::pronta).count();
        int blocked = (int) safeCapabilities.stream().filter(PjbTribunalReadinessCapability::bloqueada).count();
        int pending = (int) safeCapabilities.stream().filter(capability -> !capability.pronta()).count();
        List<String> blockers = blockers(safeCapabilities);
        PjbTribunalReadinessStatus status = status(total, ready, blocked, blockers, safeCapabilities);
        Instant generatedAt = Instant.now(clock == null ? Clock.systemUTC() : clock);
        return new PjbTribunalReadinessSnapshot(tribunalCode, status, total, ready, blocked, pending, safeCapabilities, blockers, generatedAt);
    }

    private PjbTribunalReadinessCapability toCapability(PjbSubstituicaoCapacidadeNacional capacidade,
                                                        Set<String> homologadas,
                                                        Set<String> bloqueadas) {
        String codigo = capacidade.codigo();
        boolean homologada = homologadas.contains(codigo) || capacidade.status() == PjbSubstituicaoCapacidadeStatus.PRESENTE;
        boolean bloqueada = bloqueadas.contains(codigo);
        return new PjbTribunalReadinessCapability(
                codigo,
                capacidade.titulo(),
                capacidade.status(),
                homologada,
                bloqueada,
                capacidade.eixoPjb(),
                capacidade.proximaEntrega()
        );
    }

    private PjbTribunalReadinessStatus status(int total,
                                              int ready,
                                              int blocked,
                                              List<String> blockers,
                                              List<PjbTribunalReadinessCapability> capabilities) {
        if (blocked > 0) {
            return blockedStatus(blockers);
        }
        if (total > 0 && ready == total) {
            return PjbTribunalReadinessStatus.READY_FOR_PRODUCTION;
        }
        boolean hasMissing = capabilities.stream().anyMatch(capability -> capability.status() == PjbSubstituicaoCapacidadeStatus.FALTANTE);
        return hasMissing ? PjbTribunalReadinessStatus.BLOCKED_BY_GOVERNANCE : PjbTribunalReadinessStatus.READY_FOR_PILOT;
    }

    private PjbTribunalReadinessStatus blockedStatus(List<String> blockers) {
        if (blockers.stream().anyMatch(value -> value.contains("mni") || value.contains("datajud") || value.contains("conector"))) {
            return PjbTribunalReadinessStatus.BLOCKED_BY_CONNECTOR;
        }
        if (blockers.stream().anyMatch(value -> value.contains("migracao") || value.contains("acervo"))) {
            return PjbTribunalReadinessStatus.BLOCKED_BY_MIGRATION;
        }
        if (blockers.stream().anyMatch(value -> value.contains("operacao") || value.contains("slo") || value.contains("runtime"))) {
            return PjbTribunalReadinessStatus.BLOCKED_BY_OPERATIONAL_RISK;
        }
        return PjbTribunalReadinessStatus.BLOCKED_BY_GOVERNANCE;
    }

    private List<String> blockers(List<PjbTribunalReadinessCapability> capabilities) {
        return capabilities.stream()
                .filter(capability -> capability.bloqueada() || !capability.pronta())
                .map(capability -> capability.codigo() + ":" + capability.proximaEntrega())
                .filter(value -> !value.endsWith(":"))
                .distinct()
                .toList();
    }

    private Set<String> normalized(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(value -> !value.isBlank())
                .forEach(normalized::add);
        return Set.copyOf(normalized);
    }
}
