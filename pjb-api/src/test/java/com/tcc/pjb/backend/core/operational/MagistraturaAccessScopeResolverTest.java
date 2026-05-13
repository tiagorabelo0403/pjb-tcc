package com.tcc.pjb.backend.core.operational;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.junit.jupiter.api.Test;

class MagistraturaAccessScopeResolverTest {

    @Test
    void deveResolverJuizFederalComPainelInstitucionalFederal() {
        MagistraturaAccessScopeResolver.AccessScope scope = MagistraturaAccessScopeResolver.resolve(
                TipoUsuario.JUIZ_FEDERAL,
                "TRF5",
                "SECAO_JUDICIARIA_CE",
                "GABINETE_01"
        );

        assertThat(scope.justiceAxis()).isEqualTo("FEDERAL");
        assertThat(scope.tribunalAxis()).isEqualTo("TRF5");
        assertThat(scope.authorityClass()).isEqualTo("JUIZ");
        assertThat(scope.panelCode()).isEqualTo("MAGISTRATURA_JUIZ_FEDERAL_TRF5");
        assertThat(scope.landingPath()).contains("/api/v1/juiz/gabinete-decisoes")
                .contains("justica=FEDERAL")
                .contains("tribunal=TRF5")
                .contains("unidadeCodigo=SECAO_JUDICIARIA_CE")
                .contains("caixaCodigo=GABINETE_01");
    }

    @Test
    void deveResolverDesembargadorFederalSemCriarMalhaParalela() {
        MagistraturaAccessScopeResolver.AccessScope scope = MagistraturaAccessScopeResolver.resolve(
                TipoUsuario.DESEMBARGADOR_FEDERAL,
                "TRF5",
                "TRF5_GABINETE_RELATOR",
                "CAMARA_2"
        );

        assertThat(scope.justiceAxis()).isEqualTo("FEDERAL");
        assertThat(scope.authorityClass()).isEqualTo("DESEMBARGADOR");
        assertThat(scope.panelCode()).isEqualTo("MAGISTRATURA_DESEMBARGADOR_FEDERAL_TRF5");
        assertThat(scope.landingPath()).contains("/api/v1/desembargador/colegiado")
                .contains("justica=FEDERAL")
                .contains("tribunal=TRF5")
                .contains("unidadeCodigo=TRF5_GABINETE_RELATOR")
                .contains("caixaCodigo=CAMARA_2");
    }

    @Test
    void deveResolverMinistroComCorteSuperiorCorreta() {
        MagistraturaAccessScopeResolver.AccessScope scope = MagistraturaAccessScopeResolver.resolve(
                TipoUsuario.MINISTRO,
                "STJ",
                "GABINETE_MINISTRO",
                "TURMA_01"
        );

        assertThat(scope.justiceAxis()).isEqualTo("SUPERIOR");
        assertThat(scope.authorityClass()).isEqualTo("MINISTRO");
        assertThat(scope.panelCode()).isEqualTo("MAGISTRATURA_MINISTRO_STJ");
        assertThat(scope.landingPath()).contains("/api/v1/ministro/plenario")
                .contains("tribunal=STJ")
                .contains("unidadeCodigo=GABINETE_MINISTRO")
                .contains("caixaCodigo=TURMA_01");
    }
}
