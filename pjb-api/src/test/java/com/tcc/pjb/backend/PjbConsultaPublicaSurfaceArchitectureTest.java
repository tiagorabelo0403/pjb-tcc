package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.controller.publico.ConsultasPublicasController;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.consultapublica.ConsultaPublicaSearchService;
import com.tcc.pjb.backend.service.consultapublica.ConsultaPublicaWorkspaceService;
import com.tcc.pjb.backend.service.publico.PublicProcessoConsultaService;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

class PjbConsultaPublicaSurfaceArchitectureTest {

    @Test
    void leiturasCentraisDaConsultaPublicaDevemDeclararBudgetsExplicitos() throws NoSuchMethodException {
        List<Method> methods = List.of(
                ConsultaPublicaSearchService.class.getDeclaredMethod("searchPublic", String.class, String.class, String.class, int.class, int.class),
                ConsultaPublicaSearchService.class.getDeclaredMethod("resolvePublicPage", String.class),
                ConsultaPublicaWorkspaceService.class.getDeclaredMethod("workspace"),
                ConsultaPublicaWorkspaceService.class.getDeclaredMethod("detail", String.class),
                PublicProcessoConsultaService.class.getDeclaredMethod("consultarPorNumero", String.class)
        );

        for (Method method : methods) {
            assertThat(method.isAnnotationPresent(PjbTransactionalBudget.class))
                    .as(method.getDeclaringClass().getSimpleName() + "." + method.getName())
                    .isTrue();
        }
    }

    @Test
    void controllerPublicoDeConsultaDeveExporWorkspaceSearchDetailEPageResolve() throws NoSuchMethodException {
        List<Method> methods = List.of(
                ConsultasPublicasController.class.getDeclaredMethod("workspace", HttpServletRequest.class),
                ConsultasPublicasController.class.getDeclaredMethod("search", String.class, String.class, String.class, int.class, int.class, HttpServletRequest.class),
                ConsultasPublicasController.class.getDeclaredMethod("detail", String.class, HttpServletRequest.class),
                ConsultasPublicasController.class.getDeclaredMethod("resolvePage", String.class, HttpServletRequest.class)
        );

        for (Method method : methods) {
            assertThat(method.isAnnotationPresent(GetMapping.class))
                    .as(method.getName())
                    .isTrue();
        }
    }
}
