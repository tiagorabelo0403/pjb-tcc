package com.tcc.pjb.backend.core.quality.finalclosure.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceIssue;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityIssue;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureBlockerView;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureReadinessView;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureSummary;
import com.tcc.pjb.backend.core.quality.finalclosure.domain.PjbFinalClosureSweepView;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbModuleBoundaryReadinessApplicationService;
import com.tcc.pjb.backend.core.quality.roadmap.application.PjbRoadmapClosureApplicationService;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapBlockingView;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapClosureSummary;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapMacroblockView;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbProjectPathResolver;
import org.springframework.stereotype.Service;

@Service
public class PjbFinalClosureApplicationService {

    private final BuildGateGovernanceService buildGateGovernanceService;
    private final PjbCodebaseSanityApplicationService codebaseSanityApplicationService;
    private final PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService;
    private final PjbModuleBoundaryReadinessApplicationService modularizationApplicationService;
    private final PjbRoadmapClosureApplicationService roadmapClosureApplicationService;
    private final AuditLedgerService auditLedgerService;
    private final Path projectRoot;

    public PjbFinalClosureApplicationService(BuildGateGovernanceService buildGateGovernanceService,
                                            PjbCodebaseSanityApplicationService codebaseSanityApplicationService,
                                            PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                            PjbModuleBoundaryReadinessApplicationService modularizationApplicationService,
                                            PjbRoadmapClosureApplicationService roadmapClosureApplicationService,
                                            AuditLedgerService auditLedgerService) {
        this(buildGateGovernanceService,
                codebaseSanityApplicationService,
                apiSurfaceSanityApplicationService,
                modularizationApplicationService,
                roadmapClosureApplicationService,
                auditLedgerService,
                Path.of(""));
    }

