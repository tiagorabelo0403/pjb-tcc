package com.tcc.pjb.backend.core.security.webauthn;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.PasskeySession;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;

@Service
public class PasskeySessionService {

    private final PasskeySessionRepository repo;
    private final WebAuthnProperties props;
    private final TermosAceiteService termosAceiteService;
    private final SecureRandom random = new SecureRandom();

    public PasskeySessionService(PasskeySessionRepository repo, WebAuthnProperties props, TermosAceiteService termosAceiteService) {
        this.repo = Objects.requireNonNull(repo);
        this.props = Objects.requireNonNull(props);
        this.termosAceiteService = Objects.requireNonNull(termosAceiteService);
    }

    @Transactional
    public IssuedPasskeySession issue(Usuario usuario, Long deviceId, String ip) {
        boolean termosPendentes = termosAceiteService.precisaAceitar(usuario);
        return issueScoped(usuario, deviceId, ip, null, null, false, ttlMinutesForLogin(), termosPendentes);
    }

    @Transactional
    public IssuedPasskeySession issueStepUp(Usuario usuario, Long deviceId, String ip, String scopeAction, String scopeRequestHash, boolean oneShot) {
        int ttl = Math.max(1, props.getPasskeyStepUpTtlMinutes());
        return issueScoped(usuario, deviceId, ip, scopeAction, scopeRequestHash, oneShot, ttl, false);
    }

    private IssuedPasskeySession issueScoped(Usuario usuario,
                                            Long deviceId,
                                            String ip,
                                            String scopeAction,
                                            String scopeRequestHash,
                                            boolean scopeOneShot,
                                            int ttlMinutes,
                                            boolean termosPendentes) {
        Objects.requireNonNull(usuario, "usuario");

        String token = generateToken();
        String hash = sha256Hex(token);

        PasskeySession s = new PasskeySession();
        s.setUsuario(usuario);
        s.setDeviceId(deviceId);
        s.setTokenHash(hash);
        s.setIp(safe(ip));
        s.setScopeAction(safeScope(scopeAction));
        s.setScopeRequestHash(safeHash(scopeRequestHash));
        s.setScopeOneShot(scopeOneShot);
        s.setTermosPendentes(termosPendentes);
        s.setExpiresAt(LocalDateTime.now().plusMinutes(Math.max(1, ttlMinutes)));
        repo.save(s);

        return new IssuedPasskeySession(token, s.getExpiresAt(), s.getId(), termosPendentes);
    }

    private int ttlMinutesForLogin() {
        return Math.max(5, props.getPasskeySessionTtlMinutes());
    }

    private String generateToken() {
        byte[] b = new byte[32];
        random.nextBytes(b);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    public static String sha256Hex(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String safe(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > 64) s = s.substring(0, 64);
        return s;
    }

    private static String safeScope(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > 40) s = s.substring(0, 40);
        return s;
    }

    private static String safeHash(String v) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() != 64) throw new IllegalArgumentException("scopeRequestHash inválido");
        return s;
    }

    public record IssuedPasskeySession(String token, LocalDateTime expiresAt, Long sessionId, boolean termosPendentes) {}
}
