package com.tcc.pjb.backend.configs.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.TrustedDevice;
import com.tcc.pjb.backend.model.repository.security.TrustedDeviceRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PasskeyRequirementEnforcerTest {

    @Mock
    TrustedDeviceRepository trustedDeviceRepository;

    PasskeyRequirementEnforcer enforcer() {
        return new PasskeyRequirementEnforcer(trustedDeviceRepository);
    }

    @Test
    void magistradoSemPasskeyRegistrada_lancaPasskeyRequiredException() {
        when(trustedDeviceRepository.findActiveByUser(1L)).thenReturn(List.of());
        assertThatThrownBy(() -> enforcer().exigirParaMagistratura(1L, TipoUsuario.MAGISTRADO))
                .isInstanceOf(PasskeyRequiredException.class);
    }

    @Test
    void magistradoComPasskeyFracaCrossPlatformOuSemAttestationConfiavel_lancaPasskeyRequiredException() {
        TrustedDevice device = dispositivo("cross-platform", false, "packed");
        when(trustedDeviceRepository.findActiveByUser(1L)).thenReturn(List.of(device));
        assertThatThrownBy(() -> enforcer().exigirParaMagistratura(1L, TipoUsuario.MAGISTRADO))
                .isInstanceOf(PasskeyRequiredException.class);
    }

    @Test
    void magistradoComPasskeyPlatformTpmConfiavel_permite() {
        TrustedDevice device = dispositivo("platform", true, "tpm");
        when(trustedDeviceRepository.findActiveByUser(1L)).thenReturn(List.of(device));
        assertThatCode(() -> enforcer().exigirParaMagistratura(1L, TipoUsuario.MAGISTRADO))
                .doesNotThrowAnyException();
    }

    @Test
    void magistradoComPasskeyPlatformAppleConfiavel_permite() {
        TrustedDevice device = dispositivo("platform", true, "apple");
        when(trustedDeviceRepository.findActiveByUser(1L)).thenReturn(List.of(device));
        assertThatCode(() -> enforcer().exigirParaMagistratura(1L, TipoUsuario.MAGISTRADO))
                .doesNotThrowAnyException();
    }

    @Test
    void juizSemPasskey_lancaPasskeyRequiredException() {
        when(trustedDeviceRepository.findActiveByUser(2L)).thenReturn(List.of());
        assertThatThrownBy(() -> enforcer().exigirParaMagistratura(2L, TipoUsuario.JUIZ))
                .isInstanceOf(PasskeyRequiredException.class);
    }

    @Test
    void desembargadorSemPasskey_lancaPasskeyRequiredException() {
        when(trustedDeviceRepository.findActiveByUser(3L)).thenReturn(List.of());
        assertThatThrownBy(() -> enforcer().exigirParaMagistratura(3L, TipoUsuario.DESEMBARGADOR))
                .isInstanceOf(PasskeyRequiredException.class);
    }

    @Test
    void ministroSemPasskey_lancaPasskeyRequiredException() {
        when(trustedDeviceRepository.findActiveByUser(4L)).thenReturn(List.of());
        assertThatThrownBy(() -> enforcer().exigirParaMagistratura(4L, TipoUsuario.MINISTRO))
                .isInstanceOf(PasskeyRequiredException.class);
    }

    @Test
    void cidadaoSemPasskey_naoEVerificado() {
        assertThatCode(() -> enforcer().exigirParaMagistratura(5L, TipoUsuario.CIDADAO))
                .doesNotThrowAnyException();
    }

    @Test
    void tipoNulo_naoEVerificado() {
        assertThatCode(() -> enforcer().exigirParaMagistratura(6L, null))
                .doesNotThrowAnyException();
    }

    private TrustedDevice dispositivo(String attachment, boolean attestationTrusted, String fmt) {
        TrustedDevice d = new TrustedDevice();
        d.setCredentialId("cred-1");
        d.setAuthenticatorAttachment(attachment);
        d.setAttestationTrusted(attestationTrusted);
        d.setAttestationFmt(fmt);
        return d;
    }
}
