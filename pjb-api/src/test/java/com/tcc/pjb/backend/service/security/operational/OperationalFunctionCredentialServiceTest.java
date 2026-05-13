package com.tcc.pjb.backend.service.security.operational;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.SecurityChallengeService;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialDirectorProvisionRequest;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialSnapshotResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.OperationalFunctionCredential;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.OperationalFunctionCredentialRepository;
import com.tcc.pjb.backend.model.repository.security.OperationalFunctionUnlockSessionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class OperationalFunctionCredentialServiceTest {

    @Test
    void directorProvisionKeepsActiveSecretWhenScopeChangesWithoutForceReset() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        OperationalFunctionCredentialRepository credentialRepository = mock(OperationalFunctionCredentialRepository.class);
        OperationalFunctionUnlockSessionRepository unlockSessionRepository = mock(OperationalFunctionUnlockSessionRepository.class);
        OperationalFunctionCredentialAuthorityService authorityService = mock(OperationalFunctionCredentialAuthorityService.class);
        SecurityChallengeService challengeService = mock(SecurityChallengeService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        ClientIpResolver ipResolver = mock(ClientIpResolver.class);

        Usuario director = usuario(1L, "Diretoria Fórum TJCE", TipoUsuario.SERVIDOR_FORUM, "TJCE", "CE", "Fortaleza");
        Usuario target = usuario(2L, "Secretaria Vara 1", TipoUsuario.SERVIDOR, "TJCE", "CE", "Fortaleza");
        OperationalFunctionCredential existing = new OperationalFunctionCredential();
        existing.setId(77L);
        existing.setUsuario(target);
        existing.setFunctionCode(OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE);
        existing.setStatus("ACTIVE");
        existing.setSecretHash("argon-hash");
        existing.setTribunalCodigo("TJCE");

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));
        when(authorityService.requireDirectorForTarget(target, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE)).thenReturn(director);
        when(authorityService.resolveTribunal(target)).thenReturn("TJCE");
        when(credentialRepository.findLockedByUsuarioIdAndFunctionCode(2L, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE)).thenReturn(Optional.of(existing));
        when(credentialRepository.findAllByUsuarioIdAndFunctionCodeIn(any(), any())).thenReturn(java.util.List.of(existing));

        OperationalFunctionCredentialService service = new OperationalFunctionCredentialService(
                currentUserService,
                usuarioRepository,
                credentialRepository,
                unlockSessionRepository,
                authorityService,
                challengeService,
                new ObjectMapper(),
                requestProvider,
                ipResolver
        );

        OperationalCredentialSnapshotResponse response = service.directorProvision(new OperationalCredentialDirectorProvisionRequest(
                2L,
                OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE,
                "ESTADUAL",
                "TJCE",
                "FORUM_FORTALEZA",
                "VARA_1",
                "1ª Vara Cível",
                "ajuste de escopo",
                false
        ));

        assertEquals("ACTIVE", existing.getStatus());
        assertEquals("argon-hash", existing.getSecretHash());
        assertEquals("FORUM_FORTALEZA", existing.getForumCode());
        assertEquals("VARA_1", existing.getUnitCode());
        assertNotNull(existing.getAuditTrailJson());
        assertEquals("SECRETARIAT", response.laneCode());
        verify(credentialRepository).save(existing);
        verify(credentialRepository, never()).findLockedByUsuarioIdAndFunctionCode(2L, OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE);
    }

    @Test
    void directorProvisionForcesResetWhenRequested() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        OperationalFunctionCredentialRepository credentialRepository = mock(OperationalFunctionCredentialRepository.class);
        OperationalFunctionUnlockSessionRepository unlockSessionRepository = mock(OperationalFunctionUnlockSessionRepository.class);
        OperationalFunctionCredentialAuthorityService authorityService = mock(OperationalFunctionCredentialAuthorityService.class);
        SecurityChallengeService challengeService = mock(SecurityChallengeService.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        ClientIpResolver ipResolver = mock(ClientIpResolver.class);

        Usuario director = usuario(10L, "Diretoria Fórum TJCE", TipoUsuario.SERVIDOR_FORUM, "TJCE", "CE", "Fortaleza");
        Usuario target = usuario(11L, "Oficial de Justiça Estadual", TipoUsuario.OFICIAL_JUSTICA, "TJCE", "CE", "Fortaleza");
        OperationalFunctionCredential existing = new OperationalFunctionCredential();
        existing.setId(88L);
        existing.setUsuario(target);
        existing.setFunctionCode(OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE);
        existing.setStatus("ACTIVE");
        existing.setSecretHash("argon-hash");
        existing.setTribunalCodigo("TJCE");

        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(target));
        when(authorityService.requireDirectorForTarget(target, OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE)).thenReturn(director);
        when(authorityService.resolveTribunal(target)).thenReturn("TJCE");
        when(credentialRepository.findLockedByUsuarioIdAndFunctionCode(11L, OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE)).thenReturn(Optional.of(existing));
        when(credentialRepository.findAllByUsuarioIdAndFunctionCodeIn(any(), any())).thenReturn(java.util.List.of(existing));

        OperationalFunctionCredentialService service = new OperationalFunctionCredentialService(
                currentUserService,
                usuarioRepository,
                credentialRepository,
                unlockSessionRepository,
                authorityService,
                challengeService,
                new ObjectMapper(),
                requestProvider,
                ipResolver
        );

        service.directorProvision(new OperationalCredentialDirectorProvisionRequest(
                11L,
                OperationalFunctionCredentialService.OFFICIAL_PERSONAL_SERVICE_WRITE,
                "ESTADUAL",
                "TJCE",
                "FORUM_FORTALEZA",
                "CENTRAL_MANDADOS",
                "Central de Mandados",
                "recuperação da credencial",
                true
        ));

        assertEquals("PENDING_SETUP", existing.getStatus());
        assertEquals(null, existing.getSecretHash());
        assertNotNull(existing.getLastResetAt());
        verify(credentialRepository).save(existing);
    }

    private Usuario usuario(Long id, String perfil, TipoUsuario tipoUsuario, String registro, String uf, String comarca) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome(perfil);
        usuario.setPerfil(perfil);
        usuario.setRegistroProfissional(registro);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setUf(uf);
        usuario.setComarca(comarca);
        usuario.setEmail(id + "@tribunal.gov.br");
        return usuario;
    }
}
