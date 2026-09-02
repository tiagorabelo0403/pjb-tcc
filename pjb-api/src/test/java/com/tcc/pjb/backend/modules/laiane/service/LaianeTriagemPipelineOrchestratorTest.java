package com.tcc.pjb.backend.modules.laiane.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionRequest;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoValidateResponse;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LaianeTriagemPipelineOrchestratorTest {

    private final TriagemNacionalIAEngine triagemEngine = mock(TriagemNacionalIAEngine.class);
    private final LaianePeticaoValidatorService validatorService = mock(LaianePeticaoValidatorService.class);
    private final TetoProcessualService tetoService = mock(TetoProcessualService.class);
    private final MapaCompetenciaDinamicoEngine mapaEngine = mock(MapaCompetenciaDinamicoEngine.class);
    private final RadarPadroesService radarService = mock(RadarPadroesService.class);
    private final ProntuarioNacionalService prontuarioService = mock(ProntuarioNacionalService.class);
    private final LaianeTriagemPipelineOrchestrator orchestrator = new LaianeTriagemPipelineOrchestrator(
            triagemEngine, validatorService, tetoService, mapaEngine, radarService, prontuarioService);

    @Test
    void validateDelegaComOs4Argumentos() {
        var response = mock(LaianePeticaoValidateResponse.class);
        when(validatorService.validate("texto", "COMUM_ORDINARIO", "CLASSE-1", "CIVIL")).thenReturn(response);

        assertThat(orchestrator.validate("texto", "COMUM_ORDINARIO", "CLASSE-1", "CIVIL")).isSameAs(response);
    }

    @Test
    void diagnosticarTetoParseiaEnumsEChamaTetoService() {
        var response = mock(TetoProcessualService.DiagnosticoTetoProcessual.class);
        when(tetoService.diagnosticar(
                new BigDecimal("1000"),
                TipoJustica.fromString("ESTADUAL"),
                RamoDireito.fromString("CIVIL"),
                "COMUM_ORDINARIO",
                null,
                LocalDate.now())).thenReturn(response);

        assertThat(orchestrator.diagnosticarTeto(new BigDecimal("1000"), "ESTADUAL", "CIVIL", "COMUM_ORDINARIO")).isSameAs(response);
    }

    @Test
    void distribuirCompetenciaDesempacotaOptional() {
        var request = mock(DynamicCompetenceDistributionRequest.class);
        var response = mock(DynamicCompetenceDistributionResponse.class);
        when(mapaEngine.distribuir(request)).thenReturn(Optional.of(response));

        assertThat(orchestrator.distribuirCompetencia(request)).isSameAs(response);
    }

    @Test
    void distribuirCompetenciaRetornaNullQuandoOptionalVazio() {
        var request = mock(DynamicCompetenceDistributionRequest.class);
        when(mapaEngine.distribuir(request)).thenReturn(Optional.empty());

        assertThat(orchestrator.distribuirCompetencia(request)).isNull();
    }

    @Test
    void triarDelegaSemMapeamentoExtra() {
        var pedido = mock(TriagemNacionalIAEngine.PedidoTriagem.class);
        var resultado = mock(TriagemNacionalIAEngine.ResultadoTriagem.class);
        when(triagemEngine.triar(pedido)).thenReturn(resultado);

        assertThat(orchestrator.triar(pedido)).isSameAs(resultado);
    }

    @Test
    void detectarLitispendenciaOuCoisaJulgadaDelegaComOs3Argumentos() {
        var response = mock(ProntuarioNacionalService.AnaliseConflitoProcessual.class);
        when(prontuarioService.detectarLitispendenciaOuCoisaJulgada("11111111111", "22222222222", RamoDireito.fromString("CIVIL"))).thenReturn(response);

        assertThat(orchestrator.detectarLitispendenciaOuCoisaJulgada("11111111111", "22222222222", RamoDireito.fromString("CIVIL"))).isSameAs(response);
    }

    @Test
    void analisarRadarEConstruirFingerprintDelegam() {
        var contexto = mock(RadarPadroesService.ContextoRadar.class);
        var resultado = mock(RadarPadroesService.AnaliseRadarResultado.class);
        var fingerprint = mock(RadarPadroesService.FingerprintPeticao.class);
        when(radarService.analisar(contexto)).thenReturn(resultado);
        when(radarService.construirFingerprint(contexto)).thenReturn(fingerprint);

        assertThat(orchestrator.analisarRadar(contexto)).isSameAs(resultado);
        assertThat(orchestrator.construirFingerprintRadar(contexto)).isSameAs(fingerprint);
    }
}
