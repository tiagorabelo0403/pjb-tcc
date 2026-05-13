package com.tcc.pjb.backend.service.security.govbr;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcClient;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcUrls;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrPkce;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrTokenResponse;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrUserInfoResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.identity.GovBrStepUpState;
import com.tcc.pjb.backend.model.entity.security.SecurityChallenge;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.GovBrStepUpStateRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.SecurityChallengeRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GovBrStepUpService {

  private static final HexFormat HEX = HexFormat.of();

  private final GovBrOidcProperties props;
  private final GovBrOidcClient client;
  private final GovBrStepUpStateRepository stateRepo;
  private final UsuarioRepository usuarioRepo;
  private final TrustedDeviceRepository deviceRepo;
  private final SecurityChallengeRepository challengeRepo;
  private final AuditLedgerService auditLedger;
  private final GovBrAccountProfileSynchronizationService accountProfileSynchronizationService;

  public GovBrStepUpService(GovBrOidcProperties props,
                           GovBrOidcClient client,
                           GovBrStepUpStateRepository stateRepo,
                           UsuarioRepository usuarioRepo,
                           TrustedDeviceRepository deviceRepo,
                           SecurityChallengeRepository challengeRepo,
                           AuditLedgerService auditLedger,
                           GovBrAccountProfileSynchronizationService accountProfileSynchronizationService) {
    this.props = Objects.requireNonNull(props);
    this.client = Objects.requireNonNull(client);
    this.stateRepo = Objects.requireNonNull(stateRepo);
    this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
    this.deviceRepo = Objects.requireNonNull(deviceRepo);
    this.challengeRepo = Objects.requireNonNull(challengeRepo);
    this.auditLedger = Objects.requireNonNull(auditLedger);
    this.accountProfileSynchronizationService = Objects.requireNonNull(accountProfileSynchronizationService);
  }

  @Transactional
  @PjbTransactionalBudget(operation = "govbr.stepup.start.persist", maxMillis = 1500, critical = true)
  public StartResult start(Usuario u, Long deviceId, String requestIp) {
    requireEnabled();

    Usuario usuario = Objects.requireNonNull(u, "user");
    String cpf = normalizeCpf(usuario.getCpf());
    if (cpf == null || usuario.getId() == null) throw new IllegalArgumentException("cpf");

    UUID stateId = UUID.randomUUID();
    GovBrPkce.Generated pkce = GovBrPkce.generate();

    Instant now = Instant.now();
    Instant expires = now.plus(props.stateTtl());

    String scope = props.effectiveStepUpScope();
    String ip = safeIp(requestIp);

    GovBrStepUpState st = new GovBrStepUpState(
        stateId,
        usuario.getId(),
        cpf,
        deviceId,
        pkce.codeVerifier(),
        pkce.nonce(),
        scope,
        ip,
        expires,
        now
    );
    stateRepo.save(st);

    String authorizeUrl = GovBrOidcUrls.authorizeUrl(
        props,
        props.effectiveStepUpRedirectUri(),
        scope,
        stateId.toString(),
        pkce.codeChallenge(),
        pkce.nonce()
    );
    return new StartResult(authorizeUrl, expires);
  }

  @Transactional
  @PjbTransactionalBudget(operation = "govbr.stepup.callback.persist", maxMillis = 4000, critical = true)
  public String handleCallback(String code, String state) throws IOException, InterruptedException {
    requireEnabled();

    if (code == null || code.isBlank() || state == null || state.isBlank()) {
      return errorRedirect("missing");
    }

    UUID stateId;
    try {
      stateId = UUID.fromString(state.trim());
    } catch (Exception e) {
      return errorRedirect("bad_state");
    }

    GovBrStepUpState st = stateRepo.findById(stateId).orElse(null);
    if (st == null) {
      return errorRedirect("state_not_found");
    }

    Instant now = Instant.now();
    if (st.isUsed() || st.isExpired(now)) {
      return errorRedirect("state_expired");
    }

    st.markUsed(now);
    stateRepo.save(st);

    GovBrTokenResponse token = client.exchangeCode(code.trim(), st.getCodeVerifier(), props.effectiveStepUpRedirectUri());
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

    String sub = normalizeCpf(verified.claims().getSubject());
    if (sub == null) {
      try {
        GovBrUserInfoResponse ui = client.userInfo(token.accessToken());
        sub = normalizeCpf(ui != null ? ui.sub() : null);
      } catch (Exception ignored) {
      }
    }
    if (!Objects.equals(sub, st.getCpf())) {
      return errorRedirect("cpf_mismatch");
    }

    GovBrOidcClient.GovBrAccessTokenSignals accessSignals = client.extractAccessTokenSignals(token.accessToken());
    if (!accessSignals.mfaPresent()) {
      return errorRedirect("mfa_absent");
    }

    GovBrUserInfoResponse info;
    try {
      info = client.userInfo(token.accessToken());
    } catch (Exception ignored) {
      info = null;
    }

    Usuario usuario = usuarioRepo.findById(st.getUsuarioId()).orElse(null);
    if (usuario == null) {
      return errorRedirect("user_not_found");
    }

    accountProfileSynchronizationService.sincronizar(usuario, sub, info, Instant.now());
    recordStepUpEvidence(usuario.getId(), st.getDeviceId(), token.idToken(), verified.kid(), accessSignals);
    tryVerifyPendingDeviceAfterGovBr(st, usuario);

    return successRedirect();
  }

  public record StartResult(String authorizeUrl, Instant expiresAt) {
  }

  private void requireEnabled() {
    if (!props.enabled()) throw new IllegalStateException("govbr_disabled");
  }

  private void tryVerifyPendingDeviceAfterGovBr(GovBrStepUpState st, Usuario usuario) {
    if (st == null || usuario == null) return;
    Long deviceId = st.getDeviceId();
    if (deviceId == null) return;

    TrustedDevice d = deviceRepo.findByIdAndUser(deviceId, usuario.getId()).orElse(null);
    if (d == null || d.isRevogado()) return;

    Long challengeId = d.getPendingChallengeId();
    if (challengeId == null) return;

    SecurityChallenge c = challengeRepo.findByIdSafe(challengeId).orElse(null);
    if (c == null) return;

    String tipo = c.getTipo();
    if (!"GOVBR_STEPUP".equalsIgnoreCase(tipo)) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    c.setConsumedAt(now);
    challengeRepo.save(c);

    d.setPendingChallengeId(null);
    d.setVerifiedAt(now);
    deviceRepo.save(d);
  }

  private void recordStepUpEvidence(Long userId,
                                    Long deviceId,
                                    String idTokenRaw,
                                    String kid,
                                    GovBrOidcClient.GovBrAccessTokenSignals accessSignals) {
    if (idTokenRaw == null || idTokenRaw.isBlank()) return;

    String idTokenHash = sha256Hex(idTokenRaw);
    String safeKid = safeKid(kid);
    String amr = accessSignals == null || accessSignals.amr().isEmpty() ? "-" : String.join(",", accessSignals.amr());
    String preimage = "govbr_stepup|id_token_sha256=" + idTokenHash + "|kid=" + (safeKid != null ? safeKid : "-") + "|amr=" + amr;
    String evidenceHash = sha256Hex(preimage);

    String resourceId = buildResourceId(userId, deviceId, safeKid);
    auditLedger.appendSafely("GOVBR_STEPUP_EVIDENCE", "GOVBR_STEPUP", resourceId, evidenceHash);
  }

  private static String buildResourceId(Long userId, Long deviceId, String kid) {
    StringBuilder sb = new StringBuilder();
    if (userId != null) sb.append("u=").append(userId);
    if (deviceId != null) {
      if (sb.length() > 0) sb.append('|');
      sb.append("d=").append(deviceId);
    }
    if (kid != null && !kid.isBlank()) {
      if (sb.length() > 0) sb.append('|');
      sb.append("kid=").append(kid);
    }
    String s = sb.toString();
    if (s.isBlank()) return null;
    return s.length() <= 120 ? s : s.substring(0, 120);
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
    return s + sep + "e=" + URLEncoder.encode(code == null ? "" : code, StandardCharsets.UTF_8);
  }

  private static String normalizeCpf(String cpf) {
    if (cpf == null) return null;
    String d = cpf.replaceAll("\\D", "");
    return d.length() == 11 ? d : null;
  }

  private static String safeIp(String ip) {
    if (ip == null) return null;
    String s = ip.trim();
    return s.isBlank() ? null : s;
  }

  private static String safeKid(String kid) {
    if (kid == null) return null;
    String s = kid.trim();
    return s.isBlank() ? null : s;
  }

  private static String sha256Hex(String value) {
    if (value == null || value.isBlank()) return null;
    return HEX.formatHex(com.tcc.pjb.backend.core.util.Hashes.sha256(value.getBytes(StandardCharsets.UTF_8)));
  }
}
