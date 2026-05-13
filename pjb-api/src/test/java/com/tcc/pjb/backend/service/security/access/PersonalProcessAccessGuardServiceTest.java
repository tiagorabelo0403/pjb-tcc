package com.tcc.pjb.backend.service.security.access;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;

class PersonalProcessAccessGuardServiceTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void permiteAcessoPessoalQuandoSessaoGovBrEstaPresente() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserSecurityProfileRepository repository = mock(UserSecurityProfileRepository.class);
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setCpf("12345678900");
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        usuario.setAtivo(true);
        when(currentUserService.getRequired()).thenReturn(usuario);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setGovVerifiedAt(LocalDateTime.now().minusMinutes(5));
        when(repository.findByUsuarioId(10L)).thenReturn(java.util.Optional.of(profile));
        SecurityContextHolder.getContext().setAuthentication(jwt("10", true, false, false, List.of("ROLE_ADVOGADO")));
        PersonalProcessAccessGuardService service = new PersonalProcessAccessGuardService(currentUserService, repository);
        var envelope = service.resolveOwnProcessAccess("MEUS_PROCESSOS_PESSOAIS");
        assertTrue(envelope.allowed());
        assertEquals("GOVBR_PESSOAL", envelope.accessMode());
        assertTrue(envelope.govBrAuthenticated());
    }

    @Test
    void bloqueiaPerfilCriticoSemAutenticacaoForteRecente() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserSecurityProfileRepository repository = mock(UserSecurityProfileRepository.class);
        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setCpf("98765432100");
        usuario.setTipoUsuario(TipoUsuario.JUIZ);
        usuario.setAtivo(true);
        when(currentUserService.getRequired()).thenReturn(usuario);
        UserSecurityProfile profile = new UserSecurityProfile();
        profile.setGovVerifiedAt(LocalDateTime.now().minusMinutes(10));
        when(repository.findByUsuarioId(20L)).thenReturn(java.util.Optional.of(profile));
        SecurityContextHolder.getContext().setAuthentication(jwt("20", true, false, false, List.of("ROLE_JUIZ")));
        PersonalProcessAccessGuardService service = new PersonalProcessAccessGuardService(currentUserService, repository);
        var envelope = service.resolveOwnProcessAccess("OVERVIEW_PROCESSO_PESSOAL");
        assertFalse(envelope.allowed());
        assertTrue(envelope.blockers().contains("REINFORCED_STRONG_AUTH_REQUIRED"));
    }

    private JwtAuthenticationToken jwt(String sub,
                                       boolean govBr,
                                       boolean strong,
                                       boolean device,
                                       List<String> roles) {
        java.util.Map<String, Object> claims = new java.util.LinkedHashMap<>();
        claims.put("sub", sub);
        claims.put("govbr_authenticated", govBr);
        claims.put("strong_auth", strong);
        claims.put("device_bound", device);
        claims.put("auth_source", govBr ? "GOVBR" : "CERTIFICATE");
        claims.put("amr", strong ? List.of("pwd", "mfa") : List.of("pwd"));
        Jwt jwt = new Jwt("token-value", java.time.Instant.now(), java.time.Instant.now().plusSeconds(3600), java.util.Map.of("alg", "none"), claims);
        return new JwtAuthenticationToken(jwt, roles.stream().map(SimpleGrantedAuthority::new).toList());
    }
}
