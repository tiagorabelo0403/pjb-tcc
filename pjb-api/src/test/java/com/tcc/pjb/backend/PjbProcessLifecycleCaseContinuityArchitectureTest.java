package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleMachine;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.casefile.CaseContinuityOrchestratorService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbProcessLifecycleCaseContinuityArchitectureTest {

    @Test
    void lifecycleMachineDeveSincronizarPeloOrquestradorDeContinuidadeESemRepositoriosDiretos() {
        assertThat(hasField(ProcessoLifecycleMachine.class, CaseContinuityOrchestratorService.class)).isTrue();
        assertThat(List.of(ProcessoLifecycleMachine.class.getDeclaredFields()).stream()
                .map(Field::getType)
                .map(Class::getSimpleName)
                .toList())
                .noneMatch(name -> name.contains("Repository"));
    }

    @Test
    void writesCentraisDaContinuidadeDevemTerBudgetExplicito() throws NoSuchMethodException {
        List<Method> methods = List.of(
                CaseContinuityOrchestratorService.class.getDeclaredMethod("ensureRootCase", Long.class, String.class),
                CaseContinuityOrchestratorService.class.getDeclaredMethod("syncFromLifecycle", com.tcc.pjb.backend.model.entity.Processo.class, com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction.class),
                CaseContinuityOrchestratorService.class.getDeclaredMethod("unifyLinkedCases", Long.class, Long.class, com.tcc.pjb.backend.model.entity.enums.VinculoProcessualTipo.class, String.class)
        );

        for (Method method : methods) {
            assertThat(method.isAnnotationPresent(PjbTransactionalBudget.class))
                    .as(method.getName())
                    .isTrue();
        }
    }

    private boolean hasField(Class<?> source, Class<?> fieldType) {
        for (Field field : source.getDeclaredFields()) {
            if (field.getType().equals(fieldType)) {
                return true;
            }
        }
        return false;
    }
}
