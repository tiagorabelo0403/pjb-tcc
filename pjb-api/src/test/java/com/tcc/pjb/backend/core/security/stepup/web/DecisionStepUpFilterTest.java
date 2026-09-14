package com.tcc.pjb.backend.core.security.stepup.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.security.PasskeyRequiredException;
import com.tcc.pjb.backend.configs.security.PasskeyRequirementEnforcer;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.stepup.DecisionStepUpTokenService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * F5 (plano de melhoria v3): HttpServletResponse/FilterChain viram MockHttpServletResponse (fake
 * real do Spring) e lambda com flag em vez de mock -- ver MagistraturaGeofenceFilterTest para a
 * justificativa completa.
 */
class DecisionStepUpFilterTest {

    private final DecisionStepUpTokenService tokenService = mock(DecisionStepUpTokenService.class);
    private final PasskeyRequirementEnforcer passkeyRequirementEnforcer = mock(PasskeyRequirementEnforcer.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final DecisionStepUpFilter filter =
            new DecisionStepUpFilter(tokenService, passkeyRequirementEnforcer, currentUserService);

    @Test
    void promotorSemPasskeyAindaConsegueChegarNoEnrollOptionsParaCadastrarAPrimeiraPasskey() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
        doThrow(new PasskeyRequiredException("passkey_tpm_biometrica_obrigatoria_carreira_juridica_essencial"))
                .when(passkeyRequirementEnforcer).exigirParaMagistratura(anyLong(), eq(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/security/webauthn/enroll/options");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(passkeyRequirementEnforcer, never()).exigirParaMagistratura(anyLong(), eq(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
    }

    @Test
    void defensorSemPasskeyAindaConsegueChegarNoEnrollFinishParaConcluirOCadastro() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.DEFENSOR_PUBLICO));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/security/webauthn/enroll/finish");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(passkeyRequirementEnforcer, never()).exigirParaMagistratura(anyLong(), eq(TipoUsuario.DEFENSOR_PUBLICO));
    }

    @Test
    void promotorSemPasskeyContinuaBloqueadoEmQualquerOutraRota() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
        doThrow(new PasskeyRequiredException("passkey_tpm_biometrica_obrigatoria_carreira_juridica_essencial"))
                .when(passkeyRequirementEnforcer).exigirParaMagistratura(anyLong(), eq(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/recursal/processos/7/recurso");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(response.getContentAsString()).contains("PJB_PASSKEY_REQUIRED");
        assertThat(chainCalled.get()).isFalse();
    }

    @Test
    void magistradoComPasskeyForteSeguePeloEnrollTambemSemProblema() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.JUIZ));
        doNothing().when(passkeyRequirementEnforcer).exigirParaMagistratura(anyLong(), eq(TipoUsuario.JUIZ));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/security/webauthn/enroll/options");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (req, resp) -> chainCalled.set(true);

        filter.doFilter(request, response, chain);

        assertThat(chainCalled.get()).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        verify(passkeyRequirementEnforcer, never()).exigirParaMagistratura(anyLong(), eq(TipoUsuario.JUIZ));
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
