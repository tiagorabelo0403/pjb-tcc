package com.tcc.pjb.backend.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

class WorkItemRepositoryEntityGraphGuardTest {

    @Test
    void queueAndDueQueriesMustKeepEntityGraphToPreventNPlusOneRegression() throws Exception {
        Map<String, Class<?>[]> guardedMethods = Map.of(
                "inboxByUser", new Class<?>[]{Long.class, org.springframework.data.domain.Pageable.class},
                "inboxByRoleAndTerritory", new Class<?>[]{com.tcc.pjb.backend.model.entity.enums.TipoUsuario.class, String.class, String.class, org.springframework.data.domain.Pageable.class},
                "findDueByAssignedUser", new Class<?>[]{Long.class, java.time.Instant.class, org.springframework.data.domain.Pageable.class},
                "findDueByRoleAndTerritory", new Class<?>[]{com.tcc.pjb.backend.model.entity.enums.TipoUsuario.class, String.class, String.class, java.time.Instant.class, org.springframework.data.domain.Pageable.class}
        );

        for (Map.Entry<String, Class<?>[]> entry : guardedMethods.entrySet()) {
            Method method = WorkItemRepository.class.getMethod(entry.getKey(), entry.getValue());
            EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);
            assertThat(entityGraph)
                    .as(entry.getKey() + " deve permanecer com EntityGraph para proteger filas e painéis contra N+1")
                    .isNotNull();
            assertThat(entityGraph.attributePaths())
                    .contains("processo", "assignedUser");
        }
    }
}
