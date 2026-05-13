package com.tcc.pjb.backend.core.quality.codebase;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.domain.PjbCodebaseSanityAggregate;
import org.junit.jupiter.api.Test;

class PjbRepositoryEntityDisciplineTest {

    @Test
    void deveManterRepositoriosInternosComEntidadesResolvidas() {
        PjbCodebaseSanityApplicationService service = new PjbCodebaseSanityApplicationService();
        PjbCodebaseSanityAggregate aggregate = service.auditar();
        assertTrue(aggregate.issues().stream().noneMatch(item -> "repository.entity.quebrada".equals(item.codigo())));
    }
}
