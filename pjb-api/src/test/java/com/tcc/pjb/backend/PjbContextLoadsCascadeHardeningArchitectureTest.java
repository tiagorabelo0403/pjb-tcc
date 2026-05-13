package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class PjbContextLoadsCascadeHardeningArchitectureTest {

    private static final Path MAIN_SOURCES = Path.of("src", "main", "java");
    private static final Path TEST_PROFILE = Path.of("src", "test", "resources", "application-test.yml");
    private static final Pattern CONFIGURATION_PROPERTIES = Pattern.compile("@ConfigurationProperties\\s*\\(");
    private static final Pattern RECORD_DECLARATION = Pattern.compile("\\brecord\\s+([A-Z][A-Za-z0-9_]*)\\s*\\(");

    @Test
    void configurationPropertiesRecordsMustNotDeclareOverloadedConstructors() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path source : javaSources()) {
            String content = Files.readString(source, StandardCharsets.UTF_8);
            if (!CONFIGURATION_PROPERTIES.matcher(content).find()) {
                continue;
            }

            var recordMatcher = RECORD_DECLARATION.matcher(content);
            if (!recordMatcher.find()) {
                continue;
            }

            String simpleName = recordMatcher.group(1);
            Pattern overloadedConstructor = Pattern.compile("\\b(?:public|protected|private)\\s+" + Pattern.quote(simpleName) + "\\s*\\(");
            if (overloadedConstructor.matcher(content).find()) {
                violations.add(MAIN_SOURCES.relativize(source).toString());
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "ConfigurationProperties records com construtor sobrecarregado quebram o binding do Spring Boot e podem derrubar BackendApplicationTests.contextLoads: " + violations);
    }

    @Test
    void testProfileMustDisableSchedulingForDeterministicContextLoads() throws IOException {
        String content = Files.readString(TEST_PROFILE, StandardCharsets.UTF_8);
        assertTrue(
                content.contains("scheduling:") && content.contains("enabled: false"),
                "application-test.yml deve manter pjb.scheduling.enabled=false para impedir execução recorrente durante contextLoads");
    }

    private static List<Path> javaSources() throws IOException {
        try (var stream = Files.walk(MAIN_SOURCES)) {
            return stream.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }
}
