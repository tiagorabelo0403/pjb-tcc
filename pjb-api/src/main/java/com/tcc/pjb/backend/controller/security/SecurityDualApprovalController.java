package com.tcc.pjb.backend.controller.security;

import java.util.Objects;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.device.SecurityDualApprovalService;
import com.tcc.pjb.backend.model.dto.security.DualApprovalResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityDualApprovalRequest;
import com.tcc.pjb.backend.service.security.device.TrustedDeviceVerificationService;

@RestController
@PreAuthorize("isAuthenticated()")
@RequestMapping("/api/v1/security/approvals")
public class SecurityDualApprovalController {

    private final CurrentUserService currentUserService;
    private final SecurityDualApprovalService service;
    private final TrustedDeviceVerificationService deviceVerificationService;

    public SecurityDualApprovalController(CurrentUserService currentUserService,
                                         SecurityDualApprovalService service,
                                         TrustedDeviceVerificationService deviceVerificationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.service = Objects.requireNonNull(service);
        this.deviceVerificationService = Objects.requireNonNull(deviceVerificationService);
    }

    @GetMapping("/{id}")
    public DualApprovalResponse get(@PathVariable Long id) {
        currentUserService.getRequired();
        SecurityDualApprovalRequest r = service.getRequired(id);
        return map(r);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<DualApprovalResponse> approve(@PathVariable Long id, HttpServletRequest request) {
        Usuario u = currentUserService.getRequired();
        enforceStrongAuthAndDevice(u, request);
        SecurityDualApprovalRequest r = service.approve(id, u);
        return ResponseEntity.ok(map(r));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<DualApprovalResponse> reject(@PathVariable Long id, HttpServletRequest request) {
        Usuario u = currentUserService.getRequired();
        enforceStrongAuthAndDevice(u, request);
        SecurityDualApprovalRequest r = service.reject(id, u);
        return ResponseEntity.ok(map(r));
    }

    private void enforceStrongAuthAndDevice(Usuario u, HttpServletRequest request) {
        Object strong = request.getAttribute("PJB_STRONG_AUTH_AT");
        if (strong == null) {
            throw new IllegalStateException("autenticação forte obrigatória");
        }
        Long deviceId = parseLong(request.getHeader("X-Device-ID"));
        if (deviceId == null) {
            Object fromSession = request.getAttribute("PJB_DEVICE_ID");
            deviceId = fromSession instanceof Long l ? l : parseLong(fromSession != null ? String.valueOf(fromSession) : null);
        }
        if (deviceId == null) {
            throw new IllegalArgumentException("device obrigatório");
        }
        deviceVerificationService.requireTrustedVerifiedDevice(u, deviceId);
    }

    private DualApprovalResponse map(SecurityDualApprovalRequest r) {
        return DualApprovalResponse.builder()
                .id(r.getId())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .action(r.getAction())
                .method(r.getMethod())
                .path(r.getPath())
                .ruleId(r.getRuleId())
                .createdAt(r.getCreatedAt())
                .expiresAt(r.getExpiresAt())
                .requesterUserId(r.getRequester() != null ? r.getRequester().getId() : null)
                .requesterDeviceId(r.getRequesterDeviceId())
                .equipeId(r.getEquipeId())
                .approvedByUserId(r.getApprovedBy() != null ? r.getApprovedBy().getId() : null)
                .approvedAt(r.getApprovedAt())
                .rejectedByUserId(r.getRejectedBy() != null ? r.getRejectedBy().getId() : null)
                .rejectedAt(r.getRejectedAt())
                .build();
    }

    private Long parseLong(String v) {
        try {
            if (v == null) return null;
            String s = v.trim();
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Exception e) {
            return null;
        }
    }
}
