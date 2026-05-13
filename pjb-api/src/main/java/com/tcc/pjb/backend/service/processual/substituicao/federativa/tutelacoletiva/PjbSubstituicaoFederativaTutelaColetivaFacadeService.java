package com.tcc.pjb.backend.service.processual.substituicao.federativa.tutelacoletiva;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaTutelaColetivaApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaTribunal;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.tutelacoletiva.PjbSubstituicaoFederativaTutelaColetivaCompetenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.tutelacoletiva.PjbSubstituicaoFederativaTutelaColetivaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.tutelacoletiva.PjbSubstituicaoFederativaTutelaColetivaTribunalResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoFederativaTutelaColetivaFacadeService {

    private final PjbSubstituicaoFederativaTutelaColetivaApplicationService applicationService;

    public PjbSubstituicaoFederativaTutelaColetivaFacadeService(PjbSubstituicaoFederativaTutelaColetivaApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbSubstituicaoFederativaTutelaColetivaResponse avaliar() {
        PjbSubstituicaoFederativaTutelaColetivaAggregate aggregate = applicationService.avaliar();
        return new PjbSubstituicaoFederativaTutelaColetivaResponse(
                aggregate.scoreNacional(),
                aggregate.malhaTutelaColetivaPronta(),
                aggregate.tutelaColetivaConectada(),
                aggregate.demandasEstruturaisGovernadas(),
                aggregate.execucaoColetivaGovernada(),
                aggregate.cumprimentoMassaGovernado(),
                aggregate.tribunaisProntos(),
                aggregate.tribunais().stream().map(this::mapTribunal).toList(),
                aggregate.bloqueadoresCriticos(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public PjbSubstituicaoFederativaTutelaColetivaTribunalResponse avaliarTribunal(String tribunalCodigo) {
        return mapTribunal(applicationService.avaliarTribunal(tribunalCodigo));
    }

    private PjbSubstituicaoFederativaTutelaColetivaTribunalResponse mapTribunal(PjbSubstituicaoFederativaTutelaColetivaTribunal tribunal) {
        return new PjbSubstituicaoFederativaTutelaColetivaTribunalResponse(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.legadoPrincipal(),
                tribunal.ondaAtual(),
                tribunal.scoreGeral(),
                tribunal.scoreTutelaColetiva(),
                tribunal.scoreDemandasEstruturais(),
                tribunal.scoreExecucaoColetiva(),
                tribunal.scoreCumprimentoMassa(),
                tribunal.prontoMalhaPrecedentes(),
                tribunal.malhaTutelaColetivaPronta(),
                tribunal.totalCompetencias(),
                tribunal.competencias().stream().map(this::mapCompetencia).toList(),
                tribunal.bloqueadores(),
                tribunal.proximasAcoes(),
                tribunal.fundamentos()
        );
    }

    private PjbSubstituicaoFederativaTutelaColetivaCompetenciaResponse mapCompetencia(PjbSubstituicaoFederativaTutelaColetivaCompetencia competencia) {
        return new PjbSubstituicaoFederativaTutelaColetivaCompetenciaResponse(
                competencia.competenciaCodigo(),
                competencia.ramoCodigo(),
                competencia.ramoNome(),
                competencia.ritoCodigo(),
                competencia.totalProcessos(),
                competencia.scoreTutelaColetiva(),
                competencia.scoreDemandasEstruturais(),
                competencia.scoreExecucaoColetiva(),
                competencia.scoreCumprimentoMassa(),
                competencia.malhaTutelaColetivaPronta(),
                competencia.tutelaColetivaAtiva(),
                competencia.demandaEstruturalAtiva(),
                competencia.execucaoColetivaAtiva(),
                competencia.cumprimentoMassaAtivo(),
                competencia.roteamentoColetivoAtivo(),
                competencia.janelaAtual(),
                competencia.guardrails(),
                competencia.fundamentos(),
                competencia.processoReferenciaId(),
                competencia.numeroReferencia()
        );
    }
}
