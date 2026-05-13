package com.tcc.pjb.backend.contracts.provider;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PactProviderSpring6TargetArchitectureTest {

    private static final Path PROVIDER_TESTS = Path.of("src/test/java/com/tcc/pjb/backend/contracts/provider");

    @Test
    void spring6MockMvcProvidersDevemUsarExtensionSpring6() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(PROVIDER_TESTS)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("ProviderContractTest.java"))
                    .sorted()
                    .forEach(path -> inspect(path, offenders));
        }
        assertTrue(offenders.isEmpty(), "Pact providers com Spring6MockMvcTestTarget devem usar PactVerificationSpring6Provider: " + offenders);
    }

    private static void inspect(Path path, List<String> offenders) {
        String source;
        try {
            source = Files.readString(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao ler provider contract: " + path, ex);
        }
        if (source.contains("Spring6MockMvcTestTarget") && !source.contains("PactVerificationSpring6Provider")) {
            offenders.add(path.toString());
        }
        if (source.contains("PactVerificationInvocationContextProvider")) {
            offenders.add(path + " usa extension JUnit generica");
        }
    }
}
