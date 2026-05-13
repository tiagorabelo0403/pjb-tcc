package com.tcc.pjb.backend.core.quality.codebase.application;

import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseCriticalFlow;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseExtractionBlueprint;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseExtractionLane;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseLearningAggregate;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseLearningSlice;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

final class PjbCodebaseLearningSnapshotBuilder {

    private final PjbCodebaseProjectLayout layout;
    private final PjbCodebaseLearningSettings settings;
    private final PjbCodebaseSourceExplorer sourceExplorer;

    PjbCodebaseLearningSnapshotBuilder(PjbCodebaseProjectLayout layout,
                                       PjbCodebaseLearningSettings settings,
                                       PjbCodebaseSourceExplorer sourceExplorer) {
        this.layout = Objects.requireNonNull(layout);
        this.settings = Objects.requireNonNull(settings);
        this.sourceExplorer = Objects.requireNonNull(sourceExplorer);
    }

    PjbCodebaseLearningAggregate build(Instant generatedAt) {
        if (!Files.isDirectory(layout.mainRoot())) {
            return new PjbCodebaseLearningAggregate(false, 0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), generatedAt);
        }
        List<Path> mainFiles = sourceExplorer.listJavaFiles(layout.mainRoot());
        List<Path> testFiles = Files.isDirectory(layout.testRoot()) ? sourceExplorer.listJavaFiles(layout.testRoot()) : List.of();
        List<Path> integrationTestFiles = testFiles.stream().filter(this::isIntegrationTest).toList();
        Map<String, SliceAccumulator> slices = mapSlices(mainFiles, testFiles);
        enrichDependencies(mainFiles, slices);
        List<PjbCodebaseLearningSlice> hotspots = slices.values().stream()
                .filter(SliceAccumulator::candidate)
                .map(SliceAccumulator::toSlice)
                .sorted(Comparator.comparingInt(PjbCodebaseLearningSlice::pressaoExtracao).reversed()
                        .thenComparing(PjbCodebaseLearningSlice::fatia))
                .limit(10)
                .toList();
        List<PjbCodebaseExtractionBlueprint> blueprints = buildBlueprints(hotspots);
        List<PjbCodebaseCriticalFlow> criticalFlows = buildCriticalFlows(integrationTestFiles);
        return new PjbCodebaseLearningAggregate(
                true,
                mainFiles.size(),
                testFiles.size(),
                integrationTestFiles.size(),
                (int) slices.keySet().stream().filter(PjbCodebaseLearningSnapshotBuilder::isTrackedCoreSlice).count(),
                hotspots,
                blueprints,
                criticalFlows,
                buildWaves(hotspots, blueprints),
                buildLearnings(hotspots, blueprints, criticalFlows, integrationTestFiles.size(), mainFiles.size()),
                generatedAt
        );
    }

    private Map<String, SliceAccumulator> mapSlices(List<Path> mainFiles, List<Path> testFiles) {
        Map<String, SliceAccumulator> slices = new LinkedHashMap<>();
        for (Path file : mainFiles) {
            Path relative = layout.mainRoot().relativize(file);
            String slice = sourceExplorer.sliceFromRelative(relative);
            if (!isTrackedCoreSlice(slice)) {
                continue;
            }
            slices.computeIfAbsent(slice, SliceAccumulator::new)
                    .registerMain(sourceExplorer.laneFromRelative(relative));
        }
        for (Path file : testFiles) {
            Path relative = layout.testRoot().relativize(file);
            String slice = sourceExplorer.sliceFromRelative(relative);
            if (!isTrackedCoreSlice(slice)) {
                continue;
            }
            slices.computeIfAbsent(slice, SliceAccumulator::new)
                    .registerTest(sourceExplorer.laneFromRelative(relative));
        }
        return slices;
    }

    private void enrichDependencies(List<Path> mainFiles, Map<String, SliceAccumulator> slices) {
        for (Path file : mainFiles) {
            String sourceSlice = sourceExplorer.sliceFromRelative(layout.mainRoot().relativize(file));
            Set<String> importedSlices = sourceExplorer.importedSlices(sourceExplorer.read(file), settings.basePackage());
            if (isTrackedCoreSlice(sourceSlice)) {
                SliceAccumulator source = slices.computeIfAbsent(sourceSlice, SliceAccumulator::new);
                for (String importedSlice : importedSlices) {
                    if (importedSlice.equals(sourceSlice) || !isTrackedCoreSlice(importedSlice)) {
                        continue;
                    }
                    source.outgoingCoreDependencies.add(importedSlice);
                    slices.computeIfAbsent(importedSlice, SliceAccumulator::new).incomingCoreDependencies.add(sourceSlice);
                }
            }
            if (sourceSlice.startsWith("controller/")) {
                for (String importedSlice : importedSlices) {
                    if (isTrackedCoreSlice(importedSlice)) {
                        slices.computeIfAbsent(importedSlice, SliceAccumulator::new).controllerConsumers.add(sourceSlice);
                    }
                }
            }
        }
    }

    private boolean isIntegrationTest(Path file) {
        return Objects.toString(file.getFileName(), "").endsWith("IT.java");
    }

    private static boolean isTrackedCoreSlice(String slice) {
        return slice.startsWith("core/");
    }

    private List<PjbCodebaseExtractionBlueprint> buildBlueprints(List<PjbCodebaseLearningSlice> hotspots) {
        return hotspots.stream()
                .flatMap(slice -> slice.trilhasExtracao().stream().map(lane -> toBlueprint(slice, lane)))
                .sorted(Comparator.comparingInt(PjbCodebaseExtractionBlueprint::scorePrioridade).reversed()
                        .thenComparing(PjbCodebaseExtractionBlueprint::fatia)
                        .thenComparing(PjbCodebaseExtractionBlueprint::trilha))
                .limit(settings.blueprintLimit())
                .toList();
    }

    private PjbCodebaseExtractionBlueprint toBlueprint(PjbCodebaseLearningSlice slice, PjbCodebaseExtractionLane lane) {
        String packageTarget = targetPackage(slice.fatia(), lane.nome());
        String classStem = classStem(slice.fatia(), lane.nome());
        ArrayList<String> blockers = new ArrayList<>();
        if (PjbCodebaseLearningMessages.laneHarden().equals(lane.prontidao())) {
            blockers.add("Baixa cobertura de testes para trilha crítica.");
        }
        if (slice.consumidoresController() > 0) {
            blockers.add("Há pressão direta de controllers sobre a fatia e os DTOs precisam ser congelados primeiro.");
        }
        blockers.addAll(lane.sinais());
        ArrayList<String> actions = new ArrayList<>();
        actions.add(PjbCodebaseLearningMessages.blueprintContractStabilization(packageTarget));
        actions.add(PjbCodebaseLearningMessages.blueprintIntegrationFirst(classStem + "IT"));
        actions.add(PjbCodebaseLearningMessages.blueprintModuleAfterCoverage(packageTarget));
        int score = Math.max(0, slice.pressaoExtracao() + lane.arquivosMain() * 4 - lane.arquivosTeste() * 2 + readinessBoost(lane.prontidao()));
        return new PjbCodebaseExtractionBlueprint(
                slice.fatia(),
                lane.nome(),
                lane.prontidao(),
                score,
                packageTarget,
                classStem + "Facade",
                classStem + "Port",
                classStem + "IT",
                List.copyOf(blockers),
                List.copyOf(actions)
        );
    }

    private List<PjbCodebaseCriticalFlow> buildCriticalFlows(List<Path> integrationTestFiles) {
        ArrayList<PjbCodebaseCriticalFlow> flows = new ArrayList<>();
        for (PjbCodebaseCriticalFlowDefinition flow : settings.criticalFlows()) {
            ArrayList<String> relatedTests = new ArrayList<>();
            LinkedHashSet<String> coveredTokens = new LinkedHashSet<>();
            for (Path file : integrationTestFiles) {
                String probe = (Objects.toString(file.getFileName(), "") + "\n" + sourceExplorer.read(file)).toLowerCase(Locale.ROOT);
                LinkedHashSet<String> localTokens = new LinkedHashSet<>();
                for (String token : flow.tokens()) {
                    if (probe.contains(token)) {
                        localTokens.add(token);
                    }
                }
                if (!localTokens.isEmpty()) {
                    relatedTests.add(file.getFileName().toString());
                    coveredTokens.addAll(localTokens);
                }
            }
            double coverage = flow.tokens().isEmpty() ? 0.0d : (double) coveredTokens.size() / (double) flow.tokens().size();
            String status = coverage >= 0.75d && !relatedTests.isEmpty() ? "COBERTO" : coverage >= 0.30d ? "PARCIAL" : "AUSENTE";
            ArrayList<String> signals = new ArrayList<>();
            switch (status) {
                case "COBERTO" -> signals.add(PjbCodebaseLearningMessages.criticalFlowCovered(flow.nome(), coverage));
                case "PARCIAL" -> signals.add(PjbCodebaseLearningMessages.criticalFlowPartial(flow.nome(), coverage));
                default -> signals.add(PjbCodebaseLearningMessages.criticalFlowMissing(flow.nome()));
            }
            ArrayList<String> actions = new ArrayList<>();
            if ("AUSENTE".equals(status)) {
                actions.add(PjbCodebaseLearningMessages.criticalFlowActionCreate(flow.nome()));
            } else if ("PARCIAL".equals(status)) {
                actions.add(PjbCodebaseLearningMessages.criticalFlowActionExpand(flow.nome()));
            }
            actions.add(PjbCodebaseLearningMessages.criticalFlowActionVerify());
            flows.add(new PjbCodebaseCriticalFlow(flow.nome(), status, coverage, List.copyOf(relatedTests), List.copyOf(signals), List.copyOf(actions)));
        }
        return List.copyOf(flows);
    }

    private List<String> buildWaves(List<PjbCodebaseLearningSlice> hotspots, List<PjbCodebaseExtractionBlueprint> blueprints) {
        if (hotspots.isEmpty()) {
            return List.of("Sem hotspots core suficientes para recomendar onda estrutural neste snapshot.");
        }
        ArrayList<String> waves = new ArrayList<>();
        List<String> firstWave = hotspots.stream().limit(2).map(PjbCodebaseLearningSlice::fatia).toList();
        waves.add(PjbCodebaseLearningMessages.firstWave(String.join(", ", firstWave)));
        List<String> secondWave = hotspots.stream().skip(2).limit(3).map(PjbCodebaseLearningSlice::fatia).toList();
        if (!secondWave.isEmpty()) {
            waves.add(PjbCodebaseLearningMessages.secondWave(String.join(", ", secondWave)));
        }
        List<String> hardeningWave = hotspots.stream()
                .filter(item -> item.razaoTeste() < settings.testRatioFloor())
                .limit(3)
                .map(PjbCodebaseLearningSlice::fatia)
                .toList();
        if (!hardeningWave.isEmpty()) {
            waves.add("Onda paralela de endurecimento: elevar cobertura e contratos em " + String.join(", ", hardeningWave) + ".");
        }
        List<String> blueprintWave = blueprints.stream()
                .limit(3)
                .map(item -> item.fatia() + "/" + item.trilha())
                .toList();
        if (!blueprintWave.isEmpty()) {
            waves.add("Blueprints imediatos de extração: iniciar por " + String.join(", ", blueprintWave) + ".");
        }
        return List.copyOf(waves);
    }

    private List<String> buildLearnings(List<PjbCodebaseLearningSlice> hotspots,
                                        List<PjbCodebaseExtractionBlueprint> blueprints,
                                        List<PjbCodebaseCriticalFlow> criticalFlows,
                                        int integrationTests,
                                        int mainFiles) {
        if (hotspots.isEmpty()) {
            return List.of(PjbCodebaseLearningMessages.learningLegacyJudgeIsolation());
        }
        ArrayList<String> learnings = new ArrayList<>();
        learnings.add(PjbCodebaseLearningMessages.learningMainConcentration(
                hotspots.stream().limit(3).map(PjbCodebaseLearningSlice::fatia).collect(Collectors.joining(", "))
        ));
        String testDebt = hotspots.stream()
                .filter(item -> item.razaoTeste() < settings.testRatioFloor())
                .limit(3)
                .map(PjbCodebaseLearningSlice::fatia)
                .collect(Collectors.joining(", "));
        if (!testDebt.isBlank()) {
            learnings.add(PjbCodebaseLearningMessages.learningTestDebt(testDebt));
        }
        String controllerPressure = hotspots.stream()
                .filter(item -> item.consumidoresController() > 0)
                .limit(3)
                .map(PjbCodebaseLearningSlice::fatia)
                .collect(Collectors.joining(", "));
        if (!controllerPressure.isBlank()) {
            learnings.add(PjbCodebaseLearningMessages.learningControllerPressure(controllerPressure));
        }
        double integrationRatio = mainFiles == 0 ? 0.0d : (double) integrationTests / (double) mainFiles;
        learnings.add(PjbCodebaseLearningMessages.learningIntegrationDebt(integrationTests, integrationRatio));
        String blueprintFocus = blueprints.stream()
                .limit(3)
                .map(item -> item.fatia() + "/" + item.trilha())
                .collect(Collectors.joining(", "));
        if (!blueprintFocus.isBlank()) {
            learnings.add(PjbCodebaseLearningMessages.learningBlueprintFocus(blueprintFocus));
        }
        String criticalFlowDebt = criticalFlows.stream()
                .filter(item -> !"COBERTO".equals(item.status()))
                .limit(3)
                .map(PjbCodebaseCriticalFlow::nome)
                .collect(Collectors.joining(", "));
        if (!criticalFlowDebt.isBlank()) {
            learnings.add(PjbCodebaseLearningMessages.learningCriticalFlowDebt(criticalFlowDebt));
        }
        learnings.add(PjbCodebaseLearningMessages.learningLegacyJudgeIsolation());
        return List.copyOf(learnings);
    }

    private static int readinessBoost(String readiness) {
        return switch (Objects.toString(readiness, PjbCodebaseLearningMessages.lanePrepare())) {
            case "PRONTA" -> 120;
            case "PREPARAR" -> 60;
            default -> 0;
        };
    }

    private static String targetPackage(String slice, String lane) {
        return "com.tcc.pjb.backend." + slice.replace('/', '.') + "." + normalizeSegment(lane);
    }

    private static String classStem(String slice, String lane) {
        String[] parts = slice.split("/");
        String second = parts.length > 1 ? parts[1] : slice;
        return capitalize(normalizeSegment(second)) + capitalize(normalizeSegment(lane));
    }

    private static String normalizeSegment(String value) {
        return Objects.toString(value, "canonico").trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
    }

    private static String capitalize(String value) {
        String normalized = Objects.toString(value, "").replace('-', ' ').trim();
        if (normalized.isBlank()) {
            return "Canonico";
        }
        StringBuilder builder = new StringBuilder();
        for (String token : normalized.split("\\s+")) {
            if (!token.isBlank()) {
                builder.append(Character.toUpperCase(token.charAt(0))).append(token.substring(1));
            }
        }
        return builder.toString();
    }

    private final class SliceAccumulator {

        private final String slice;
        private int mainFiles;
        private int testFiles;
        private final Set<String> incomingCoreDependencies = new LinkedHashSet<>();
        private final Set<String> outgoingCoreDependencies = new LinkedHashSet<>();
        private final Set<String> controllerConsumers = new LinkedHashSet<>();
        private final Map<String, LaneAccumulator> lanes = new LinkedHashMap<>();

        private SliceAccumulator(String slice) {
            this.slice = Objects.toString(slice, "").trim();
        }

        private void registerMain(String lane) {
            mainFiles++;
            lanes.computeIfAbsent(normalizeLane(lane), LaneAccumulator::new).mainFiles++;
        }

        private void registerTest(String lane) {
            testFiles++;
            lanes.computeIfAbsent(normalizeLane(lane), LaneAccumulator::new).testFiles++;
        }

        private boolean candidate() {
            return mainFiles >= 30 || incomingCoreDependencies.size() >= 10 || (controllerConsumers.size() >= 3 && mainFiles >= 10);
        }

        private PjbCodebaseLearningSlice toSlice() {
            double testRatio = mainFiles == 0 ? 0.0d : (double) testFiles / (double) mainFiles;
            ArrayList<String> signals = new ArrayList<>();
            if (testRatio < settings.testRatioFloor()) {
                signals.add(PjbCodebaseLearningMessages.testDebt(testRatio));
            }
            if (incomingCoreDependencies.size() >= 10) {
                signals.add(PjbCodebaseLearningMessages.incomingPressure(incomingCoreDependencies.size()));
            }
            if (outgoingCoreDependencies.size() >= 10) {
                signals.add(PjbCodebaseLearningMessages.outgoingPressure(outgoingCoreDependencies.size()));
            }
            if (!controllerConsumers.isEmpty()) {
                signals.add(PjbCodebaseLearningMessages.controllerPressure(controllerConsumers.size()));
            }
            ArrayList<String> actions = new ArrayList<>();
            actions.add(PjbCodebaseLearningMessages.freezeExpansion());
            actions.add(PjbCodebaseLearningMessages.extractPortsAndFacade());
            if (testRatio < settings.testRatioFloor()) {
                actions.add(PjbCodebaseLearningMessages.raiseTestDensity());
            }
            if (!controllerConsumers.isEmpty()) {
                actions.add(PjbCodebaseLearningMessages.prioritizeControllerBoundary());
            }
            List<PjbCodebaseExtractionLane> extractionLanes = lanes.values().stream()
                    .filter(item -> item.mainFiles > 0)
                    .sorted(Comparator.comparingInt(LaneAccumulator::pressure).reversed().thenComparing(item -> item.lane))
                    .map(LaneAccumulator::toLane)
                    .limit(4)
                    .toList();
            return new PjbCodebaseLearningSlice(
                    slice,
                    mainFiles,
                    testFiles,
                    incomingCoreDependencies.size(),
                    outgoingCoreDependencies.size(),
                    controllerConsumers.size(),
                    testRatio,
                    pressure(testRatio),
                    priority(testRatio),
                    List.copyOf(signals),
                    List.copyOf(actions),
                    extractionLanes
            );
        }

        private int pressure(double testRatio) {
            int score = mainFiles * 3 + incomingCoreDependencies.size() * 8 + outgoingCoreDependencies.size() * 4 + controllerConsumers.size() * 10;
            if (testRatio < settings.testRatioFloor()) {
                score += (int) Math.round((settings.testRatioFloor() - testRatio) * 200.0d);
            }
            return Math.max(score, 0);
        }

        private String priority(double testRatio) {
            if (mainFiles >= 120 || (mainFiles >= 80 && incomingCoreDependencies.size() >= 12 && testRatio < settings.testRatioFloor())) {
                return "CRITICA";
            }
            if (mainFiles >= 50 || incomingCoreDependencies.size() >= 8 || (controllerConsumers.size() >= 4 && mainFiles >= 10)) {
                return "ALTA";
            }
            return "MODERADA";
        }

        private String normalizeLane(String lane) {
            String normalized = Objects.toString(lane, "canonico").trim();
            return normalized.isBlank() ? "canonico" : normalized;
        }

        private final class LaneAccumulator {

            private final String lane;
            private int mainFiles;
            private int testFiles;

            private LaneAccumulator(String lane) {
                this.lane = lane;
            }

            private int pressure() {
                double ratio = testRatio();
                int score = mainFiles * 5;
                if (ratio < settings.testRatioFloor()) {
                    score += (int) Math.round((settings.testRatioFloor() - ratio) * 120.0d);
                }
                return Math.max(score, 0);
            }

            private double testRatio() {
                return mainFiles == 0 ? 0.0d : (double) testFiles / (double) mainFiles;
            }

            private PjbCodebaseExtractionLane toLane() {
                double ratio = testRatio();
                ArrayList<String> signals = new ArrayList<>();
                signals.add(PjbCodebaseLearningMessages.laneCanonicalSurface(lane));
                if (ratio < settings.testRatioFloor()) {
                    signals.add(PjbCodebaseLearningMessages.laneNeedsCoverage(lane, ratio));
                }
                ArrayList<String> actions = new ArrayList<>();
                actions.add(PjbCodebaseLearningMessages.laneActionStabilizeContracts(lane));
                if (ratio < settings.laneReadyFloor()) {
                    actions.add(PjbCodebaseLearningMessages.laneActionRaiseCoverage(lane));
                }
                actions.add(PjbCodebaseLearningMessages.laneActionSeparateModule(lane));
                return new PjbCodebaseExtractionLane(
                        lane,
                        mainFiles,
                        testFiles,
                        ratio,
                        readiness(ratio),
                        List.copyOf(signals),
                        List.copyOf(actions)
                );
            }

            private String readiness(double ratio) {
                if (mainFiles >= 20 && ratio >= settings.laneReadyFloor()) {
                    return PjbCodebaseLearningMessages.laneReady();
                }
                if (ratio < settings.laneHardenThreshold()) {
                    return PjbCodebaseLearningMessages.laneHarden();
                }
                return PjbCodebaseLearningMessages.lanePrepare();
            }
        }
    }
}
