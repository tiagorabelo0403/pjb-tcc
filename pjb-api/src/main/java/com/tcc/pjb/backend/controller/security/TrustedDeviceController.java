package com.tcc.pjb.backend.controller.security;

import java.util.List;
import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.tcc.pjb.backend.configs.security.perimeter.ClientIpResolver;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.TrustedDeviceService;
import com.tcc.pjb.backend.model.dto.security.DeviceRegisterRequest;
import com.tcc.pjb.backend.model.dto.security.TrustedDeviceResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.entity.security.SecurityChallenge;
import com.tcc.pjb.backend.service.security.device.SecurityChallengeLookupService;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/v1/security/devices")
public class TrustedDeviceController {

    private final CurrentUserService currentUserService;
    private final TrustedDeviceService deviceService;
    private final ClientIpResolver ipResolver;
    private final SecurityChallengeLookupService challengeLookupService;

    public TrustedDeviceController(CurrentUserService currentUserService,
                                   TrustedDeviceService deviceService,
                                   ClientIpResolver ipResolver,
                                   SecurityChallengeLookupService challengeLookupService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.deviceService = Objects.requireNonNull(deviceService);
        this.ipResolver = Objects.requireNonNull(ipResolver);
        this.challengeLookupService = Objects.requireNonNull(challengeLookupService);
    }

    @GetMapping
    public List<TrustedDeviceResponse> myDevices() {
        Usuario u = currentUserService.getRequired();
        return deviceService.listActiveDevices(u.getId()).stream().map(this::map).toList();
    }

    @PostMapping
    public ResponseEntity<TrustedDeviceResponse> register(@Valid @RequestBody DeviceRegisterRequest req,
                                                         HttpServletRequest request) {
        Usuario u = currentUserService.getRequired();
        String ip = ipResolver.resolve(request);
        TrustedDevice d = deviceService.register(
                u,
                req.getCredentialId(),
                req.getPublicKey(),
                req.getAlias(),
                req.getAaguid(),
                req.getAttestationFmt(),
                req.isAttestationTrusted(),
                ip
        );
        return ResponseEntity.ok(map(d));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> revoke(@PathVariable Long deviceId,
                                       @RequestParam(value = "reason", required = false) String reason) {
        Usuario u = currentUserService.getRequired();
        deviceService.revoke(u, deviceId, reason);
        return ResponseEntity.noContent().build();
    }

    private TrustedDeviceResponse map(TrustedDevice d) {
        boolean verified = d.getVerifiedAt() != null;
        Long pendingId = d.getPendingChallengeId();
        String type = null;
        String hint = null;
        if (pendingId != null) {
            SecurityChallenge c = challengeLookupService.findSafe(pendingId).orElse(null);
            type = c != null && c.getTipo() != null ? c.getTipo().trim() : null;
            if (type != null) {
                if ("GOVBR_STEPUP".equalsIgnoreCase(type)) {
                    hint = "/api/v1/auth/govbr/stepup/start";
                } else if ("EMAIL_OTP".equalsIgnoreCase(type)) {
                    hint = "/api/v1/security/challenges/" + pendingId + "/verify";
                }
            }
        }
        return TrustedDeviceResponse.builder()
                .id(d.getId())
                .alias(d.getAlias())
                .verified(verified)
                .attestationTrusted(d.isAttestationTrusted())
                .riskScoreEnroll(d.getRiskScoreEnroll())
                .enrollSuspectNetwork(d.isEnrollSuspectNetwork())
                .quarentenaAte(d.getQuarentenaAte())
                .criadoEm(d.getCriadoEm())
                .ultimoUsoEm(d.getUltimoUsoEm())
                .pendingChallengeId(pendingId)
                .pendingChallengeType(type)
                .pendingChallengeHint(hint)
                .build();
    }
}
