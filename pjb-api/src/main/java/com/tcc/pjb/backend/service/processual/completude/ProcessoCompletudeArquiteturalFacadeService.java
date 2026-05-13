package com.tcc.pjb.backend.service.processual.completude;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoLegadosApplicationService;
import com.tcc.pjb.backend.core.processo.analytics.application.ProcessoAnalyticsNacionalApplicationService;
import com.tcc.pjb.backend.core.processo.completude.application.ProcessoFechamentoTotalApplicationService;
import com.tcc.pjb.backend.core.processo.plantao.application.ProcessoPlantaoSubstituicaoApplicationService;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.processo.sinalizacao.application.ProcessoSinalizacaoRegraApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoInfraestruturaSoberanaApplicationService;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import com.tcc.pjb.backend.core.quality.certificacao.application.PjbCertificacaoOperacionalApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseLearningApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.application.PjbCodebaseSanityApplicationService;
import com.tcc.pjb.backend.core.quality.codebase.presentation.PjbCodebaseLearningResponseMapper;
import com.tcc.pjb.backend.core.processo.orfandade.application.ProcessoAntiOrfaoApplicationService;
import com.tcc.pjb.backend.model.dto.processual.completude.apisurface.ProcessoApiSurfaceSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.certificacao.ProcessoCertificacaoOperacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseLearningResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.ProcessoCompletudeModuloResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.ProcessoFechamentoTotalResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.infraestrutura.ProcessoInfraestruturaSoberanaResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.substituicao.ProcessoSubstituicaoLegadosResponse;
import com.tcc.pjb.backend.service.processual.completude.assembler.ProcessoCompletudeArquiteturalResponseAssembler;
import org.springframework.stereotype.Service;

@Service
public class ProcessoCompletudeArquiteturalFacadeService {

    private final ProcessoAntiOrfaoApplicationService processoAntiOrfaoApplicationService;
    private final ProcessoSinalizacaoRegraApplicationService processoSinalizacaoRegraApplicationService;
    private final ProcessoPlantaoSubstituicaoApplicationService processoPlantaoSubstituicaoApplicationService;
    private final ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService;
    private final ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService;
    private final ProcessoFechamentoTotalApplicationService processoFechamentoTotalApplicationService;
    private final ProcessoInfraestruturaSoberanaApplicationService processoInfraestruturaSoberanaApplicationService;
    private final PjbCertificacaoOperacionalApplicationService pjbCertificacaoOperacionalApplicationService;
    private final PjbSubstituicaoLegadosApplicationService pjbSubstituicaoLegadosApplicationService;
    private final PjbCodebaseLearningApplicationService pjbCodebaseLearningApplicationService;
    private final PjbCodebaseSanityApplicationService pjbCodebaseSanityApplicationService;
    private final PjbApiSurfaceSanityApplicationService pjbApiSurfaceSanityApplicationService;
    private final ProcessoCompletudeArquiteturalResponseAssembler responseAssembler;

