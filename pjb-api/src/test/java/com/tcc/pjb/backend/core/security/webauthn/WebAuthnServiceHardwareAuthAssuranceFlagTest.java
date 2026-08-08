package com.tcc.pjb.backend.core.security.webauthn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class WebAuthnServiceHardwareAuthAssuranceFlagTest {

    @Test
    void promotorEDefensorTambemExigemHardwareAuthAssuranceNoEnrollment() {
        assertThat(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.requiresHardwareAuthAssurance()).isTrue();
        assertThat(TipoUsuario.DEFENSOR_PUBLICO.requiresHardwareAuthAssurance()).isTrue();
    }

    @Test
    void promotorComPasskeyCrossPlatformFalhaNaValidacaoDeRequisitos() {
        boolean hardwareAuthRequired = TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.requiresHardwareAuthAssurance();
        assertThatThrownBy(() -> WebAuthnService.validarRequisitosMagistratura(hardwareAuthRequired, "cross-platform", "tpm"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("platform authenticator");
    }

    @Test
    void defensorComPasskeyPlatformETpmPassaNaValidacaoDeRequisitos() {
        boolean hardwareAuthRequired = TipoUsuario.DEFENSOR_PUBLICO.requiresHardwareAuthAssurance();
        assertThatCode(() -> WebAuthnService.validarRequisitosMagistratura(hardwareAuthRequired, "platform", "tpm"))
                .doesNotThrowAnyException();
    }
}
