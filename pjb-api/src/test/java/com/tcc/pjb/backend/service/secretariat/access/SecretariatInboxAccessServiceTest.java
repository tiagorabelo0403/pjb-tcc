package com.tcc.pjb.backend.service.secretariat.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SecretariatInboxAccessServiceTest {

    @Test
    void adminAindaDeveReceberInboxNormalizadoEValido() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getOrNull()).thenReturn(user(TipoUsuario.ADMINISTRADOR, "CE", "Morada Nova", EnteFederativo.ESTADO));

        SecretariatInboxAccessService service = new SecretariatInboxAccessService(currentUserService);

        String normalized = service.requireAccess("SEC:TJCE:FORTALEZA:CE:morada-nova:1a-vara");

        assertEquals("SEC:TJCE:FORTALEZA:COMUM:CE:morada-nova:1a-vara", normalized);
    }

    @Test
    void servidorDaSecretariaDeveAcessarInboxDaSuaUfEComarca() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getOrNull()).thenReturn(user(TipoUsuario.SERVIDOR_FORUM, "CE", "Morada Nova", EnteFederativo.ESTADO));

        SecretariatInboxAccessService service = new SecretariatInboxAccessService(currentUserService);

        String normalized = service.requireAccess("SEC:TJCE:FORTALEZA:COMUM:CE:morada-nova:1a-vara");

        assertEquals("SEC:TJCE:FORTALEZA:COMUM:CE:morada-nova:1a-vara", normalized);
    }

    @Test
    void deveBloquearInboxComControleInjetado() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getOrNull()).thenReturn(user(TipoUsuario.ADMINISTRADOR, "CE", "Morada Nova", EnteFederativo.ESTADO));

        SecretariatInboxAccessService service = new SecretariatInboxAccessService(currentUserService);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requireAccess("SEC:TJCE:FORTALEZA:COMUM:CE:morada-nova\n:1a-vara"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void deveBloquearFilaForaDaComarcaDoServidor() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        when(currentUserService.getOrNull()).thenReturn(user(TipoUsuario.SERVIDOR_FORUM, "CE", "Morada Nova", EnteFederativo.ESTADO));

        SecretariatInboxAccessService service = new SecretariatInboxAccessService(currentUserService);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.requireAccess("SEC:TJCE:FORTALEZA:COMUM:CE:quixada:1a-vara"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    private Usuario user(TipoUsuario tipoUsuario, String uf, String comarca, EnteFederativo enteFederativo) {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNome("Servidor Operacional");
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setUf(uf);
        usuario.setComarca(comarca);
        usuario.setEnteFederativo(enteFederativo);
        return usuario;
    }
}
