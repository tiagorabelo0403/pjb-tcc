package com.tcc.pjb.backend.service.procedural;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class ProceduralLegacyBoundaryAuditService {

    public record BoundaryViolation(
            String path,
            String packageName,
            String reason,
            List<Integer> lines,
            boolean directImport,
            boolean directReference
    ) {
        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("path", path);
            out.put("packageName", packageName);
            out.put("reason", reason);
            out.put("lines", lines);
            out.put("directImport", directImport);
            out.put("directReference", directReference);
            return Collections.unmodifiableMap(out);
        }
    }

    public record BoundaryReport(
            Instant generatedAt,
            boolean available,
            boolean clean,
            int scannedFiles,
            List<String> scannedRoots,
            List<BoundaryViolation> violations
    ) {
        public Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
            out.put("available", available);
            out.put("clean", clean);
            out.put("scannedFiles", scannedFiles);
            out.put("scannedRoots", scannedRoots);
            out.put("violations", violations.stream().map(BoundaryViolation::toMap).toList());
            return Collections.unmodifiableMap(out);
        }
    }

    private static final Set<String> FORBIDDEN_PACKAGE_FRAGMENTS = Set.of(
            "/ai/juridica/api/",
            "/core/preflight/",
            "/core/validator/",
            "/integration/judicial/routing/",
            "/service/competencia/",
            "/service/feedback/",
            "/service/cockpit/",
            "/engine/triagem/",
            "/engine/inferencia/",
            "/ai/juridica/v3/core/",
            "/service/intelligence/",
            "/modules/laiane/service/"
    );

    private static final Set<String> ALLOWED_FILE_NAMES = Set.of(
            "RitoProcessual.java",
            "ProceduralCatalogSupport.java",
            "ProceduralCatalogService.java",
            "ProceduralCanonicalResolver.java",
            "CanonicalContextOperator.java",
            "RitoPackService.java",
            "RitoResolutionService.java",
            "ProcessoCanonicalPayloadFactory.java",
            "ProceduralInputContractMapper.java",
            "CanonicalRitoSelector.java"
    );

    private final ProceduralBootstrapGovernanceProperties properties;

    public ProceduralLegacyBoundaryAuditService(ProceduralBootstrapGovernanceProperties properties) {
        this.properties = Objects.requireNonNull(properties);
    }

    public BoundaryReport report() {
        ArrayList<Path> roots = new ArrayList<>();
        for (String root : properties.getSourceRoots()) {
            if (root == null || root.isBlank()) {
                continue;
            }
            try {
                Path path = Path.of(root.strip()).normalize();
                if (Files.isDirectory(path)) {
                    roots.add(path);
                }
            } catch (InvalidPathException ignored) {
            }
        }
        if (roots.isEmpty()) {
            return new BoundaryReport(Instant.now(), false, true, 0, List.copyOf(properties.getSourceRoots()), List.of());
        }

        int scanned = 0;
        ArrayList<BoundaryViolation> violations = new ArrayList<>();
        for (Path root : roots) {
            try (Stream<Path> stream = Files.walk(root)) {
                List<Path> files = stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .toList();
                for (Path file : files) {
                    scanned++;
                    if (isForbiddenOperationalFile(file) && !ALLOWED_FILE_NAMES.contains(file.getFileName().toString())) {
                        BoundaryViolation violation = analyze(root, file);
                        if (violation != null) {
                            violations.add(violation);
                            if (violations.size() >= properties.getMaxViolations()) {
                                return new BoundaryReport(Instant.now(), true, false, scanned, roots.stream().map(Path::toString).toList(), List.copyOf(violations));
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return new BoundaryReport(Instant.now(), true, violations.isEmpty(), scanned, roots.stream().map(Path::toString).toList(), List.copyOf(violations));
    }

    private BoundaryViolation analyze(Path root, Path file) {
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            boolean hasDirectImport = false;
            boolean hasDirectReference = false;
            LinkedHashSet<Integer> matched = new LinkedHashSet<>();
            String packageName = "";
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                String normalized = line.strip();
                if (normalized.startsWith("package ")) {
                    packageName = normalized.substring(8, normalized.length() - 1);
                }
                if (normalized.equals("import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;")) {
                    hasDirectImport = true;
                    matched.add(i + 1);
                } else if (normalized.contains("RitoProcessual") && !normalized.startsWith("package ") && !normalized.startsWith("import ")) {
                    hasDirectReference = true;
                    matched.add(i + 1);
                }
            }
            if (!hasDirectImport && !hasDirectReference) {
                return null;
            }
            String reason = hasDirectImport && hasDirectReference
                    ? "uso_direto_enum_legado_em_camada_operacional"
                    : hasDirectImport
                    ? "import_direto_enum_legado_em_camada_operacional"
                    : "referencia_direta_enum_legado_em_camada_operacional";
            return new BoundaryViolation(root.relativize(file).toString(), packageName, reason, List.copyOf(matched), hasDirectImport, hasDirectReference);
        } catch (IOException ignored) {
            return null;
        }
    }

    private boolean isForbiddenOperationalFile(Path file) {
        String normalized = file.toString().replace('\\', '/').toLowerCase(Locale.ROOT);
        for (String fragment : FORBIDDEN_PACKAGE_FRAGMENTS) {
            if (normalized.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}
