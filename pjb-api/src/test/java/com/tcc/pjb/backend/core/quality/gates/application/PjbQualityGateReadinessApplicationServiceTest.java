package com.tcc.pjb.backend.core.quality.gates.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.dto.governance.BuildGateEvaluationResponse;
import com.tcc.pjb.backend.model.dto.governance.TestQualityMatrixResponse;
import com.tcc.pjb.backend.service.governance.BuildGateGovernanceService;
import com.tcc.pjb.backend.service.governance.TestQualityMatrixService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PjbQualityGateReadinessApplicationServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void summary_deveConsolidarReadinessDeQualidade() throws Exception {
        Path apiModule = tempDir.resolve("pjb-api");
        Files.createDirectories(apiModule.resolve("src/test/java/com/tcc/pjb/backend"));
        Files.createDirectories(apiModule.resolve("src/test/java/com/tcc/pjb/backend/core/procedural"));
        Files.createDirectories(apiModule.resolve("src/test/resources"));
        Files.createDirectories(apiModule.resolve("src/test/resources/pacts/provider"));
        Files.createDirectories(tempDir.resolve(".github/workflows"));
        Files.createDirectories(tempDir.resolve(".zap"));
        Files.createDirectories(apiModule.resolve("src/test/java/com/tcc/pjb/backend/governance/layout"));
        Files.createDirectories(apiModule.resolve("src/test/java/com/tcc/pjb/backend/governance/source"));
        Files.writeString(tempDir.resolve("pom.xml"),
                "<project>archunit-junit5 au.com.dius.pact.consumer junit5 au.com.dius.pact.provider spring6 pitest-maven mutationThreshold coverageThreshold <targetClasses> target/pacts</project>",
                StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("pom.xml"),
                "<project>archunit-junit5 au.com.dius.pact.consumer junit5 au.com.dius.pact.provider spring6 pitest-maven mutationThreshold coverageThreshold <targetClasses> target/pacts</project>",
                StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("src/test/java/com/tcc/pjb/backend/PjbArchitectureTest.java"), "class PjbArchitectureTest {}", StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("src/test/java/com/tcc/pjb/backend/PjbIntegrationTestBase.java"), "class PjbIntegrationTestBase {}", StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("src/test/java/com/tcc/pjb/backend/core/procedural/NationalProceduralRoutingCompetenceContractTest.java"), "@Pact @PactTestFor class NationalProceduralRoutingCompetenceContractTest {}", StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("src/test/java/com/tcc/pjb/backend/PasskeyAuthControllerProviderContractTest.java"), "@Provider @ExtendWith(PactVerificationSpring6Provider.class) class PasskeyAuthControllerProviderContractTest {}", StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("src/test/java/com/tcc/pjb/backend/SampleIT.java"), "class SampleIT {}", StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("src/test/resources/pacts/provider/PjbAuthenticationConsumer-PjbAuthenticationProvider.json"), "{\"provider\":{\"name\":\"PjbAuthenticationProvider\"}}", StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("src/test/resources/application-integration-test.yml"), """
                spring:
                  flyway:
                    enabled: true
                """, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve(".github/workflows/quality-gates.yml"), """
                mutation
                target/pacts
                pact
                ProviderContractTest
                """, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve(".github/workflows/dast.yml"), """
                staging
                action-api-scan
                /v3/api-docs
                """, StandardCharsets.UTF_8);
        Files.writeString(tempDir.resolve(".zap/rules.tsv"), "10010 IGNORE\n", StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("src/test/java/com/tcc/pjb/backend/governance/layout/PomQualityGateGovernanceTest.java"), "class PomQualityGateGovernanceTest {}", StandardCharsets.UTF_8);
        Files.writeString(apiModule.resolve("src/test/java/com/tcc/pjb/backend/governance/source/SourceGovernanceScanner.java"), "class SourceGovernanceScanner {}", StandardCharsets.UTF_8);

        BuildGateGovernanceService build = mock(BuildGateGovernanceService.class);
        TestQualityMatrixService matrix = mock(TestQualityMatrixService.class);
        when(build.evaluate()).thenReturn(new BuildGateEvaluationResponse(true, true, true, true, true, true, true, 0, List.of(), List.of()));
        when(matrix.verify()).thenReturn(new TestQualityMatrixResponse(10, 5, 10, 5, 1, List.of("PrazoProcessualNacionalService"), List.of(), List.of()));

        PjbQualityGateReadinessApplicationService service = new PjbQualityGateReadinessApplicationService(build, matrix, mock(AuditLedgerService.class), tempDir);

        var summary = service.summary();

        assertThat(summary.buildApproved()).isTrue();
        assertThat(summary.architectureReady()).isTrue();
        assertThat(summary.contractReady()).isTrue();
        assertThat(summary.mutationReady()).isTrue();
        assertThat(summary.dastReady()).isTrue();
        assertThat(summary.integrationReady()).isTrue();
        assertThat(summary.blockerCount()).isZero();
    }

    @Test
    void blockers_deveApontarLacunasQuandoArquivosNaoExistem() {
        BuildGateGovernanceService build = mock(BuildGateGovernanceService.class);
        TestQualityMatrixService matrix = mock(TestQualityMatrixService.class);
        when(build.evaluate()).thenReturn(new BuildGateEvaluationResponse(false, true, true, true, true, true, false, 1, List.of("build bloqueado"), List.of()));
        when(matrix.verify()).thenReturn(new TestQualityMatrixResponse(0, 0, 1, 1, 1, List.of(), List.of("sem cobertura de integracao"), List.of()));

        PjbQualityGateReadinessApplicationService service = new PjbQualityGateReadinessApplicationService(build, matrix, mock(AuditLedgerService.class), tempDir);

        var blockers = service.blockers();

        assertThat(blockers).extracting("scope").contains("build", "architecture", "contracts", "mutation", "dast", "integration");
    }
}
