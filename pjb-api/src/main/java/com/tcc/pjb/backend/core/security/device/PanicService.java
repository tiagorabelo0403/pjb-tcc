package com.tcc.pjb.backend.core.security.device;

import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalStatus;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import com.tcc.pjb.backend.model.repository.security.SecurityChallengeRepository;
import com.tcc.pjb.backend.model.repository.security.SecurityDualApprovalRequestRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import com.tcc.pjb.backend.model.repository.security.WebAuthnSessionRepository;

@Service
public class PanicService {

    private final DeviceSecurityProperties props;
    private final UserSecurityProfileRepository profileRepo;
    private final TrustedDeviceRepository deviceRepo;
    private final PasskeySessionRepository passkeySessionRepository;
    private final WebAuthnSessionRepository webAuthnSessionRepository;
    private final SecurityChallengeRepository securityChallengeRepository;
    private final SecurityDualApprovalRequestRepository dualApprovalRequestRepository;
    private final SecurityAlertService alertService;
    private final AuditLedgerService auditLedgerService;

    public PanicService(DeviceSecurityProperties props,
                        UserSecurityProfileRepository profileRepo,
                        TrustedDeviceRepository deviceRepo,
                        PasskeySessionRepository passkeySessionRepository,
                        WebAuthnSessionRepository webAuthnSessionRepository,
                        SecurityChallengeRepository securityChallengeRepository,
                        SecurityDualApprovalRequestRepository dualApprovalRequestRepository,
                        SecurityAlertService alertService,
                        AuditLedgerService auditLedgerService) {
        this.props = Objects.requireNonNull(props);
        this.profileRepo = Objects.requireNonNull(profileRepo);
        this.deviceRepo = Objects.requireNonNull(deviceRepo);
        this.passkeySessionRepository = Objects.requireNonNull(passkeySessionRepository);
        this.webAuthnSessionRepository = Objects.requireNonNull(webAuthnSessionRepository);
        this.securityChallengeRepository = Objects.requireNonNull(securityChallengeRepository);
        this.dualApprovalRequestRepository = Objects.requireNonNull(dualApprovalRequestRepository);
        this.alertService = Objects.requireNonNull(alertService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional
    public PanicResult trigger(Usuario usuario, Long deviceId, String ip, String reason, Integer freezeMinutes, boolean hard) {
        if (usuario == null || usuario.getId() == null) throw new IllegalArgumentException("usuario obrigatório");

        LocalDateTime now = LocalDateTime.now();
        UserSecurityProfile p = profileRepo.findByUserId(usuario.getId()).orElse(null);
        if (p == null) {
            p = new UserSecurityProfile();
            p.setUsuario(usuario);
        }

        p.setFrozenAt(now);
        LocalDateTime until = null;
        if (!hard) {
            int minutes = freezeMinutes != null ? freezeMinutes : props.getPanicDefaultFreezeMinutes();
            minutes = Math.max(10, Math.min(minutes, 60 * 24 * 30));
            until = now.plusMinutes(minutes);
        }
        p.setFrozenUntil(until);
        p.setFreezeReason(trim(reason, 200));
        profileRepo.save(p);

        deviceRepo.revokeAllByUser(usuario.getId(), now);
        passkeySessionRepository.revokeAllByUser(usuario.getId(), now);
        webAuthnSessionRepository.deleteAllByUser(usuario.getId());
        securityChallengeRepository.deleteAllByUser(usuario.getId());
        dualApprovalRequestRepository.expireAllByRequester(usuario.getId(), SecurityDualApprovalStatus.PENDING, SecurityDualApprovalStatus.EXPIRED, now);

        String r = p.getFreezeReason() != null ? p.getFreezeReason() : "panic";
        alertService.create(usuario, "PANIC_TRIGGERED", "Conta congelada", r, ip, 95);
        auditLedgerService.appendSafely("PANIC_TRIGGERED", "SECURITY", String.valueOf(usuario.getId()), r);

        return new PanicResult(p.getFrozenAt(), p.getFrozenUntil(), p.getFreezeReason(), deviceId);
    }

    @Transactional(readOnly = true)
    public PanicStatus status(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) throw new IllegalArgumentException("usuario obrigatório");
        UserSecurityProfile p = profileRepo.findByUserId(usuario.getId()).orElse(null);
        boolean frozen = p != null && p.isFrozenNow();
        return new PanicStatus(frozen,
                p != null ? p.getFrozenAt() : null,
                p != null ? p.getFrozenUntil() : null,
                p != null ? p.getFreezeReason() : null);
    }

    private static String trim(String v, int max) {
        if (v == null) return null;
        String s = v.trim();
        if (s.isEmpty()) return null;
        if (s.length() > max) s = s.substring(0, max);
        return s;
    }

    public record PanicStatus(boolean frozen, LocalDateTime frozenAt, LocalDateTime frozenUntil, String reason) {}

    public record PanicResult(LocalDateTime frozenAt, LocalDateTime frozenUntil, String reason, Long deviceId) {}
}
