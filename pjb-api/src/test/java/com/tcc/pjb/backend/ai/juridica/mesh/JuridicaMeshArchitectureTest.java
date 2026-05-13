package com.tcc.pjb.backend.ai.juridica.mesh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;

import static org.junit.jupiter.api.Assertions.*;

class JuridicaMeshArchitectureTest {

    @Test
    void deveManterSurfaceEMalhaJuridicaAtualizandoV1V2V3() throws IOException {
        Path root = PjbTestPaths.backendMainRoot();
        assertTrue(Files.exists(root.resolve("ai/juridica/api/JuridicaMeshProfileController.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mesh/JuridicaUnifiedMeshProfileService.java")));
        assertTrue(Files.exists(root.resolve("ai/juridica/mesh/JuridicaLegalToolCatalogService.java")));

        String v1 = Files.readString(root.resolve("ai/juridica/v1/IAJuridicaV1.java"));
        String v2 = Files.readString(root.resolve("ai/juridica/v2/IAJuridicaV2.java"));
        String v3 = Files.readString(root.resolve("ai/juridica/v3/IAJuridicaV3.java"));
        assertTrue(v1.contains("juridica_mesh_profile"));
        assertTrue(v2.contains("juridica_mesh_profile"));
        assertTrue(v3.contains("juridica_mesh_profile"));
    }
}
