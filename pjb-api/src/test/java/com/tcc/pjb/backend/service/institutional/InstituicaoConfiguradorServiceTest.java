package com.tcc.pjb.backend.service.institutional;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InstituicaoConfiguradorServiceTest {

    private final InstituicaoConfiguradorService configurador = new InstituicaoConfiguradorService();

    private static final UUID ID = UUID.randomUUID();
    private static final UUID COMARCA_ID = UUID.randomUUID();

    @Test
    void varaUnicaInteriorEConfiguradaComDistribuicaoManual() {
        var vara = configurador.configurarVaraUnicaInterior(ID, "Vara Única de Quixadá", "CE", COMARCA_ID);
        assertThat(vara.eVaraUnica()).isTrue();
        assertThat(vara.eComarcaDoInterior()).isTrue();
        assertThat(vara.configuracao().distribuicaoManual()).isTrue();
    }

    @Test
    void varaUnicaAceitaTodosOsRitos() {
        var vara = configurador.configurarVaraUnicaInterior(ID, "Vara Única", "PI", COMARCA_ID);
        assertThat(vara.ritosSuportados()).contains(RitoProcessual.PROCEDIMENTO_PENAL_COMUM);
        assertThat(vara.ritosSuportados()).contains(RitoProcessual.COMUM_ORDINARIO);
    }

    @Test
    void juizadoEspecialSoAceitaRitosSumarissimos() {
        var jec = configurador.configurarJuizadoEspecial(ID, "JEC Teste", "SP", COMARCA_ID);
        assertThat(jec.tipo()).isInstanceOf(InstituicaoJudicialTipo.JuizadoEspecial.class);
        assertThat(jec.aceitaRito(RitoProcessual.JUIZADO_ESPECIAL_CIVEL)).isTrue();
        assertThat(jec.aceitaRito(RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO)).isTrue();
    }

    @Test
    void jecItineranteRegistraMunicipioAlvo() {
        var jec = configurador.configurarJecItinerante(ID, "JEC Itinerante", "CE", COMARCA_ID, "Itapiúna");
        assertThat(jec.eJecItinerante()).isTrue();
        assertThat(jec.descricaoTipo()).contains("Itapiúna");
    }

    @Test
    void plantaoTemHorarioContinuo() {
        var plantao = configurador.configurarPlantao(ID, "Plantão", "RJ", COMARCA_ID, "PLANTAO-2026-01");
        assertThat(plantao.ePlantao()).isTrue();
        assertThat(plantao.suportaJuizSubstituto()).isTrue();
        assertThat(plantao.configuracao().plantaoAtivo()).isTrue();
    }

    @Test
    void camaraTribunalESegundoGrau() {
        var camara = configurador.configurarCamara(ID, "2ª Câmara Cível", "MG", COMARCA_ID, 2);
        assertThat(camara.tipo()).isInstanceOf(InstituicaoJudicialTipo.Camara.class);
        assertThat(camara.tipo().grauJurisdicao().name()).isEqualTo("SEGUNDO_GRAU");
    }

    @Test
    void competenciaJecLimitadaA40SalariosMinimos() {
        var competencia = configurador.definirCompetenciaJEC(ID, "Comarca de Fortaleza");
        assertThat(competencia.aceitaValor(new java.math.BigDecimal("39999"))).isTrue();
        assertThat(competencia.aceitaValor(new java.math.BigDecimal("40001"))).isFalse();
    }

    @Test
    void configuracaoMenorForumTemLimiteProcessosMenor() {
        var vara = configurador.configurarVaraUnicaInterior(ID, "Vara", "AC", COMARCA_ID);
        assertThat(vara.configuracao().limiteProcessosPorServidor()).isLessThanOrEqualTo(200);
    }

    @Test
    void juizadoAdmitePartesSemAdvogado() {
        var jec = configurador.configurarJuizadoEspecial(ID, "JEC", "BA", COMARCA_ID);
        assertThat(jec.tipo().admiteJurisdicionadoSemAdvogado()).isTrue();
    }
}
