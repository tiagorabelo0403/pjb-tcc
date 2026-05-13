package com.tcc.pjb.backend.core.quality.codebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PjbVirtualThreadDisciplineTest {

    @Test
    void deveManterVirtualThreadsDiretasSomenteNaEspinhaPermitida() {
        PjbCodebaseSanityApplicationService service = new PjbCodebaseSanityApplicationService();
        PjbCodebaseSanityAggregate aggregate = service.auditar();
        assertTrue(aggregate.issues().stream().map(item -> item.arquivo()).filter(path -> !path.isBlank()).allMatch(path -> !Set.of(
                "src/test/java/com/tcc/pjb/backend/core/quality/codebase/PjbVirtualThreadDisciplineTest.java",
                "src/test/java/com/tcc/pjb/backend/core/quality/codebase/PjbCodebaseSanityApplicationServiceTest.java"
        ).contains(path)));
        assertEquals(0, aggregate.virtualThreadsDiretas());
    }
}
