package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.ai.orchestrator.IAOrchestrator;
import com.tcc.pjb.backend.command.AjuizarProcessoCommand;
import com.tcc.pjb.backend.command.ajuizamento.AjuizarProcessoCommandPostCommitEffectsService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorLifecycleService;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.processo.ProcessoPostAjuizamentoOrchestratorService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbAjuizamentoCommandPathArchitectureTest {

    @Test
    void commandPathCentralDoAjuizamentoDeveDeclararBudgetsExplicitos() throws NoSuchMethodException {
        List<Method> methods = List.of(
                AjuizamentoService.class.getDeclaredMethod("ajuizar", Processo.class),
                AjuizarProcessoCommand.class.getDeclaredMethod("execute", Processo.class, Usuario.class, List.class, boolean.class),
                ProcessoPostAjuizamentoOrchestratorService.class.getDeclaredMethod("onProcessoAjuizado", ProcessoAjuizadoEvent.class),
                AjuizarProcessoCommandPostCommitEffectsService.class.getDeclaredMethod("onProcessoAjuizado", ProcessoAjuizadoEvent.class)
        );

        for (Method method : methods) {
            assertThat(method.isAnnotationPresent(PjbTransactionalBudget.class))
                    .as(method.getDeclaringClass().getSimpleName() + "." + method.getName())
                    .isTrue();
        }
    }

    @Test
    void ajuizarProcessoCommandNaoDeveReterColaboradoresNaoBloqueantesNoPathTransacionalCentral() {
        List<Class<?>> forbidden = List.of(
                IAOrchestrator.class,
                AuditoriaInteligenteService.class,
                JudicialConnectorLifecycleService.class
        );

        List<Class<?>> fieldTypes = List.of(AjuizarProcessoCommand.class.getDeclaredFields()).stream()
                .map(Field::getType)
                .toList();

        assertThat(fieldTypes).doesNotContainAnyElementsOf(forbidden);
    }
}
