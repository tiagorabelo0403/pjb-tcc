package com.tcc.pjb.backend.core.quality.apisurface;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbSurfaceMinisterAdminInfraCriminalDisciplineTest {

    @Test
    void updatedControllersShouldNotExposeNestedServiceContractsOrRawMaps() throws IOException {
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/ministro/MinistroPlenarioAvancadoController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/ministro/MinistroTemaPrecedenteController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/admin/AdminJudicialConnectorDataPlaneController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/admin/AdminJudicialConnectorControlPlaneController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/admin/AdminJudicialConnectorSecuritySessionsController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/admin/ScaleArchitectureController.java");
        assertControllerClean("src/main/java/com/tcc/pjb/backend/controller/criminal/InqueritoPolicialDigitalController.java");
    }

    private void assertControllerClean(String path) throws IOException {
        String content = Files.readString(Path.of(path));
        assertFalse(content.contains("Map<String, Object>"), path + " should not expose raw Map responses");
        assertFalse(content.matches("(?s).*Service\\.[A-Z][A-Za-z0-9_]*.*"), path + " should not expose nested service contracts");
        assertFalse(content.contains(" record ") || content.contains("\nrecord "), path + " should not declare inline record DTOs");
    }
}
