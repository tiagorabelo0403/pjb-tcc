package com.tcc.pjb.backend.core.quality.roadmap.application;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import com.tcc.pjb.backend.core.quality.modularization.application.PjbModuleBoundaryReadinessApplicationService;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryIssue;
import com.tcc.pjb.backend.core.quality.modularization.domain.PjbModuleBoundaryReadinessSnapshot;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapBlockingView;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapClosureSummary;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapMacroblockView;
import com.tcc.pjb.backend.core.quality.roadmap.domain.PjbRoadmapQualityGateView;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.governance.TestQualityMatrixService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

@Service
public class PjbRoadmapClosureApplicationService {

    private static final Pattern MACROBLOCK = Pattern.compile("^(\\d+)\\.\\s+(.+?)\\s+—\\s+\\*\\*(.+?)\\*\\*$");

    private final BuildGateGovernanceService buildGateGovernanceService;
    private final TestQualityMatrixService testQualityMatrixService;
    private final PjbCodebaseSanityApplicationService codebaseSanityApplicationService;
    private final PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService;
    private final PjbModuleBoundaryReadinessApplicationService modularizationApplicationService;
    private final Path projectRoot;

    @Inject
    public PjbRoadmapClosureApplicationService(BuildGateGovernanceService buildGateGovernanceService,
                                               TestQualityMatrixService testQualityMatrixService,
                                               PjbCodebaseSanityApplicationService codebaseSanityApplicationService,
                                               PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                               PjbModuleBoundaryReadinessApplicationService modularizationApplicationService) {
        this(buildGateGovernanceService, testQualityMatrixService, codebaseSanityApplicationService, apiSurfaceSanityApplicationService, modularizationApplicationService, Path.of(""));
    }

    static PjbRoadmapClosureApplicationService forProjectRoot(BuildGateGovernanceService buildGateGovernanceService,
                                                              TestQualityMatrixService testQualityMatrixService,
                                                              PjbCodebaseSanityApplicationService codebaseSanityApplicationService,
                                                              PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                                              PjbModuleBoundaryReadinessApplicationService modularizationApplicationService,
                                                              Path projectRoot) {
        return new PjbRoadmapClosureApplicationService(buildGateGovernanceService, testQualityMatrixService, codebaseSanityApplicationService, apiSurfaceSanityApplicationService, modularizationApplicationService, projectRoot);
    }

    private PjbRoadmapClosureApplicationService(BuildGateGovernanceService buildGateGovernanceService,
                                                TestQualityMatrixService testQualityMatrixService,
                                                PjbCodebaseSanityApplicationService codebaseSanityApplicationService,
                                                PjbApiSurfaceSanityApplicationService apiSurfaceSanityApplicationService,
                                                PjbModuleBoundaryReadinessApplicationService modularizationApplicationService,
                                                Path projectRoot) {
        this.buildGateGovernanceService = Objects.requireNonNull(buildGateGovernanceService);
        this.testQualityMatrixService = Objects.requireNonNull(testQualityMatrixService);
        this.codebaseSanityApplicationService = Objects.requireNonNull(codebaseSanityApplicationService);
        this.apiSurfaceSanityApplicationService = Objects.requireNonNull(apiSurfaceSanityApplicationService);
        this.modularizationApplicationService = Objects.requireNonNull(modularizationApplicationService);
        this.projectRoot = projectRoot == null ? Path.of("").toAbsolutePath().normalize() : projectRoot.toAbsolutePath().normalize();
    }

    public PjbRoadmapClosureSummary summary() {
        List<PjbRoadmapMacroblockView> macroblocks = macroblocks();
        int closed = (int) macroblocks.stream().filter(item -> "Fechado".equalsIgnoreCase(item.status())).count();
        int partial = (int) macroblocks.stream().filter(item -> "Parcial".equalsIgnoreCase(item.status())).count();
        int notStarted = (int) macroblocks.stream().filter(item -> "Não iniciado".equalsIgnoreCase(item.status())).count();
        int surfacedPartial = (int) macroblocks.stream().filter(item -> item.adminSurfaceKnown() && "Parcial".equalsIgnoreCase(item.status())).count();
        int completion = macroblocks.isEmpty() ? 0 : Math.round((closed * 100.0f) / macroblocks.size());
        return new PjbRoadmapClosureSummary(macroblocks.size(), closed, partial, notStarted, completion, surfacedPartial, blockers().size(), Instant.now());
    }

