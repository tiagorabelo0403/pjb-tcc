package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.comunicacao.judicial.CuradorEspecialAutomaticoService;
import com.tcc.pjb.backend.core.lgpd.PjbProcessoSigiloRlsEntryPointSupport;
import com.tcc.pjb.backend.platform.jusos.v2.notificacao.NotificacaoInteligentePJB;
import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;
import com.tcc.pjb.backend.query.consumer.ProcessoMaterializadoConsumer;
import com.tcc.pjb.backend.query.consumer.ProntuarioNacionalConsumer;
import com.tcc.pjb.backend.service.infra.PjbProcessualReadModelProjector;
import com.tcc.pjb.backend.workflow.consumer.ProcessoAjuizadoWorkflowBridge;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class PjbSigiloNonHttpEntryPointArchitectureTest {

    @Test
    void entryPointsNaoHttpSensíveisDevemDependerDoSuporteDeSigilo() {
        List<Class<?>> entryPoints = List.of(
                ProcessoMaterializadoConsumer.class,
                PjbProcessualReadModelProjector.class,
                ProntuarioNacionalConsumer.class,
                ProcessoAjuizadoWorkflowBridge.class,
                CuradorEspecialAutomaticoService.class,
                NotificacaoInteligentePJB.class
        );

        for (Class<?> entryPoint : entryPoints) {
            assertThat(hasField(entryPoint, PjbProcessoSigiloRlsEntryPointSupport.class))
                    .as(entryPoint.getSimpleName())
                    .isTrue();
        }
    }

    @Test
    void listenerKafkaDeMaterializacaoNaoDeveAbrirTransacaoAntesDoBindDeSigilo() throws NoSuchMethodException {
        Method method = ProcessoMaterializadoConsumer.class.getDeclaredMethod("handleProcessoMaterializado", Map.class);

        assertThat(method.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(hasField(ProcessoMaterializadoConsumer.class, PjbTransactionalExecutionSupport.class)).isTrue();
    }

    private boolean hasField(Class<?> type, Class<?> fieldType) {
        for (Field field : type.getDeclaredFields()) {
            if (field.getType().equals(fieldType)) {
                return true;
            }
        }
        return false;
    }
}
