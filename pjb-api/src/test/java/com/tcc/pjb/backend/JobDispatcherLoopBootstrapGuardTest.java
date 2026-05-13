package com.tcc.pjb.backend;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JobDispatcherLoopBootstrapGuardTest {

    @Test
    void dispatcherDeveRespeitarPropriedadeEnabledAntesDeAutoStartup() throws IOException {
        Path root = Path.of(System.getProperty("user.dir")).resolve("src/main/java");
        Path loop = root.resolve("com/tcc/pjb/backend/core/jobs/runtime/JobDispatcherLoop.java");
        Path properties = root.resolve("com/tcc/pjb/backend/core/jobs/runtime/JobDispatcherProperties.java");
        if (!Files.exists(loop) || !Files.exists(properties)) {
            return;
        }
        String loopSource = Files.readString(loop);
        String propertiesSource = Files.readString(properties);
        assertTrue(propertiesSource.contains("private boolean enabled = true"));
        assertTrue(propertiesSource.contains("boolean isEnabled()"));
        assertTrue(loopSource.contains("return props.isEnabled()"));
        assertTrue(loopSource.contains("if (!props.isEnabled())"));
    }
}
