package com.tcc.pjb.backend.service.processual.substituicao.federativa.malhajulgadora;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaMalhaJulgadoraApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraUnidade;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.malhajulgadora.PjbSubstituicaoFederativaMalhaJulgadoraResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.malhajulgadora.PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.malhajulgadora.PjbSubstituicaoFederativaMalhaJulgadoraUnidadeResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoFederativaMalhaJulgadoraFacadeService {

    private final PjbSubstituicaoFederativaMalhaJulgadoraApplicationService applicationService;

    public PjbSubstituicaoFederativaMalhaJulgadoraFacadeService(PjbSubstituicaoFederativaMalhaJulgadoraApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbSubstituicaoFederativaMalhaJulgadoraResponse avaliar() {
        PjbSubstituicaoFederativaMalhaJulgadoraAggregate aggregate = applicationService.avaliar();
        return new PjbSubstituicaoFederativaMalhaJulgadoraResponse(
                aggregate.scoreNacional(),
                aggregate.malhaJulgadoraPronta(),
                aggregate.incidentesConectados(),
                aggregate.colegiadosConectados(),
                aggregate.unidadesJulgadorasConectadas(),
                aggregate.tribunaisProntos(),
                aggregate.tribunais().stream().map(this::mapTribunal).toList(),
                aggregate.bloqueadoresCriticos(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse avaliarTribunal(String tribunalCodigo) {
        return mapTribunal(applicationService.avaliarTribunal(tribunalCodigo));
    }

    private PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse mapTribunal(PjbSubstituicaoFederativaMalhaJulgadoraTribunal tribunal) {
        return new PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.legadoPrincipal(),
                tribunal.ondaAtual(),
                tribunal.scoreGeral(),
                tribunal.scoreIncidentes(),
                tribunal.scoreColegiados(),
                tribunal.scoreUnidadesJulgadoras(),
                tribunal.prontoNucleoDuro(),
                tribunal.malhaJulgadoraPronta(),
                tribunal.totalUnidades(),
                tribunal.unidades().stream().map(this::mapUnidade).toList(),
                tribunal.bloqueadores(),
                tribunal.proximasAcoes(),
                tribunal.fundamentos()
        );
    }

    private PjbSubstituicaoFederativaMalhaJulgadoraUnidadeResponse mapUnidade(PjbSubstituicaoFederativaMalhaJulgadoraUnidade unidade) {
        return new PjbSubstituicaoFederativaMalhaJulgadoraUnidadeResponse(
                unidade.unidadeCodigo(),
                unidade.unidadeNome(),
                unidade.ramoCodigo(),
                unidade.ritoCodigo(),
                unidade.totalProcessos(),
                unidade.scoreIncidentes(),
                unidade.scoreColegiado(),
                unidade.scorePrevencaoRedistribuicao(),
                unidade.malhaJulgadoraPronta(),
                unidade.possuiIncidenteAtivo(),
                unidade.possuiColegiadoAtivo(),
                unidade.janelaAtual(),
                unidade.guardrails(),
                unidade.fundamentos(),
                unidade.processoReferenciaId(),
                unidade.numeroReferencia()
        );
    }
}
