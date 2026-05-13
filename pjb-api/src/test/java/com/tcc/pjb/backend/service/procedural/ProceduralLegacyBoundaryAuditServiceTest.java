package com.tcc.pjb.backend.service.procedural;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProceduralLegacyBoundaryAuditServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void detectsDirectLegacyEnumUsageInOperationalPackage() throws Exception {
        Path srcRoot = tempDir.resolve("src/main/java");
        Path operational = srcRoot.resolve("com/tcc/pjb/backend/core/preflight/Example.java");
        Files.createDirectories(operational.getParent());
        Files.writeString(operational, "package com.tcc.pjb.backend.core.preflight;\n"
                + "import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;\n"
                + "public class Example { RitoProcessual rito; }\n");

        ProceduralBootstrapGovernanceProperties properties = new ProceduralBootstrapGovernanceProperties();
        properties.setSourceRoots(List.of(srcRoot.toString()));
        ProceduralLegacyBoundaryAuditService service = new ProceduralLegacyBoundaryAuditService(properties);

        var report = service.report();

        assertTrue(report.available());
        assertFalse(report.clean());
        assertTrue(report.violations().stream().anyMatch(v -> v.path().endsWith("Example.java")));
    }

    @Test
    void ignoresAllowedCompatibilityFiles() throws Exception {
        Path srcRoot = tempDir.resolve("src/main/java");
        Path compatibility = srcRoot.resolve("com/tcc/pjb/backend/service/rito/RitoResolutionService.java");
        Files.createDirectories(compatibility.getParent());
        Files.writeString(compatibility, "package com.tcc.pjb.backend.service.rito;\n"
                + "import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;\n"
                + "public class RitoResolutionService { RitoProcessual rito; }\n");

        ProceduralBootstrapGovernanceProperties properties = new ProceduralBootstrapGovernanceProperties();
        properties.setSourceRoots(List.of(srcRoot.toString()));
        ProceduralLegacyBoundaryAuditService service = new ProceduralLegacyBoundaryAuditService(properties);

        var report = service.report();

        assertTrue(report.clean());
    }
    @Test
    void detectsLegacyEnumUsageInIntentEngineBoundary() throws Exception {
        Path srcRoot = tempDir.resolve("src/main/java");
        Path operational = srcRoot.resolve("com/tcc/pjb/backend/ai/juridica/v3/core/Example.java");
        Files.createDirectories(operational.getParent());
        Files.writeString(operational, "package com.tcc.pjb.backend.ai.juridica.v3.core;\n"
                + "import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;\n"
                + "public class Example { RitoProcessual rito; }\n");

        ProceduralBootstrapGovernanceProperties properties = new ProceduralBootstrapGovernanceProperties();
        properties.setSourceRoots(List.of(srcRoot.toString()));
        ProceduralLegacyBoundaryAuditService service = new ProceduralLegacyBoundaryAuditService(properties);

        var report = service.report();

        assertFalse(report.clean());
        assertTrue(report.violations().stream().anyMatch(v -> v.path().endsWith("Example.java")));
    }


    
    @Test
    void normalizesJsonLikeSourceRootsBeforePathResolution() throws Exception {
        Path srcRoot = tempDir.resolve("src/main/java");
        Path operational = srcRoot.resolve("com/tcc/pjb/backend/core/preflight/Example.java");
        Files.createDirectories(operational.getParent());
        Files.writeString(operational, "package com.tcc.pjb.backend.core.preflight;\n"
                + "import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;\n"
                + "public class Example { RitoProcessual rito; }\n");

        ProceduralBootstrapGovernanceProperties properties = new ProceduralBootstrapGovernanceProperties();
        properties.setSourceRoots(List.of("[\"" + srcRoot + "\"]"));
        ProceduralLegacyBoundaryAuditService service = new ProceduralLegacyBoundaryAuditService(properties);

        var report = service.report();

        assertTrue(report.available());
        assertFalse(report.clean());
    }

}
