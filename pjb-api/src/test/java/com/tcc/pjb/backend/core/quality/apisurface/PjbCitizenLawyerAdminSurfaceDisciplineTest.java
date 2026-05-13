package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;

class PjbCitizenLawyerAdminSurfaceDisciplineTest {

    @Test
    void naoDeveExporTiposAninhadosDeServiceOuRecordInlineNosControllersAtualizados() {
        PjbApiSurfaceSanityApplicationService service = new PjbApiSurfaceSanityApplicationService(Path.of("."));
        var aggregate = service.auditar();
        Set<String> alvos = Set.of(
                "src/main/java/com/tcc/pjb/backend/controller/cidadao/CidadaoDashboardEnhancedController.java",
                "src/main/java/com/tcc/pjb/backend/controller/cidadao/CidadaoMeusProcessosController.java",
                "src/main/java/com/tcc/pjb/backend/controller/advogado/AdvogadoDashboardController.java",
                "src/main/java/com/tcc/pjb/backend/controller/advogado/AdvogadoCockpitController.java",
                "src/main/java/com/tcc/pjb/backend/controller/advogado/AdvogadoEscritorioController.java",
                "src/main/java/com/tcc/pjb/backend/controller/admin/AdminBackfillReviewController.java"
        );
        boolean hasIssue = aggregate.issues().stream()
                .filter(issue -> alvos.contains(issue.alvo()))
                .anyMatch(issue -> issue.codigo().equals("service.nested.exposure")
                        || issue.codigo().equals("controller.inline.dto")
                        || issue.codigo().equals("controller.aggregate.exposure"));
        assertFalse(hasIssue);
    }
}
