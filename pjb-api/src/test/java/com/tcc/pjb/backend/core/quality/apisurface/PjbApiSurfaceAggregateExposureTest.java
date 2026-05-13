package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PjbApiSurfaceAggregateExposureTest {

    @Test
    void auditarMarcaAggregateDeDominioExpostoDiretamente() throws Exception {
        Path root = Files.createTempDirectory("pjb-api-surface");
        Path controller = root.resolve("src/main/java/com/tcc/pjb/backend/controller/processual/TesteController.java");
        Path dto = root.resolve("src/main/java/com/tcc/pjb/backend/model/dto/processual/TesteResponse.java");
        Files.createDirectories(controller.getParent());
        Files.createDirectories(dto.getParent());
        Files.writeString(controller, """
                package com.tcc.pjb.backend.controller.processual;
                import org.springframework.http.ResponseEntity;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                @RequestMapping(\"/api/v1/teste\")
                public class TesteController {
                    @GetMapping(\"/aggregate\")
                    public ResponseEntity<AlgumAggregate> abrir() {
                        return ResponseEntity.ok(null);
                    }
                }
                """);
        Files.writeString(dto, """
                package com.tcc.pjb.backend.model.dto.processual;
                public record TesteResponse(String value) {
                }
                """);
        var aggregate = new PjbApiSurfaceSanityApplicationService(root).auditar();
        assertTrue(aggregate.entidadesExpostasDiretamente() > 0);
        assertTrue(aggregate.issues().stream().anyMatch(issue -> issue.codigo().equals("controller.aggregate.exposure")));
    }
}
