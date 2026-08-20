package com.tcc.pjb.backend.core.security.webauthn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import org.junit.jupiter.api.Test;

class PasskeySessionServiceTest {

    private final PasskeySessionRepository repo = mock(PasskeySessionRepository.class);
    private final WebAuthnProperties props = new WebAuthnProperties();
    private final TermosAceiteService termosAceiteService = mock(TermosAceiteService.class);
    private final PasskeySessionService service = new PasskeySessionService(repo, props, termosAceiteService);

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        return usuario;
    }

    @Test
    void issueMarcaSessaoComoPendenteQuandoUsuarioNaoAceitouOsTermos() {
        Usuario usuario = usuario();
        when(termosAceiteService.precisaAceitar(usuario)).thenReturn(true);

        PasskeySessionService.IssuedPasskeySession issued = service.issue(usuario, null, "1.2.3.4");

        assertThat(issued.termosPendentes()).isTrue();
        verify(repo).save(argThatTermosPendentes(true));
    }

    @Test
    void issueNaoMarcaSessaoComoPendenteQuandoUsuarioJaAceitouOsTermos() {
        Usuario usuario = usuario();
        when(termosAceiteService.precisaAceitar(usuario)).thenReturn(false);

        PasskeySessionService.IssuedPasskeySession issued = service.issue(usuario, null, "1.2.3.4");

        assertThat(issued.termosPendentes()).isFalse();
        verify(repo).save(argThatTermosPendentes(false));
    }

    @Test
    void issueStepUpNuncaFicaPendenteDeTermosMesmoQuandoUsuarioDeveria() {
        Usuario usuario = usuario();

        PasskeySessionService.IssuedPasskeySession issued =
                service.issueStepUp(usuario, null, "1.2.3.4", "ACAO", "a".repeat(64), true);

        assertThat(issued.termosPendentes()).isFalse();
        verify(termosAceiteService, org.mockito.Mockito.never()).precisaAceitar(any());
    }

    private static PasskeySession argThatTermosPendentes(boolean expected) {
        return org.mockito.ArgumentMatchers.argThat(session -> session != null && session.isTermosPendentes() == expected);
    }
}
