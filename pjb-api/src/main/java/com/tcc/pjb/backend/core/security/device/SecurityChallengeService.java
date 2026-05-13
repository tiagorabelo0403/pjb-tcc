package com.tcc.pjb.backend.core.security.device;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityChallenge;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.security.SecurityChallengeRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SecurityChallengeService {

    private final SecurityChallengeRepository repo;
    private final JavaMailSender mailSender;
    private final TrustedDeviceRepository deviceRepo;
    private final DeviceSecurityProperties props;
    private final SecureRandom random = new SecureRandom();

    public SecurityChallengeService(SecurityChallengeRepository repo,
                                    ObjectProvider<JavaMailSender> mailSenderProvider,
                                    TrustedDeviceRepository deviceRepo,
                                    DeviceSecurityProperties props) {
        this.repo = Objects.requireNonNull(repo);
        this.mailSender = mailSenderProvider == null ? null : mailSenderProvider.getIfAvailable();
        this.deviceRepo = Objects.requireNonNull(deviceRepo);
        this.props = Objects.requireNonNull(props);
    }

    @Transactional
    public SecurityChallenge createEmailOtp(Usuario usuario, String ip, String details) {
        Objects.requireNonNull(usuario, "usuario");

        String code = String.format("%06d", random.nextInt(1_000_000));

        SecurityChallenge c = new SecurityChallenge();
        c.setUsuario(usuario);
        c.setTipo("EMAIL_OTP");
        c.setCodeHash(sha256Hex(code));
        c.setIp(safe(ip, 64));
        c.setDetails(safe(details, 1200));
        c.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        c.setAttempts(0);
        c.setLockedUntil(null);
        c.setLastAttemptAt(null);
        repo.save(c);

        sendEmailOtp(usuario.getEmail(), code, c.getId());
        return c;
    }

    @Transactional
    public SecurityChallenge createGovBrStepUpChallenge(Usuario usuario, String ip, String details) {
        Objects.requireNonNull(usuario, "usuario");

        String nonce = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));

        SecurityChallenge c = new SecurityChallenge();
        c.setUsuario(usuario);
        c.setTipo("GOVBR_STEPUP");
        c.setNonce(nonce);
        c.setIp(safe(ip, 64));
        c.setDetails(safe(details, 1200));
        c.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        c.setAttempts(0);
        c.setLockedUntil(null);
        c.setLastAttemptAt(null);
        repo.save(c);
        return c;
    }

    @Transactional
    public SecurityChallenge createBaptismNonce(Usuario usuario, String ip, String details) {
        Objects.requireNonNull(usuario, "usuario");

        String nonce = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes(32));

        SecurityChallenge c = new SecurityChallenge();
        c.setUsuario(usuario);
        c.setTipo("A3_NONCE");
        c.setNonce(nonce);
        c.setIp(safe(ip, 64));
        c.setDetails(safe(details, 1200));
        c.setExpiresAt(LocalDateTime.now().plusMinutes(15));
        repo.save(c);
        return c;
    }

    @Transactional
    public void consumeOtp(Long challengeId, Usuario usuario, String code) {
        SecurityChallenge c = repo.findByIdSafe(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("Challenge não encontrado"));

        if (c.isConsumed()) throw new IllegalStateException("Challenge já consumido");
        if (c.isExpired()) throw new IllegalStateException("Challenge expirado");
        if (c.isLocked()) throw new IllegalStateException("Challenge bloqueado");
        if (c.getUsuario() == null || usuario == null || !Objects.equals(c.getUsuario().getId(), usuario.getId())) {
            throw new IllegalArgumentException("Challenge não pertence ao usuário");
        }
        if (c.getCodeHash() == null) throw new IllegalArgumentException("Challenge não é OTP");

        String norm = normalizeCode(code);
        String expected = c.getCodeHash();
        LocalDateTime now = LocalDateTime.now();

        if (!sha256Hex(norm).equalsIgnoreCase(expected)) {
            int maxAttempts = Math.max(3, props.getOtpMaxAttempts());
            int lockMin = Math.max(1, props.getOtpLockMinutes());

            c.setAttempts(Math.max(0, c.getAttempts()) + 1);
            c.setLastAttemptAt(now);
            if (c.getAttempts() >= maxAttempts) {
                c.setLockedUntil(now.plusMinutes(lockMin));
            }
            repo.save(c);
            throw new IllegalArgumentException("Código inválido");
        }

        c.setConsumedAt(now);
        c.setAttempts(0);
        c.setLockedUntil(null);
        c.setLastAttemptAt(now);
        repo.save(c);

        if (usuario.getId() != null) {
            var devices = deviceRepo.findPendingByUserAndChallengeId(usuario.getId(), challengeId);
            if (!devices.isEmpty()) {
                for (TrustedDevice d : devices) {
                    d.setPendingChallengeId(null);
                    d.setVerifiedAt(now);
                }
                deviceRepo.saveAll(devices);
            }
        }
    }

    @Transactional(readOnly = true)
    public SecurityChallenge getRequired(Long id) {
        return repo.findByIdSafe(id).orElseThrow(() -> new IllegalArgumentException("Challenge não encontrado"));
    }

    @Transactional(readOnly = true)
    public java.util.List<Long> pendingDeviceIdsForChallenge(Long userId, Long challengeId) {
        if (userId == null || challengeId == null) return java.util.List.of();
        return deviceRepo.findPendingByUserAndChallengeId(userId, challengeId).stream().map(TrustedDevice::getId).toList();
    }

    @Transactional
    public void markConsumed(SecurityChallenge c) {
        if (c == null) return;
        c.setConsumedAt(LocalDateTime.now());
        repo.save(c);
    }

    private void sendEmailOtp(String email, String code, Long challengeId) {
        if (email == null || email.isBlank()) return;

        if (mailSender == null) {
            log.warn("[SECURITY_CHALLENGE] EMAIL_OTP challengeId={} email={} smtp=DISABLED", challengeId, email);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("PJB - Confirmação de segurança");
            msg.setText("Seu código de confirmação é: " + code + "\n\n" +
                    "Se você não solicitou este código, ignore e revogue o acesso.");
            mailSender.send(msg);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail OTP: {}", e.getMessage());
        }
    }

    private byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        random.nextBytes(b);
        return b;
    }

    private static String normalizeCode(String code) {
        if (code == null) return "";
        String s = code.trim().replaceAll("[^0-9]", "");
        if (s.length() > 12) s = s.substring(0, 12);
        return s;
    }

    private static String sha256Hex(String v) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(v.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    private static String safe(String v, int max) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) s = s.substring(0, max);
        return s;
    }
}
