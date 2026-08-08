package com.tcc.pjb.backend.model.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TipoUsuarioRequiresHardwareAuthAssuranceTest {

    @Test
    void magistraturaExigeHardwareAuthAssurance() {
        assertThat(TipoUsuario.JUIZ.requiresHardwareAuthAssurance()).isTrue();
        assertThat(TipoUsuario.DESEMBARGADOR.requiresHardwareAuthAssurance()).isTrue();
        assertThat(TipoUsuario.MINISTRO.requiresHardwareAuthAssurance()).isTrue();
    }

    @Test
    void ministerioPublicoExigeHardwareAuthAssurance() {
        assertThat(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.requiresHardwareAuthAssurance()).isTrue();
        assertThat(TipoUsuario.PROCURADOR_GERAL_REPUBLICA.requiresHardwareAuthAssurance()).isTrue();
    }

    @Test
    void defensoriaPublicaExigeHardwareAuthAssurance() {
        assertThat(TipoUsuario.DEFENSOR_PUBLICO.requiresHardwareAuthAssurance()).isTrue();
        assertThat(TipoUsuario.DEFENSOR_PUBLICO_FEDERAL.requiresHardwareAuthAssurance()).isTrue();
    }

    @Test
    void cidadaoAdvogadoENaoExigemHardwareAuthAssurance() {
        assertThat(TipoUsuario.CIDADAO.requiresHardwareAuthAssurance()).isFalse();
        assertThat(TipoUsuario.ADVOGADO.requiresHardwareAuthAssurance()).isFalse();
    }

    @Test
    void procuradoriaNaoExigeHardwareAuthAssurance() {
        assertThat(TipoUsuario.PROCURADOR.requiresHardwareAuthAssurance()).isFalse();
        assertThat(TipoUsuario.PROCURADORIA_MUNICIPAL.requiresHardwareAuthAssurance()).isFalse();
        assertThat(TipoUsuario.PROCURADORIA_ESTADUAL.requiresHardwareAuthAssurance()).isFalse();
        assertThat(TipoUsuario.PROCURADORIA_FEDERAL.requiresHardwareAuthAssurance()).isFalse();
    }
}
