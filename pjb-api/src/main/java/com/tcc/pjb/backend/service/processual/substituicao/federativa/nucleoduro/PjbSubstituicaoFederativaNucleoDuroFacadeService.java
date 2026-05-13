package com.tcc.pjb.backend.service.processual.substituicao.federativa.nucleoduro;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaNucleoDuroApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroTribunal;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.nucleoduro.PjbSubstituicaoFederativaNucleoDuroCompetenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.nucleoduro.PjbSubstituicaoFederativaNucleoDuroResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.nucleoduro.PjbSubstituicaoFederativaNucleoDuroTribunalResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoFederativaNucleoDuroFacadeService {

    private final PjbSubstituicaoFederativaNucleoDuroApplicationService applicationService;

    public PjbSubstituicaoFederativaNucleoDuroFacadeService(PjbSubstituicaoFederativaNucleoDuroApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbSubstituicaoFederativaNucleoDuroResponse avaliar() {
        PjbSubstituicaoFederativaNucleoDuroAggregate aggregate = applicationService.avaliar();
        return new PjbSubstituicaoFederativaNucleoDuroResponse(
                aggregate.scoreNacional(),
                aggregate.prontoNucleoDuro(),
                aggregate.comunicacaoSigiloConectados(),
                aggregate.prevencaoRedistribuicaoConectadas(),
                aggregate.fluxoRecursalConectado(),
                aggregate.tribunaisProntosNucleoDuro(),
                aggregate.tribunais().stream().map(this::mapTribunal).toList(),
                aggregate.bloqueadoresCriticos(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public PjbSubstituicaoFederativaNucleoDuroTribunalResponse avaliarTribunal(String tribunalCodigo) {
        return mapTribunal(applicationService.avaliarTribunal(tribunalCodigo));
    }

    private PjbSubstituicaoFederativaNucleoDuroTribunalResponse mapTribunal(PjbSubstituicaoFederativaNucleoDuroTribunal tribunal) {
        return new PjbSubstituicaoFederativaNucleoDuroTribunalResponse(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.legadoPrincipal(),
                tribunal.ondaAtual(),
                tribunal.scoreGeral(),
                tribunal.scoreComunicacaoSigilo(),
                tribunal.scorePrevencaoRedistribuicao(),
                tribunal.scoreFluxoRecursal(),
                tribunal.scoreInfraestrutura(),
                tribunal.prontoCutover(),
                tribunal.prontoNucleoDuro(),
                tribunal.prevencaoAtiva(),
                tribunal.redistribuicaoAssistida(),
                tribunal.fluxoRecursalPronto(),
                tribunal.totalCompetencias(),
                tribunal.competencias().stream().map(this::mapCompetencia).toList(),
                tribunal.bloqueadores(),
                tribunal.proximasAcoes(),
                tribunal.fundamentos()
        );
    }

    private PjbSubstituicaoFederativaNucleoDuroCompetenciaResponse mapCompetencia(PjbSubstituicaoFederativaNucleoDuroCompetencia competencia) {
        return new PjbSubstituicaoFederativaNucleoDuroCompetenciaResponse(
                competencia.ramoCodigo(),
                competencia.ramoDescricao(),
                competencia.ritoCodigo(),
                competencia.totalProcessos(),
                competencia.scoreComunicacaoSigilo(),
                competencia.scorePrevencao(),
                competencia.scoreRedistribuicao(),
                competencia.scoreFluxoRecursal(),
                competencia.prontoNucleoDuro(),
                competencia.unidadePreventa(),
                competencia.janelaAtual(),
                competencia.guardrails(),
                competencia.fundamentos(),
                competencia.processoReferenciaId(),
                competencia.numeroReferencia()
        );
    }
}
