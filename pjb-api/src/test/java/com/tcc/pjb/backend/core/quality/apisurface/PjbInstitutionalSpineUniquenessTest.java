package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface.NationalCommunicationInstitutionalSurfaceAssemblerSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PjbInstitutionalSpineUniquenessTest {

    @Test
    void institutionalSpinesMustExistOnlyOnceInMainSourceTree() throws IOException {
        assertSingle("InstitutionalApiRoutes.java");
        assertSingle("NationalCommunicationInstitutionalStateBundleFacadeService.java");
        assertSingle("PjbAuthenticatedSessionFacadeService.java");
        assertSingle("PjbInstitutionalDataPlaneFilter.java");
        assertSingle("NationalCommunicationInstitutionalSurfaceAssemblerSupport.java");
    }

    private void assertSingle(String fileName) throws IOException {
        try (Stream<Path> stream = Files.walk(ApiSurfaceTestSupport.MAIN_JAVA)) {
            List<Path> matches = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .toList();
            assertEquals(1, matches.size(), () -> "Espinha institucional duplicada ou ausente: " + fileName + " -> " + matches);
        }
    }
}