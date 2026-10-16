package com.tcc.pjb.backend.service.security.device;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class TrustedDeviceVerificationService {

    private final TrustedDeviceRepository trustedDeviceRepository;

    public TrustedDeviceVerificationService(TrustedDeviceRepository trustedDeviceRepository) {
        this.trustedDeviceRepository = trustedDeviceRepository;
    }

    public Long requireStrongAuthOnTrustedDevice(Usuario usuario, HttpServletRequest request) {
        if (request.getAttribute("PJB_STRONG_AUTH_AT") == null) {
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
        requireTrustedVerifiedDevice(usuario, deviceId);
        return deviceId;
    }

    private static Long parseLong(String v) {
        if (v == null || v.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void requireTrustedVerifiedDevice(Usuario usuario, Long deviceId) {
        TrustedDevice device = trustedDeviceRepository.findByIdAndUser(deviceId, usuario.getId()).orElse(null);
        if (device == null || device.isRevogado()) {
            throw new IllegalArgumentException("device inválido");
        }
        if (device.getPendingChallengeId() != null || device.getVerifiedAt() == null) {
            throw new IllegalStateException("device não verificado");
        }
        LocalDateTime quarantineUntil = device.getQuarentenaAte();
        if (quarantineUntil != null && quarantineUntil.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException("device em quarentena");
        }
    }
}
