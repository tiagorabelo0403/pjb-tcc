package com.tcc.pjb.backend.core.security.webauthn;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WebAuthnServiceMagistraturaValidationTest {

    @Test
    void naoMagistraturaNuncaExigeRequisitosDeAttachmentOuFmt() {
        assertThatCode(() -> WebAuthnService.validarRequisitosMagistratura(false, null, null))
                .doesNotThrowAnyException();
        assertThatCode(() -> WebAuthnService.validarRequisitosMagistratura(false, "cross-platform", "packed"))
                .doesNotThrowAnyException();
    }

    @Test
    void magistraturaComAuthenticatorCrossPlatformLancaErro() {
        assertThatThrownBy(() -> WebAuthnService.validarRequisitosMagistratura(true, "cross-platform", "tpm"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("platform authenticator");
    }

    @Test
    void magistraturaComFmtNaoTpmNemAppleLancaErro() {
        assertThatThrownBy(() -> WebAuthnService.validarRequisitosMagistratura(true, "platform", "packed"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TPM");
    }

    @Test
    void magistraturaComPlatformETpmPassa() {
        assertThatCode(() -> WebAuthnService.validarRequisitosMagistratura(true, "platform", "tpm"))
                .doesNotThrowAnyException();
    }

    @Test
    void magistraturaComPlatformEAppleplPassa() {
        assertThatCode(() -> WebAuthnService.validarRequisitosMagistratura(true, "platform", "apple"))
                .doesNotThrowAnyException();
    }
}
