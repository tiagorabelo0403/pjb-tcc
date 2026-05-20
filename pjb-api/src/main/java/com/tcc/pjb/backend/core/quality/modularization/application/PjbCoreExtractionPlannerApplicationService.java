package com.tcc.pjb.backend.core.quality.modularization.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreExtractionCandidateView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreExtractionDependencyIssue;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreExtractionMovePlan;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreExtractionPomPreview;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbCoreExtractionSnapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class PjbCoreExtractionPlannerApplicationService {

    private static final List<String> CORE_MARKERS = List.of(
            "/core/",
            "/platform/runtime/",
            "/configs/datasource/ReadAfterWriteConsistencyPolicy.java",
            "/configs/datasource/PjbRuntimeContainerBudgetService.java",
            "/configs/datasource/PjbRuntimeSizingService.java"
    );

    private final AuditLedgerService auditLedgerService;
    private final Path projectRoot;

    @Inject
    @Autowired
    public PjbCoreExtractionPlannerApplicationService(AuditLedgerService auditLedgerService) {
        this(auditLedgerService, Path.of(""));
    }

    PjbCoreExtractionPlannerApplicationService(AuditLedgerService auditLedgerService, Path projectRoot) {
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.projectRoot = projectRoot == null ? Path.of("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PjbCoreExtractionSnapshot snapshot() {
        List<PjbCoreExtractionCandidateView> candidates = candidates();
        List<PjbCoreExtractionDependencyIssue> issues = dependencies();
        boolean aggregatorPomPresent = read(projectRoot.resolve("pom.xml")).contains("<modules>");
        long safeCount = candidates.stream().filter(candidate -> "BAIXO".equals(candidate.risk())).count();
        boolean readyForScaffold = aggregatorPomPresent && issues.stream().noneMatch(issue -> "CRITICO".equals(issue.severity()));
        List<String> recommendedSteps = List.of(
                "Criar estrutura agregadora com modulos pjb-core e pjb-api sem mover mais de um pacote por rodada.",
                "Comecar por pacotes de baixo risco em core.audit, core.util e runtime sem dependencias de web.",
                "Eliminar dependencias core -> service/repository/controller antes de mover pacotes de prazos, icp e seguranca."
        );
        PjbCoreExtractionSnapshot snapshot = new PjbCoreExtractionSnapshot(
                readyForScaffold,
                aggregatorPomPresent,
                candidates.size(),
                (int) safeCount,
                issues.size(),
                issues,
                recommendedSteps,
                Instant.now());
        auditLedgerService.appendSafely("MODULARIZATION_CORE_EXTRACTION_SNAPSHOT_QUERY", "MODULARIZATION", "PJB_CORE", null, "issues=" + issues.size());
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<PjbCoreExtractionCandidateView> candidates() {
        Map<String, List<Path>> grouped = candidateFiles().stream().collect(Collectors.groupingBy(this::packageName, LinkedHashMap::new, Collectors.toList()));
        List<PjbCoreExtractionCandidateView> views = grouped.entrySet().stream()
                .map(entry -> buildCandidate(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(PjbCoreExtractionCandidateView::risk).thenComparing(PjbCoreExtractionCandidateView::fileCount, Comparator.reverseOrder()))
                .toList();
        auditLedgerService.appendSafely("MODULARIZATION_CORE_EXTRACTION_CANDIDATES_QUERY", "MODULARIZATION", "PJB_CORE", null, "count=" + views.size());
        return views;
    }

    @Transactional(readOnly = true)
    public List<PjbCoreExtractionDependencyIssue> dependencies() {
        List<PjbCoreExtractionDependencyIssue> issues = new ArrayList<>();
        for (Path file : candidateFiles()) {
            String relative = relativize(file);
            String source = read(file);
            addDependencyIssue(issues, relative, source, "import com.tcc.pjb.backend.service.", "CRITICO", "core.depends.on.service", "inverter dependencia para application/facade");
            addDependencyIssue(issues, relative, source, "import com.tcc.pjb.backend.controller.", "CRITICO", "core.depends.on.controller", "remover dependencia de web da camada core");
            addDependencyIssue(issues, relative, source, "import com.tcc.pjb.backend.model.repository.", "ALTO", "core.depends.on.repository", "isolar consulta em porta ou mover consumidor para lifecycle/application");
            addDependencyIssue(issues, relative, source, "import org.springframework.web.", "ALTO", "core.depends.on.spring-web", "manter adaptadores web fora do modulo pjb-core");
        }
        auditLedgerService.appendSafely("MODULARIZATION_CORE_EXTRACTION_DEPENDENCIES_QUERY", "MODULARIZATION", "PJB_CORE", null, "count=" + issues.size());
        return List.copyOf(issues);
    }

    @Transactional(readOnly = true)
    public PjbCoreExtractionPomPreview pomPreview() {
        boolean aggregatorPomPresent = read(projectRoot.resolve("pom.xml")).contains("<modules>");
        PjbCoreExtractionPomPreview preview = new PjbCoreExtractionPomPreview(
                !aggregatorPomPresent,
                List.of("pjb-core", "pjb-api"),
                List.of(
                        "<modules>",
                        "  <module>pjb-core</module>",
                        "  <module>pjb-api</module>",
                        "</modules>",
                        "",
                        "<!-- Fase 1: extrair pjb-core sem mover mais de um pacote por rodada -->"
                ),
                Instant.now());
        auditLedgerService.appendSafely("MODULARIZATION_CORE_EXTRACTION_POM_PREVIEW_QUERY", "MODULARIZATION", "PJB_CORE", null, "aggregatorRequired=" + preview.aggregatorPomRequired());
        return preview;
    }

    @Transactional(readOnly = true)
    public List<PjbCoreExtractionMovePlan> movePlan() {
        List<PjbCoreExtractionCandidateView> candidates = candidates();
        List<PjbCoreExtractionDependencyIssue> issues = dependencies();
        List<String> blockers = issues.stream()
                .map(PjbCoreExtractionDependencyIssue::summary)
                .distinct()
                .toList();
        List<String> lowRiskPackages = candidates.stream()
                .filter(candidate -> "BAIXO".equals(candidate.risk()))
                .limit(6)
                .map(PjbCoreExtractionCandidateView::packageName)
                .toList();
        List<String> mediumRiskPackages = candidates.stream()
                .filter(candidate -> "MEDIO".equals(candidate.risk()))
                .limit(6)
                .map(PjbCoreExtractionCandidateView::packageName)
                .toList();
        List<PjbCoreExtractionMovePlan> plan = List.of(
                new PjbCoreExtractionMovePlan(
                        "Fase 1",
                        "Scaffold do agregador e migracao de pacotes seguros",
                        estimateFiles(candidates, lowRiskPackages),
                        lowRiskPackages,
                        List.of(),
                        List.of(
                                "Criar pom agregador com pjb-core e pjb-api.",
                                "Mover primeiro core.audit, core.util e runtime de baixo risco.",
                                "Executar sweep de imports e testes arquiteturais antes do proximo pacote."
                        )
                ),
                new PjbCoreExtractionMovePlan(
                        "Fase 2",
                        "Migracao controlada de pacotes core com dependencias intermediarias",
                        estimateFiles(candidates, mediumRiskPackages),
                        mediumRiskPackages,
                        blockers,
                        List.of(
                                "Eliminar dependencias para repository/service nas classes candidatas.",
                                "Substituir acessos diretos por portas ou facades finas.",
                                "So mover prazos, icp e seguranca depois do saneamento."
                        )
                )
        );
        auditLedgerService.appendSafely("MODULARIZATION_CORE_EXTRACTION_MOVE_PLAN_QUERY", "MODULARIZATION", "PJB_CORE", null, "phases=" + plan.size());
        return plan;
    }

    private PjbCoreExtractionCandidateView buildCandidate(String packageName, List<Path> files) {
        LinkedHashSet<String> notes = new LinkedHashSet<>();
        String risk = "BAIXO";
        for (Path file : files) {
            String source = read(file);
            if (source.contains("import com.tcc.pjb.backend.service.") || source.contains("import com.tcc.pjb.backend.controller.")) {
                risk = "ALTO";
                notes.add("dependencia cruzada para service/controller");
            } else if (source.contains("import com.tcc.pjb.backend.model.repository.") || source.contains("import org.springframework.web.")) {
                if (!"ALTO".equals(risk)) {
                    risk = "MEDIO";
                }
                notes.add("dependencia intermediaria para repository/web");
            }
        }
        if (notes.isEmpty()) {
            notes.add("candidato limpo para fase inicial de extracao");
        }
        return new PjbCoreExtractionCandidateView(packageName, "pjb-core", files.size(), risk, List.copyOf(notes));
    }

    private int estimateFiles(List<PjbCoreExtractionCandidateView> candidates, List<String> packages) {
        return candidates.stream()
                .filter(candidate -> packages.contains(candidate.packageName()))
                .mapToInt(PjbCoreExtractionCandidateView::fileCount)
                .sum();
    }

    private void addDependencyIssue(List<PjbCoreExtractionDependencyIssue> issues,
                                    String relative,
                                    String source,
                                    String token,
                                    String severity,
                                    String type,
                                    String action) {
        if (!source.contains(token)) {
            return;
        }
        issues.add(new PjbCoreExtractionDependencyIssue(
                severity,
                relative,
                type,
                relative + " ainda depende de fronteira incompatível com pjb-core",
                List.of(action)
        ));
    }

    private List<Path> candidateFiles() {
        Path root = projectRoot.resolve("src/main/java/com/tcc/pjb/backend");
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .filter(path -> {
                        String relative = relativize(path);
                        return CORE_MARKERS.stream().anyMatch(relative::contains);
                    })
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private String packageName(Path path) {
        return packageName(relativize(path));
    }

    private String packageName(String relative) {
        String normalized = relative.replace('/', '.');
        if (normalized.endsWith(".java")) {
            normalized = normalized.substring(0, normalized.length() - 5);
        }
        int marker = normalized.indexOf("com.tcc.pjb.backend");
        return marker >= 0 ? normalized.substring(marker) : normalized;
    }

    private String read(Path file) {
        if (file == null || !Files.exists(file)) {
            return "";
        }
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private String relativize(Path file) {
        return projectRoot.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }
}
