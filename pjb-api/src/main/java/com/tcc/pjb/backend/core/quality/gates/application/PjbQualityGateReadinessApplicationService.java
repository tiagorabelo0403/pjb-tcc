package com.tcc.pjb.backend.core.quality.gates.application;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbArchitectureQualityView;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbContractQualityView;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbDastQualityView;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbIntegrationQualityView;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbMutationQualityView;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbQualityBlockerView;
import com.tcc.pjb.backend.core.quality.gates.domain.PjbQualityGateSummary;
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
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbProjectPathResolver;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PjbQualityGateReadinessApplicationService {

    private final BuildGateGovernanceService buildGateGovernanceService;
    private final TestQualityMatrixService testQualityMatrixService;
    private final AuditLedgerService auditLedgerService;
    private final Path projectRoot;

    @Inject
    public PjbQualityGateReadinessApplicationService(BuildGateGovernanceService buildGateGovernanceService,
                                                     TestQualityMatrixService testQualityMatrixService,
                                                     AuditLedgerService auditLedgerService) {
        this(buildGateGovernanceService, testQualityMatrixService, auditLedgerService, Path.of(""));
    }

    PjbQualityGateReadinessApplicationService(BuildGateGovernanceService buildGateGovernanceService,
                                              TestQualityMatrixService testQualityMatrixService,
                                              AuditLedgerService auditLedgerService,
                                              Path projectRoot) {
        this.buildGateGovernanceService = Objects.requireNonNull(buildGateGovernanceService);
        this.testQualityMatrixService = Objects.requireNonNull(testQualityMatrixService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.projectRoot = PjbProjectPathResolver.apiModuleRoot(projectRoot);
    }

    @Transactional(readOnly = true)
    public PjbQualityGateSummary summary() {
        BuildGateEvaluationResponse build = buildGate();
        PjbArchitectureQualityView architecture = architecture();
        PjbContractQualityView contracts = contracts();
        PjbMutationQualityView mutation = mutation();
        PjbDastQualityView dast = dast();
        PjbIntegrationQualityView integration = integration();
        PjbQualityGateSummary summary = new PjbQualityGateSummary(
                build.approved(),
                build.qualityMatrixGateApproved(),
                architecture.ready(),
                contracts.ready(),
                mutation.ready(),
                dast.ready(),
                integration.ready(),
                blockers().size(),
                Instant.now());
        auditLedgerService.appendSafely("QUALITY_GATE_SUMMARY_QUERY", "QUALITY_GATES", "SUMMARY", null, "blockers=" + summary.blockerCount());
        return summary;
    }

    @Transactional(readOnly = true)
    public BuildGateEvaluationResponse buildGate() {
        return buildGateGovernanceService.evaluate();
    }

    @Transactional(readOnly = true)
    public TestQualityMatrixResponse matrix() {
        return testQualityMatrixService.verify();
    }

    @Transactional(readOnly = true)
    public PjbArchitectureQualityView architecture() {
        String pom = read(projectRoot.resolve("pom.xml"));
        boolean archUnitDependencyPresent = pom.contains("archunit-junit5");
        boolean architectureTestPresent = exists("src/test/java/com/tcc/pjb/backend/PjbArchitectureTest.java");
        boolean governanceScannerCoveragePresent = exists("src/test/java/com/tcc/pjb/backend/governance/layout/PomQualityGateGovernanceTest.java")
                && exists("src/test/java/com/tcc/pjb/backend/governance/source/SourceGovernanceScanner.java");
        int matchingTests = countTestFilesContaining("PjbArchitectureTest", "ArchUnit");
        LinkedHashSet<String> details = new LinkedHashSet<>();
        if (!archUnitDependencyPresent) {
            details.add("pom.sem.archunit");
        }
        if (!architectureTestPresent) {
            details.add("teste.arquitetura.ausente");
        }
        if (!governanceScannerCoveragePresent) {
            details.add("scanner.governanca.arquitetural.incompleto");
        }
        return new PjbArchitectureQualityView(
                archUnitDependencyPresent,
                architectureTestPresent,
                governanceScannerCoveragePresent,
                matchingTests,
                details.isEmpty(),
                List.copyOf(details));
    }

    @Transactional(readOnly = true)
    public PjbContractQualityView contracts() {
        Path workspacePom = PjbProjectPathResolver.workspaceRoot(projectRoot).resolve("pom.xml");
        String pom = read(projectRoot.resolve("pom.xml")) + "\n" + read(workspacePom);
        String workflow = read(PjbProjectPathResolver.workspaceRoot(projectRoot).resolve(".github/workflows/quality-gates.yml"));
        boolean pactConsumerDependencyPresent = pom.contains("au.com.dius.pact.consumer") && pom.contains("junit5");
        boolean pactProviderDependencyPresent = pom.contains("au.com.dius.pact.provider")
                && (pom.contains("spring6") || pom.contains("junit5"));
        boolean consumerContractTestsPresent = exists("src/test/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingCompetenceContractTest.java")
                || countTestFilesContaining("@Pact", "PactTestFor") > 0;
        boolean providerContractTestsPresent = countTestFilesContaining("@Provider", "PactVerificationSpring6Provider") > 0
                || countTestFilesContaining("@Provider", "PactVerificationInvocationContextProvider") > 0;
        boolean contractTestsPresent = consumerContractTestsPresent && providerContractTestsPresent;
        boolean pactOutputConfigured = pom.contains("target/pacts")
                || workflow.contains("target/pacts")
                || !read(projectRoot.resolve("src/test/resources/pacts/provider")).isBlank();
        boolean qualityWorkflowPresent = workflow.contains("pact")
                && workflow.contains("ProviderContractTest");
        int matchingTests = countTestFilesContaining("@Pact", "PactTestFor")
                + countTestFilesContaining("@Provider", "PactVerificationSpring6Provider")
                + countTestFilesContaining("@Provider", "PactVerificationInvocationContextProvider");
        LinkedHashSet<String> details = new LinkedHashSet<>();
        if (!pactConsumerDependencyPresent) {
            details.add("pom.sem.pact.consumer");
        }
        if (!pactProviderDependencyPresent) {
            details.add("pom.sem.pact.provider");
        }
        if (!consumerContractTestsPresent) {
            details.add("teste.contrato.consumer.ausente");
        }
        if (!providerContractTestsPresent) {
            details.add("teste.contrato.provider.ausente");
        }
        if (!pactOutputConfigured) {
            details.add("saida.pacts.nao.configurada");
        }
        if (!qualityWorkflowPresent) {
            details.add("workflow.qualidade.sem.provider-verification");
        }
        return new PjbContractQualityView(
                pactConsumerDependencyPresent && pactProviderDependencyPresent,
                contractTestsPresent,
                pactOutputConfigured,
                qualityWorkflowPresent,
                matchingTests,
                details.isEmpty(),
                List.copyOf(details));
    }

    @Transactional(readOnly = true)
    public PjbMutationQualityView mutation() {
        Path workspaceRoot = PjbProjectPathResolver.workspaceRoot(projectRoot);
        String pom = read(projectRoot.resolve("pom.xml")) + System.lineSeparator() + read(workspaceRoot.resolve("pom.xml"));
        String workflow = read(workspaceRoot.resolve(".github/workflows/quality-gates.yml"));
        boolean pluginPresent = pom.contains("pitest-maven");
        boolean thresholdConfigured = pom.contains("mutationThreshold") && pom.contains("coverageThreshold");
        boolean qualityWorkflowPresent = workflow.contains("mutation") || workflow.contains("pitest");
        boolean targetClassesConfigured = pom.contains("<targetClasses>") || pom.contains("targetClasses");
        LinkedHashSet<String> details = new LinkedHashSet<>();
        if (!pluginPresent) {
            details.add("plugin.pitest.ausente");
        }
        if (!thresholdConfigured) {
            details.add("threshold.mutacao.ausente");
        }
        if (!qualityWorkflowPresent) {
            details.add("workflow.qualidade.sem.mutation");
        }
        if (!targetClassesConfigured) {
            details.add("pitest.sem.target-classes");
        }
        return new PjbMutationQualityView(pluginPresent, thresholdConfigured, qualityWorkflowPresent, targetClassesConfigured, details.isEmpty(), List.copyOf(details));
    }

    @Transactional(readOnly = true)
    public PjbDastQualityView dast() {
        Path workspaceRoot = PjbProjectPathResolver.workspaceRoot(projectRoot);
        String workflow = read(workspaceRoot.resolve(".github/workflows/dast.yml"));
        String rules = read(workspaceRoot.resolve(".zap/rules.tsv"));
        boolean workflowPresent = !workflow.isBlank();
        boolean rulesPresent = !rules.isBlank();
        boolean openApiScanConfigured = workflow.contains("action-api-scan") || workflow.contains("/v3/api-docs");
        boolean stagingGuardPresent = workflow.toLowerCase(Locale.ROOT).contains("staging") || workflow.contains("localhost:8080");
        LinkedHashSet<String> details = new LinkedHashSet<>();
        if (!workflowPresent) {
            details.add("workflow.dast.ausente");
        }
        if (!rulesPresent) {
            details.add("zap.rules.ausente");
        }
        if (!openApiScanConfigured) {
            details.add("api.scan.open-api.ausente");
        }
        if (!stagingGuardPresent) {
            details.add("guard.staging.ausente");
        }
        return new PjbDastQualityView(workflowPresent, rulesPresent, openApiScanConfigured, stagingGuardPresent, details.isEmpty(), List.copyOf(details));
    }

    @Transactional(readOnly = true)
    public PjbIntegrationQualityView integration() {
        boolean baseClassPresent = exists("src/test/java/com/tcc/pjb/backend/PjbIntegrationTestBase.java");
        Path profilePath = projectRoot.resolve("src/test/resources/application-integration-test.yml");
        String profile = read(profilePath);
        boolean profilePresent = !profile.isBlank();
        boolean flywayEnabledInProfile = profile.contains("spring.flyway.enabled") || profile.contains("flyway:");
        int integrationTests = countTestFilesBySuffix("IT.java");
        LinkedHashSet<String> details = new LinkedHashSet<>();
        if (!baseClassPresent) {
            details.add("base.integration.ausente");
        }
        if (!profilePresent) {
            details.add("perfil.integration-test.ausente");
        }
        if (!flywayEnabledInProfile) {
            details.add("perfil.integration.sem.flyway");
        }
        if (integrationTests == 0) {
            details.add("nenhum.it.detectado");
        }
        return new PjbIntegrationQualityView(baseClassPresent, profilePresent, flywayEnabledInProfile, integrationTests, details.isEmpty(), List.copyOf(details));
    }

    @Transactional(readOnly = true)
    public List<PjbQualityBlockerView> blockers() {
        BuildGateEvaluationResponse build = buildGate();
        TestQualityMatrixResponse matrix = matrix();
        PjbArchitectureQualityView architecture = architecture();
        PjbContractQualityView contracts = contracts();
        PjbMutationQualityView mutation = mutation();
        PjbDastQualityView dast = dast();
        PjbIntegrationQualityView integration = integration();
        ArrayList<PjbQualityBlockerView> blockers = new ArrayList<>();
        if (!build.approved()) {
            blockers.add(new PjbQualityBlockerView("build", "build.gate.blocked", "CRITICO", "Gate estrutural de build ainda não aprovado"));
        }
        if (!build.qualityMatrixGateApproved()) {
            blockers.add(new PjbQualityBlockerView("build", "quality.matrix.blocked", "ALTO", "Matriz de testes mínima ainda insuficiente"));
        }
        for (String issue : build.outstandingIssues()) {
            blockers.add(new PjbQualityBlockerView("build", "build.issue", "ALTO", issue));
        }
        for (String risk : matrix.structuralRisks()) {
            blockers.add(new PjbQualityBlockerView("matrix", "test.matrix.risk", "MEDIO", risk));
        }
        addBlockers(blockers, "architecture", architecture.ready(), architecture.details(), "arquitetura");
        addBlockers(blockers, "contracts", contracts.ready(), contracts.details(), "contratos");
        addBlockers(blockers, "mutation", mutation.ready(), mutation.details(), "mutacao");
        addBlockers(blockers, "dast", dast.ready(), dast.details(), "dast");
        addBlockers(blockers, "integration", integration.ready(), integration.details(), "integration");
        auditLedgerService.appendSafely("QUALITY_GATE_BLOCKERS_QUERY", "QUALITY_GATES", "BLOCKERS", null, "count=" + blockers.size());
        return List.copyOf(blockers);
    }

    private void addBlockers(List<PjbQualityBlockerView> out,
                             String scope,
                             boolean ready,
                             List<String> details,
                             String prefix) {
        if (ready) {
            return;
        }
        if (details == null || details.isEmpty()) {
            out.add(new PjbQualityBlockerView(scope, prefix + ".blocked", "ALTO", scope + " ainda nao esta pronto"));
            return;
        }
        for (String detail : details) {
            out.add(new PjbQualityBlockerView(scope, prefix + ".blocked", "ALTO", detail));
        }
    }

    private boolean exists(String relative) {
        return Files.exists(projectRoot.resolve(relative));
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

    private int countTestFilesContaining(String... tokens) {
        Path testRoot = projectRoot.resolve("src/test/java");
        if (!Files.isDirectory(testRoot)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(testRoot)) {
            return (int) walk.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        String source = read(path);
                        for (String token : tokens) {
                            if (source.contains(token)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .count();
        } catch (IOException exception) {
            return 0;
        }
    }

    private int countTestFilesBySuffix(String suffix) {
        Path testRoot = projectRoot.resolve("src/test/java");
        if (!Files.isDirectory(testRoot)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(testRoot)) {
            return (int) walk.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(suffix))
                    .count();
        } catch (IOException exception) {
            return 0;
        }
    }
}
