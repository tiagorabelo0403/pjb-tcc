package com.tcc.pjb.backend.modules.laiane.service;

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
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de LaianePeticaoAssistService: primeira etapa do pipeline de assistência
 * à petição -- triagem nacional de IA, validação estrutural, teto/econômico, competência
 * dinâmica, radar de padrões e conflito processual do prontuário. Os 6 colaboradores são
 * chamados independentemente (não há encadeamento entre eles) mas todos os resultados
 * alimentam o pipeline advisory subsequente e o cálculo de readiness.
 */
@Service
public class LaianeTriagemPipelineOrchestrator {

    private final TriagemNacionalIAEngine triagemNacionalIAEngine;
    private final LaianePeticaoValidatorService validatorService;
    private final TetoProcessualService tetoProcessualService;
    private final MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine;
    private final RadarPadroesService radarPadroesService;
    private final ProntuarioNacionalService prontuarioNacionalService;

    public LaianeTriagemPipelineOrchestrator(TriagemNacionalIAEngine triagemNacionalIAEngine,
                                              LaianePeticaoValidatorService validatorService,
                                              TetoProcessualService tetoProcessualService,
                                              MapaCompetenciaDinamicoEngine mapaCompetenciaDinamicoEngine,
                                              RadarPadroesService radarPadroesService,
                                              ProntuarioNacionalService prontuarioNacionalService) {
        this.triagemNacionalIAEngine = Objects.requireNonNull(triagemNacionalIAEngine);
        this.validatorService = Objects.requireNonNull(validatorService);
        this.tetoProcessualService = Objects.requireNonNull(tetoProcessualService);
        this.mapaCompetenciaDinamicoEngine = Objects.requireNonNull(mapaCompetenciaDinamicoEngine);
        this.radarPadroesService = Objects.requireNonNull(radarPadroesService);
        this.prontuarioNacionalService = Objects.requireNonNull(prontuarioNacionalService);
    }

    public TriagemNacionalIAEngine.ResultadoTriagem triar(TriagemNacionalIAEngine.PedidoTriagem pedido) {
        return triagemNacionalIAEngine.triar(pedido);
    }

    public LaianePeticaoValidateResponse validate(String textoBase, String rito, String classe, String ramo) {
        return validatorService.validate(textoBase, rito, classe, ramo);
    }

    public TetoProcessualService.DiagnosticoTetoProcessual diagnosticarTeto(BigDecimal valorCausa,
                                                                             String tipoJustica,
                                                                             String ramoDireito,
                                                                             String rito) {
        return tetoProcessualService.diagnosticar(
                valorCausa,
                TipoJustica.fromString(tipoJustica),
                RamoDireito.fromString(ramoDireito),
                rito,
                null,
                LocalDate.now());
    }

    public DynamicCompetenceDistributionResponse distribuirCompetencia(DynamicCompetenceDistributionRequest request) {
        return mapaCompetenciaDinamicoEngine.distribuir(request).orElse(null);
    }

    public RadarPadroesService.AnaliseRadarResultado analisarRadar(RadarPadroesService.ContextoRadar contexto) {
        return radarPadroesService.analisar(contexto);
    }

    public RadarPadroesService.FingerprintPeticao construirFingerprintRadar(RadarPadroesService.ContextoRadar contexto) {
        return radarPadroesService.construirFingerprint(contexto);
    }

    public ProntuarioNacionalService.AnaliseConflitoProcessual detectarLitispendenciaOuCoisaJulgada(String cpfCnpjAutor,
                                                                                                     String cpfCnpjReu,
                                                                                                     RamoDireito ramo) {
        return prontuarioNacionalService.detectarLitispendenciaOuCoisaJulgada(cpfCnpjAutor, cpfCnpjReu, ramo);
    }
}
