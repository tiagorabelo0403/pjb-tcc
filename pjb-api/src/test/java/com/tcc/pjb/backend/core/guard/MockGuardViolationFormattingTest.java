package com.tcc.pjb.backend.core.guard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MockGuardViolationFormattingTest {

    @Test
    void violationOf_criaComCamposCorretos() {
        MockGuardViolation violation = MockGuardViolation.of("hsm", "pjb.hsm.mock-enabled", MockGuardProfile.PROD);

        assertThat(violation.servico()).isEqualTo("hsm");
        assertThat(violation.propriedade()).isEqualTo("pjb.hsm.mock-enabled");
        assertThat(violation.perfil()).isEqualTo(MockGuardProfile.PROD);
        assertThat(violation.motivo()).contains("pjb.hsm.mock-enabled").contains("PROD");
        assertThat(violation.detectadoEm()).isNotNull();
        assertThat(violation.violationId()).isNotNull();
    }

    @Test
    void violationId_eUuidV7Valido() {
        MockGuardViolation violation = MockGuardViolation.of("bnmp", "pjb.bnmp.mock-enabled", MockGuardProfile.STAGING);
        UUID id = violation.violationId();

        assertThat(id).isNotNull();
        // UUIDv7: version bits = 7 (bits 12-15 do MSB)
        assertThat((id.getMostSignificantBits() >> 12) & 0xF).isEqualTo(7L);
    }

    @Test
    void excecao_mensagemContemPropertyEPerfil() {
        MockGuardViolation violation = MockGuardViolation.of("hsm", "pjb.hsm.mock-enabled", MockGuardProfile.HOMOLOG);
        MockGuardViolationException ex = new MockGuardViolationException(violation);

        assertThat(ex.getMessage())
                .contains("pjb.hsm.mock-enabled")
                .contains("HOMOLOG")
                .contains("hsm");
        assertThat(ex.getViolation()).isSameAs(violation);
    }
}
