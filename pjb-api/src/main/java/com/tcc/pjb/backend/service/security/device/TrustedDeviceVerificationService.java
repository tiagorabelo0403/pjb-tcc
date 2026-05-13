package com.tcc.pjb.backend.service.security.device;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class TrustedDeviceVerificationService {

    private final TrustedDeviceRepository trustedDeviceRepository;

    public TrustedDeviceVerificationService(TrustedDeviceRepository trustedDeviceRepository) {
        this.trustedDeviceRepository = trustedDeviceRepository;
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
