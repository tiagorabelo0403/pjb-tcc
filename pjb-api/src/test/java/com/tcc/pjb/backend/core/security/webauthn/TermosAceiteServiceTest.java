package com.tcc.pjb.backend.core.security.webauthn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;
import com.tcc.pjb.backend.model.entity.security.TermosAceite;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import com.tcc.pjb.backend.model.repository.security.TermosAceiteRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TermosAceiteServiceTest {

    private final TermosAceiteRepository repository = mock(TermosAceiteRepository.class);
    private final PasskeySessionRepository passkeySessionRepository = mock(PasskeySessionRepository.class);
    private TermosAceiteService service;

    @BeforeEach
    void setUp() {
        service = new TermosAceiteService(repository, passkeySessionRepository, "v2");
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        return usuario;
    }

    @Test
    void precisaAceitarQuandoNenhumRegistroExisteParaAVersaoAtual() {
        Usuario usuario = usuario();
        when(repository.findByUsuarioIdAndVersao(10L, "v2")).thenReturn(Optional.empty());

        assertThat(service.precisaAceitar(usuario)).isTrue();
    }

    @Test
    void naoPrecisaAceitarQuandoJaExisteRegistroParaAVersaoAtual() {
        Usuario usuario = usuario();
        when(repository.findByUsuarioIdAndVersao(10L, "v2")).thenReturn(Optional.of(mock(TermosAceite.class)));

        assertThat(service.precisaAceitar(usuario)).isFalse();
    }

    @Test
    void usuarioNuloOuSemIdSempreExigeAceite() {
        assertThat(service.precisaAceitar(null)).isTrue();
        assertThat(service.precisaAceitar(new Usuario())).isTrue();
    }

    @Test
    void registrarAceiteRejeitaVersaoDiferenteDaVigente() {
        Usuario usuario = usuario();

        assertThatThrownBy(() -> service.registrarAceite(usuario, "v1-antiga", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrarAceiteNaoDuplicaQuandoJaAceito() {
        Usuario usuario = usuario();
        when(repository.findByUsuarioIdAndVersao(10L, "v2")).thenReturn(Optional.of(mock(TermosAceite.class)));

        service.registrarAceite(usuario, "v2", "1.2.3.4");

        verify(repository, never()).save(any());
    }

    @Test
    void confirmarAceitePorTokenRegistraAceiteELiberaASessao() {
        Usuario usuario = usuario();
        PasskeySession sessaoPendente = new PasskeySession();
        sessaoPendente.setUsuario(usuario);
        sessaoPendente.setTokenHash(PasskeySessionService.sha256Hex("token-real"));
        sessaoPendente.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        sessaoPendente.setTermosPendentes(true);
        when(passkeySessionRepository.findActiveByTokenHash(eq(PasskeySessionService.sha256Hex("token-real"))))
                .thenReturn(Optional.of(sessaoPendente));
        when(repository.findByUsuarioIdAndVersao(10L, "v2")).thenReturn(Optional.empty());

        service.confirmarAceitePorToken("token-real", "v2", "1.2.3.4");

        assertThat(sessaoPendente.isTermosPendentes()).isFalse();
        verify(repository).save(any(TermosAceite.class));
        verify(passkeySessionRepository).save(sessaoPendente);
    }

    @Test
    void confirmarAceitePorTokenFalhaQuandoSessaoNaoEstaPendente() {
        Usuario usuario = usuario();
        PasskeySession sessaoJaLiberada = new PasskeySession();
        sessaoJaLiberada.setUsuario(usuario);
        sessaoJaLiberada.setTokenHash(PasskeySessionService.sha256Hex("token-livre"));
        sessaoJaLiberada.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        sessaoJaLiberada.setTermosPendentes(false);
        when(passkeySessionRepository.findActiveByTokenHash(eq(PasskeySessionService.sha256Hex("token-livre"))))
                .thenReturn(Optional.of(sessaoJaLiberada));

        assertThatThrownBy(() -> service.confirmarAceitePorToken("token-livre", "v2", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirmarAceitePorTokenFalhaQuandoSessaoExpirada() {
        Usuario usuario = usuario();
        PasskeySession sessaoExpirada = new PasskeySession();
        sessaoExpirada.setUsuario(usuario);
        sessaoExpirada.setTokenHash(PasskeySessionService.sha256Hex("token-expirado"));
        sessaoExpirada.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        sessaoExpirada.setTermosPendentes(true);
        when(passkeySessionRepository.findActiveByTokenHash(eq(PasskeySessionService.sha256Hex("token-expirado"))))
                .thenReturn(Optional.of(sessaoExpirada));

        assertThatThrownBy(() -> service.confirmarAceitePorToken("token-expirado", "v2", null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void confirmarAceitePorTokenFalhaQuandoTokenNaoEncontrado() {
        when(passkeySessionRepository.findActiveByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmarAceitePorToken("token-inexistente", "v2", null))
                .isInstanceOf(IllegalStateException.class);
    }
}