    public PjbFinalClosureApplicationService(BuildGateGovernanceService buildGateGovernanceService,
                                            PjbCodebaseSanityApplicationService codebaseSanityApplicationService,
                                            PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                            PjbModuleBoundaryReadinessApplicationService modularizationApplicationService,
                                            PjbRoadmapClosureApplicationService roadmapClosureApplicationService,
                                            AuditLedgerService auditLedgerService,
                                            Path projectRoot) {
        this.buildGateGovernanceService = Objects.requireNonNull(buildGateGovernanceService);
        this.codebaseSanityApplicationService = Objects.requireNonNull(codebaseSanityApplicationService);
        this.apiSurfaceSanityApplicationService = Objects.requireNonNull(apiSurfaceSanityApplicationService);
        this.modularizationApplicationService = Objects.requireNonNull(modularizationApplicationService);
        this.roadmapClosureApplicationService = Objects.requireNonNull(roadmapClosureApplicationService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.projectRoot = PjbProjectPathResolver.apiModuleRoot(projectRoot);
    }

    public PjbFinalClosureSummary summary() {
        BuildGateEvaluationResponse build = buildGateGovernanceService.evaluate();
        var codebase = codebaseSanityApplicationService.auditar();
        var api = apiSurfaceSanityApplicationService.auditar();
        var modularization = modularizationApplicationService.snapshot();
        PjbRoadmapClosureSummary roadmap = roadmapClosureApplicationService.summary();
        PjbFinalClosureSweepView sweep = sweep();
        List<PjbFinalClosureBlockerView> blockers = blockers();
        LinkedHashSet<String> criticalBlockers = new LinkedHashSet<>();
        for (PjbFinalClosureBlockerView blocker : blockers) {
            if ("CRITICO".equalsIgnoreCase(blocker.severity()) || "ALTO".equalsIgnoreCase(blocker.severity())) {
                criticalBlockers.add(blocker.summary());
            }
            if (criticalBlockers.size() == 8) {
                break;
            }
        }
        boolean roadmapClosed = roadmap.total() > 0 && roadmap.closed() == roadmap.total();
        boolean endToEndValidated = false;
        boolean overallReady = build.approved()
                && codebase.limpo()
                && api.limpo()
                && modularization.coreExtractionReady()
                && roadmapClosed
                && endToEndValidated;
        auditLedgerService.appendSafely(
                "FINAL_CLOSURE_SUMMARY_QUERY",
                "QUALITY",
                "PJB",
                "overall=" + overallReady + " blockers=" + blockers.size() + " roadmapClosed=" + roadmapClosed);
        return new PjbFinalClosureSummary(
                overallReady,
                build.approved(),
                codebase.limpo(),
                api.limpo(),
                modularization.coreExtractionReady(),
                roadmapClosed,
                endToEndValidated,
                roadmap.total(),
                roadmap.closed(),
                roadmap.partial(),
                roadmap.notStarted(),
                blockers.size(),
                sweep.adminControllers(),
                sweep.applicationServices(),
                roadmap.surfacedPartial(),
                List.copyOf(criticalBlockers),
                Instant.now());
    }

    public List<PjbFinalClosureBlockerView> blockers() {
        List<PjbFinalClosureBlockerView> out = new ArrayList<>();
        for (PjbRoadmapBlockingView blocker : roadmapClosureApplicationService.blockers()) {
            out.add(new PjbFinalClosureBlockerView("roadmap", blocker.code(), blocker.severity(), blocker.scope(), blocker.summary()));
        }
        for (PjbCodebaseSanityIssue issue : codebaseSanityApplicationService.auditar().issues()) {
            out.add(new PjbFinalClosureBlockerView("codebase", issue.codigo(), issue.severidade(), issue.arquivo(), issue.detalhe()));
        }
        for (PjbApiSurfaceIssue issue : apiSurfaceSanityApplicationService.auditar().issues()) {
            out.add(new PjbFinalClosureBlockerView("api-surface", issue.codigo(), issue.severidade(), issue.alvo(), String.join(" | ", issue.detalhes())));
        }
        BuildGateEvaluationResponse build = buildGateGovernanceService.evaluate();
        for (String issue : build.outstandingIssues()) {
            out.add(new PjbFinalClosureBlockerView("build-gate", "build.gate.issue", build.approved() ? "INFO" : "ALTO", "BuildGateGovernanceService", issue));
        }
        auditLedgerService.appendSafely(
                "FINAL_CLOSURE_BLOCKERS_QUERY",
                "QUALITY",
                "PJB",
                "count=" + out.size());
        return List.copyOf(out);
    }

    public List<PjbFinalClosureReadinessView> readiness() {
        BuildGateEvaluationResponse build = buildGateGovernanceService.evaluate();
        var codebase = codebaseSanityApplicationService.auditar();
        var api = apiSurfaceSanityApplicationService.auditar();
        var modularization = modularizationApplicationService.snapshot();
        PjbRoadmapClosureSummary roadmap = roadmapClosureApplicationService.summary();
        List<PjbFinalClosureReadinessView> views = List.of(
                new PjbFinalClosureReadinessView(
                        "build-gate",
                        build.approved() ? "READY" : "BLOCKED",
                        "OutstandingIssues=" + build.totalOutstandingIssues(),
                        build.nextActions()),
                new PjbFinalClosureReadinessView(
                        "codebase-sanity",
                        codebase.limpo() ? "READY" : "BLOCKED",
                        "Score=" + codebase.score() + ", issues=" + codebase.issues().size(),
                        codebase.issues().stream().limit(5).map(issue -> issue.codigo() + " => " + issue.detalhe()).toList()),
                new PjbFinalClosureReadinessView(
                        "api-surface",
                        api.limpo() ? "READY" : "BLOCKED",
                        "Score=" + api.score() + ", duplicatedRoutes=" + api.rotasDuplicadas(),
                        api.issues().stream().limit(5).map(issue -> issue.codigo() + " => " + issue.alvo()).toList()),
                new PjbFinalClosureReadinessView(
                        "modularization-phase1",
                        modularization.coreExtractionReady() ? "READY" : "BLOCKED",
                        "aggregatorPomPresent=" + modularization.aggregatorPomPresent() + ", blockers=" + modularization.blockers().size(),
                        modularization.recommendedNextActions()),
                new PjbFinalClosureReadinessView(
                        "roadmap-closure",
                        roadmap.total() > 0 && roadmap.closed() == roadmap.total() ? "READY" : "BLOCKED",
                        "closed=" + roadmap.closed() + "/" + roadmap.total() + ", partial=" + roadmap.partial() + ", notStarted=" + roadmap.notStarted(),
                        List.of("Fechar macroblocos ainda parciais ou não iniciados antes de declarar conclusão global.")),
                new PjbFinalClosureReadinessView(
                        "end-to-end-validation",
                        "BLOCKED",
                        "Não existe prova automática consolidada nesta base de build global + testes + integração executados nesta rodada.",
                        List.of("Executar build global.", "Executar testes unitários.", "Executar testes de integração.", "Consolidar evidência de validação antes de declarar término."))
        );
        auditLedgerService.appendSafely(
                "FINAL_CLOSURE_READINESS_QUERY",
                "QUALITY",
                "PJB",
                "dimensions=" + views.size());
        return views;
    }

    public PjbFinalClosureSweepView sweep() {
        List<String> adminControllers = listJavaNames(projectRoot.resolve("src/main/java/com/tcc/pjb/backend/controller/admin"), "Controller.java");
        List<String> applicationServices = listJavaNames(projectRoot.resolve("src/main/java/com/tcc/pjb/backend"), "ApplicationService.java");
        List<PjbRoadmapMacroblockView> macroblocks = roadmapClosureApplicationService.macroblocks();
        int partialWithoutAdminSurface = (int) macroblocks.stream()
                .filter(item -> "Parcial".equalsIgnoreCase(item.status()) && !item.adminSurfaceKnown())
                .count();
        PjbFinalClosureSweepView view = new PjbFinalClosureSweepView(
                adminControllers.size(),
                applicationServices.size(),
                (int) macroblocks.stream().filter(PjbRoadmapMacroblockView::adminSurfaceKnown).count(),
                partialWithoutAdminSurface,
                adminControllers.stream().sorted().limit(12).toList(),
                applicationServices.stream().sorted().limit(12).toList(),
                Instant.now());
        auditLedgerService.appendSafely(
                "FINAL_CLOSURE_SWEEP_QUERY",
                "QUALITY",
                "PJB",
                "controllers=" + view.adminControllers() + " applications=" + view.applicationServices());
        return view;
    }

    private List<String> listJavaNames(Path root, String suffix) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(suffix))
                    .map(path -> projectRoot.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/'))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }
}
