package com.tcc.pjb.backend.service.security.govbr;

import com.tcc.pjb.backend.core.security.webauthn.PasskeySessionService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcClient;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcUrls;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrPkce;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrTokenResponse;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrUserInfoResponse;
import com.tcc.pjb.backend.model.dto.govbr.GovBrLoginSessionResponse;
import com.tcc.pjb.backend.model.dto.govbr.GovBrLoginStartResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.identity.GovBrLoginState;
import com.tcc.pjb.backend.model.repository.GovBrLoginStateRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovBrLoginService {

  private static final Logger log = LoggerFactory.getLogger(GovBrLoginService.class);

  private final GovBrOidcProperties props;
  private final GovBrOidcClient client;
  private final GovBrLoginStateRepository stateRepo;
  private final UsuarioRepository usuarioRepository;
  private final PasskeySessionService passkeySessionService;

  public GovBrLoginService(GovBrOidcProperties props,
      GovBrOidcClient client,
      GovBrLoginStateRepository stateRepo,
      UsuarioRepository usuarioRepository,
      PasskeySessionService passkeySessionService) {
    this.props = Objects.requireNonNull(props);
    this.client = Objects.requireNonNull(client);
    this.stateRepo = Objects.requireNonNull(stateRepo);
    this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
    this.passkeySessionService = Objects.requireNonNull(passkeySessionService);
  }

  @Transactional
  public GovBrLoginStartResponse start() {
    if (!props.enabled()) {
      throw new IllegalStateException("govbr_disabled");
    }

    UUID stateId = UUID.randomUUID();
    GovBrPkce.Generated pkce = GovBrPkce.generate();

    Instant now = Instant.now();
    Instant expires = now.plus(props.stateTtl());

    GovBrLoginState st = new GovBrLoginState(stateId, pkce.codeVerifier(), pkce.nonce(), expires, now);
    stateRepo.save(st);

    String url = GovBrOidcUrls.authorizeUrl(
        props,
        props.redirectUriLogin(),
        props.effectiveCitizenLinkScope(),
        stateId.toString(),
        pkce.codeChallenge(),
        pkce.nonce()
    );

    return new GovBrLoginStartResponse(url, stateId.toString());
  }

  @Transactional
  public String handleCallback(String code, String state) throws IOException, InterruptedException {
    if (!props.enabled()) {
      throw new IllegalStateException("govbr_disabled");
    }
    if (code == null || code.isBlank() || state == null || state.isBlank()) {
      return errorRedirect("missing");
    }

    UUID stateId;
    try {
      stateId = UUID.fromString(state.trim());
    } catch (Exception e) {
      return errorRedirect("bad_state");
    }

    GovBrLoginState st = stateRepo.findById(stateId).orElse(null);
    if (st == null) {
      return errorRedirect("state_not_found");
    }

    Instant now = Instant.now();
    if (st.isUsed() || st.isExpired(now)) {
      return errorRedirect("state_expired");
    }

    GovBrTokenResponse token = client.exchangeCode(code.trim(), st.getCodeVerifier(), props.redirectUriLogin());
    if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
      return errorRedirect("token");
    }
    if (token.idToken() == null || token.idToken().isBlank()) {
      return errorRedirect("id_token_missing");
    }

    GovBrOidcClient.VerifiedIdToken verified;
    try {
      verified = client.parseAndVerifyIdToken(token.idToken());
    } catch (Exception e) {
      return errorRedirect("id_token_invalid");
    }
    String nonce = null;
    try {
      nonce = verified.claims().getStringClaim("nonce");
    } catch (Exception ignored) {
    }
    if (nonce == null || !nonce.equals(st.getNonce())) {
      return errorRedirect("nonce_mismatch");
    }

    GovBrUserInfoResponse info = client.userInfo(token.accessToken());
    String cpf = normalizeCpf(verified.claims().getSubject());
    if (cpf == null) {
      cpf = normalizeCpf(info != null ? info.sub() : null);
    }
    if (cpf == null) {
      return errorRedirect("cpf_missing");
    }

    Usuario usuario = usuarioRepository.findByCpf(cpf).orElse(null);
    if (usuario == null) {
      return errorRedirect("cpf_nao_cadastrado");
    }

    st.markUsed(now, usuario.getId());
    stateRepo.save(st);

    return successRedirect(stateId);
  }

  @Transactional
  public GovBrLoginSessionResponse retrieveSession(String state, String ip) {
    UUID stateId;
    try {
      stateId = UUID.fromString(Objects.requireNonNull(state).trim());
    } catch (Exception e) {
      throw new IllegalArgumentException("state_invalido");
    }

    GovBrLoginState st = stateRepo.findById(stateId)
        .orElseThrow(() -> new IllegalArgumentException("state_nao_encontrado"));

    Instant now = Instant.now();
    if (!st.isUsed() || st.getUsuarioId() == null) {
      throw new IllegalStateException("login_nao_concluido");
    }
    if (st.isSessionRetrieved()) {
      throw new IllegalStateException("sessao_ja_recuperada");
    }
    if (st.isExpired(now)) {
      throw new IllegalStateException("state_expirado");
    }

    Usuario usuario = usuarioRepository.findById(st.getUsuarioId())
        .orElseThrow(() -> new IllegalStateException("usuario_nao_encontrado"));

    st.markSessionRetrieved(now);
    stateRepo.save(st);

    var issued = passkeySessionService.issue(usuario, null, ip);
    return new GovBrLoginSessionResponse(issued.token(), issued.expiresAt());
  }

  private String successRedirect(UUID stateId) {
    String s = props.frontendLoginSuccessRedirect();
    if (s == null || s.isBlank()) return "";
    String sep = s.contains("?") ? "&" : "?";
    return s + sep + "state=" + stateId;
  }

  private String errorRedirect(String code) {
    String s = props.frontendLoginErrorRedirect();
    if (s == null || s.isBlank()) return "";
    String sep = s.contains("?") ? "&" : "?";
    return s + sep + "e=" + java.net.URLEncoder.encode(code == null ? "" : code, java.nio.charset.StandardCharsets.UTF_8);
  }

  private static String normalizeCpf(String cpf) {
    if (cpf == null) return null;
    String d = cpf.replaceAll("\\D", "");
    if (d.length() != 11) return null;
    return d;
  }
}
