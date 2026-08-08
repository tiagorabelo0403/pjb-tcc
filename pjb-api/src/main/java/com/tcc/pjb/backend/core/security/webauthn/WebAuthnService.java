package com.tcc.pjb.backend.core.security.webauthn;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.entity.security.WebAuthnSession;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import com.tcc.pjb.backend.model.repository.security.WebAuthnSessionRepository;
import com.yubico.webauthn.AssertionRequest;
import com.yubico.webauthn.AssertionResult;
import com.yubico.webauthn.FinishAssertionOptions;
import com.yubico.webauthn.FinishRegistrationOptions;
import com.yubico.webauthn.RegistrationResult;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.StartAssertionOptions;
import com.yubico.webauthn.StartRegistrationOptions;
import com.yubico.webauthn.data.AttestationConveyancePreference;
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria;
import com.yubico.webauthn.data.ResidentKeyRequirement;
import com.yubico.webauthn.data.UserIdentity;
import com.yubico.webauthn.data.UserVerificationRequirement;

@Service
public class WebAuthnService {

    private final RelyingParty rp;
    private final WebAuthnProperties props;
    private final WebAuthnSessionRepository sessionRepo;
    private final UsuarioRepository usuarioRepo;
    private final com.tcc.pjb.backend.core.security.device.TrustedDeviceService trustedDeviceService;
    private final TrustedDeviceRepository deviceRepo;
    private final PasskeySessionService passkeySessionService;
    private final UserSecurityProfileRepository profileRepo;
    private final ObjectMapper objectMapper;

