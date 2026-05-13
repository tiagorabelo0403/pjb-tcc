package com.tcc.pjb.backend.service.security.operational;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class OperationalFunctionCredentialAuthorityServiceTest {

    @Test
    void recognizesForumDirectorAndAllowsProvisionWithinSameInstitution() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        Usuario director = usuario("Diretoria do Fórum TJCE", TipoUsuario.SERVIDOR_FORUM, "TJCE", "CE", "Fortaleza");
        Usuario target = usuario("Secretaria Vara 1 TJCE", TipoUsuario.SERVIDOR, "TJCE", "CE", "Fortaleza");
        when(currentUserService.getRequired()).thenReturn(director);

        OperationalFunctionCredentialAuthorityService service = new OperationalFunctionCredentialAuthorityService(currentUserService);

        assertTrue(service.isDirector(director));
        assertEquals(director, service.requireDirectorForTarget(target, "SECRETARIAT_PROCESS_WRITE"));
    }

    @Test
    void rejectsProvisionWhenInstitutionDoesNotMatch() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        Usuario director = usuario("Diretoria do Fórum TJCE", TipoUsuario.SERVIDOR_FORUM, "TJCE", "CE", "Fortaleza");
        Usuario target = usuario("Oficial Federal TRF5", TipoUsuario.OFICIAL_JUSTICA, "TRF5", "CE", "Fortaleza");
        when(currentUserService.getRequired()).thenReturn(director);

        OperationalFunctionCredentialAuthorityService service = new OperationalFunctionCredentialAuthorityService(currentUserService);

        assertThrows(ResponseStatusException.class,
                () -> service.requireDirectorForTarget(target, "OFFICIAL_PERSONAL_SERVICE_WRITE"));
    }

    @Test
    void nonDirectorServidorCannotProvisionCredential() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        Usuario actor = usuario("Servidor comum", TipoUsuario.SERVIDOR, "TJCE", "CE", "Fortaleza");
        when(currentUserService.getRequired()).thenReturn(actor);

        OperationalFunctionCredentialAuthorityService service = new OperationalFunctionCredentialAuthorityService(currentUserService);

        assertFalse(service.isDirector(actor));
    }

    private Usuario usuario(String perfil, TipoUsuario tipoUsuario, String registro, String uf, String comarca) {
        Usuario usuario = new Usuario();
        usuario.setId((long) Math.abs(perfil.hashCode()));
        usuario.setNome(perfil);
        usuario.setPerfil(perfil);
        usuario.setRegistroProfissional(registro);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setUf(uf);
        usuario.setComarca(comarca);
        return usuario;
    }
}
