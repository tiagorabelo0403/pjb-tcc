package com.tcc.pjb.backend.service.security.govbr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.jwt.JWTClaimsSet;
import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcClient;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrTokenResponse;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrUserInfoResponse;
import com.tcc.pjb.backend.model.dto.govbr.GovBrLoginSessionResponse;
import com.tcc.pjb.backend.model.dto.govbr.GovBrLoginStartResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.identity.GovBrLoginState;
import com.tcc.pjb.backend.model.repository.GovBrLoginStateRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GovBrLoginServiceTest {

    private GovBrOidcProperties props;
    private GovBrOidcClient client;
    private GovBrLoginStateRepository stateRepo;
    private UsuarioRepository usuarioRepository;
    private PasskeySessionService passkeySessionService;
    private GovBrLoginService service;

    @BeforeEach
    void setUp() {
        props = enabledProps();
        client = mock(GovBrOidcClient.class);
        stateRepo = mock(GovBrLoginStateRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        passkeySessionService = mock(PasskeySessionService.class);
        service = new GovBrLoginService(props, client, stateRepo, usuarioRepository, passkeySessionService);
        when(stateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private GovBrOidcProperties enabledProps() {
        return new GovBrOidcProperties(
                true, false,
                "https://sso.acesso.gov.br/authorize", "https://sso.acesso.gov.br/token",
                "https://sso.acesso.gov.br/userinfo", "https://sso.acesso.gov.br/picture",
                "client-id", "client-secret",
                "https://pjb.local/redirect", "https://pjb.local/step-up", "https://pjb.local/login/callback",
                "step_up_scope", "https://sso.acesso.gov.br/jwks", "https://sso.acesso.gov.br",
                "https://pjb.local/sucesso", "https://pjb.local/erro",
                "https://frontend.local/entrar/govbr/sucesso", "https://frontend.local/entrar/govbr/erro",
                Duration.ofSeconds(4), Duration.ofSeconds(6), Duration.ofMinutes(5));
    }

    @Test
    void startComGovbrDesabilitadoLancaIllegalState() {
        GovBrOidcProperties disabled = new GovBrOidcProperties(
                false, false, "u", "u", "u", "u", "c", "s", "r", "r2", "r3",
                "scope", "j", "i", "fs", "fe", "fls", "fle",
                Duration.ofSeconds(4), Duration.ofSeconds(6), Duration.ofMinutes(5));
        GovBrLoginService disabledService = new GovBrLoginService(disabled, client, stateRepo, usuarioRepository, passkeySessionService);

        assertThatThrownBy(disabledService::start).isInstanceOf(IllegalStateException.class);
        verify(stateRepo, never()).save(any());
    }

    @Test
    void startGeraPkceESalvaEstadoComExpiracaoFutura() {
        GovBrLoginStartResponse response = service.start();

        assertThat(response.authorizeUrl()).startsWith("https://sso.acesso.gov.br/authorize");
        assertThat(response.state()).isNotBlank();
        assertThat(UUID.fromString(response.state())).isNotNull();

        var captor = org.mockito.ArgumentCaptor.forClass(GovBrLoginState.class);
        verify(stateRepo).save(captor.capture());
        GovBrLoginState saved = captor.getValue();
        assertThat(saved.getStateId().toString()).isEqualTo(response.state());
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        assertThat(saved.isUsed()).isFalse();
    }

    @Test
    void startUsaRedirectUriLoginQuandoConfiguradoNaUrlDeAutorizacao() {
        GovBrLoginStartResponse response = service.start();

        assertThat(response.authorizeUrl()).contains(java.net.URLEncoder.encode("https://pjb.local/login/callback", java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void startCaiParaRedirectUriPrincipalQuandoRedirectUriLoginNaoConfigurado() {
        GovBrOidcProperties semRedirectLogin = new GovBrOidcProperties(
                true, false,
                "https://sso.acesso.gov.br/authorize", "https://sso.acesso.gov.br/token",
                "https://sso.acesso.gov.br/userinfo", "https://sso.acesso.gov.br/picture",
                "client-id", "client-secret",
                "https://pjb.local/redirect", "https://pjb.local/step-up", null,
                "step_up_scope", "https://sso.acesso.gov.br/jwks", "https://sso.acesso.gov.br",
                "https://pjb.local/sucesso", "https://pjb.local/erro",
                "https://frontend.local/entrar/govbr/sucesso", "https://frontend.local/entrar/govbr/erro",
                Duration.ofSeconds(4), Duration.ofSeconds(6), Duration.ofMinutes(5));
        GovBrLoginService semRedirectLoginService = new GovBrLoginService(semRedirectLogin, client, stateRepo, usuarioRepository, passkeySessionService);

        GovBrLoginStartResponse response = semRedirectLoginService.start();

        assertThat(response.authorizeUrl()).contains(java.net.URLEncoder.encode("https://pjb.local/redirect", java.nio.charset.StandardCharsets.UTF_8));
        assertThat(response.authorizeUrl()).doesNotContain("null");
    }

    @Test
    void handleCallbackUsaOMesmoRedirectUriEfetivoDeStartAoTrocarOCode() throws Exception {
        GovBrOidcProperties semRedirectLogin = new GovBrOidcProperties(
                true, false,
                "https://sso.acesso.gov.br/authorize", "https://sso.acesso.gov.br/token",
                "https://sso.acesso.gov.br/userinfo", "https://sso.acesso.gov.br/picture",
                "client-id", "client-secret",
                "https://pjb.local/redirect", "https://pjb.local/step-up", null,
                "step_up_scope", "https://sso.acesso.gov.br/jwks", "https://sso.acesso.gov.br",
                "https://pjb.local/sucesso", "https://pjb.local/erro",
                "https://frontend.local/entrar/govbr/sucesso", "https://frontend.local/entrar/govbr/erro",
                Duration.ofSeconds(4), Duration.ofSeconds(6), Duration.ofMinutes(5));
        GovBrLoginService semRedirectLoginService = new GovBrLoginService(semRedirectLogin, client, stateRepo, usuarioRepository, passkeySessionService);

        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce", Instant.now().plusSeconds(300), Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(client.exchangeCode(eq("code-real"), eq("verifier"), eq("https://pjb.local/redirect")))
                .thenReturn(new GovBrTokenResponse("access-token", null, "Bearer", 3600, "openid"));

        semRedirectLoginService.handleCallback("code-real", stateId.toString());

        verify(client).exchangeCode("code-real", "verifier", "https://pjb.local/redirect");
    }

    @Test
    void handleCallbackComCodeOuStateAusenteRedirecionaComErro() throws Exception {
        String redirect = service.handleCallback(null, null);

        assertThat(redirect).contains("e=missing");
        verify(stateRepo, never()).findById(any());
    }

    @Test
    void handleCallbackComStateForaDoFormatoUuidRedirecionaComErro() throws Exception {
        String redirect = service.handleCallback("code-real", "nao-e-um-uuid");

        assertThat(redirect).contains("e=bad_state");
    }

    @Test
    void handleCallbackComStateInexistenteRedirecionaComErro() throws Exception {
        UUID stateId = UUID.randomUUID();
        when(stateRepo.findById(stateId)).thenReturn(Optional.empty());

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("e=state_not_found");
    }

    @Test
    void handleCallbackComStateJaUsadoRedirecionaComErro() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce-1", Instant.now().plusSeconds(300), Instant.now());
        st.markUsed(Instant.now(), 1L);
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("e=state_expired");
    }

    @Test
    void handleCallbackComStateExpiradoRedirecionaComErro() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce-1", Instant.now().minusSeconds(1), Instant.now().minusSeconds(400));
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("e=state_expired");
    }

    @Test
    void handleCallbackComFalhaNaTrocaDoCodeRedirecionaComErro() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce-1", Instant.now().plusSeconds(300), Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(client.exchangeCode(eq("code-real"), eq("verifier"), anyString())).thenReturn(null);

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("e=token");
    }

    @Test
    void handleCallbackComIdTokenAusenteRedirecionaComErro() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce-1", Instant.now().plusSeconds(300), Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(client.exchangeCode(eq("code-real"), eq("verifier"), anyString()))
                .thenReturn(new GovBrTokenResponse("access-token", null, "Bearer", 3600, "openid"));

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("e=id_token_missing");
    }

    @Test
    void handleCallbackComIdTokenInvalidoRedirecionaComErro() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce-1", Instant.now().plusSeconds(300), Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(client.exchangeCode(eq("code-real"), eq("verifier"), anyString()))
                .thenReturn(new GovBrTokenResponse("access-token", "id-token-raw", "Bearer", 3600, "openid"));
        when(client.parseAndVerifyIdToken("id-token-raw")).thenThrow(new java.io.IOException("assinatura invalida"));

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("e=id_token_invalid");
    }

    @Test
    void handleCallbackComNonceDivergenteRedirecionaComErro() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce-esperado", Instant.now().plusSeconds(300), Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(client.exchangeCode(eq("code-real"), eq("verifier"), anyString()))
                .thenReturn(new GovBrTokenResponse("access-token", "id-token-raw", "Bearer", 3600, "openid"));
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("11122233344").claim("nonce", "nonce-divergente").build();
        when(client.parseAndVerifyIdToken("id-token-raw"))
                .thenReturn(new GovBrOidcClient.VerifiedIdToken(claims, "kid-1", Instant.now(), Instant.now().plusSeconds(300), List.of("client-id")));

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("e=nonce_mismatch");
    }

    @Test
    void handleCallbackComCpfNaoCadastradoRedirecionaComErro() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce-esperado", Instant.now().plusSeconds(300), Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(client.exchangeCode(eq("code-real"), eq("verifier"), anyString()))
                .thenReturn(new GovBrTokenResponse("access-token", "id-token-raw", "Bearer", 3600, "openid"));
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("111.222.333-44").claim("nonce", "nonce-esperado").build();
        when(client.parseAndVerifyIdToken("id-token-raw"))
                .thenReturn(new GovBrOidcClient.VerifiedIdToken(claims, "kid-1", Instant.now(), Instant.now().plusSeconds(300), List.of("client-id")));
        when(client.userInfo("access-token")).thenReturn(null);
        when(usuarioRepository.findByCpf("11122233344")).thenReturn(Optional.empty());

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("e=cpf_nao_cadastrado");
    }

    @Test
    void handleCallbackComSucessoMarcaEstadoUsadoERedirecionaParaSucesso() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce-esperado", Instant.now().plusSeconds(300), Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(client.exchangeCode(eq("code-real"), eq("verifier"), anyString()))
                .thenReturn(new GovBrTokenResponse("access-token", "id-token-raw", "Bearer", 3600, "openid"));
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("111.222.333-44").claim("nonce", "nonce-esperado").build();
        when(client.parseAndVerifyIdToken("id-token-raw"))
                .thenReturn(new GovBrOidcClient.VerifiedIdToken(claims, "kid-1", Instant.now(), Instant.now().plusSeconds(300), List.of("client-id")));
        when(client.userInfo("access-token")).thenReturn(null);
        Usuario usuario = new Usuario();
        usuario.setId(42L);
        when(usuarioRepository.findByCpf("11122233344")).thenReturn(Optional.of(usuario));

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("https://frontend.local/entrar/govbr/sucesso").contains("state=" + stateId);
        assertThat(st.isUsed()).isTrue();
        assertThat(st.getUsuarioId()).isEqualTo(42L);
        verify(stateRepo).save(st);
    }

    @Test
    void handleCallbackUsaCpfDoUserInfoQuandoSubjectDoIdTokenNaoEhCpfValido() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce-esperado", Instant.now().plusSeconds(300), Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(client.exchangeCode(eq("code-real"), eq("verifier"), anyString()))
                .thenReturn(new GovBrTokenResponse("access-token", "id-token-raw", "Bearer", 3600, "openid"));
        JWTClaimsSet claims = new JWTClaimsSet.Builder().subject("subject-opaco-nao-cpf").claim("nonce", "nonce-esperado").build();
        when(client.parseAndVerifyIdToken("id-token-raw"))
                .thenReturn(new GovBrOidcClient.VerifiedIdToken(claims, "kid-1", Instant.now(), Instant.now().plusSeconds(300), List.of("client-id")));
        when(client.userInfo("access-token"))
                .thenReturn(new GovBrUserInfoResponse("555.666.777-88", "Nome", null, "usuario@example.com", true, null, null, null));
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        when(usuarioRepository.findByCpf("55566677788")).thenReturn(Optional.of(usuario));

        String redirect = service.handleCallback("code-real", stateId.toString());

        assertThat(redirect).contains("state=" + stateId);
        assertThat(st.getUsuarioId()).isEqualTo(7L);
    }

    @Test
    void retrieveSessionComStateInvalidoLancaIllegalArgument() {
        assertThatThrownBy(() -> service.retrieveSession("nao-e-um-uuid", "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retrieveSessionComStateInexistenteLancaIllegalArgument() {
        UUID stateId = UUID.randomUUID();
        when(stateRepo.findById(stateId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retrieveSession(stateId.toString(), "127.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retrieveSessionComLoginNaoConcluidoLancaIllegalState() {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce", Instant.now().plusSeconds(300), Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));

        assertThatThrownBy(() -> service.retrieveSession(stateId.toString(), "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("login_nao_concluido");
    }

    @Test
    void retrieveSessionComSessaoJaRecuperadaLancaIllegalState() {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce", Instant.now().plusSeconds(300), Instant.now());
        st.markUsed(Instant.now(), 1L);
        st.markSessionRetrieved(Instant.now());
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));

        assertThatThrownBy(() -> service.retrieveSession(stateId.toString(), "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sessao_ja_recuperada");
    }

    @Test
    void retrieveSessionComStateExpiradoLancaIllegalState() {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce", Instant.now().minusSeconds(1), Instant.now().minusSeconds(400));
        st.markUsed(Instant.now().minusSeconds(300), 1L);
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));

        assertThatThrownBy(() -> service.retrieveSession(stateId.toString(), "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("state_expirado");
    }

    @Test
    void retrieveSessionComUsuarioNaoEncontradoLancaIllegalState() {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce", Instant.now().plusSeconds(300), Instant.now());
        st.markUsed(Instant.now(), 99L);
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.retrieveSession(stateId.toString(), "127.0.0.1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("usuario_nao_encontrado");
    }

    @Test
    void retrieveSessionComSucessoMarcaRecuperadaEEmiteSessaoReal() {
        UUID stateId = UUID.randomUUID();
        GovBrLoginState st = new GovBrLoginState(stateId, "verifier", "nonce", Instant.now().plusSeconds(300), Instant.now());
        st.markUsed(Instant.now(), 99L);
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        Usuario usuario = new Usuario();
        usuario.setId(99L);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(usuario));
        PasskeySessionService.IssuedPasskeySession issued =
                new PasskeySessionService.IssuedPasskeySession("bearer-real-token", LocalDateTime.now().plusHours(8), 123L, false);
        when(passkeySessionService.issue(usuario, null, "203.0.113.5")).thenReturn(issued);

        GovBrLoginSessionResponse response = service.retrieveSession(stateId.toString(), "203.0.113.5");

        assertThat(response.token()).isEqualTo("bearer-real-token");
        assertThat(response.expiresAt()).isEqualTo(issued.expiresAt());
        assertThat(st.isSessionRetrieved()).isTrue();
        verify(passkeySessionService).issue(usuario, null, "203.0.113.5");
    }
}
