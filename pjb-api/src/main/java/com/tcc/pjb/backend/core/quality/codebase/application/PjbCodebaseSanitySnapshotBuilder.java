package com.tcc.pjb.backend.core.quality.codebase.application;

import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityIssue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class PjbCodebaseSanitySnapshotBuilder {

    private static final String LEGACY_JUDGE_IMPORT = "com.tcc.pjb.backend.service.judge.";

    private final PjbCodebaseSanityProjectLayout layout;
    private final PjbCodebaseSanitySettings settings;
    private final PjbCodebaseSanitySourceExplorer sourceExplorer;

    PjbCodebaseSanitySnapshotBuilder(PjbCodebaseSanityProjectLayout layout,
                                     PjbCodebaseSanitySettings settings,
                                     PjbCodebaseSanitySourceExplorer sourceExplorer) {
        this.layout = Objects.requireNonNull(layout);
        this.settings = Objects.requireNonNull(settings);
        this.sourceExplorer = Objects.requireNonNull(sourceExplorer);
    }

    PjbCodebaseSanityAggregate build(Instant generatedAt) {
        if (layout.sourceRoots().stream().noneMatch(Files::isDirectory)) {
            return new PjbCodebaseSanityAggregate(
                    false,
                    true,
                    0,
                    0,
                    0,
                    0,
                    List.of(),
                    layout.sourceRoots().stream().map(Path::toString).toList(),
                    List.of(),
                    generatedAt
            );
        }
        List<Path> files = sourceExplorer.listJavaFiles(layout.sourceRoots());
        TypeIndex typeIndex = indexTypes(files);
        AuditCounters counters = new AuditCounters();
        ArrayList<PjbCodebaseSanityIssue> issues = new ArrayList<>();
        auditDuplicateTypes(typeIndex, counters, issues);
        auditFiles(files, typeIndex.knownTypes(), counters, issues);
        auditLegacyJudgePackage(files, issues);
        auditBuildTooling(issues);
        List<String> orphanDirs = settings.orphanDirectories().stream()
                .map(layout.projectRoot()::resolve)
                .filter(Files::isDirectory)
                .map(Path::toString)
                .toList();
        boolean clean = counters.duplicatedTypes == 0
                && counters.brokenInternalImports == 0
                && counters.virtualThreadViolations == 0
                && orphanDirs.isEmpty()
                && issues.stream().noneMatch(issue -> settings.blockingIssueCodes().contains(issue.codigo()));
        return new PjbCodebaseSanityAggregate(
                true,
                clean,
                files.size(),
                counters.duplicatedTypes,
                counters.brokenInternalImports,
                counters.virtualThreadViolations,
                orphanDirs,
                layout.sourceRoots().stream().map(Path::toString).toList(),
                List.copyOf(issues),
                generatedAt
        );
    }

    private TypeIndex indexTypes(List<Path> files) {
        LinkedHashMap<String, List<String>> fqns = new LinkedHashMap<>();
        LinkedHashSet<String> knownTypes = new LinkedHashSet<>();
        for (Path file : files) {
            String source = sourceExplorer.read(file);
            String packageName = sourceExplorer.packageName(source);
            String topLevelType = sourceExplorer.topLevelType(source);
            if (topLevelType.isBlank()) {
                continue;
            }
            String fqn = packageName.isBlank() ? topLevelType : packageName + '.' + topLevelType;
            knownTypes.add(fqn);
            fqns.computeIfAbsent(fqn, key -> new ArrayList<>()).add(file.toString());
        }
        return new TypeIndex(Map.copyOf(fqns), Set.copyOf(knownTypes));
    }

    private void auditDuplicateTypes(TypeIndex typeIndex,
                                     AuditCounters counters,
                                     List<PjbCodebaseSanityIssue> issues) {
        for (Map.Entry<String, List<String>> entry : typeIndex.fqns().entrySet()) {
            if (entry.getValue().size() <= 1) {
                continue;
            }
            counters.duplicatedTypes++;
            issues.add(new PjbCodebaseSanityIssue(
                    "duplicate.fqn",
                    "CRITICO",
                    entry.getKey(),
                    List.of(),
                    String.join(" | ", entry.getValue())
            ));
        }
    }

    private void auditFiles(List<Path> files,
                            Set<String> knownTypes,
                            AuditCounters counters,
                            List<PjbCodebaseSanityIssue> issues) {
        for (Path file : files) {
            String source = sourceExplorer.read(file);
            int virtualThreadViolations = countVirtualThreadViolations(file, source);
            if (virtualThreadViolations > 0) {
                counters.virtualThreadViolations += virtualThreadViolations;
                issues.add(new PjbCodebaseSanityIssue(
                        "virtualthread.direct",
                        "ALTO",
                        sourceExplorer.relativize(layout.projectRoot(), file),
                        List.of(),
                        PjbCodebaseSanityMessages.directVirtualThreadOutsideSpine()
                ));
            }
            if (usesLegacyJudgeImport(file, source)) {
                issues.add(new PjbCodebaseSanityIssue(
                        "legacy.judge.import",
                        "ALTO",
                        sourceExplorer.relativize(layout.projectRoot(), file),
                        List.of(),
                        PjbCodebaseSanityMessages.legacyJudgeImportIsolationBridge()
                ));
            }
            for (String imported : sourceExplorer.imports(source)) {
                if (!imported.startsWith("com.tcc.pjb.backend.") || resolvesImport(imported, knownTypes)) {
                    continue;
                }
                counters.brokenInternalImports++;
                issues.add(new PjbCodebaseSanityIssue(
                        "import.interno.quebrado",
                        "CRITICO",
                        sourceExplorer.relativize(layout.projectRoot(), file),
                        List.of(),
                        imported
                ));
            }
            String repositoryEntity = repositoryEntity(file, source, knownTypes);
            if (!repositoryEntity.isBlank()) {
                issues.add(new PjbCodebaseSanityIssue(
                        "repository.entity.quebrada",
                        "CRITICO",
                        sourceExplorer.relativize(layout.projectRoot(), file),
                        List.of(),
                        repositoryEntity
                ));
            }
        }
    }

    private void auditLegacyJudgePackage(List<Path> files, List<PjbCodebaseSanityIssue> issues) {
        List<String> legacyJudgeFiles = files.stream()
                .map(file -> sourceExplorer.relativize(layout.projectRoot(), file))
                .filter(path -> path.contains("/service/judge/"))
                .sorted()
                .toList();
        if (legacyJudgeFiles.size() > 1) {
            issues.add(new PjbCodebaseSanityIssue(
                    "legacy.judge.package.expansion",
                    "CRITICO",
                    "src/main/java/com/tcc/pjb/backend/service/judge",
                    List.of(),
                    String.join(" | ", legacyJudgeFiles)
            ));
        }
    }

    private void auditBuildTooling(List<PjbCodebaseSanityIssue> issues) {
        String pom = sourceExplorer.read(layout.pomFile());
        if (!pom.contains("jacoco-maven-plugin")) {
            issues.add(new PjbCodebaseSanityIssue(
                    "quality.coverage.absent",
                    "ALTO",
                    "pom.xml",
                    List.of(),
                    PjbCodebaseSanityMessages.jacocoAbsentFromMavenCycle()
            ));
        }
        if (!pom.contains("maven-checkstyle-plugin")) {
            issues.add(new PjbCodebaseSanityIssue(
                    "quality.checkstyle.absent",
                    "ALTO",
                    "pom.xml",
                    List.of(),
                    PjbCodebaseSanityMessages.checkstyleAbsentFromMavenCycle()
            ));
        }
        if (!pom.contains("config/checkstyle/checkstyle.xml") || !pom.contains("config/checkstyle/bounded-contexts.xml")) {
            issues.add(new PjbCodebaseSanityIssue(
                    "quality.checkstyle.config.absent",
                    "ALTO",
                    "pom.xml",
                    List.of(),
                    PjbCodebaseSanityMessages.checkstyleConfigsMustBeWiredToMaven()
            ));
        }
    }

    private int countVirtualThreadViolations(Path file, String source) {
        String normalized = sourceExplorer.relativize(layout.projectRoot(), file);
        if (normalized.contains("src/test/java/")) {
            return 0;
        }
        if (settings.allowlistedVirtualThreadFiles().contains(file.getFileName().toString())) {
            return 0;
        }
        int count = 0;
        count += sourceExplorer.count(source, "Executors.new" + "VirtualThreadPerTaskExecutor(");
        count += sourceExplorer.count(source, "Executors.newThreadPerTaskExecutor(" + "Thread.of" + "Virtual()");
        count += sourceExplorer.count(source, "Thread.of" + "Virtual()" + ".start(");
        count += sourceExplorer.count(source, ".virtualThreads(true)");
        return count;
    }

    private boolean usesLegacyJudgeImport(Path file, String source) {
        String normalized = sourceExplorer.relativize(layout.projectRoot(), file);
        return source.contains("import " + LEGACY_JUDGE_IMPORT) && !settings.allowlistedLegacyJudgeImports().contains(normalized);
    }

    private boolean resolvesImport(String imported, Set<String> knownTypes) {
        if (knownTypes.contains(imported)) {
            return true;
        }
        String[] parts = imported.split("\\.");
        for (int i = parts.length - 1; i > 1; i--) {
            String prefix = String.join(".", Arrays.copyOf(parts, i));
            if (knownTypes.contains(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String repositoryEntity(Path file, String source, Set<String> knownTypes) {
        String normalized = sourceExplorer.relativize(layout.projectRoot(), file);
        if (!normalized.contains("/model/repository/") || !source.contains("Repository<")) {
            return "";
        }
        String entitySimpleName = sourceExplorer.repositoryEntity(source);
        if (entitySimpleName.isBlank()) {
            return "";
        }
        for (String imported : sourceExplorer.imports(source)) {
            if (imported.endsWith('.' + entitySimpleName) && resolvesImport(imported, knownTypes)) {
                return "";
            }
        }
        for (String knownType : knownTypes) {
            if (knownType.endsWith('.' + entitySimpleName)) {
                return "";
            }
        }
        return entitySimpleName;
    }

    private record TypeIndex(
            Map<String, List<String>> fqns,
            Set<String> knownTypes
    ) {
    }

    private static final class AuditCounters {
        private int duplicatedTypes;
        private int brokenInternalImports;
        private int virtualThreadViolations;
    }
}
