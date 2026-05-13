package com.tcc.pjb.backend.core.quality.modularization.application;

import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryIssue;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryPackageView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryPhaseView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryReadinessSnapshot;
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
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbProjectPathResolver;
import org.springframework.stereotype.Service;

@Service
public class PjbModuleBoundaryReadinessApplicationService {

    private static final Pattern AUTOWIRED = Pattern.compile("@Autowired\\b");

    private final Path projectRoot;

    public PjbModuleBoundaryReadinessApplicationService() {
        this(Path.of(""));
    }

    public PjbModuleBoundaryReadinessApplicationService(Path projectRoot) {
        this.projectRoot = PjbProjectPathResolver.apiModuleRoot(projectRoot);
    }

    public PjbModuleBoundaryReadinessSnapshot snapshot() {
        Path mainJava = projectRoot.resolve("src/main/java/com/tcc/pjb/backend");
        List<Path> javaFiles = listJavaFiles(mainJava);
        List<PjbModuleBoundaryIssue> issues = new ArrayList<>();
        Map<String, Long> packageCounters = new LinkedHashMap<>();
        long controllerPackages = 0;
        long servicePackages = 0;
        long candidateCorePackages = 0;
        for (Path file : javaFiles) {
            String source = read(file);
            String relative = relativize(file);
            String packageName = packageName(relative);
            packageCounters.merge(packageName, 1L, Long::sum);
            if (relative.contains("/controller/")) {
                controllerPackages++;
            }
            if (relative.contains("/service/") || relative.contains("ApplicationService.java")) {
                servicePackages++;
            }
            if (relative.contains("/core/")) {
                candidateCorePackages++;
            }
            if (relative.contains("/core/") && source.contains("import com.tcc.pjb.backend.service.")) {
                issues.add(new PjbModuleBoundaryIssue(
                        "core.depends.on.service",
                        "CRITICO",
                        relative,
                        "Pacote core ainda depende de service e bloqueia extração de pjb-core",
                        List.of("mover dependência para application/facade ou inverter fronteira")
                ));
            }
            if (relative.contains("/core/") && source.contains("import com.tcc.pjb.backend.controller.")) {
                issues.add(new PjbModuleBoundaryIssue(
                        "core.depends.on.controller",
                        "CRITICO",
                        relative,
                        "Pacote core depende de controller e bloqueia modularização",
                        List.of("remover dependência de web da camada core")
                ));
            }
            if (relative.contains("/controller/") && source.contains("import com.tcc.pjb.backend.model.repository.")) {
                issues.add(new PjbModuleBoundaryIssue(
                        "controller.depends.on.repository",
                        "ALTO",
                        relative,
                        "Controller depende de repository diretamente e quebra fronteira futura",
                        List.of("introduzir service/application facade")
                ));
            }
            if (relative.contains("/model/entity/") && source.contains("import com.tcc.pjb.backend.controller.")) {
                issues.add(new PjbModuleBoundaryIssue(
                        "entity.depends.on.controller",
                        "CRITICO",
                        relative,
                        "Entity depende de controller diretamente",
                        List.of("mover lógica web para camada própria")
                ));
            }
            if (AUTOWIRED.matcher(source).find()) {
                issues.add(new PjbModuleBoundaryIssue(
                        "legacy.autowired",
                        "MEDIO",
                        relative,
                        "Uso de @Autowired atrasa extração limpa de módulos",
                        List.of("manter somente injeção por construtor")
                ));
            }
        }
        String pom = read(projectRoot.resolve("pom.xml"));
        boolean aggregatorPomPresent = pom.contains("<modules>");
        if (!aggregatorPomPresent) {
            issues.add(new PjbModuleBoundaryIssue(
                    "root.without.modules",
                    "ALTO",
                    "pom.xml",
                    "POM raiz ainda não declara modules para a migração incremental",
                    List.of("introduzir fase 1 da extração com pjb-core e pjb-api")
            ));
        }
        List<String> nextActions = new ArrayList<>();
        nextActions.add("Congelar primeira extração em pjb-core com dependências puras de core/model/util.");
        nextActions.add("Eliminar imports core->service e controller->repository restantes antes da mudança de POM.");
        nextActions.add("Introduzir agregador de módulos em fase 1 sem mover mais de um pacote por rodada.");
        boolean ready = issues.stream().noneMatch(issue -> "CRITICO".equals(issue.severity())) && aggregatorPomPresent;
        return new PjbModuleBoundaryReadinessSnapshot(
                aggregatorPomPresent,
                ready,
                issues.size(),
                (int) packageCounters.keySet().stream().filter(pkg -> pkg.startsWith("com.tcc.pjb.backend.core")).count(),
                (int) packageCounters.keySet().stream().filter(pkg -> pkg.contains(".controller.")).count(),
                (int) packageCounters.keySet().stream().filter(pkg -> pkg.contains(".service.") || pkg.contains(".application")).count(),
                List.copyOf(issues),
                List.copyOf(new LinkedHashSet<>(nextActions)),
                Instant.now());
    }