    public List<PjbRoadmapMacroblockView> macroblocks() {
        Path statusFile = projectRoot.resolve("docs/ROADMAP_23_MACROBLOCOS_STATUS.md");
        if (!Files.exists(statusFile)) {
            return List.of();
        }
        List<PjbRoadmapMacroblockView> out = new ArrayList<>();
        String currentPart = "";
        try {
            for (String line : Files.readAllLines(statusFile, StandardCharsets.UTF_8)) {
                if (line.startsWith("## Parte 1")) {
                    currentPart = "Parte 1";
                    continue;
                }
                if (line.startsWith("## Parte 2")) {
                    currentPart = "Parte 2";
                    continue;
                }
                Matcher matcher = MACROBLOCK.matcher(line.trim());
                if (!matcher.matches()) {
                    continue;
                }
                int number = Integer.parseInt(matcher.group(1));
                String name = matcher.group(2).trim();
                String status = matcher.group(3).trim();
                boolean adminSurfaceKnown = adminSurfaceKnown(number, currentPart, name);
                out.add(new PjbRoadmapMacroblockView(number, currentPart, name, status, operationalState(status, adminSurfaceKnown), adminSurfaceKnown));
            }
        } catch (IOException exception) {
            return List.of();
        }
        return List.copyOf(out);
    }

    public List<PjbRoadmapBlockingView> blockers() {
        List<PjbRoadmapBlockingView> blockers = new ArrayList<>();
        for (PjbRoadmapMacroblockView macroblock : macroblocks()) {
            if (!"Fechado".equalsIgnoreCase(macroblock.status())) {
                blockers.add(new PjbRoadmapBlockingView(
                        macroblock.part() + "#" + macroblock.number(),
                        "roadmap.status",
                        "Parcial".equalsIgnoreCase(macroblock.status()) ? "ALTO" : "CRITICO",
                        macroblock.name() + " ainda está em status " + macroblock.status()));
            }
        }
        for (PjbModuleBoundaryIssue issue : modularizationApplicationService.blockers()) {
            blockers.add(new PjbRoadmapBlockingView("modularization", issue.code(), issue.severity(), issue.summary()));
        }
        return List.copyOf(blockers);
    }

    public PjbRoadmapQualityGateView quality() {
        BuildGateEvaluationResponse build = buildGateGovernanceService.evaluate();
        TestQualityMatrixResponse matrix = testQualityMatrixService.verify();
        PjbCodebaseSanityAggregate codebase = codebaseSanityApplicationService.auditar();
        PjbApiSurfaceSanityAggregate api = apiSurfaceSanityApplicationService.auditar();
        PjbModuleBoundaryReadinessSnapshot modularization = modularizationApplicationService.snapshot();
        LinkedHashSet<String> recommendations = new LinkedHashSet<>();
        recommendations.addAll(build.nextActions());
        recommendations.addAll(matrix.recommendations());
        recommendations.addAll(modularization.recommendedNextActions());
        int totalOutstandingIssues = build.totalOutstandingIssues() + codebase.issues().size() + api.issues().size() + modularization.blockers().size();
        return new PjbRoadmapQualityGateView(
                build.approved(),
                api.limpo(),
                codebase.limpo(),
                modularization.coreExtractionReady(),
                totalOutstandingIssues,
                List.copyOf(recommendations),
                matrix.criticalModules());
    }

    private boolean adminSurfaceKnown(int number, String part, String name) {
        if ("Parte 1".equals(part) && number == 8) {
            return false;
        }
        if (name.toLowerCase().contains("multi-module")) {
            return true;
        }
        return true;
    }

    private String operationalState(String status, boolean adminSurfaceKnown) {
        if ("Fechado".equalsIgnoreCase(status)) {
            return "FECHADO";
        }
        if (adminSurfaceKnown) {
            return "MATERIALIZADO";
        }
        return "ESTRUTURAL";
    }
}
