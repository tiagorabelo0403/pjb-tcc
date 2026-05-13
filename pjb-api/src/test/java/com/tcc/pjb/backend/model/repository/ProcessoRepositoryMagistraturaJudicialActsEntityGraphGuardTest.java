package com.tcc.pjb.backend.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.EntityGraph;

class ProcessoRepositoryMagistraturaJudicialActsEntityGraphGuardTest {

    @Test
    void findMagistraturaActsScopedByIdDeveManterEntityGraphParaBlindarWorkspacePreviewEExecucaoContraNPlusOne() throws Exception {
        Method method = ProcessoRepository.class.getMethod("findMagistraturaActsScopedById", Long.class);
        EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);

        assertThat(entityGraph)
                .as("findMagistraturaActsScopedById deve preservar EntityGraph para atos da magistratura")
                .isNotNull();
        assertThat(entityGraph.attributePaths())
                .contains("usuario", "jurisdicao", "equipe");
    }
}
