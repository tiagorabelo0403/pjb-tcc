package com.tcc.pjb.backend.service.processual.substituicao.federativa.cutover;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaCutoverMatrixApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverMatrixAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverTribunal;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.cutover.PjbSubstituicaoFederativaCutoverCompetenciaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.cutover.PjbSubstituicaoFederativaCutoverMatrixResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.cutover.PjbSubstituicaoFederativaCutoverTribunalResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoFederativaCutoverMatrixFacadeService {

    private final PjbSubstituicaoFederativaCutoverMatrixApplicationService applicationService;

    public PjbSubstituicaoFederativaCutoverMatrixFacadeService(PjbSubstituicaoFederativaCutoverMatrixApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbSubstituicaoFederativaCutoverMatrixResponse avaliar() {
        PjbSubstituicaoFederativaCutoverMatrixAggregate aggregate = applicationService.avaliar();
        return new PjbSubstituicaoFederativaCutoverMatrixResponse(
                aggregate.scoreGeral(),
                aggregate.freezeNacionalAtivo(),
                aggregate.prontoJanelaMaterial(),
                aggregate.tribunaisLiberados(),
                aggregate.competenciasLiberadas(),
                aggregate.tribunais().stream().map(this::mapTribunal).toList(),
                aggregate.bloqueadoresCriticos(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    public PjbSubstituicaoFederativaCutoverTribunalResponse avaliarTribunal(String tribunalCodigo) {
        return mapTribunal(applicationService.avaliarTribunal(tribunalCodigo));
    }

    private PjbSubstituicaoFederativaCutoverTribunalResponse mapTribunal(PjbSubstituicaoFederativaCutoverTribunal tribunal) {
        return new PjbSubstituicaoFederativaCutoverTribunalResponse(
                tribunal.tribunalCodigo(),
                tribunal.tribunalNome(),
                tribunal.ramoJustica(),
                tribunal.legadoPrincipal(),
                tribunal.ondaAtual(),
                tribunal.scoreGeral(),
                tribunal.scoreMaterial(),
                tribunal.scoreComunicacao(),
                tribunal.scoreSigilo(),
                tribunal.scoreGovernanca(),
                tribunal.corteLiberado(),
                tribunal.freezeAtivo(),
                tribunal.janelaAtual(),
                tribunal.totalCompetencias(),
                tribunal.competencias().stream().map(this::mapCompetencia).toList(),
                tribunal.bloqueadores(),
                tribunal.fundamentos()
        );
    }

    private PjbSubstituicaoFederativaCutoverCompetenciaResponse mapCompetencia(PjbSubstituicaoFederativaCutoverCompetencia competencia) {
        return new PjbSubstituicaoFederativaCutoverCompetenciaResponse(
                competencia.ramoCodigo(),
                competencia.ramoDescricao(),
                competencia.ritoCodigo(),
                competencia.totalProcessos(),
                competencia.scoreMaterial(),
                competencia.scoreComunicacao(),
                competencia.scoreSigilo(),
                competencia.corteLiberado(),
                competencia.janelaAtual(),
                competencia.guardrails(),
                competencia.proximasAcoes(),
                competencia.processoReferenciaId(),
                competencia.numeroReferencia()
        );
    }
}
