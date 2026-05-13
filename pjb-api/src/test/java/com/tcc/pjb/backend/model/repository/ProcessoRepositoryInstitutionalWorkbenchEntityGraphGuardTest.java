package com.tcc.pjb.backend.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

class ProcessoRepositoryInstitutionalWorkbenchEntityGraphGuardTest {

    @Test
    void findWorkspaceScopedByIdDeveManterEntityGraphParaBlindarWorkbenchContraNPlusOne() throws Exception {
        Method method = ProcessoRepository.class.getMethod("findWorkspaceScopedById", Long.class);
        EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);

        assertThat(entityGraph)
                .as("findWorkspaceScopedById deve preservar EntityGraph para previews e quick actions do workbench")
                .isNotNull();
        assertThat(entityGraph.attributePaths())
                .contains("usuario", "jurisdicao", "equipe");
    }
}
