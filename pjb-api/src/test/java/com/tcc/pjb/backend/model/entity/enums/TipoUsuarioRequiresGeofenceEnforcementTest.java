package com.tcc.pjb.backend.model.entity.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TipoUsuarioRequiresGeofenceEnforcementTest {

    @Test
    void magistraturaExigeGeofence() {
        assertThat(TipoUsuario.JUIZ.requiresGeofenceEnforcement()).isTrue();
        assertThat(TipoUsuario.DESEMBARGADOR.requiresGeofenceEnforcement()).isTrue();
        assertThat(TipoUsuario.MINISTRO.requiresGeofenceEnforcement()).isTrue();
    }

    @Test
    void ministerioPublicoExigeGeofence() {
        assertThat(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO.requiresGeofenceEnforcement()).isTrue();
        assertThat(TipoUsuario.PROCURADOR_GERAL_REPUBLICA.requiresGeofenceEnforcement()).isTrue();
    }

    @Test
    void defensoriaPublicaExigeGeofence() {
        assertThat(TipoUsuario.DEFENSOR_PUBLICO.requiresGeofenceEnforcement()).isTrue();
        assertThat(TipoUsuario.DEFENSOR_PUBLICO_FEDERAL.requiresGeofenceEnforcement()).isTrue();
    }

    @Test
    void procuradoriaExigeGeofenceMesmoSemExigirHardwareAuthAssurance() {
        assertThat(TipoUsuario.PROCURADOR.requiresGeofenceEnforcement()).isTrue();
        assertThat(TipoUsuario.PROCURADORIA_MUNICIPAL.requiresGeofenceEnforcement()).isTrue();
        assertThat(TipoUsuario.PROCURADORIA_ESTADUAL.requiresGeofenceEnforcement()).isTrue();
        assertThat(TipoUsuario.PROCURADORIA_FEDERAL.requiresGeofenceEnforcement()).isTrue();

        assertThat(TipoUsuario.PROCURADOR.requiresHardwareAuthAssurance()).isFalse();
    }

    @Test
    void cidadaoAdvogadoENaoExigemGeofence() {
        assertThat(TipoUsuario.CIDADAO.requiresGeofenceEnforcement()).isFalse();
        assertThat(TipoUsuario.ADVOGADO.requiresGeofenceEnforcement()).isFalse();
    }
}
