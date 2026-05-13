package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.controller.magistratura.MagistraturaJudicialActsController;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.magistratura.acts.MagistraturaJudicialActWorkbenchService;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

class PjbMagistraturaJudicialActsSurfaceArchitectureTest {

    @Test
    void leiturasEExecucaoDaSurfaceDeAtosDaMagistraturaDevemDeclararBudgetsExplicitos() throws NoSuchMethodException {
        List<Method> methods = List.of(
                MagistraturaJudicialActWorkbenchService.class.getDeclaredMethod("workspace", Long.class),
                MagistraturaJudicialActWorkbenchService.class.getDeclaredMethod("preview", Long.class, String.class),
                MagistraturaJudicialActWorkbenchService.class.getDeclaredMethod("preview", Long.class, com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest.class),
                MagistraturaJudicialActWorkbenchService.class.getDeclaredMethod("execute", Long.class, com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest.class)
        );

        for (Method method : methods) {
            assertThat(method.isAnnotationPresent(PjbTransactionalBudget.class))
                    .as(method.getDeclaringClass().getSimpleName() + "." + method.getName())
                    .isTrue();
        }
    }

    @Test
    void controllerDeAtosDaMagistraturaDeveExporWorkspacePreviewAutomationPreviewEExecute() throws NoSuchMethodException {
        assertThat(MagistraturaJudicialActsController.class.getDeclaredMethod("workspace", Long.class, org.springframework.security.core.Authentication.class)
                        .isAnnotationPresent(GetMapping.class))
                .isTrue();
        assertThat(MagistraturaJudicialActsController.class.getDeclaredMethod("preview", Long.class, String.class, org.springframework.security.core.Authentication.class)
                        .isAnnotationPresent(GetMapping.class))
                .isTrue();
        assertThat(MagistraturaJudicialActsController.class.getDeclaredMethod("automationPreview", Long.class, com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest.class, org.springframework.security.core.Authentication.class)
                        .isAnnotationPresent(PostMapping.class))
                .isTrue();
        assertThat(MagistraturaJudicialActsController.class.getDeclaredMethod("execute", Long.class, com.tcc.pjb.backend.model.dto.magistratura.MagistraturaJudicialActCommandRequest.class, org.springframework.security.core.Authentication.class)
                        .isAnnotationPresent(PostMapping.class))
                .isTrue();
    }
}
