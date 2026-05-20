package com.tcc.pjb.backend.core.quality.modularization.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBuildOrderView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleDirectoryScaffoldView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModulePomScaffoldView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleScaffoldSnapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class PjbModuleScaffoldApplicationService {

    private static final Pattern ARTIFACT_ID = Pattern.compile("<artifactId>([^<]+)</artifactId>");
    private static final Pattern PACKAGING = Pattern.compile("<packaging>([^<]+)</packaging>");

    private final AuditLedgerService auditLedgerService;
    private final Path projectRoot;
    @Inject
    @Autowired
    public PjbModuleScaffoldApplicationService(AuditLedgerService auditLedgerService) {
        this(auditLedgerService, Path.of(""));
    }

    PjbModuleScaffoldApplicationService(AuditLedgerService auditLedgerService, Path projectRoot) {
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.projectRoot = projectRoot == null ? Path.of("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PjbModuleScaffoldSnapshot snapshot() {
        List<PjbModulePomScaffoldView> modulePoms = modulePoms();
        List<PjbModuleDirectoryScaffoldView> directories = directories();
        String rootPom = read(projectRoot.resolve("pom.xml"));
        boolean aggregatorLinked = rootPom.contains("<module>pjb-core</module>") && rootPom.contains("<module>pjb-api</module>");
        boolean scaffoldPresent = modulePoms.stream().allMatch(PjbModulePomScaffoldView::present)
                && directories.stream().allMatch(PjbModuleDirectoryScaffoldView::present);
        PjbModuleScaffoldSnapshot snapshot = new PjbModuleScaffoldSnapshot(
                scaffoldPresent,
                aggregatorLinked,
                (int) modulePoms.stream().filter(PjbModulePomScaffoldView::present).count(),
                (int) directories.stream().filter(PjbModuleDirectoryScaffoldView::present).count(),
                List.of(
                        "Ligar o agregador raiz aos modulos pjb-core e pjb-api quando o primeiro pacote seguro estiver pronto para mover.",
                        "Manter o scaffold fisico enquanto a Fase 1 elimina dependencias core -> service/controller/repository.",
                        "Usar o move plan do core extraction para escolher o primeiro pacote de baixo risco antes de ativar <modules>."
                ),
                Instant.now());
        auditLedgerService.appendSafely("MODULARIZATION_SCAFFOLD_SNAPSHOT_QUERY", "MODULARIZATION", "SCAFFOLD", null, "pomCount=" + snapshot.modulePomCount());
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<PjbModulePomScaffoldView> modulePoms() {
        List<PjbModulePomScaffoldView> views = List.of(
                pomView("pjb-core", projectRoot.resolve("pjb-core/pom.xml")),
                pomView("pjb-api", projectRoot.resolve("pjb-api/pom.xml"))
        );
        auditLedgerService.appendSafely("MODULARIZATION_SCAFFOLD_POMS_QUERY", "MODULARIZATION", "SCAFFOLD", null, "count=" + views.size());
        return views;
    }

    @Transactional(readOnly = true)
    public List<PjbModuleDirectoryScaffoldView> directories() {
        List<PjbModuleDirectoryScaffoldView> views = List.of(
                dirView("pjb-core", "pjb-core/src/main/java"),
                dirView("pjb-core", "pjb-core/src/test/java"),
                dirView("pjb-api", "pjb-api/src/main/java"),
                dirView("pjb-api", "pjb-api/src/test/java")
        );
        auditLedgerService.appendSafely("MODULARIZATION_SCAFFOLD_DIRECTORIES_QUERY", "MODULARIZATION", "SCAFFOLD", null, "count=" + views.size());
        return views;
    }

    @Transactional(readOnly = true)
    public List<PjbModuleBuildOrderView> buildOrder() {
        List<PjbModuleBuildOrderView> views = List.of(
                new PjbModuleBuildOrderView(1, "pjb-core", "READY_FOR_EXTRACT_PREP", "Primeiro modulo da Fase 1 para concentrar core, runtime e utilitarios sem web.", List.of("scaffold fisico presente", "eliminar dependencias criticas core -> service/controller/repository", "ativar agregador raiz quando houver primeiro pacote seguro")),
                new PjbModuleBuildOrderView(2, "pjb-api", "WAITING_FOR_CORE", "Modulo de API depende da estabilizacao do pjb-core e da fronteira de controllers.", List.of("pjb-core estabilizado", "controllers sem repository direto", "surface admin consolidada"))
        );
        auditLedgerService.appendSafely("MODULARIZATION_SCAFFOLD_BUILD_ORDER_QUERY", "MODULARIZATION", "SCAFFOLD", null, "count=" + views.size());
        return views;
    }

    private PjbModulePomScaffoldView pomView(String moduleName, Path pomPath) {
        String xml = read(pomPath);
        String parentArtifactId = nthArtifactId(xml, 0);
        String artifactId = nthArtifactId(xml, 1);
        return new PjbModulePomScaffoldView(
                moduleName,
                projectRoot.relativize(pomPath.toAbsolutePath().normalize()).toString().replace('\\', '/'),
                Files.exists(pomPath),
                extract(xml, PACKAGING),
                artifactId,
                parentArtifactId
        );
    }

    private PjbModuleDirectoryScaffoldView dirView(String moduleName, String relative) {
        Path path = projectRoot.resolve(relative);
        return new PjbModuleDirectoryScaffoldView(moduleName, relative, Files.isDirectory(path));
    }

    private String nthArtifactId(String xml, int index) {
        Matcher matcher = ARTIFACT_ID.matcher(xml);
        int cursor = 0;
        while (matcher.find()) {
            if (cursor == index) {
                return matcher.group(1);
            }
            cursor++;
        }
        return "";
    }

    private String extract(String xml, Pattern pattern) {
        Matcher matcher = pattern.matcher(xml);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String read(Path path) {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }
}
