package com.tcc.pjb.backend.service.magistratura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.device.SecurityChallengeService;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.SituacaoConta;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MagistradoAtivacaoServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final SecurityChallengeService securityChallengeService = mock(SecurityChallengeService.class);
    private final PasskeySessionService passkeySessionService = mock(PasskeySessionService.class);
    private final MagistradoAtivacaoService service =
            new MagistradoAtivacaoService(usuarioRepository, securityChallengeService, passkeySessionService);

    @Test
    void confirmarAtivacaoComCodigoValidoAtivaAContaEEmiteSessao() {
        Usuario usuario = usuarioPendente();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        doNothing().when(securityChallengeService).consumeOtp(eq(10L), eq(usuario), eq("123456"));
        var issued = new PasskeySessionService.IssuedPasskeySession("token-xyz", LocalDateTime.now().plusMinutes(30), 99L);
        when(passkeySessionService.issue(usuario, null, "127.0.0.1")).thenReturn(issued);

        var resultado = service.confirmarAtivacao(1L, 10L, "123456", "127.0.0.1");

        assertThat(resultado.token()).isEqualTo("token-xyz");
        assertThat(usuario.getSituacaoConta()).isEqualTo(SituacaoConta.ATIVA);
        verify(securityChallengeService).consumeOtp(10L, usuario, "123456");
    }

    @Test
    void confirmarAtivacaoDeContaJaAtivaLancaErro() {
        Usuario usuario = usuarioPendente();
        usuario.setSituacaoConta(SituacaoConta.ATIVA);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.confirmarAtivacao(1L, 10L, "123456", "127.0.0.1"))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void confirmarAtivacaoComCodigoInvalidoPropagaErroSemAtivar() {
        Usuario usuario = usuarioPendente();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        doThrow(new IllegalArgumentException("Código inválido"))
                .when(securityChallengeService).consumeOtp(eq(10L), eq(usuario), eq("000000"));

        assertThatThrownBy(() -> service.confirmarAtivacao(1L, 10L, "000000", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(usuario.getSituacaoConta()).isEqualTo(SituacaoConta.PENDENTE_ATIVACAO);
    }

    @Test
    void confirmarAtivacaoDeUsuarioInexistenteLancaErro() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmarAtivacao(999L, 10L, "123456", "127.0.0.1"))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    private Usuario usuarioPendente() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNome("Juiz Teste");
        u.setSituacaoConta(SituacaoConta.PENDENTE_ATIVACAO);
        return u;
    }
}
