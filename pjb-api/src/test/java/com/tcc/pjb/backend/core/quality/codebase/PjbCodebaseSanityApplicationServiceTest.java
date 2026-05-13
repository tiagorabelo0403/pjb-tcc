package com.tcc.pjb.backend.core.quality.codebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import org.junit.jupiter.api.Test;

class PjbCodebaseSanityApplicationServiceTest {

    @Test
    void deveIdentificarCodebaseSemDuplicidadeNemImportsInternosQuebrados() {
        PjbCodebaseSanityApplicationService service = new PjbCodebaseSanityApplicationService();
        PjbCodebaseSanityAggregate aggregate = service.auditar();
        assertTrue(aggregate.disponivel());
        assertEquals(0, aggregate.fqnsDuplicados());
        assertEquals(0, aggregate.importsInternosQuebrados());
        assertEquals(0L, aggregate.issues().stream().filter(item -> "repository.entity.quebrada".equals(item.codigo())).count());
        assertEquals(0L, aggregate.issues().stream().filter(item -> "legacy.judge.import".equals(item.codigo())).count());
        assertEquals(0L, aggregate.issues().stream().filter(item -> "legacy.judge.package.expansion".equals(item.codigo())).count());
        assertEquals(0L, aggregate.issues().stream().filter(item -> "quality.coverage.absent".equals(item.codigo())).count());
        assertEquals(0L, aggregate.issues().stream().filter(item -> "quality.checkstyle.absent".equals(item.codigo())).count());
        assertEquals(0L, aggregate.issues().stream().filter(item -> "quality.checkstyle.config.absent".equals(item.codigo())).count());
    }
}
