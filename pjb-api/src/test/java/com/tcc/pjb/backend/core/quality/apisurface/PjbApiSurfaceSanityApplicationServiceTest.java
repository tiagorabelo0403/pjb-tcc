package com.tcc.pjb.backend.core.quality.apisurface;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.domain.PjbApiSurfaceSanityAggregate;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PjbApiSurfaceSanityApplicationServiceTest {

    @Test
    void auditarProjetoAtualRetornaRaizEncontrada() {
        PjbApiSurfaceSanityApplicationService service = new PjbApiSurfaceSanityApplicationService(Path.of("").toAbsolutePath().normalize());
        PjbApiSurfaceSanityAggregate aggregate = service.auditar();
        assertTrue(aggregate.raizEncontrada());
        assertTrue(aggregate.controllersInspecionados() > 0);
        assertTrue(aggregate.dtoInspecionados() > 0);
    }
}
