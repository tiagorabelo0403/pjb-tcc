package com.tcc.pjb.backend.service.security.govbr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.nimbusds.jwt.JWTClaimsSet;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.govbr.GovBrAssuranceLevel;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcClient;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrTokenResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.identity.GovBrStepUpState;
import com.tcc.pjb.backend.model.repository.GovBrStepUpStateRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.SecurityChallengeRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GovBrStepUpServiceAssuranceTest {

    @Mock GovBrOidcClient client;
    @Mock GovBrStepUpStateRepository stateRepo;
    @Mock UsuarioRepository usuarioRepo;
    @Mock TrustedDeviceRepository deviceRepo;
    @Mock SecurityChallengeRepository challengeRepo;
    @Mock AuditLedgerService ledger;
    @Mock GovBrAccountProfileSynchronizationService syncService;

    private static final String CPF = "12345678901";
    private static final String NONCE = "test-nonce-abc";

    private GovBrStepUpService service() {
        GovBrOidcProperties props = new GovBrOidcProperties(
                true, false,
                "https://sso.gov.br/authorize",
                "https://sso.gov.br/token",
                "https://sso.gov.br/userinfo",
                null,
                "client-id",
                "",
                "https://pjb.jus.br/callback",
                "https://pjb.jus.br/stepup/callback",
                "openid email profile govbr_confiabilidades",
                "https://sso.gov.br/jwks",
                "https://sso.gov.br",
                "https://fe/success",
                "https://fe/error",
                Duration.ofSeconds(2),
                Duration.ofSeconds(2),
                Duration.ofMinutes(5));
        return new GovBrStepUpService(props, client, stateRepo, usuarioRepo, deviceRepo, challengeRepo, ledger, syncService);
    }

    private UUID setupValidState() throws Exception {
        UUID stateId = UUID.randomUUID();
        GovBrStepUpState st = new GovBrStepUpState(
                stateId, 1L, CPF, null,
                "verifier", NONCE, "scope", "1.1.1.1",
                Instant.now().plusSeconds(300), Instant.now().minusSeconds(5));
        when(stateRepo.findById(stateId)).thenReturn(Optional.of(st));
        when(stateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GovBrTokenResponse token = new GovBrTokenResponse("access-tok", "id-tok", "Bearer", 3600, "scope");
        when(client.exchangeCode(anyString(), anyString(), anyString())).thenReturn(token);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(CPF)
                .claim("nonce", NONCE)
                .build();
        GovBrOidcClient.VerifiedIdToken verified = new GovBrOidcClient.VerifiedIdToken(
                claims, "kid", Instant.now().minusSeconds(5), Instant.now().plusSeconds(300), List.of("client-id"));
        when(client.parseAndVerifyIdToken(anyString())).thenReturn(verified);

        return stateId;
    }

    @Test
    void callbackComAcrBronze_retornaErro() throws Exception {
        UUID stateId = setupValidState();

        when(client.extractAccessTokenSignals("access-tok")).thenReturn(
                new GovBrOidcClient.GovBrAccessTokenSignals(
                        true, true, List.of("mfa"), null, null, null, Map.of(), GovBrAssuranceLevel.BRONZE));

        String result = service().handleCallback("code", stateId.toString());

        assertThat(result).contains("e=assurance_insuficiente");
    }

    @Test
    void callbackComAcrNulo_retornaErro() throws Exception {
        UUID stateId = setupValidState();

        when(client.extractAccessTokenSignals("access-tok")).thenReturn(
                new GovBrOidcClient.GovBrAccessTokenSignals(
                        true, true, List.of("mfa"), null, null, null, Map.of(), null));

        String result = service().handleCallback("code", stateId.toString());

        assertThat(result).contains("e=assurance_insuficiente");
    }

    @Test
    void callbackComAcrPrata_permitido() throws Exception {
        UUID stateId = setupValidState();

        when(client.extractAccessTokenSignals("access-tok")).thenReturn(
                new GovBrOidcClient.GovBrAccessTokenSignals(
                        true, true, List.of("mfa"), null, null, null, Map.of(), GovBrAssuranceLevel.PRATA));

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuario));

        String result = service().handleCallback("code", stateId.toString());

        assertThat(result).isEqualTo("https://fe/success");
    }
}
