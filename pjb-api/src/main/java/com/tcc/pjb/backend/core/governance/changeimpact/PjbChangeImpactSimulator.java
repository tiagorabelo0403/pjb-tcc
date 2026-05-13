package com.tcc.pjb.backend.core.governance.changeimpact;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class PjbChangeImpactSimulator {

    public PjbChangeImpactPlan simulate(String changeSummary,
                                        Set<PjbChangeSurface> touchedSurfaces,
                                        int currentFailingTests,
                                        boolean touchesPublicContract,
                                        boolean touchesAuthorization,
                                        boolean touchesPersistence) {
        LinkedHashSet<PjbChangeSurface> surfaces = touchedSurfaces == null || touchedSurfaces.isEmpty()
                ? new LinkedHashSet<>(Set.of(PjbChangeSurface.TEST_GOVERNANCE))
                : new LinkedHashSet<>(touchedSurfaces);
        List<PjbChangeImpactSignal> signals = new ArrayList<>();
        LinkedHashSet<String> guards = new LinkedHashSet<>();
        LinkedHashSet<String> rollback = new LinkedHashSet<>();
        guards.add("scripts/repository_cleanliness_guard.py");
        guards.add("scripts/readme_truthfulness_guard.py");
        guards.add("scripts/import_sanity_probe.py");
        guards.add("scripts/architecture_hygiene_guard.py");

        if (currentFailingTests > 0) {
            signals.add(signal(PjbChangeSurface.TEST_GOVERNANCE, PjbChangeImpactSeverity.HIGH, "pjb-api", "há falhas conhecidas antes da alteração", "pjb-api-clean-test-errors"));
            rollback.add("reverter somente os arquivos tocados pela alteração se a contagem de falhas aumentar");
        }
        if (touchesPublicContract || surfaces.contains(PjbChangeSurface.API_ROUTE)) {
            signals.add(signal(PjbChangeSurface.API_ROUTE, PjbChangeImpactSeverity.HIGH, "controllers", "contrato público exige validação canônica de rotas", "PjbRouteGovernanceCoverageTest"));
            guards.add("scripts/canonical_institutional_route_guard.py");
        }
        if (touchesAuthorization || surfaces.contains(PjbChangeSurface.SECURITY)) {
            signals.add(signal(PjbChangeSurface.SECURITY, PjbChangeImpactSeverity.CRITICAL, "core.security", "alteração de autorização pode violar sigilo, ABAC ou LGPD", "PjbSensitiveControllerAccessDisciplineTest"));
            rollback.add("restaurar política anterior de autorização se qualquer guard de superfície sensível falhar");
        }
        if (touchesPersistence || surfaces.contains(PjbChangeSurface.DATABASE)) {
            signals.add(signal(PjbChangeSurface.DATABASE, PjbChangeImpactSeverity.HIGH, "db/migration", "persistência exige compatibilidade com Flyway, JPA e H2", "RecentMigrationForeignKeyNamingGuardTest"));
            guards.add("scripts/flyway_migration_version_guard.py");
        }
        if (surfaces.contains(PjbChangeSurface.DEADLINE)) {
            guards.add("scripts/access_key_and_unavailability_guard.py");
        }
        if (surfaces.contains(PjbChangeSurface.MIGRATION)) {
            guards.add("scripts/replacement_matrix_guard.py");
        }

        String summary = Objects.toString(changeSummary, "").trim();
        boolean safe = signals.stream().noneMatch(PjbChangeImpactSignal::blocking) && currentFailingTests <= 13;
        String status = safe ? "SAFE_WITH_GUARDS" : "HUMAN_REVIEW_REQUIRED";
        if (summary.isEmpty()) {
            signals.add(signal(PjbChangeSurface.TEST_GOVERNANCE, PjbChangeImpactSeverity.MEDIUM, "change-request", "alteração sem resumo dificulta auditoria", "PjbChangeImpactSimulator"));
        }
        if (rollback.isEmpty()) {
            rollback.add("preservar commit anterior e aplicar reversão por arquivo em caso de regressão verificável");
        }
        return new PjbChangeImpactPlan(status, safe, signals, List.copyOf(guards), List.copyOf(rollback));
    }

    private PjbChangeImpactSignal signal(PjbChangeSurface surface,
                                         PjbChangeImpactSeverity severity,
                                         String path,
                                         String reason,
                                         String testAnchor) {
        return new PjbChangeImpactSignal(surface, severity, path, reason, testAnchor);
    }
}