    public List<PjbModuleBoundaryIssue> blockers() {
        return snapshot().blockers();
    }

    public List<PjbModuleBoundaryPackageView> packages() {
        Path mainJava = projectRoot.resolve("src/main/java/com/tcc/pjb/backend");
        Map<String, List<String>> buckets = new LinkedHashMap<>();
        buckets.put("pjb-core", List.of("/core/", "/platform/runtime/", "/configs/datasource/"));
        buckets.put("pjb-processo-lifecycle", List.of("/model/entity/", "/model/repository/", "/model/dto/"));
        buckets.put("pjb-integration", List.of("/integration/"));
        buckets.put("pjb-api", List.of("/controller/"));
        buckets.put("pjb-service-legacy", List.of("/service/"));
        List<Path> javaFiles = listJavaFiles(mainJava);
        List<PjbModuleBoundaryPackageView> views = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : buckets.entrySet()) {
            List<Path> files = javaFiles.stream()
                    .filter(path -> entry.getValue().stream().anyMatch(marker -> relativize(path).contains(marker)))
                    .toList();
            Map<String, Long> counts = files.stream()
                    .map(path -> packageName(relativize(path)))
                    .collect(Collectors.groupingBy(name -> name, LinkedHashMap::new, Collectors.counting()));
            List<String> topPackages = counts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(item -> item.getKey() + "=" + item.getValue())
                    .toList();
            views.add(new PjbModuleBoundaryPackageView(entry.getKey(), files.size(), topPackages));
        }
        return views;
    }

    public List<PjbModuleBoundaryPhaseView> phases() {
        PjbModuleBoundaryReadinessSnapshot snapshot = snapshot();
        return List.of(
                new PjbModuleBoundaryPhaseView(1, "Extracao inicial pjb-core", snapshot.coreExtractionReady() ? "READY" : "BLOCKED", "Isolar core, runtime, prazos, audit, security e utilitarios sem web", blockersByCode(snapshot.blockers(), "core.depends.on.service", "core.depends.on.controller", "legacy.autowired", "root.without.modules")),
                new PjbModuleBoundaryPhaseView(2, "Extracao processo lifecycle", "PLANNED", "Separar entities, repositories e DTOs do ciclo processual", blockersByCode(snapshot.blockers(), "entity.depends.on.controller")),
                new PjbModuleBoundaryPhaseView(3, "Extracao api e integracoes", "PLANNED", "Conectar api e integration sobre modulos já estabilizados", blockersByCode(snapshot.blockers(), "controller.depends.on.repository"))
        );
    }

    private List<String> blockersByCode(List<PjbModuleBoundaryIssue> issues, String... codes) {
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        List<String> keys = List.of(codes);
        for (PjbModuleBoundaryIssue issue : issues) {
            if (keys.contains(issue.code())) {
                selected.add(issue.summary());
            }
        }
        return List.copyOf(selected);
    }

    private List<Path> listJavaFiles(Path root) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
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

    private String packageName(String relative) {
        String normalized = relative.replace('/', '.');
        if (normalized.endsWith(".java")) {
            normalized = normalized.substring(0, normalized.length() - 5);
        }
        int marker = normalized.indexOf("com.tcc.pjb.backend");
        if (marker >= 0) {
            return normalized.substring(marker).toLowerCase(Locale.ROOT);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }
}
