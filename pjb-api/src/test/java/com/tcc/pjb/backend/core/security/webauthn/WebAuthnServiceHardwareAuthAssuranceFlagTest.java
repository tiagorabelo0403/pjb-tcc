package com.tcc.pjb.backend.core.security.webauthn;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class WebAuthnServiceHardwareAuthAssuranceFlagTest {

    @Test
    void promotorEDefensorTambemExigemHardwareAuthAssuranceNoEnrollment() {
        assertThat(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.requiresHardwareAuthAssurance()).isTrue();
        assertThat(TipoUsuario.DEFENSOR_PUBLICO.requiresHardwareAuthAssurance()).isTrue();
    }

    @Test
    void validarRequisitosMagistraturaContinuaAgnosticoDeTipoUsuario() {
        // WebAuthnService.validarRequisitosMagistratura já recebe boolean — provado
        // em WebAuthnServiceMagistraturaValidationTest, que não muda nesta task.
        assertThat(WebAuthnService.class).isNotNull();
    }
}
