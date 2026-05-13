package com.tcc.pjb.backend.core.comunicacao.institucional.processual;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class InstitutionalProcessWorkspaceApplicationServiceStructuralSeparationTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/processual/application/InstitutionalProcessWorkspaceApplicationService.java"
    );
    private static final Path SNAPSHOT = Path.of(
            "src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/processual/application/InstitutionalProcessWorkspaceSnapshotResolver.java"
    );
    private static final Path ASSEMBLER = Path.of(
            "src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/processual/application/InstitutionalProcessWorkspaceAssembler.java"
    );
    private static final Path DIAGNOSTIC = Path.of(
            "src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/processual/application/InstitutionalProcessWorkspaceDiagnosticResolver.java"
    );

    @Test
    void mustKeepWorkspaceApplicationServiceAsShortOrchestrator() throws Exception {
        String source = Files.readString(SERVICE, StandardCharsets.UTF_8);
        assertTrue(source.contains("InstitutionalProcessWorkspaceSnapshotResolver"));
        assertTrue(source.contains("InstitutionalProcessWorkspaceAssembler"));
        assertTrue(source.contains("InstitutionalProcessWorkspaceDiagnosticResolver"));
        assertFalse(source.contains("private InstitutionalProcessWorkspace toWorkspace("));
        assertFalse(source.contains("private ProcessSnapshot loadSnapshot("));
        assertFalse(source.contains("private void inspectWorkspace("));
    }

    @Test
    void mustKeepSnapshotLoadingOutsideApplicationService() throws Exception {
        String source = Files.readString(SNAPSHOT, StandardCharsets.UTF_8);
        assertTrue(source.contains("InstitutionalProcessWorkspaceSnapshot loadSnapshot"));
        assertTrue(source.contains("InstitutionalProcessWorkspaceSnapshot snapshotFromEntity"));
        assertTrue(source.contains("private boolean isUrgent("));
    }

    @Test
    void mustKeepAssemblyAndDiagnosticsInDedicatedCollaborators() throws Exception {
        String assemblerSource = Files.readString(ASSEMBLER, StandardCharsets.UTF_8);
        String diagnosticSource = Files.readString(DIAGNOSTIC, StandardCharsets.UTF_8);
        assertTrue(assemblerSource.contains("InstitutionalProcessWorkspace toWorkspace"));
        assertTrue(assemblerSource.contains("InstitutionalProcessWorkspaceSummary summarize"));
        assertTrue(diagnosticSource.contains("InstitutionalProcessDiagnosticReport diagnosticar"));
        assertTrue(diagnosticSource.contains("private void inspectWorkspace("));
    }
}
