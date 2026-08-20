package com.tcc.pjb.backend.service.security.govbr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcClient;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.identity.GovBrStepUpState;
import com.tcc.pjb.backend.model.repository.GovBrStepUpStateRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.SecurityChallengeRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GovBrStepUpServiceTest {

  @Mock GovBrOidcClient client;
  @Mock GovBrStepUpStateRepository stateRepo;
  @Mock UsuarioRepository usuarioRepo;
  @Mock TrustedDeviceRepository deviceRepo;
  @Mock SecurityChallengeRepository challengeRepo;
  @Mock AuditLedgerService ledger;
  @Mock GovBrAccountProfileSynchronizationService syncService;

  @Captor ArgumentCaptor<GovBrStepUpState> stateCaptor;

  private static GovBrOidcProperties props(boolean enabled) {
    return new GovBrOidcProperties(
        enabled,
        false,
        "https://sso.gov.br/authorize",
        "https://sso.gov.br/token",
        "https://sso.gov.br/userinfo",
        "https://sso.gov.br/picture",
        "client",
        "",
        "https://pjb.jus.br/api/v1/cidadao/govbr/link/callback",
        "https://pjb.jus.br/api/v1/auth/govbr/stepup/callback",
        null,
        "openid email profile govbr_confiabilidades",
        "https://sso.gov.br/jwks",
        "https://sso.gov.br",
        "https://fe/success",
        "https://fe/error",
        null,
        null,
        Duration.ofSeconds(2),
        Duration.ofSeconds(2),
        Duration.ofMinutes(5)
    );
  }

  private GovBrStepUpService service(GovBrOidcProperties props) {
    return new GovBrStepUpService(props, client, stateRepo, usuarioRepo, deviceRepo, challengeRepo, ledger, syncService);
  }

  @Test
  void start_disabled_throws() {
    GovBrStepUpService svc = service(props(false));
    Usuario u = new Usuario();
    u.setId(1L);
    u.setCpf("12345678901");

    assertThatThrownBy(() -> svc.start(u, 10L, "1.1.1.1"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("govbr_disabled");
  }

  @Test
  void start_cpf_null_throws() {
    GovBrStepUpService svc = service(props(true));
    Usuario u = new Usuario();
    u.setId(1L);
    u.setCpf(null);

    assertThatThrownBy(() -> svc.start(u, 10L, "1.1.1.1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cpf");
  }

  @Test
  void start_persists_state_and_returns_url_with_state() {
    GovBrStepUpService svc = service(props(true));
    Usuario u = new Usuario();
    u.setId(99L);
    u.setCpf("123.456.789-01");

    when(stateRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

    GovBrStepUpService.StartResult r = svc.start(u, 777L, "2.2.2.2");

    verify(stateRepo).save(stateCaptor.capture());
    GovBrStepUpState saved = stateCaptor.getValue();

    assertThat(saved.getUsuarioId()).isEqualTo(99L);
    assertThat(saved.getDeviceId()).isEqualTo(777L);
    assertThat(saved.getCpf()).isEqualTo("12345678901");
    assertThat(saved.getCodeVerifier()).isNotBlank();
    assertThat(saved.getNonce()).isNotBlank();
    assertThat(saved.getExpiresAt()).isAfter(Instant.now());

    assertThat(r.authorizeUrl()).contains("state=" + saved.getStateId());
    assertThat(r.authorizeUrl()).contains("code_challenge=");
    assertThat(r.authorizeUrl()).contains("code_challenge_method=S256");
    assertThat(r.authorizeUrl()).contains("nonce=");
  }

  @Test
  void handleCallback_state_not_found() throws Exception {
    GovBrStepUpService svc = service(props(true));
    UUID id = UUID.randomUUID();
    when(stateRepo.findById(id)).thenReturn(Optional.empty());

    String r = svc.handleCallback("code", id.toString());

    assertThat(r).contains("e=state_not_found");
    verify(client, never()).exchangeCode(anyString(), anyString(), anyString());
  }

  @Test
  void handleCallback_state_expired() throws Exception {
    GovBrStepUpService svc = service(props(true));
    UUID id = UUID.randomUUID();

    GovBrStepUpState st = new GovBrStepUpState(
        id,
        1L,
        "12345678901",
        10L,
        "verifier",
        "nonce",
        "scope",
        "1.1.1.1",
        Instant.now().minusSeconds(5),
        Instant.now().minusSeconds(10)
    );

    when(stateRepo.findById(id)).thenReturn(Optional.of(st));

    String r = svc.handleCallback("code", id.toString());

    assertThat(r).contains("e=state_expired");
    verify(client, never()).exchangeCode(anyString(), anyString(), anyString());
  }
}