    public WebAuthnService(RelyingParty rp,
                          WebAuthnProperties props,
                          WebAuthnSessionRepository sessionRepo,
                          UsuarioRepository usuarioRepo,
                          com.tcc.pjb.backend.core.security.device.TrustedDeviceService trustedDeviceService,
                          TrustedDeviceRepository deviceRepo,
                          PasskeySessionService passkeySessionService,
                          UserSecurityProfileRepository profileRepo,
                          ObjectMapper objectMapper) {
        this.rp = Objects.requireNonNull(rp);
        this.props = Objects.requireNonNull(props);
        this.sessionRepo = Objects.requireNonNull(sessionRepo);
        this.usuarioRepo = Objects.requireNonNull(usuarioRepo);
        this.trustedDeviceService = Objects.requireNonNull(trustedDeviceService);
        this.deviceRepo = Objects.requireNonNull(deviceRepo);
        this.passkeySessionService = Objects.requireNonNull(passkeySessionService);
        this.profileRepo = Objects.requireNonNull(profileRepo);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public StartResponse startEnrollment(Usuario usuario) {
        Objects.requireNonNull(usuario, "usuario");
        boolean magistratura = usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura();

        UserIdentity userIdentity = UserIdentity.builder()
                .name(usuario.getEmail())
                .displayName(usuario.getNome())
                .id(WebAuthnCredentialRepository.userHandleFor(usuario.getId()))
                .build();

        var selBuilder = AuthenticatorSelectionCriteria.builder()
                .userVerification(magistratura || props.isRequireUserVerification()
                        ? UserVerificationRequirement.REQUIRED
                        : UserVerificationRequirement.PREFERRED)
                .residentKey(props.isPreferResidentKey() ? ResidentKeyRequirement.PREFERRED : ResidentKeyRequirement.DISCOURAGED);
        if (magistratura) {
            selBuilder.authenticatorAttachment(com.yubico.webauthn.data.AuthenticatorAttachment.PLATFORM);
        }
        AuthenticatorSelectionCriteria sel = selBuilder.build();

        AttestationConveyancePreference att = props.isRequireAttestationOnEnroll()
                ? AttestationConveyancePreference.DIRECT
                : AttestationConveyancePreference.NONE;

        var creationOptions = rp.startRegistration(
                StartRegistrationOptions.builder()
                        .user(userIdentity)
                        .authenticatorSelection(sel)
                        .build()
        );

        String creationOptionsJson = safeJson(creationOptions::toJson, "registrationOptionsJson");

        WebAuthnSession s = new WebAuthnSession();
        s.setUsuario(usuario);
        s.setTipo("REGISTRATION");
        s.setRequestJson(creationOptionsJson);
        s.setExpiresAt(LocalDateTime.now().plusMinutes(Math.max(2, props.getSessionTtlMinutes())));
        sessionRepo.save(s);

        return new StartResponse(s.getId(), creationOptionsJson);
    }

    @Transactional
    public EnrollFinishResult finishEnrollment(Usuario usuario, Long sessionId, String credentialJson, String alias, String ip) {
        Objects.requireNonNull(usuario, "usuario");
        if (sessionId == null) throw new IllegalArgumentException("sessionId obrigatório");

        WebAuthnSession s = sessionRepo.findByIdSafe(sessionId).orElseThrow(() -> new IllegalArgumentException("Sessão WebAuthn não encontrada"));
        if (s.getUsuario() == null || !Objects.equals(s.getUsuario().getId(), usuario.getId())) {
            throw new IllegalArgumentException("Sessão não pertence ao usuário");
        }
        if (s.isExpired()) throw new IllegalStateException("Sessão WebAuthn expirada");
        if (!"REGISTRATION".equalsIgnoreCase(s.getTipo())) throw new IllegalArgumentException("Sessão inválida");

        try {
            var request = parseRegistrationRequest(s.getRequestJson());
            var response = parseRegistrationResponse(credentialJson);

            RegistrationResult result = finishRegistration(request, response);

            AttestationInfo att = parseAttestationInfo(credentialJson);
            String fmt = att.fmt();
            String aaguid = att.aaguid();
            String attachment = att.authenticatorAttachment();

            boolean magistratura = usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura();

            if (props.isRequireAttestationOnEnroll()) {
                requireAttestationAllowed(fmt);
            }

            String credIdB64 = result.getKeyId().getId().getBase64Url();
            String pkCoseB64 = result.getPublicKeyCose().getBase64Url();
            long signCount = result.getSignatureCount();

            boolean trusted = isTrustedByPolicy(fmt, aaguid);

            if (props.isRequireAttestationOnEnroll() && !trusted) {
                throw new IllegalStateException("Attestation não confiável para cadastro");
            }

            validarRequisitosMagistratura(magistratura, attachment, fmt);

            TrustedDevice d = trustedDeviceService.registerWebAuthn(
                    usuario,
                    credIdB64,
                    pkCoseB64,
                    alias,
                    aaguid,
                    fmt,
                    trusted,
                    attachment,
                    signCount,
                    ip
            );

            return new EnrollFinishResult(d.getId(), d.getPendingChallengeId());

        } finally {
            sessionRepo.deleteById(sessionId);
        }
    }

    @Transactional
    public StartResponse startPasskeyLogin(String email) {
        String user = (email == null) ? null : email.trim().toLowerCase(Locale.ROOT);
        if (user == null || user.isBlank()) throw new IllegalArgumentException("email obrigatório");

        Usuario u = usuarioRepo.findByEmail(user).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        boolean magistratura = u.getTipoUsuario() != null && u.getTipoUsuario().isMagistratura();

        AssertionRequest req = rp.startAssertion(
                StartAssertionOptions.builder()
                        .username(user)
                        .userVerification(magistratura || props.isRequireUserVerification()
                                ? UserVerificationRequirement.REQUIRED
                                : UserVerificationRequirement.PREFERRED)
                        .build()
        );

        String assertionRequestJson = safeJson(req::toJson, "assertionRequestJson");

        WebAuthnSession s = new WebAuthnSession();
        s.setUsuario(u);
        s.setTipo("ASSERTION");
        s.setRequestJson(assertionRequestJson);
        s.setExpiresAt(LocalDateTime.now().plusMinutes(Math.max(2, props.getSessionTtlMinutes())));
        sessionRepo.save(s);

        return new StartResponse(s.getId(), assertionRequestJson);
    }

    @Transactional
    public PasskeyLoginResult finishPasskeyLogin(Long sessionId, String credentialJson, String ip) {
        if (sessionId == null) throw new IllegalArgumentException("sessionId obrigatório");

        WebAuthnSession s = sessionRepo.findByIdSafe(sessionId).orElseThrow(() -> new IllegalArgumentException("Sessão WebAuthn não encontrada"));
        if (s.isExpired()) throw new IllegalStateException("Sessão WebAuthn expirada");
        if (!"ASSERTION".equalsIgnoreCase(s.getTipo())) throw new IllegalArgumentException("Sessão inválida");
        Usuario u = s.getUsuario();
        if (u == null) throw new IllegalStateException("Sessão sem usuário");

        try {
            AssertionRequest req = parseAssertionRequest(s.getRequestJson());
            var response = parseAssertionResponse(credentialJson);

            AssertionResult result = finishAssertion(req, response);

            if (!result.isSuccess()) {
                throw new IllegalArgumentException("Passkey inválida");
            }

            String credId = result.getCredential().getCredentialId().getBase64Url();
            TrustedDevice d = deviceRepo.findByCredentialId(credId).orElse(null);
            Long deviceId = null;
            if (d != null && d.getUsuario() != null && Objects.equals(d.getUsuario().getId(), u.getId())) {
                d.setSignCount(Math.max(d.getSignCount(), result.getSignatureCount()));
                d.setUltimoUsoEm(LocalDateTime.now());
                d.setUserVerified(result.isUserVerified());
                deviceRepo.save(d);
                deviceId = d.getId();
            }

            var issued = passkeySessionService.issue(u, deviceId, ip);

            UserSecurityProfile p = profileRepo.findByUserId(u.getId()).orElse(null);
            if (p == null) {
                p = new UserSecurityProfile();
                p.setUsuario(u);
            }
            p.setLastStrongAuthAt(LocalDateTime.now());
            profileRepo.save(p);

            return new PasskeyLoginResult(issued.token(), issued.expiresAt(), deviceId);
        } finally {
            sessionRepo.deleteById(sessionId);
        }
    }

    @Transactional
    public StartResponse startStepUp(Usuario usuario, String action, String requestHash) {
        Objects.requireNonNull(usuario, "usuario");
        String a = normalizeAction(action);
        String rh = normalizeReqHash(requestHash);
        boolean magistratura = usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isMagistratura();

        AssertionRequest req = rp.startAssertion(
                StartAssertionOptions.builder()
                        .username(usuario.getEmail())
                        .userVerification(magistratura || props.isRequireUserVerification()
                                ? UserVerificationRequirement.REQUIRED
                                : UserVerificationRequirement.PREFERRED)
                        .build()
        );

        String assertionRequestJson = safeJson(req::toJson, "assertionRequestJson");

        WebAuthnSession s = new WebAuthnSession();
        s.setUsuario(usuario);
        s.setTipo("STEPUP_ASSERTION");
        s.setRequestJson(wrapStepUpRequest(assertionRequestJson, a, rh));
        s.setExpiresAt(LocalDateTime.now().plusMinutes(Math.max(1, props.getSessionTtlMinutes())));
        sessionRepo.save(s);

        return new StartResponse(s.getId(), assertionRequestJson);
    }

    @Transactional
    public PasskeyLoginResult finishStepUp(Usuario usuario, Long sessionId, String credentialJson, String ip) {
        Objects.requireNonNull(usuario, "usuario");
        if (sessionId == null) throw new IllegalArgumentException("sessionId obrigatório");

        WebAuthnSession s = sessionRepo.findByIdSafe(sessionId).orElseThrow(() -> new IllegalArgumentException("Sessão WebAuthn não encontrada"));
        if (s.isExpired()) throw new IllegalStateException("Sessão WebAuthn expirada");
        if (!"STEPUP_ASSERTION".equalsIgnoreCase(s.getTipo())) throw new IllegalArgumentException("Sessão inválida");
        if (s.getUsuario() == null || !Objects.equals(s.getUsuario().getId(), usuario.getId())) throw new IllegalArgumentException("Sessão não pertence ao usuário");

        StepUpWrapped w = parseStepUpWrapped(s.getRequestJson());

        try {
            AssertionRequest req = parseAssertionRequest(w.assertionRequestJson());
            var response = parseAssertionResponse(credentialJson);

            AssertionResult result = finishAssertion(req, response);

            if (!result.isSuccess()) throw new IllegalArgumentException("Passkey inválida");

            String credId = result.getCredential().getCredentialId().getBase64Url();
            TrustedDevice d = deviceRepo.findByCredentialId(credId).orElse(null);
            Long deviceId = null;
            if (d != null && d.getUsuario() != null && Objects.equals(d.getUsuario().getId(), usuario.getId())) {
                d.setSignCount(Math.max(d.getSignCount(), result.getSignatureCount()));
                d.setUltimoUsoEm(LocalDateTime.now());
                d.setUserVerified(result.isUserVerified());
                deviceRepo.save(d);
                deviceId = d.getId();
            }

            if (deviceId == null) throw new IllegalStateException("Device não associado ao usuário");

            var issued = passkeySessionService.issueStepUp(usuario, deviceId, ip, w.scopeAction(), w.scopeRequestHash(), true);

            UserSecurityProfile p = profileRepo.findByUserId(usuario.getId()).orElse(null);
            if (p == null) {
                p = new UserSecurityProfile();
                p.setUsuario(usuario);
            }
            p.setLastStrongAuthAt(LocalDateTime.now());
            profileRepo.save(p);

            return new PasskeyLoginResult(issued.token(), issued.expiresAt(), deviceId);
        } finally {
            sessionRepo.deleteById(sessionId);
        }
    }

    private com.yubico.webauthn.data.PublicKeyCredentialCreationOptions parseRegistrationRequest(String requestJson) {
        try {
            return Objects.requireNonNull(
                    com.yubico.webauthn.data.PublicKeyCredentialCreationOptions.fromJson(Objects.requireNonNull(requestJson, "requestJson")),
                    "registrationRequest"
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Requisição WebAuthn de cadastro inválida", e);
        }
    }

    private com.yubico.webauthn.data.PublicKeyCredential<com.yubico.webauthn.data.AuthenticatorAttestationResponse, com.yubico.webauthn.data.ClientRegistrationExtensionOutputs> parseRegistrationResponse(String credentialJson) {
        try {
            return Objects.requireNonNull(
                    com.yubico.webauthn.data.PublicKeyCredential.parseRegistrationResponseJson(Objects.requireNonNull(credentialJson, "credentialJson")),
                    "registrationResponse"
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Resposta WebAuthn de cadastro inválida", e);
        }
    }

    private AssertionRequest parseAssertionRequest(String requestJson) {
        try {
            return Objects.requireNonNull(AssertionRequest.fromJson(Objects.requireNonNull(requestJson, "requestJson")), "assertionRequest");
        } catch (Exception e) {
            throw new IllegalArgumentException("Requisição WebAuthn de autenticação inválida", e);
        }
    }

    private com.yubico.webauthn.data.PublicKeyCredential<com.yubico.webauthn.data.AuthenticatorAssertionResponse, com.yubico.webauthn.data.ClientAssertionExtensionOutputs> parseAssertionResponse(String credentialJson) {
        try {
            return Objects.requireNonNull(
                    com.yubico.webauthn.data.PublicKeyCredential.parseAssertionResponseJson(Objects.requireNonNull(credentialJson, "credentialJson")),
                    "assertionResponse"
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Resposta WebAuthn de autenticação inválida", e);
        }
    }

    private RegistrationResult finishRegistration(
            com.yubico.webauthn.data.PublicKeyCredentialCreationOptions request,
            com.yubico.webauthn.data.PublicKeyCredential<com.yubico.webauthn.data.AuthenticatorAttestationResponse, com.yubico.webauthn.data.ClientRegistrationExtensionOutputs> response) {
        try {
            return rp.finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(request)
                            .response(response)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha na finalização do cadastro WebAuthn", e);
        }
    }

    private AssertionResult finishAssertion(
            AssertionRequest request,
            com.yubico.webauthn.data.PublicKeyCredential<com.yubico.webauthn.data.AuthenticatorAssertionResponse, com.yubico.webauthn.data.ClientAssertionExtensionOutputs> response) {
        try {
            return rp.finishAssertion(
                    FinishAssertionOptions.builder()
                            .request(request)
                            .response(response)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha na validação da credencial WebAuthn", e);
        }
    }

    @FunctionalInterface
    private interface CheckedJsonSupplier {
        String get() throws Exception;
    }

    private String safeJson(CheckedJsonSupplier supplier, String label) {
        try {
            return Objects.requireNonNull(supplier.get(), label);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar payload WebAuthn", e);
        }
    }

    private String wrapStepUpRequest(String assertionRequestJson, String action, String requestHash) {
        try {
            var n = objectMapper.createObjectNode();
            n.put("assertionRequestJson", assertionRequestJson);
            n.put("scopeAction", action);
            n.put("scopeRequestHash", requestHash);
            return objectMapper.writeValueAsString(n);
        } catch (Exception e) {
            throw new IllegalStateException("falha ao serializar sessão", e);
        }
    }

    private StepUpWrapped parseStepUpWrapped(String requestJson) {
        try {
            JsonNode root = objectMapper.readTree(requestJson);
            String ar = root.hasNonNull("assertionRequestJson") ? root.get("assertionRequestJson").asText() : null;
            String a = root.hasNonNull("scopeAction") ? root.get("scopeAction").asText() : null;
            String rh = root.hasNonNull("scopeRequestHash") ? root.get("scopeRequestHash").asText() : null;
            if (ar == null || ar.isBlank()) throw new IllegalArgumentException("Sessão inválida");
            if (a == null || a.isBlank()) throw new IllegalArgumentException("Sessão inválida");
            if (rh == null || rh.isBlank()) throw new IllegalArgumentException("Sessão inválida");
            return new StepUpWrapped(ar, normalizeAction(a), normalizeReqHash(rh));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Sessão inválida");
        }
    }

    private static String normalizeAction(String action) {
        if (action == null) throw new IllegalArgumentException("action obrigatório");
        String s = action.trim().toUpperCase(Locale.ROOT);
        if (s.isEmpty() || s.length() > 40) throw new IllegalArgumentException("action inválido");
        return s;
    }

    private static String normalizeReqHash(String requestHash) {
        if (requestHash == null) throw new IllegalArgumentException("requestHash obrigatório");
        String s = requestHash.trim();
        if (s.length() != 64) throw new IllegalArgumentException("requestHash inválido");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.digit(c, 16) < 0) throw new IllegalArgumentException("requestHash inválido");
        }
        return s.toLowerCase(Locale.ROOT);
    }

    private record StepUpWrapped(String assertionRequestJson, String scopeAction, String scopeRequestHash) {}

    static void validarRequisitosMagistratura(boolean magistratura, String authenticatorAttachment, String attestationFmt) {
        if (!magistratura) {
            return;
        }
        if (!"platform".equalsIgnoreCase(authenticatorAttachment)) {
            throw new IllegalStateException("Magistratura exige passkey embutida no dispositivo (platform authenticator).");
        }
        if (!"tpm".equalsIgnoreCase(attestationFmt) && !"apple".equalsIgnoreCase(attestationFmt)) {
            throw new IllegalStateException("Magistratura exige attestation TPM (Windows Hello) ou Apple (Touch/Face ID).");
        }
    }

    private void requireAttestationAllowed(String fmt) {
        if (props.getAllowedAttestationFormats() == null || props.getAllowedAttestationFormats().isEmpty()) return;
        String f = fmt == null ? "" : fmt.trim().toLowerCase(Locale.ROOT);
        boolean ok = props.getAllowedAttestationFormats().stream()
                .filter(v -> v != null && !v.isBlank())
                .anyMatch(v -> v.trim().equalsIgnoreCase(f));
        if (!ok) {
            throw new IllegalStateException("Attestation fmt não permitido: " + f);
        }
    }

    private boolean isTrustedByPolicy(String fmt, String aaguid) {
        String f = fmt == null ? "" : fmt.trim().toLowerCase(Locale.ROOT);

        if (props.getAllowedAttestationFormats() != null && !props.getAllowedAttestationFormats().isEmpty()) {
            boolean ok = props.getAllowedAttestationFormats().stream()
                    .filter(v -> v != null && !v.isBlank())
                    .anyMatch(v -> v.trim().equalsIgnoreCase(f));
            if (!ok) return false;
        }
        if (props.getAllowedAaguids() != null && !props.getAllowedAaguids().isEmpty()) {
            if (aaguid == null) return false;
            return props.getAllowedAaguids().stream().anyMatch(v -> v != null && v.equalsIgnoreCase(aaguid));
        }
        return true;
    }

    private AttestationInfo parseAttestationInfo(String credentialJson) {
        String authenticatorAttachment;
        try {
            JsonNode root = objectMapper.readTree(credentialJson);
            authenticatorAttachment = text(root, "authenticatorAttachment");

            String attObjB64 = text(root, "response", "attestationObject");
            if (attObjB64 == null || attObjB64.isBlank()) {
                return new AttestationInfo("unknown", null, authenticatorAttachment);
            }

            byte[] attObj = base64UrlDecode(attObjB64);
            CborTopLevel top = CborLite.parseAttestationObject(attObj);

            String fmt = top.fmt != null ? top.fmt : "unknown";
            String aaguid = null;
            if (top.authData != null) {
                aaguid = extractAaguid(top.authData);
            }

            return new AttestationInfo(fmt, aaguid, authenticatorAttachment);
        } catch (Exception e) {
            return new AttestationInfo("unknown", null, null);
        }
    }

    private static String extractAaguid(byte[] authData) {
        try {
            if (authData.length < 37) return null;
            int flags = authData[32] & 0xFF;
            boolean hasAttestedCredData = (flags & 0x40) != 0;
            if (!hasAttestedCredData) return null;

            int offset = 37;
            if (authData.length < offset + 16) return null;
            byte[] a = new byte[16];
            System.arraycopy(authData, offset, a, 0, 16);
            return toUuidString(a);
        } catch (Exception e) {
            return null;
        }
    }

    private static String toUuidString(byte[] b16) {
        if (b16 == null || b16.length != 16) return null;
        ByteBuffer bb = ByteBuffer.wrap(b16);
        long high = bb.getLong();
        long low = bb.getLong();
        return new java.util.UUID(high, low).toString();
    }

    private static String text(JsonNode root, String... path) {
        JsonNode n = root;
        for (String p : path) {
            if (n == null) return null;
            n = n.get(p);
        }
        if (n == null || n.isNull()) return null;
        String v = n.asText();
        return v == null ? null : v.trim();
    }

    private static byte[] base64UrlDecode(String s) {
        String v = s.trim();
        int mod = v.length() % 4;
        if (mod == 2) v = v + "==";
        else if (mod == 3) v = v + "=";
        else if (mod == 1) throw new IllegalArgumentException("Base64Url inválido");
        return java.util.Base64.getUrlDecoder().decode(v);
    }

    public record StartResponse(Long sessionId, String optionsJson) {}

    public record EnrollFinishResult(Long deviceId, Long pendingChallengeId) {}

    public record PasskeyLoginResult(String token, LocalDateTime expiresAt, Long deviceId) {}

    public record AttestationInfo(String fmt, String aaguid, String authenticatorAttachment) {}

    private static final class CborTopLevel {
        private final String fmt;
        private final byte[] authData;

        private CborTopLevel(String fmt, byte[] authData) {
            this.fmt = fmt;
            this.authData = authData;
        }
    }

    private static final class CborLite {

        private static CborTopLevel parseAttestationObject(byte[] cbor) {
            Reader r = new Reader(cbor);
            int major = r.peekMajorType();
            if (major != 5) throw new IllegalArgumentException("attestationObject inválido");
            long mapLen = r.readMapLength();
            String fmt = null;
            byte[] authData = null;

            long pairs = mapLen >= 0 ? mapLen : Long.MAX_VALUE;
            for (long i = 0; i < pairs; i++) {
                if (mapLen < 0 && r.isBreak()) {
                    r.readBreak();
                    break;
                }

                String key = r.readTextString();
                if ("fmt".equals(key)) {
                    fmt = r.readTextString();
                } else if ("authData".equals(key)) {
                    authData = r.readByteString();
                } else {
                    r.skipValue();
                }
            }

            return new CborTopLevel(fmt, authData);
        }

        private static final class Reader {
            private final byte[] b;
            private int p;

            private Reader(byte[] b) {
                this.b = b == null ? new byte[0] : b;
                this.p = 0;
            }

            private int readU8() {
                if (p >= b.length) throw new IllegalArgumentException("CBOR truncado");
                return b[p++] & 0xFF;
            }

            private int peekU8() {
                if (p >= b.length) throw new IllegalArgumentException("CBOR truncado");
                return b[p] & 0xFF;
            }

            private int peekMajorType() {
                return (peekU8() >> 5) & 0x7;
            }

            private boolean isBreak() {
                return peekU8() == 0xFF;
            }

            private void readBreak() {
                int x = readU8();
                if (x != 0xFF) throw new IllegalArgumentException("CBOR break inválido");
            }

            private long readLength(int ai) {
                if (ai < 24) return ai;
                if (ai == 24) return readU8();
                if (ai == 25) return readUIntN(2);
                if (ai == 26) return readUIntN(4);
                if (ai == 27) return readUIntN(8);
                if (ai == 31) return -1;
                throw new IllegalArgumentException("CBOR length ai inválido");
            }

            private long readUIntN(int n) {
                long v = 0;
                for (int i = 0; i < n; i++) {
                    v = (v << 8) | (readU8() & 0xFF);
                }
                return v;
            }

            private long readMapLength() {
                int ib = readU8();
                int major = (ib >> 5) & 0x7;
                int ai = ib & 0x1F;
                if (major != 5) throw new IllegalArgumentException("CBOR não é map");
                return readLength(ai);
            }

            private String readTextString() {
                int ib = readU8();
                int major = (ib >> 5) & 0x7;
                int ai = ib & 0x1F;
                if (major != 3) throw new IllegalArgumentException("CBOR não é text string");
                long len = readLength(ai);
                if (len < 0) throw new IllegalArgumentException("text string indefinida não suportada");
                if (len > Integer.MAX_VALUE) throw new IllegalArgumentException("text string grande");
                if (p + (int) len > b.length) throw new IllegalArgumentException("CBOR truncado");
                String s = new String(b, p, (int) len, java.nio.charset.StandardCharsets.UTF_8);
                p += (int) len;
                return s;
            }

            private byte[] readByteString() {
                int ib = readU8();
                int major = (ib >> 5) & 0x7;
                int ai = ib & 0x1F;
                if (major != 2) throw new IllegalArgumentException("CBOR não é byte string");
                long len = readLength(ai);
                if (len < 0) throw new IllegalArgumentException("byte string indefinida não suportada");
                if (len > Integer.MAX_VALUE) throw new IllegalArgumentException("byte string grande");
                if (p + (int) len > b.length) throw new IllegalArgumentException("CBOR truncado");
                byte[] out = new byte[(int) len];
                System.arraycopy(b, p, out, 0, (int) len);
                p += (int) len;
                return out;
            }

            private void skipValue() {
                int ib = readU8();
                int major = (ib >> 5) & 0x7;
                int ai = ib & 0x1F;

                switch (major) {
                    case 0:
                    case 1:
                        readLength(ai);
                        return;
                    case 2: {
                        long len = readLength(ai);
                        if (len < 0) throw new IllegalArgumentException("skip indefinido não suportado");
                        if (p + (int) len > b.length) throw new IllegalArgumentException("CBOR truncado");
                        p += (int) len;
                        return;
                    }
                    case 3: {
                        long len = readLength(ai);
                        if (len < 0) throw new IllegalArgumentException("skip indefinido não suportado");
                        if (p + (int) len > b.length) throw new IllegalArgumentException("CBOR truncado");
                        p += (int) len;
                        return;
                    }
                    case 4: {
                        long len = readLength(ai);
                        if (len < 0) {
                            while (!isBreak()) {
                                skipValue();
                            }
                            readBreak();
                            return;
                        }
                        for (int i = 0; i < len; i++) skipValue();
                        return;
                    }
                    case 5: {
                        long len = readLength(ai);
                        if (len < 0) {
                            while (!isBreak()) {
                                skipValue();
                                skipValue();
                            }
                            readBreak();
                            return;
                        }
                        for (int i = 0; i < len; i++) {
                            skipValue();
                            skipValue();
                        }
                        return;
                    }
                    case 6:
                        readLength(ai);
                        skipValue();
                        return;
                    case 7:
                        if (ai < 20) return;
                        if (ai == 20 || ai == 21 || ai == 22 || ai == 23) return;
                        if (ai == 24) {
                            readU8();
                            return;
                        }
                        if (ai == 25) {
                            readUIntN(2);
                            return;
                        }
                        if (ai == 26) {
                            readUIntN(4);
                            return;
                        }
                        if (ai == 27) {
                            readUIntN(8);
                            return;
                        }
                        if (ai == 31) throw new IllegalArgumentException("break fora de contexto");
                        throw new IllegalArgumentException("CBOR simple/float inválido");
                    default:
                        throw new IllegalArgumentException("CBOR major inválido");
                }
            }
        }
    }
}
