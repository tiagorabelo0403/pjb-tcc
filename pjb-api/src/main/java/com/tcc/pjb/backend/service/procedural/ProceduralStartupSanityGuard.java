package com.tcc.pjb.backend.service.procedural;

import com.tcc.pjb.backend.service.procedural.ProceduralArchitectureSanityService.SanityReport;
import com.tcc.pjb.backend.service.procedural.ProceduralLegacyBoundaryAuditService.BoundaryReport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContextException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProceduralStartupSanityGuard implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProceduralStartupSanityGuard.class);

    private final ProceduralArchitectureSanityService architectureSanityService;
    private final ProceduralLegacyBoundaryAuditService legacyBoundaryAuditService;
    private final ProceduralBootstrapGovernanceProperties properties;
    private final Environment environment;

    public ProceduralStartupSanityGuard(ProceduralArchitectureSanityService architectureSanityService,
                                        ProceduralLegacyBoundaryAuditService legacyBoundaryAuditService,
                                        ProceduralBootstrapGovernanceProperties properties,
                                        Environment environment) {
        this.architectureSanityService = Objects.requireNonNull(architectureSanityService);
        this.legacyBoundaryAuditService = Objects.requireNonNull(legacyBoundaryAuditService);
        this.properties = Objects.requireNonNull(properties);
        this.environment = Objects.requireNonNull(environment);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        SanityReport architecture = architectureSanityService.report();
        BoundaryReport boundary = properties.isValidateLegacyBoundary()
                ? legacyBoundaryAuditService.report()
                : new BoundaryReport(null, false, true, 0, List.of(), List.of());

        LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("architectureHealthy", architecture.healthy());
        snapshot.put("architectureIssues", architecture.issues());
        snapshot.put("legacyBoundaryAvailable", boundary.available());
        snapshot.put("legacyBoundaryClean", boundary.clean());
        snapshot.put("legacyBoundaryViolations", boundary.violations().size());
        snapshot.put("activeProfiles", List.of(environment.getActiveProfiles()));

        boolean unhealthy = !architecture.healthy() || (properties.isValidateLegacyBoundary() && boundary.available() && !boundary.clean());
        if (!unhealthy) {
            log.info("PJB procedural bootstrap sanity ok: {}", snapshot);
            return;
        }

        if (shouldFailFast()) {
            throw new ApplicationContextException(buildFailureMessage(architecture, boundary));
        }

        log.warn("PJB procedural bootstrap sanity degraded: {}", snapshot);
    }

    private boolean shouldFailFast() {
        if (properties.isFailFast()) {
            return true;
        }
        List<String> actives = List.of(environment.getActiveProfiles());
        if (actives.isEmpty()) {
            return false;
        }
        for (String active : actives) {
            String normalized = active == null ? "" : active.trim().toLowerCase(Locale.ROOT);
            for (String profile : properties.getFailFastProfiles()) {
                if (normalized.equals(profile.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String buildFailureMessage(SanityReport architecture, BoundaryReport boundary) {
        ArrayList<String> parts = new ArrayList<>();
        if (!architecture.healthy()) {
            parts.add("arquitetura=" + String.join(" | ", architecture.issues()));
        }
        if (properties.isValidateLegacyBoundary() && boundary.available() && !boundary.clean()) {
            parts.add("fronteira_legado=" + boundary.violations().stream()
                    .limit(10)
                    .map(v -> v.path() + "@" + v.lines())
                    .reduce((a, b) -> a + " | " + b)
                    .orElse("violacoes_detectadas"));
        }
        return "Procedural bootstrap governance blocked: " + String.join(" || ", parts);
    }

    public Map<String, Object> snapshot() {
        SanityReport architecture = architectureSanityService.report();
        BoundaryReport boundary = properties.isValidateLegacyBoundary()
                ? legacyBoundaryAuditService.report()
                : new BoundaryReport(null, false, true, 0, List.of(), List.of());
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", properties.isEnabled());
        out.put("failFast", shouldFailFast());
        out.put("architecture", architecture.toMap());
        out.put("legacyBoundary", boundary.toMap());
        return Collections.unmodifiableMap(out);
    }
}
