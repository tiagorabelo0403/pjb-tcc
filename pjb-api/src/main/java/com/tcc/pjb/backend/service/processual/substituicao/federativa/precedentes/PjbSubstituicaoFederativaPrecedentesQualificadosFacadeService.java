package com.tcc.pjb.backend.service.processual.substituicao.federativa.precedentes;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosTribunal;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.precedentes.PjbSubstituicaoFederativaPrecedentesQualificadosCompetenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.precedentes.PjbSubstituicaoFederativaPrecedentesQualificadosResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.precedentes.PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoFederativaPrecedentesQualificadosFacadeService {

    private final PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService applicationService;

    public PjbSubstituicaoFederativaPrecedentesQualificadosFacadeService(PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbSubstituicaoFederativaPrecedentesQualificadosResponse avaliar() {
        PjbSubstituicaoFederativaPrecedentesQualificadosAggregate aggregate = applicationService.avaliar();
        return new PjbSubstituicaoFederativaPrecedentesQualificadosResponse(
                aggregate.scoreNacional(),
                aggregate.malhaPrecedentesPronta(),
                aggregate.incidentesMassaConectados(),
                aggregate.temasAfetadosGovernados(),
                aggregate.sobrestamentoGovernado(),
                aggregate.precedentesVinculantesConectados(),
                aggregate.tribunaisProntos(),
                aggregate.tribunais().stream().map(this::mapTribunal).toList(),
                aggregate.bloqueadoresCriticos(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse avaliarTribunal(String tribunalCodigo) {
        return mapTribunal(applicationService.avaliarTribunal(tribunalCodigo));
    }

    private PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse mapTribunal(PjbSubstituicaoFederativaPrecedentesQualificadosTribunal tribunal) {
        return new PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.legadoPrincipal(),
                tribunal.ondaAtual(),
                tribunal.scoreGeral(),
                tribunal.scoreIncidentesMassa(),
                tribunal.scoreTemasAfetados(),
                tribunal.scoreSobrestamento(),
                tribunal.scorePrecedentesVinculantes(),
                tribunal.prontoMalhaJulgadora(),
                tribunal.malhaPrecedentesPronta(),
                tribunal.totalCompetencias(),
                tribunal.competencias().stream().map(this::mapCompetencia).toList(),
                tribunal.bloqueadores(),
                tribunal.proximasAcoes(),
                tribunal.fundamentos()
        );
    }

    private PjbSubstituicaoFederativaPrecedentesQualificadosCompetenciaResponse mapCompetencia(PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia competencia) {
        return new PjbSubstituicaoFederativaPrecedentesQualificadosCompetenciaResponse(
                competencia.competenciaCodigo(),
                competencia.ramoCodigo(),
                competencia.ramoNome(),
                competencia.ritoCodigo(),
                competencia.totalProcessos(),
                competencia.scoreIncidentesMassa(),
                competencia.scoreAfetacao(),
                competencia.scoreSobrestamento(),
                competencia.scorePrecedentesVinculantes(),
                competencia.malhaPrecedentesPronta(),
                competencia.incidenteMassaAtivo(),
                competencia.afetacaoAtiva(),
                competencia.sobrestamentoAtivo(),
                competencia.precedenteVinculanteAtivo(),
                competencia.painelDemandasRepetitivasAtivo(),
                competencia.janelaAtual(),
                competencia.guardrails(),
                competencia.fundamentos(),
                competencia.processoReferenciaId(),
                competencia.numeroReferencia()
        );
    }
}
