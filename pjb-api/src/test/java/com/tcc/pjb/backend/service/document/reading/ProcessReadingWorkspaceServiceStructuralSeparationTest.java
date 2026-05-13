package com.tcc.pjb.backend.service.document.reading;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProcessReadingWorkspaceServiceStructuralSeparationTest {

    private static final Path SERVICE = Path.of("src/main/java/com/tcc/pjb/backend/service/document/reading/ProcessReadingWorkspaceService.java");
    private static final Path FACADE = Path.of("src/main/java/com/tcc/pjb/backend/service/document/reading/ProcessReadingWorkspaceFacade.java");
    private static final Path SESSION_RESOLVER = Path.of("src/main/java/com/tcc/pjb/backend/service/document/reading/ProcessReadingWorkspaceSessionResolver.java");
    private static final Path PRESET_CATALOG_RESOLVER = Path.of("src/main/java/com/tcc/pjb/backend/service/document/reading/ProcessReadingPresetCatalogResolver.java");

    @Test
    void mustKeepWorkspaceServiceAsShortOrchestrator() throws Exception {
        String source = Files.readString(SERVICE, StandardCharsets.UTF_8);
        assertTrue(source.contains("private final ProcessReadingWorkspaceFacade facade;"));
        assertFalse(source.contains("private ProcessReadingWorkspaceResponse assemble("));
        assertFalse(source.contains("private ProcessReadingFlowResponse buildProcessFlow("));
    }

    @Test
    void mustKeepFacadeFocusedOnProjectionAndDelegateSessionLoading() throws Exception {
        String source = Files.readString(FACADE, StandardCharsets.UTF_8);
        assertTrue(source.contains("private final ProcessReadingWorkspaceSessionResolver sessionResolver;"));
        assertTrue(source.contains("private final ProcessReadingPresetCatalogResolver presetCatalogResolver;"));
        assertFalse(source.contains("private ProcessReadingWorkspaceContext loadProcessContext("));
        assertFalse(source.contains("private ProcessReadingModeProfile resolveModeProfile("));
        assertTrue(source.contains("private ProcessReadingWorkspaceResponse assemble(ProcessReadingWorkspaceSession session)"));
        assertTrue(source.contains("private ProcessReadingFlowResponse buildProcessFlow(ProcessReadingWorkspaceSession session)"));
    }

    @Test
    void mustKeepLoadingAndPresetCatalogInDedicatedCollaborators() throws Exception {
        String sessionResolver = Files.readString(SESSION_RESOLVER, StandardCharsets.UTF_8);
        String presetCatalogResolver = Files.readString(PRESET_CATALOG_RESOLVER, StandardCharsets.UTF_8);
        assertTrue(sessionResolver.contains("ProcessReadingWorkspaceSession resolveProcessSession("));
        assertTrue(sessionResolver.contains("private ProcessReadingWorkspaceContext loadProcessContext("));
        assertTrue(presetCatalogResolver.contains("ProcessReadingPresetCatalogResponse resolve("));
        assertTrue(presetCatalogResolver.contains("static ProcessReadingPreferenceResponse toPreferenceResponse("));
    }
}
