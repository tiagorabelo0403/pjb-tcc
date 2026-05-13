package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.controller.institutional.InstitutionalWorkbenchController;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.institutional.workbench.InstitutionalWorkbenchProjectionService;
import com.tcc.pjb.backend.service.institutional.workbench.InstitutionalWorkbenchService;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

class PjbInstitutionalWorkbenchSurfaceArchitectureTest {

    @Test
    void leiturasCentraisDoInstitutionalWorkbenchDevemDeclararBudgetsExplicitos() throws NoSuchMethodException {
        List<Method> methods = List.of(
                InstitutionalWorkbenchService.class.getDeclaredMethod("workspace"),
                InstitutionalWorkbenchService.class.getDeclaredMethod("actionPreview", Long.class, String.class),
                InstitutionalWorkbenchProjectionService.class.getDeclaredMethod("quickActions", Long.class),
                InstitutionalWorkbenchProjectionService.class.getDeclaredMethod("operationalQueue", int.class),
                InstitutionalWorkbenchProjectionService.class.getDeclaredMethod("previewAction", Long.class, String.class),
                InstitutionalWorkbenchProjectionService.class.getDeclaredMethod("previewExplainability", Long.class, String.class)
        );

        for (Method method : methods) {
            assertThat(method.isAnnotationPresent(PjbTransactionalBudget.class))
                    .as(method.getDeclaringClass().getSimpleName() + "." + method.getName())
                    .isTrue();
        }
    }

    @Test
    void controllerInstitutionalWorkbenchDeveExporWorkspaceQuickActionsFilaEPreview() throws NoSuchMethodException {
        List<Method> methods = List.of(
                InstitutionalWorkbenchController.class.getDeclaredMethod("workspace"),
                InstitutionalWorkbenchController.class.getDeclaredMethod("quickActions", Long.class),
                InstitutionalWorkbenchController.class.getDeclaredMethod("operationalQueue", int.class),
                InstitutionalWorkbenchController.class.getDeclaredMethod("actionPreview", String.class, Long.class)
        );

        for (Method method : methods) {
            assertThat(method.isAnnotationPresent(GetMapping.class))
                    .as(method.getName())
                    .isTrue();
        }
    }
}
