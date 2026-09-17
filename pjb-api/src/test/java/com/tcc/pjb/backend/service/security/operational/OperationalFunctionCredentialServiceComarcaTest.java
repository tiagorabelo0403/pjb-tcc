package com.tcc.pjb.backend.service.security.operational;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.SecurityChallengeService;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialDirectorProvisionRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.OperationalFunctionCredential;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.OperationalFunctionCredentialRepository;
import com.tcc.pjb.backend.model.repository.security.OperationalFunctionUnlockSessionRepository;
import com.tcc.pjb.backend.service.competencia.ComarcaResolutionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class OperationalFunctionCredentialServiceComarcaTest {

    @Test
    void resolveEAplicaComarcaDoCatalogoAoProvisionar() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        OperationalFunctionCredentialRepository credentialRepository = mock(OperationalFunctionCredentialRepository.class);
        OperationalFunctionCredentialAuthorityService authorityService = mock(OperationalFunctionCredentialAuthorityService.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        OperationalFunctionCredentialService service = service(usuarioRepository, credentialRepository, authorityService, comarcaResolutionService);

        Usuario director = usuario(1L, TipoUsuario.SERVIDOR_FORUM, "CE", "Fortaleza");
        Usuario target = usuario(2L, TipoUsuario.SERVIDOR, "CE", "Fortaleza");
        Comarca comarcaEsperada = mock(Comarca.class);

        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(target));
        when(authorityService.requireDirectorForTarget(target, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE)).thenReturn(director);
        when(authorityService.resolveTribunal(target)).thenReturn("TJCE");
        when(credentialRepository.findLockedByUsuarioIdAndFunctionCode(2L, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE)).thenReturn(Optional.empty());
        when(credentialRepository.findAllByUsuarioIdAndFunctionCodeIn(any(), any())).thenReturn(java.util.List.of());
        when(comarcaResolutionService.resolver("Fortaleza", "CE")).thenReturn(Optional.of(comarcaEsperada));

        service.directorProvision(new OperationalCredentialDirectorProvisionRequest(
                2L, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE,
                "ESTADUAL", "TJCE", "FORUM_FORTALEZA", "VARA_1", "1ª Vara Cível", "provisionamento", false
        ));

        org.mockito.ArgumentCaptor<OperationalFunctionCredential> captor = org.mockito.ArgumentCaptor.forClass(OperationalFunctionCredential.class);
        org.mockito.Mockito.verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isSameAs(comarcaEsperada);
    }

    @Test
    void naoResolveQuandoComarcaEmBrancoENaoLancaExcecao() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        OperationalFunctionCredentialRepository credentialRepository = mock(OperationalFunctionCredentialRepository.class);
        OperationalFunctionCredentialAuthorityService authorityService = mock(OperationalFunctionCredentialAuthorityService.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        OperationalFunctionCredentialService service = service(usuarioRepository, credentialRepository, authorityService, comarcaResolutionService);

        Usuario director = usuario(3L, TipoUsuario.SERVIDOR_FORUM, null, null);
        Usuario target = usuario(4L, TipoUsuario.SERVIDOR, null, null);

        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(target));
        when(authorityService.requireDirectorForTarget(target, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE)).thenReturn(director);
        when(authorityService.resolveTribunal(target)).thenReturn("TJCE");
        when(credentialRepository.findLockedByUsuarioIdAndFunctionCode(4L, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE)).thenReturn(Optional.empty());
        when(credentialRepository.findAllByUsuarioIdAndFunctionCodeIn(any(), any())).thenReturn(java.util.List.of());

        service.directorProvision(new OperationalCredentialDirectorProvisionRequest(
                4L, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE,
                "ESTADUAL", "TJCE", "FORUM_FORTALEZA", "VARA_1", "1ª Vara Cível", "provisionamento", false
        ));

        org.mockito.ArgumentCaptor<OperationalFunctionCredential> captor = org.mockito.ArgumentCaptor.forClass(OperationalFunctionCredential.class);
        org.mockito.Mockito.verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    @Test
    void mantemComarcaEntidadeNulaQuandoResolverNaoAchaCandidata() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        OperationalFunctionCredentialRepository credentialRepository = mock(OperationalFunctionCredentialRepository.class);
        OperationalFunctionCredentialAuthorityService authorityService = mock(OperationalFunctionCredentialAuthorityService.class);
        ComarcaResolutionService comarcaResolutionService = mock(ComarcaResolutionService.class);
        OperationalFunctionCredentialService service = service(usuarioRepository, credentialRepository, authorityService, comarcaResolutionService);

        Usuario director = usuario(5L, TipoUsuario.SERVIDOR_FORUM, "CE", "Comarca Inexistente");
        Usuario target = usuario(6L, TipoUsuario.SERVIDOR, "CE", "Comarca Inexistente");

        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(target));
        when(authorityService.requireDirectorForTarget(target, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE)).thenReturn(director);
        when(authorityService.resolveTribunal(target)).thenReturn("TJCE");
        when(credentialRepository.findLockedByUsuarioIdAndFunctionCode(6L, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE)).thenReturn(Optional.empty());
        when(credentialRepository.findAllByUsuarioIdAndFunctionCodeIn(any(), any())).thenReturn(java.util.List.of());
        when(comarcaResolutionService.resolver("Comarca Inexistente", "CE")).thenReturn(Optional.empty());

        service.directorProvision(new OperationalCredentialDirectorProvisionRequest(
                6L, OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE,
                "ESTADUAL", "TJCE", "FORUM_FORTALEZA", "VARA_1", "1ª Vara Cível", "provisionamento", false
        ));

        org.mockito.ArgumentCaptor<OperationalFunctionCredential> captor = org.mockito.ArgumentCaptor.forClass(OperationalFunctionCredential.class);
        org.mockito.Mockito.verify(credentialRepository).save(captor.capture());
        assertThat(captor.getValue().getComarcaEntidade()).isNull();
    }

    private OperationalFunctionCredentialService service(UsuarioRepository usuarioRepository,
                                                          OperationalFunctionCredentialRepository credentialRepository,
                                                          OperationalFunctionCredentialAuthorityService authorityService,
                                                          ComarcaResolutionService comarcaResolutionService) {
        @SuppressWarnings("unchecked")
        ObjectProvider<jakarta.servlet.http.HttpServletRequest> requestProvider = mock(ObjectProvider.class);
        return new OperationalFunctionCredentialService(
                mock(CurrentUserService.class),
                usuarioRepository,
                credentialRepository,
                mock(OperationalFunctionUnlockSessionRepository.class),
                authorityService,
                mock(SecurityChallengeService.class),
                new ObjectMapper(),
                requestProvider,
                mock(ClientIpResolver.class),
                comarcaResolutionService
        );
    }

    private Usuario usuario(Long id, TipoUsuario tipoUsuario, String uf, String comarca) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuario " + id);
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setUf(uf);
        usuario.setComarca(comarca);
        usuario.setEmail(id + "@tribunal.gov.br");
        return usuario;
    }
}
