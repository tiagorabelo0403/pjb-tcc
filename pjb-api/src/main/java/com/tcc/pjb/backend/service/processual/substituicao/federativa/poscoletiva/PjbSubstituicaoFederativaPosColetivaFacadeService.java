package com.tcc.pjb.backend.service.processual.substituicao.federativa.poscoletiva;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaPosColetivaApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPosColetivaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPosColetivaCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPosColetivaTribunal;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.poscoletiva.PjbSubstituicaoFederativaPosColetivaCompetenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.poscoletiva.PjbSubstituicaoFederativaPosColetivaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.poscoletiva.PjbSubstituicaoFederativaPosColetivaTribunalResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoFederativaPosColetivaFacadeService {

    private final PjbSubstituicaoFederativaPosColetivaApplicationService applicationService;

    public PjbSubstituicaoFederativaPosColetivaFacadeService(PjbSubstituicaoFederativaPosColetivaApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbSubstituicaoFederativaPosColetivaResponse avaliar() {
        PjbSubstituicaoFederativaPosColetivaAggregate aggregate = applicationService.avaliar();
        return new PjbSubstituicaoFederativaPosColetivaResponse(
                aggregate.scoreNacional(),
                aggregate.malhaPosColetivaPronta(),
                aggregate.coisaJulgadaColetivaGovernada(),
                aggregate.liquidacaoColetivaGovernada(),
                aggregate.habilitacaoIndividualGovernada(),
                aggregate.cumprimentoPulverizadoLotesGovernado(),
                aggregate.tribunaisProntos(),
                aggregate.tribunais().stream().map(this::mapTribunal).toList(),
                aggregate.bloqueadoresCriticos(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public PjbSubstituicaoFederativaPosColetivaTribunalResponse avaliarTribunal(String tribunalCodigo) {
        return mapTribunal(applicationService.avaliarTribunal(tribunalCodigo));
    }

    private PjbSubstituicaoFederativaPosColetivaTribunalResponse mapTribunal(PjbSubstituicaoFederativaPosColetivaTribunal tribunal) {
        return new PjbSubstituicaoFederativaPosColetivaTribunalResponse(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.legadoPrincipal(),
                tribunal.ondaAtual(),
                tribunal.scoreGeral(),
                tribunal.scoreCoisaJulgadaColetiva(),
                tribunal.scoreLiquidacaoColetiva(),
                tribunal.scoreHabilitacaoIndividual(),
                tribunal.scoreCumprimentoPulverizadoLotes(),
                tribunal.prontoTutelaColetiva(),
                tribunal.malhaPosColetivaPronta(),
                tribunal.totalCompetencias(),
                tribunal.competencias().stream().map(this::mapCompetencia).toList(),
                tribunal.bloqueadores(),
                tribunal.proximasAcoes(),
                tribunal.fundamentos()
        );
    }

    private PjbSubstituicaoFederativaPosColetivaCompetenciaResponse mapCompetencia(PjbSubstituicaoFederativaPosColetivaCompetencia competencia) {
        return new PjbSubstituicaoFederativaPosColetivaCompetenciaResponse(
                competencia.competenciaCodigo(),
                competencia.ramoCodigo(),
                competencia.ramoNome(),
                competencia.ritoCodigo(),
                competencia.totalProcessos(),
                competencia.scoreCoisaJulgadaColetiva(),
                competencia.scoreLiquidacaoColetiva(),
                competencia.scoreHabilitacaoIndividual(),
                competencia.scoreCumprimentoPulverizadoLotes(),
                competencia.malhaPosColetivaPronta(),
                competencia.coisaJulgadaColetivaAtiva(),
                competencia.liquidacaoColetivaAtiva(),
                competencia.habilitacaoIndividualAtiva(),
                competencia.cumprimentoPulverizadoLotesAtivo(),
                competencia.janelaAtual(),
                competencia.guardrails(),
                competencia.fundamentos(),
                competencia.processoReferenciaId(),
                competencia.numeroReferencia()
        );
    }
}
