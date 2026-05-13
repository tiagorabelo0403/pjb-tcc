package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class StructuralGovernanceScannerServiceBootstrapGuardTest {

    @Test
    void governanceScannerNaoDeveExigirMvcEmContextoNaoWeb() throws IOException {
        Path source = Path.of(System.getProperty("user.dir"))
                .resolve("src/main/java/com/tcc/pjb/backend/service/governance/StructuralGovernanceScannerService.java");
        if (!Files.exists(source)) {
            return;
        }
        String content = Files.readString(source);
        assertTrue(content.contains("ObjectProvider<RequestMappingHandlerMapping>"));
        assertTrue(content.contains("handlerMappingProvider.getIfAvailable()"));
        assertFalse(content.contains("private final RequestMappingHandlerMapping handlerMapping;"));
    }
}
