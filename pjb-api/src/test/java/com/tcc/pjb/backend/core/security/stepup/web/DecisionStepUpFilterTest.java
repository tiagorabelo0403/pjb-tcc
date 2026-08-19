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
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

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
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(passkeyRequirementEnforcer, never()).exigirParaMagistratura(anyLong(), eq(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void defensorSemPasskeyAindaConsegueChegarNoEnrollFinishParaConcluirOCadastro() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.DEFENSOR_PUBLICO));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/security/webauthn/enroll/finish");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(passkeyRequirementEnforcer, never()).exigirParaMagistratura(anyLong(), eq(TipoUsuario.DEFENSOR_PUBLICO));
    }

    @Test
    void promotorSemPasskeyContinuaBloqueadoEmQualquerOutraRota() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
        doThrow(new PasskeyRequiredException("passkey_tpm_biometrica_obrigatoria_carreira_juridica_essencial"))
                .when(passkeyRequirementEnforcer).exigirParaMagistratura(anyLong(), eq(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/recursal/processos/7/recurso");
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        assertThat(sw.toString()).contains("PJB_PASSKEY_REQUIRED");
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void magistradoComPasskeyForteSeguePeloEnrollTambemSemProblema() throws Exception {
        when(currentUserService.getOrNull()).thenReturn(usuario(TipoUsuario.JUIZ));
        doNothing().when(passkeyRequirementEnforcer).exigirParaMagistratura(anyLong(), eq(TipoUsuario.JUIZ));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/security/webauthn/enroll/options");
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(passkeyRequirementEnforcer, never()).exigirParaMagistratura(anyLong(), eq(TipoUsuario.JUIZ));
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }
}
