package com.tcc.pjb.backend.modules.suporte.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SupportTicketControllerAccessTest {

    @Test
    void abrirEMeusECancelarSaoAbertosAQualquerUsuarioAutenticado() throws NoSuchMethodException {
        assertRoles("abrir", new Class<?>[]{com.tcc.pjb.backend.modules.suporte.dto.AbrirChamadoRequest.class}, "isAuthenticated()");
        assertRoles("meus", new Class<?>[]{}, "isAuthenticated()");
        assertRoles("cancelar", new Class<?>[]{Long.class}, "isAuthenticated()");
    }

    @Test
    void filaERestritaASuporteTecnicoEAdministrador() throws NoSuchMethodException {
        Method method = SupportTicketController.class.getMethod("fila",
                com.tcc.pjb.backend.modules.suporte.entity.SupportTicketStatus.class);
        String roles = method.getAnnotation(PreAuthorize.class).value();

        assertThat(roles).contains("ROLE_SUPORTE_TECNICO", "ROLE_ADMINISTRADOR");
    }

    @Test
    void assumirResolverEFecharSaoRestritosASuporteTecnico() throws NoSuchMethodException {
        assertRoles("assumir", new Class<?>[]{Long.class}, "ROLE_SUPORTE_TECNICO");
        assertRoles("resolver", new Class<?>[]{Long.class, com.tcc.pjb.backend.modules.suporte.dto.ResolverChamadoRequest.class}, "ROLE_SUPORTE_TECNICO");
        assertRoles("fechar", new Class<?>[]{Long.class}, "ROLE_SUPORTE_TECNICO");
    }

    private void assertRoles(String methodName, Class<?>[] paramTypes, String esperado) throws NoSuchMethodException {
        Method method = SupportTicketController.class.getMethod(methodName, paramTypes);
        String roles = method.getAnnotation(PreAuthorize.class).value();
        assertThat(roles).contains(esperado);
    }
}
