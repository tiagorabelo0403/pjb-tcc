package com.tcc.pjb.backend.service.security.surface;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.security.context.SecurityContextResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityDeviceResponse;
import com.tcc.pjb.backend.model.dto.security.context.SecurityHatResponse;
import com.tcc.pjb.backend.service.security.context.PjbAuthenticatedSessionFacadeService;
import com.tcc.pjb.backend.model.dto.security.context.SecurityStateResponse;
import com.tcc.pjb.backend.model.entity.MembroEquipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityChallenge;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.entity.security.UserSecurityProfile;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.security.SecurityChallengeRepository;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import com.tcc.pjb.backend.model.repository.security.UserSecurityProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class SecurityContextSurfaceFacadeService {

    private final CurrentUserService currentUserService;
    private final UserSecurityProfileRepository profileRepo;
    private final TrustedDeviceRepository deviceRepo;
    private final SecurityChallengeRepository challengeRepo;
    private final MembroEquipeRepository membroEquipeRepository;
    private final PjbAuthenticatedSessionFacadeService authenticatedSessionFacadeService;

    public SecurityContextSurfaceFacadeService(CurrentUserService currentUserService,
                                               UserSecurityProfileRepository profileRepo,
                                               TrustedDeviceRepository deviceRepo,
                                               SecurityChallengeRepository challengeRepo,
                                               MembroEquipeRepository membroEquipeRepository,
                                               PjbAuthenticatedSessionFacadeService authenticatedSessionFacadeService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.profileRepo = Objects.requireNonNull(profileRepo);
        this.deviceRepo = Objects.requireNonNull(deviceRepo);
        this.challengeRepo = Objects.requireNonNull(challengeRepo);
        this.membroEquipeRepository = Objects.requireNonNull(membroEquipeRepository);
        this.authenticatedSessionFacadeService = Objects.requireNonNull(authenticatedSessionFacadeService);
    }

    public SecurityContextResponse context(HttpServletRequest request) {
        Usuario user = currentUserService.get();
        List<SecurityHatResponse> hats = new ArrayList<>();
        hats.add(new SecurityHatResponse(null, "Atuação Independente", "INDEPENDENTE"));
        List<MembroEquipe> memberships = membroEquipeRepository.carregarComEquipe(user.getId());
        if (memberships != null) {
            memberships.stream()
                    .filter(MembroEquipe::isAtivo)
                    .sorted(Comparator.comparing(SecurityContextSurfaceFacadeService::safeName))
                    .forEach(item -> hats.add(new SecurityHatResponse(
                            item.getEquipe() != null ? item.getEquipe().getId() : null,
                            safeEquipeNome(item),
                            item.getPapel() != null ? item.getPapel().name() : "MEMBRO")));
        }
        UserSecurityProfile profile = profileRepo.findByUserId(user.getId()).orElse(null);
        boolean frozen = profile != null && profile.isFrozenNow();
        LocalDateTime frozenUntil = profile != null ? profile.getFrozenUntil() : null;
        boolean baptized = profile != null && profile.getAdvBaptizedAt() != null;
        LocalDateTime govAt = profile != null ? profile.getGovVerifiedAt() : null;
        boolean govEmailVerified = profile != null && profile.isGovEmailVerified();
        boolean govPhoneVerified = profile != null && profile.isGovPhoneVerified();
        List<TrustedDevice> devices = deviceRepo.findActiveByUser(user.getId());
        List<SecurityDeviceResponse> deviceDtos = new ArrayList<>();
        for (TrustedDevice device : devices) {
            deviceDtos.add(toDto(device));
        }
        Long activeDeviceId = activeDeviceId(request);
        SecurityDeviceResponse activeDevice = null;
        if (activeDeviceId != null) {
            for (SecurityDeviceResponse item : deviceDtos) {
                if (Objects.equals(item.deviceId(), activeDeviceId)) {
                    activeDevice = item;
                    break;
                }
            }
        }
        List<String> pending = pendingSteps(user, frozen, baptized, govAt, deviceDtos, activeDevice);
        SecurityStateResponse security = new SecurityStateResponse(frozen, frozenUntil, baptized, govAt, govEmailVerified, govPhoneVerified, pending, activeDevice, deviceDtos);
        return new SecurityContextResponse(user.getId(), user.getEmail(), tipoOf(user), hats, security, authenticatedSessionFacadeService.atual());
    }

    private SecurityDeviceResponse toDto(TrustedDevice device) {
        if (device == null) {
            return null;
        }
        boolean verified = device.getVerifiedAt() != null;
        boolean trusted = Boolean.TRUE.equals(device.getAttestationTrusted());
        Long pendingId = device.getPendingChallengeId();
        String type = null;
        String hint = null;
        if (pendingId != null) {
            SecurityChallenge challenge = challengeRepo.findByIdSafe(pendingId).orElse(null);
            type = challenge != null && challenge.getTipo() != null ? challenge.getTipo().trim() : null;
            if (type != null) {
                if ("GOVBR_STEPUP".equalsIgnoreCase(type)) {
                    hint = "/api/v1/auth/govbr/stepup/start";
                } else if ("EMAIL_OTP".equalsIgnoreCase(type)) {
                    hint = "/api/v1/security/challenges/" + pendingId + "/verify";
                }
            }
        }
        return new SecurityDeviceResponse(device.getId(), safeAlias(device.getAlias()), verified, trusted, device.getQuarentenaAte(), pendingId, type, hint);
    }

    private static String safeEquipeNome(MembroEquipe member) {
        String value = member != null && member.getEquipe() != null ? member.getEquipe().getNome() : null;
        if (value == null || value.isBlank()) {
            return "Escritório";
        }
        String out = value.trim();
        return out.length() > 60 ? out.substring(0, 60) : out;
    }

    private static String safeName(MembroEquipe member) {
        return member == null ? "" : safeEquipeNome(member);
    }

    private static String tipoOf(Usuario user) {
        return user == null || user.getTipoUsuario() == null ? "UNKNOWN" : user.getTipoUsuario().name();
    }

    private static String safeAlias(String alias) {
        if (alias == null) {
            return null;
        }
        String out = alias.trim();
        if (out.isEmpty()) {
            return null;
        }
        return out.length() > 40 ? out.substring(0, 40) : out;
    }

    private static Long activeDeviceId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute("PJB_DEVICE_ID");
        if (attr instanceof Long value) {
            return value;
        }
        if (attr != null) {
            try {
                return Long.parseLong(String.valueOf(attr));
            } catch (Exception ignored) {
            }
        }
        String header = request.getHeader("X-Device-ID");
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (Exception exception) {
            return null;
        }
    }

    private static List<String> pendingSteps(Usuario user,
                                             boolean frozen,
                                             boolean baptized,
                                             LocalDateTime govVerifiedAt,
                                             List<SecurityDeviceResponse> devices,
                                             SecurityDeviceResponse activeDevice) {
        List<String> steps = new ArrayList<>();
        if (frozen) {
            steps.add("ACCOUNT_FROZEN");
            return steps;
        }
        if (isAdvogado(user) && !baptized) {
            steps.add("ADVOGADO_BAPTISM_REQUIRED");
        }
        if (isAdvogado(user) && govVerifiedAt == null) {
            steps.add("GOVBR_STEPUP_RECOMMENDED");
        }
        if (devices == null || devices.isEmpty()) {
            steps.add("REGISTER_TRUSTED_DEVICE");
            return steps;
        }
        boolean anyVerified = devices.stream().anyMatch(SecurityDeviceResponse::verified);
        if (!anyVerified) {
            steps.add("VERIFY_TRUSTED_DEVICE");
        }
        if (activeDevice == null) {
            steps.add("SELECT_ACTIVE_DEVICE");
        } else if (activeDevice.pendingChallengeId() != null) {
            steps.add("PENDING_SECURITY_CHALLENGE");
        }
        return steps;
    }

    private static boolean isAdvogado(Usuario user) {
        if (user == null || user.getTipoUsuario() == null) {
            return false;
        }
        String tipo = user.getTipoUsuario().name();
        return tipo.contains("ADVOGADO");
    }
}
