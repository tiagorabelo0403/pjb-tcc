package com.tcc.pjb.backend.core.procedural;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProceduralCatalogSupportRefinementArchitectureTest {

    private static final Path CATALOG_SUPPORT_PATH = Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/ProceduralCatalogSupport.java");
    private static final Path DEFINITION_SUPPORT_PATH = Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/ProceduralCatalogDefinitionSupport.java");
    private static final Path STAGE_SUPPORT_PATH = Path.of("src/main/java/com/tcc/pjb/backend/core/procedural/ProceduralCatalogStageSupport.java");

    @Test
    void catalogFacadeAndSupportsMustStayBelowHotspotThresholds() throws IOException {
        assertThat(Files.lines(CATALOG_SUPPORT_PATH, StandardCharsets.UTF_8).count()).isLessThan(900);
        assertThat(Files.lines(DEFINITION_SUPPORT_PATH, StandardCharsets.UTF_8).count()).isLessThan(900);
        assertThat(Files.lines(STAGE_SUPPORT_PATH, StandardCharsets.UTF_8).count()).isLessThan(500);
    }

    @Test
    void catalogFacadeMustDelegateDefinitionsAndStageAssembly() throws IOException {
        String catalogFacade = Files.readString(CATALOG_SUPPORT_PATH, StandardCharsets.UTF_8);
        String definitionSupport = Files.readString(DEFINITION_SUPPORT_PATH, StandardCharsets.UTF_8);
        String stageSupport = Files.readString(STAGE_SUPPORT_PATH, StandardCharsets.UTF_8);

        assertThat(catalogFacade).contains("return ProceduralCatalogDefinitionSupport.snapshot(resolved);");
        assertThat(catalogFacade).contains("ProceduralCatalogStageSupport.mergeStages");
        assertThat(catalogFacade).doesNotContain("private static DefinitionSnapshot civilGeral(");
        assertThat(catalogFacade).doesNotContain("private static DefinitionSnapshot eleitoralAije(");
        assertThat(catalogFacade).doesNotContain("private static List<RitoStage> macroStages(");

        assertThat(definitionSupport).contains("static DefinitionSnapshot snapshot(");
        assertThat(definitionSupport).contains("private static DefinitionSnapshot civilGeral(");
        assertThat(definitionSupport).contains("private static DefinitionSnapshot eleitoralAije(");

        assertThat(stageSupport).contains("static List<RitoStage> macroStages(");
        assertThat(stageSupport).contains("static DefinitionSnapshot definition(");
        assertThat(stageSupport).contains("static Map<String, Object> stageToMap(");
    }
}
