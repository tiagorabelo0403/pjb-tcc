package com.tcc.pjb.backend.configs.security;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.util.List;
import java.util.Objects;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;

@Service
public class PasskeyRequirementEnforcer {

    private final TrustedDeviceRepository trustedDeviceRepository;

    @Inject
    public PasskeyRequirementEnforcer(TrustedDeviceRepository trustedDeviceRepository) {
        this.trustedDeviceRepository = Objects.requireNonNull(trustedDeviceRepository);
    }

    public void exigirParaMagistratura(Long usuarioId, TipoUsuario tipo) {
        if (tipo == null || !tipo.requiresHardwareAuthAssurance()) {
            return;
        }
        List<TrustedDevice> devices = trustedDeviceRepository.findActiveByUser(usuarioId);
        boolean temPasskeyForte = devices.stream().anyMatch(d ->
                d.getCredentialId() != null && !d.getCredentialId().isBlank()
                        && "platform".equalsIgnoreCase(d.getAuthenticatorAttachment())
                        && d.isAttestationTrusted()
                        && ("tpm".equalsIgnoreCase(d.getAttestationFmt()) || "apple".equalsIgnoreCase(d.getAttestationFmt())));
        if (!temPasskeyForte) {
            throw new PasskeyRequiredException("passkey_tpm_biometrica_obrigatoria_carreira_juridica_essencial");
        }
    }
}