    public ProcessoCompletudeArquiteturalFacadeService(ProcessoAntiOrfaoApplicationService processoAntiOrfaoApplicationService,
                                                       ProcessoSinalizacaoRegraApplicationService processoSinalizacaoRegraApplicationService,
                                                       ProcessoPlantaoSubstituicaoApplicationService processoPlantaoSubstituicaoApplicationService,
                                                       ProcessoAnalyticsNacionalApplicationService processoAnalyticsNacionalApplicationService,
                                                       ProcessoOperacaoTransversalApplicationService processoOperacaoTransversalApplicationService,
                                                       ProcessoFechamentoTotalApplicationService processoFechamentoTotalApplicationService,
                                                       ProcessoInfraestruturaSoberanaApplicationService processoInfraestruturaSoberanaApplicationService,
                                                       PjbCertificacaoOperacionalApplicationService pjbCertificacaoOperacionalApplicationService,
                                                       PjbSubstituicaoLegadosApplicationService pjbSubstituicaoLegadosApplicationService,
                                                       PjbCodebaseLearningApplicationService pjbCodebaseLearningApplicationService,
                                                       PjbCodebaseSanityApplicationService pjbCodebaseSanityApplicationService,
                                                       PjbApiSurfaceSanityApplicationService pjbApiSurfaceSanityApplicationService,
                                                       ProcessoCompletudeArquiteturalResponseAssembler responseAssembler) {
        this.processoAntiOrfaoApplicationService = processoAntiOrfaoApplicationService;
        this.processoSinalizacaoRegraApplicationService = processoSinalizacaoRegraApplicationService;
        this.processoPlantaoSubstituicaoApplicationService = processoPlantaoSubstituicaoApplicationService;
        this.processoAnalyticsNacionalApplicationService = processoAnalyticsNacionalApplicationService;
        this.processoOperacaoTransversalApplicationService = processoOperacaoTransversalApplicationService;
        this.processoFechamentoTotalApplicationService = processoFechamentoTotalApplicationService;
        this.processoInfraestruturaSoberanaApplicationService = processoInfraestruturaSoberanaApplicationService;
        this.pjbCertificacaoOperacionalApplicationService = pjbCertificacaoOperacionalApplicationService;
        this.pjbSubstituicaoLegadosApplicationService = pjbSubstituicaoLegadosApplicationService;
        this.pjbCodebaseLearningApplicationService = pjbCodebaseLearningApplicationService;
        this.pjbCodebaseSanityApplicationService = pjbCodebaseSanityApplicationService;
        this.pjbApiSurfaceSanityApplicationService = pjbApiSurfaceSanityApplicationService;
        this.responseAssembler = responseAssembler;
    }

    public ProcessoCompletudeModuloResponse antiOrfao(Long processoId) {
        return responseAssembler.antiOrfao(processoAntiOrfaoApplicationService.detalhar(processoId));
    }

    public ProcessoCompletudeModuloResponse sinalizacao(Long processoId, String profileCode) {
        return responseAssembler.sinalizacao(processoSinalizacaoRegraApplicationService.detalhar(processoId, profileCode));
    }

    public ProcessoCompletudeModuloResponse plantaoSubstituicao(Long processoId) {
        return responseAssembler.plantaoSubstituicao(processoPlantaoSubstituicaoApplicationService.detalhar(processoId));
    }

    public ProcessoCompletudeModuloResponse analyticsNacional(Long processoId) {
        return responseAssembler.analyticsNacional(processoAnalyticsNacionalApplicationService.detalhar(processoId));
    }

    public ProcessoCompletudeModuloResponse operacaoTransversal(Long processoId) {
        return responseAssembler.operacaoTransversal(processoOperacaoTransversalApplicationService.detalhar(processoId));
    }

    public ProcessoInfraestruturaSoberanaResponse infraestruturaSoberana(Long processoId) {
        return responseAssembler.infraestrutura(processoInfraestruturaSoberanaApplicationService.consolidar(processoId));
    }

    public ProcessoCertificacaoOperacionalResponse certificacaoOperacional(Long processoId) {
        return responseAssembler.certificacao(pjbCertificacaoOperacionalApplicationService.certificar(processoId));
    }

    public ProcessoSubstituicaoLegadosResponse substituicaoLegados(Long processoId) {
        return responseAssembler.substituicao(pjbSubstituicaoLegadosApplicationService.avaliar(processoId));
    }

    public ProcessoCodebaseSanityResponse sanidadeCodigo() {
        return sanidadeCodigo(false);
    }

    public ProcessoCodebaseSanityResponse sanidadeCodigo(boolean forceRefresh) {
        return responseAssembler.codebase(pjbCodebaseSanityApplicationService.auditar(forceRefresh));
    }

    public ProcessoApiSurfaceSanityResponse sanidadeApi() {
        return responseAssembler.apiSurface(pjbApiSurfaceSanityApplicationService.auditar());
    }

    public ProcessoCodebaseLearningResponse sanidadeAprendizado() {
        return sanidadeAprendizado(false);
    }

    public ProcessoCodebaseLearningResponse sanidadeAprendizado(boolean forceRefresh) {
        return PjbCodebaseLearningResponseMapper.toProcessual(pjbCodebaseLearningApplicationService.aprender(forceRefresh));
    }

    public ProcessoFechamentoTotalResponse fechamentoTotal(Long processoId, String profileCode) {
        return responseAssembler.fechamentoTotal(
                processoFechamentoTotalApplicationService.detalhar(processoId, profileCode),
                pjbApiSurfaceSanityApplicationService.auditar()
        );
    }
}
