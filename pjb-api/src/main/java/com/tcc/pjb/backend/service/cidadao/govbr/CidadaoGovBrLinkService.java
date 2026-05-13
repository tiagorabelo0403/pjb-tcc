package com.tcc.pjb.backend.service.cidadao.govbr;

import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcClient;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcUrls;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrPkce;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrTokenResponse;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrUserInfoResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.identity.GovBrLinkState;
import com.tcc.pjb.backend.model.repository.GovBrLinkStateRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.service.identity.UserAvatarService;
import com.tcc.pjb.backend.service.security.govbr.GovBrAccountProfileSynchronizationService;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CidadaoGovBrLinkService {

  private static final Logger log = LoggerFactory.getLogger(CidadaoGovBrLinkService.class);

  private final GovBrOidcProperties props;
  private final GovBrOidcClient client;
  private final GovBrLinkStateRepository stateRepo;
  private final UserAvatarService avatarService;
  private final GovBrAccountProfileSynchronizationService accountProfileSynchronizationService;
  private final UsuarioRepository usuarioRepository;

  public CidadaoGovBrLinkService(GovBrOidcProperties props,
      GovBrOidcClient client,
      GovBrLinkStateRepository stateRepo,
      UserAvatarService avatarService,
      GovBrAccountProfileSynchronizationService accountProfileSynchronizationService,
      UsuarioRepository usuarioRepository) {
    this.props = Objects.requireNonNull(props);
    this.client = Objects.requireNonNull(client);
    this.stateRepo = Objects.requireNonNull(stateRepo);
    this.avatarService = Objects.requireNonNull(avatarService);
    this.accountProfileSynchronizationService = Objects.requireNonNull(accountProfileSynchronizationService);
    this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
  }

  @Transactional
  public String startLink(Usuario u) {
    if (!props.enabled()) {
      throw new IllegalStateException("govbr_disabled");
    }

    String cpf = normalizeCpf(u.getCpf());
    UUID stateId = UUID.randomUUID();

    GovBrPkce.Generated pkce = GovBrPkce.generate();

    Instant now = Instant.now();
    Instant expires = now.plus(props.stateTtl());

    GovBrLinkState st = new GovBrLinkState(stateId, u.getId(), cpf, pkce.codeVerifier(), pkce.nonce(), expires, now);
    stateRepo.save(st);

    return GovBrOidcUrls.authorizeUrl(
        props,
        props.redirectUri(),
        props.effectiveCitizenLinkScope(),
        stateId.toString(),
        pkce.codeChallenge(),
        pkce.nonce()
    );
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

    GovBrLinkState st = stateRepo.findById(stateId).orElse(null);
    if (st == null) {
      return errorRedirect("state_not_found");
    }

    Instant now = Instant.now();
    if (st.isUsed() || st.isExpired(now)) {
      return errorRedirect("state_expired");
    }

    st.markUsed(now);
    stateRepo.save(st);

    GovBrTokenResponse token = client.exchangeCode(code.trim(), st.getCodeVerifier());
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
    String sub = normalizeCpf(verified.claims().getSubject());
    if (sub == null) {
      sub = normalizeCpf(info != null ? info.sub() : null);
    }
    if (!Objects.equals(sub, st.getCpf())) {
      return errorRedirect("cpf_mismatch");
    }

    final String linkedCpf = sub;
    final Instant syncInstant = Instant.now();
    usuarioRepository.findById(st.getUsuarioId()).ifPresent(usuario -> accountProfileSynchronizationService.sincronizar(usuario, linkedCpf, info, syncInstant));

    try {
      byte[] photo = client.userPictureBase64(token.accessToken());
      avatarService.upsert(st.getUsuarioId(), photo, "image/jpeg", "GOVBR");
    } catch (IOException e) {
      String message = e.getMessage();
      boolean pictureNotFound = message != null && message.contains("govbr_picture_http_404");
      if (!pictureNotFound) {
        log.warn("Falha ao sincronizar foto gov.br do usuário {}: {}", st.getUsuarioId(), message);
      }
    }

    return successRedirect();
  }

  private String successRedirect() {
    String s = props.frontendSuccessRedirect();
    if (s == null || s.isBlank()) return "";
    return s;
  }

  private String errorRedirect(String code) {
    String s = props.frontendErrorRedirect();
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
