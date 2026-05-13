package com.tcc.pjb.backend.governance.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import com.tcc.pjb.backend.testsupport.PjbTestPaths;
import org.junit.jupiter.api.Test;

class CodebaseLearningGovernanceArtifactsTest {

    @Test
    void readmeEAdrDevemFormalizarAprendizadoGuiadoPorHotspotsDoCore() throws IOException {
        String readme = Files.readString(PjbTestPaths.projectRoot().resolve("README.md"), StandardCharsets.UTF_8);
        String adrHotspots = Files.readString(PjbTestPaths.projectRoot().resolve("docs/adr/ADR-0003-aprendizado-guiado-por-hotspots-do-core.md"), StandardCharsets.UTF_8);
        String adrLanes = Files.readString(PjbTestPaths.projectRoot().resolve("docs/adr/ADR-0004-trilhas-internas-de-extracao-do-core.md"), StandardCharsets.UTF_8);
        String adrFlows = Files.readString(PjbTestPaths.projectRoot().resolve("docs/adr/ADR-0005-blueprints-de-extracao-e-fluxos-criticos.md"), StandardCharsets.UTF_8);
        String adrSnapshot = Files.readString(PjbTestPaths.projectRoot().resolve("docs/adr/ADR-0006-cache-curto-e-mapeador-dedicado-para-codebase-learning.md"), StandardCharsets.UTF_8);
        assertTrue(readme.contains("codebase-learning"));
        assertTrue(readme.contains("sanidade-aprendizado"));
        assertTrue(readme.contains("trilhas internas de extração"));
        assertTrue(readme.contains("blueprints de extração"));
        assertTrue(readme.contains("fluxos críticos"));
        assertTrue(readme.contains("refresh=true"));
        assertTrue(readme.contains("snapshot em memória"));
        assertTrue(adrHotspots.contains("core/comunicacao"));
        assertTrue(adrHotspots.contains("core/processo"));
        assertTrue(adrHotspots.contains("controller"));
        assertTrue(adrLanes.contains("PRONTA"));
        assertTrue(adrLanes.contains("PREPARAR"));
        assertTrue(adrLanes.contains("ENDURECER"));
        assertTrue(adrFlows.contains("peticao-triagem-secretaria-gabinete-decisao-publicacao"));
        assertTrue(adrFlows.contains("blueprints de extração"));
        assertTrue(adrSnapshot.contains("cache curto"));
        assertTrue(adrSnapshot.contains("refresh explícito"));
    }
}
