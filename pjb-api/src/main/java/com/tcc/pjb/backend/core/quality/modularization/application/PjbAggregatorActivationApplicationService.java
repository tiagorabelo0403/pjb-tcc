package com.tcc.pjb.backend.core.quality.modularization.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbAggregatorActivationChecklistView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbAggregatorActivationSnapshot;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbAggregatorModuleLinkView;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbAggregatorPomPatchView;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
@Service
public class PjbAggregatorActivationApplicationService {

    private static final List<String> MODULES = List.of("pjb-core", "pjb-api");
    private static final String AGGREGATOR_FILE = "pom.phase1-aggregator.xml";

    private final AuditLedgerService auditLedgerService;
    private final Path projectRoot;

    @Inject
    @Autowired
    public PjbAggregatorActivationApplicationService(AuditLedgerService auditLedgerService) {
        this(auditLedgerService, Path.of(""));
    }

    PjbAggregatorActivationApplicationService(AuditLedgerService auditLedgerService, Path projectRoot) {
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.projectRoot = projectRoot == null ? Path.of("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public PjbAggregatorActivationSnapshot snapshot() {
        List<PjbAggregatorActivationChecklistView> checklist = checklist();
        long satisfiedCount = checklist.stream().filter(item -> "READY".equals(item.status()) || "INFO".equals(item.status())).count();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        for (PjbAggregatorActivationChecklistView item : checklist) {
            if ("BLOCKED".equals(item.status())) {
                blockers.add(item.summary());
            }
        }
        boolean activationReady = checklist.stream().noneMatch(item -> "BLOCKED".equals(item.status()));
        boolean aggregatorPresent = Files.exists(projectRoot.resolve(AGGREGATOR_FILE));
        boolean rootLinked = isRootPomLinked();
        PjbAggregatorActivationSnapshot snapshot = new PjbAggregatorActivationSnapshot(
                aggregatorPresent,
                rootLinked,
                activationReady,
                MODULES.size(),
                (int) satisfiedCount,
                List.copyOf(blockers),
                Instant.now());
        auditLedgerService.appendSafely("MODULARIZATION_AGGREGATOR_SNAPSHOT_QUERY", "MODULARIZATION", "AGGREGATOR", null, "ready=" + snapshot.activationReady());
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<PjbAggregatorModuleLinkView> moduleLinks() {
        String aggregatorXml = read(projectRoot.resolve(AGGREGATOR_FILE));
        String rootPomXml = read(projectRoot.resolve("pom.xml"));
        List<PjbAggregatorModuleLinkView> views = MODULES.stream()
                .map(module -> new PjbAggregatorModuleLinkView(
                        module,
                        module + "/pom.xml",
                        Files.isDirectory(projectRoot.resolve(module)),
                        Files.exists(projectRoot.resolve(module + "/pom.xml")),
                        aggregatorXml.contains("<module>" + module + "</module>"),
                        rootPomXml.contains("<module>" + module + "</module>")))
                .toList();
        auditLedgerService.appendSafely("MODULARIZATION_AGGREGATOR_LINKS_QUERY", "MODULARIZATION", "AGGREGATOR", null, "count=" + views.size());
        return views;
    }

    @Transactional(readOnly = true)
    public List<PjbAggregatorActivationChecklistView> checklist() {
        List<PjbAggregatorModuleLinkView> links = moduleLinks();
        List<PjbAggregatorActivationChecklistView> items = new ArrayList<>();
        boolean aggregatorPresent = Files.exists(projectRoot.resolve(AGGREGATOR_FILE));
        items.add(new PjbAggregatorActivationChecklistView(
                "phase1.aggregator.file",
                aggregatorPresent ? "READY" : "BLOCKED",
                aggregatorPresent ? "Arquivo de agregador da Fase 1 foi gerado." : "Arquivo de agregador da Fase 1 ainda nao foi gerado.",
                List.of(AGGREGATOR_FILE)
        ));
        boolean pomsPresent = links.stream().allMatch(PjbAggregatorModuleLinkView::pomPresent);
        items.add(new PjbAggregatorActivationChecklistView(
                "phase1.module.poms",
                pomsPresent ? "READY" : "BLOCKED",
                pomsPresent ? "POMs fisicos dos modulos scaffold existem." : "Ainda faltam POMs fisicos de modulo para a Fase 1.",
                links.stream().map(PjbAggregatorModuleLinkView::relativePomPath).toList()
        ));
        boolean directoriesPresent = links.stream().allMatch(PjbAggregatorModuleLinkView::directoryPresent);
        items.add(new PjbAggregatorActivationChecklistView(
                "phase1.module.directories",
                directoriesPresent ? "READY" : "BLOCKED",
                directoriesPresent ? "Diretorios fisicos dos modulos scaffold existem." : "Ainda faltam diretorios fisicos de modulo para a Fase 1.",
                links.stream().map(PjbAggregatorModuleLinkView::moduleName).toList()
        ));
        boolean aggregatorListsModules = links.stream().allMatch(PjbAggregatorModuleLinkView::listedInAggregatorFile);
        items.add(new PjbAggregatorActivationChecklistView(
                "phase1.aggregator.modules",
                aggregatorListsModules ? "READY" : "BLOCKED",
                aggregatorListsModules ? "Agregador gerado lista pjb-core e pjb-api." : "Agregador gerado ainda nao lista todos os modulos da Fase 1.",
                MODULES
        ));
        String rootPom = read(projectRoot.resolve("pom.xml"));
        boolean rootPomPackagingPom = rootPom.contains("<packaging>pom</packaging>");
        items.add(new PjbAggregatorActivationChecklistView(
                "root.packaging.pom",
                rootPomPackagingPom ? "READY" : "BLOCKED",
                rootPomPackagingPom ? "POM raiz ja usa packaging pom." : "POM raiz ainda nao usa packaging pom; ativacao direta de modules continua bloqueada.",
                List.of("pom.xml")
        ));
        boolean rootLinked = isRootPomLinked();
        items.add(new PjbAggregatorActivationChecklistView(
                "root.modules.linked",
                rootLinked ? "READY" : "INFO",
                rootLinked ? "POM raiz ja declara modules da Fase 1." : "POM raiz ainda nao declara modules; fase permanece em scaffold/pre-activation.",
                MODULES
        ));
        auditLedgerService.appendSafely("MODULARIZATION_AGGREGATOR_CHECKLIST_QUERY", "MODULARIZATION", "AGGREGATOR", null, "count=" + items.size());
        return List.copyOf(items);
    }

    @Transactional(readOnly = true)
    public PjbAggregatorPomPatchView pomPatch() {
        Path patchFile = projectRoot.resolve(AGGREGATOR_FILE);
        List<String> patchLines = List.of(
                "<packaging>pom</packaging>",
                "<modules>",
                "    <module>pjb-core</module>",
                "    <module>pjb-api</module>",
                "</modules>",
                "<!-- manter ativacao somente apos primeiro pacote seguro extraido para pjb-core -->"
        );
        PjbAggregatorPomPatchView view = new PjbAggregatorPomPatchView(
                AGGREGATOR_FILE,
                Files.exists(patchFile),
                MODULES,
                patchLines,
                Instant.now());
        auditLedgerService.appendSafely("MODULARIZATION_AGGREGATOR_PATCH_QUERY", "MODULARIZATION", "AGGREGATOR", null, "modules=" + MODULES.size());
        return view;
    }

    private boolean isRootPomLinked() {
        String rootPom = read(projectRoot.resolve("pom.xml"));
        return MODULES.stream().allMatch(module -> rootPom.contains("<module>" + module + "</module>"));
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
