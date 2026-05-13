package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbInstitutionalCommunicationSurfaceDisciplineTest {

    @Test
    void controllersInstitucionaisModernizadosNaoDevemImportarDominioInterno() throws IOException {
        List<String> files = List.of(
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalAdvancedController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/access/NationalCommunicationInstitutionalAccessClosureController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/operations/NationalCommunicationInstitutionalOperationsController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/panel/NationalCommunicationInstitutionalFinalController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/entry/NationalCommunicationInstitutionalEntryController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/operations/NationalCommunicationInstitutionalLifecycleController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalClosureController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/topology/NationalCommunicationInstitutionalTopologyController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/workspace/NationalCommunicationInstitutionalProcessWorkspaceController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/procedural/NationalCommunicationInstitutionalProceduralCoherenceController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/governance/NationalCommunicationInstitutionalDelegatedClosureController.java",
                "src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/affiliation/NationalCommunicationInstitutionalDelegatedOnboardingController.java"
        );
        for (String relative : files) {
            String source = Files.readString(Path.of(relative));
            assertFalse(source.contains("core.comunicacao.institucional"), relative);
        }
    }
}
