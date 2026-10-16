package com.tcc.pjb.backend.core.security.webauthn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.entity.enums.OrigemAutenticacaoSessao;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PasskeySessionServiceTest {

    private final PasskeySessionRepository repo = mock(PasskeySessionRepository.class);
    private final WebAuthnProperties props = new WebAuthnProperties();
    private final TermosAceiteService termosAceiteService = mock(TermosAceiteService.class);
    private final UserSecurityProfileRepository profileRepo = mock(UserSecurityProfileRepository.class);
    private final PasskeySessionService service = new PasskeySessionService(repo, props, termosAceiteService, profileRepo);

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        return usuario;
    }

    @Test
    void contaCongeladaNaoRecebeSessaoNovaEmNenhumLogin() {
        Usuario usuario = usuario();
        UserSecurityProfile congelado = new UserSecurityProfile();
        congelado.setFrozenAt(LocalDateTime.now().minusMinutes(1));
        when(profileRepo.findByUsuarioId(5L)).thenReturn(Optional.of(congelado));

        assertThatThrownBy(() -> service.issue(usuario, null, "1.2.3.4", OrigemAutenticacaoSessao.GOVBR))
                .isInstanceOf(AccessDeniedPjbException.class);
        assertThatThrownBy(() -> service.issueStepUp(usuario, null, "1.2.3.4", null, null, false))
                .isInstanceOf(AccessDeniedPjbException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void issueMarcaSessaoComoPendenteQuandoUsuarioNaoAceitouOsTermos() {
        Usuario usuario = usuario();
        when(termosAceiteService.precisaAceitar(usuario)).thenReturn(true);

        PasskeySessionService.IssuedPasskeySession issued = service.issue(usuario, null, "1.2.3.4", OrigemAutenticacaoSessao.PASSKEY);

        assertThat(issued.termosPendentes()).isTrue();
        verify(repo).save(argThatTermosPendentes(true));
    }

    @Test
    void issueNaoMarcaSessaoComoPendenteQuandoUsuarioJaAceitouOsTermos() {
        Usuario usuario = usuario();
        when(termosAceiteService.precisaAceitar(usuario)).thenReturn(false);

        PasskeySessionService.IssuedPasskeySession issued = service.issue(usuario, null, "1.2.3.4", OrigemAutenticacaoSessao.PASSKEY);

        assertThat(issued.termosPendentes()).isFalse();
        verify(repo).save(argThatTermosPendentes(false));
    }

    @Test
    void issueStepUpNuncaFicaPendenteDeTermosMesmoQuandoUsuarioDeveria() {
        Usuario usuario = usuario();

        PasskeySessionService.IssuedPasskeySession issued =
                service.issueStepUp(usuario, null, "1.2.3.4", "ACAO", "a".repeat(64), true);

        assertThat(issued.termosPendentes()).isFalse();
        verify(termosAceiteService, never()).precisaAceitar(any());
    }

    private static PasskeySession argThatTermosPendentes(boolean expected) {
        return org.mockito.ArgumentMatchers.argThat(session -> session != null && session.isTermosPendentes() == expected);
    }
}
