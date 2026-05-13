package com.tcc.pjb.backend.governance.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PomQualityGateGovernanceTest {

    private static final Path POM = Files.exists(Path.of("..", "pom.xml")) ? Path.of("..", "pom.xml") : Path.of("pom.xml");

    @Test
    void pomDeveConterJacocoSurefireFailsafeEProfileDeQualityGates() throws IOException {
        String pom = Files.readString(POM);
        assertTrue(pom.contains("jacoco-maven-plugin"));
        assertTrue(pom.contains("prepare-agent"));
        assertTrue(pom.contains("prepare-agent-integration"));
        assertTrue(pom.contains("report-integration"));
        assertTrue(pom.contains("${surefireArgLine}"));
        assertTrue(pom.contains("${failsafeArgLine}"));
        assertTrue(pom.contains("<id>quality-gates</id>"));
        assertTrue(pom.contains("maven-checkstyle-plugin"));
        assertTrue(pom.contains("config/checkstyle/checkstyle.xml"));
        assertTrue(pom.contains("config/checkstyle/bounded-contexts.xml"));
    }
}
