package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.controller.notification.IntimacaoMulticanalController;
import com.tcc.pjb.backend.controller.notification.NotificationPreferenceController;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.notification.IntimacaoMulticanalService;
import com.tcc.pjb.backend.service.notification.NotificationPreferenceService;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

class PjbNotificationSurfaceArchitectureTest {

    @Test
    void servicosCentraisDeNotificacaoDevemDeclararBudgetsExplicitos() throws NoSuchMethodException {
        List<Method> methods = List.of(
                NotificationPreferenceService.class.getDeclaredMethod("consultar", Long.class),
                NotificationPreferenceService.class.getDeclaredMethod("salvar", Long.class, NotificationPreferenceService.PreferenceRequest.class),
                IntimacaoMulticanalService.class.getDeclaredMethod("dispatch", Long.class, Long.class, String.class, String.class, String.class, boolean.class)
        );

        for (Method method : methods) {
            assertThat(method.isAnnotationPresent(PjbTransactionalBudget.class))
                    .as(method.getDeclaringClass().getSimpleName() + "." + method.getName())
                    .isTrue();
        }
    }

    @Test
    void controllersDeNotificacaoDevemExporConsultaAtualizacaoEDispatch() throws NoSuchMethodException {
        assertThat(NotificationPreferenceController.class.getDeclaredMethod("consultar", Long.class)
                .isAnnotationPresent(GetMapping.class)).isTrue();
        assertThat(NotificationPreferenceController.class.getDeclaredMethod("salvar", Long.class, com.tcc.pjb.backend.model.dto.notification.NotificationPreferenceRequest.class)
                .isAnnotationPresent(PutMapping.class)).isTrue();
        assertThat(IntimacaoMulticanalController.class.getDeclaredMethod("dispatch", Long.class, Long.class, com.tcc.pjb.backend.model.dto.notification.IntimacaoMulticanalDispatchRequest.class)
                .isAnnotationPresent(PostMapping.class)).isTrue();
    }
}
