package com.tcc.pjb.backend.core.security.access;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

class PrivateResourceAccessGuardServiceTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final PrivateResourceAccessGuardService service = new PrivateResourceAccessGuardService(currentUserService, authorizationService);

    @Test
    void owner_can_access_private_resource() {
        Usuario actor = user(10L, TipoUsuario.ADVOGADO);
        Processo processo = new Processo();
        when(currentUserService.getRequired()).thenReturn(actor);
        doNothing().when(authorizationService).requireReadProcesso(processo);

        assertDoesNotThrow(() -> service.requireOwnerOrPrivileged(10L, processo, "bundle offline"));
    }

    @Test
    void privileged_actor_can_access_private_resource() {
        Usuario actor = user(20L, TipoUsuario.ADMINISTRADOR);
        Processo processo = new Processo();
        when(currentUserService.getRequired()).thenReturn(actor);
        doNothing().when(authorizationService).requireReadProcesso(processo);

        assertDoesNotThrow(() -> service.requireOwnerOrPrivileged(10L, processo, "bundle offline"));
    }

    @Test
    void non_owner_non_privileged_is_denied() {
        Usuario actor = user(20L, TipoUsuario.ADVOGADO);
        when(currentUserService.getRequired()).thenReturn(actor);

        assertThrows(AccessDeniedPjbException.class, () -> service.requireOwnerOrPrivileged(10L, null, "preferência de notificação"));
    }

    private Usuario user(Long id, TipoUsuario tipoUsuario) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setTipoUsuario(tipoUsuario);
        return usuario;
    }
}
