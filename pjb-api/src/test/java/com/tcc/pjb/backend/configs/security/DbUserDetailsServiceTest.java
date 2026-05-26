package com.tcc.pjb.backend.configs.security;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DbUserDetailsServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private DbUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new DbUserDetailsService(usuarioRepository);
    }

    @Test
    void usuarioAtivo_retornaUserDetailsEnabled() {
        Usuario usuario = Usuario.builder()
                .senha("{noop}senhaValida1")
                .ativo(true)
                .build();
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = service.loadUserByUsername("12345678901");

        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void usuarioInativo_retornaUserDetailsComEnabledFalso() {
        Usuario usuario = Usuario.builder()
                .senha("{noop}senhaValida1")
                .ativo(false)
                .build();
        when(usuarioRepository.findByCpf("12345678901")).thenReturn(Optional.of(usuario));

        UserDetails userDetails = service.loadUserByUsername("12345678901");

        // Falha com o bug atual: isEnabled() retorna true (hardcoded)
        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    void usernameNulo_lancaUsernameNotFoundException() {
        assertThatThrownBy(() -> service.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void usernameEmBranco_lancaUsernameNotFoundException() {
        assertThatThrownBy(() -> service.loadUserByUsername("   "))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void usuarioNaoEncontrado_lancaUsernameNotFoundException() {
        when(usuarioRepository.findByCpf("99999999999")).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmail("99999999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("99999999999"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
