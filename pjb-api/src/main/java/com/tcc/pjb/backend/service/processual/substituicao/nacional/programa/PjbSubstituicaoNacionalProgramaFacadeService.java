package com.tcc.pjb.backend.service.processual.substituicao.nacional.programa;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoNacionalProgramaApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalOnda;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalProgramaAggregate;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.programa.PjbSubstituicaoNacionalOndaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.programa.PjbSubstituicaoNacionalProgramaResponse;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoNacionalProgramaFacadeService {

    private final PjbSubstituicaoNacionalProgramaApplicationService applicationService;

    public PjbSubstituicaoNacionalProgramaFacadeService(PjbSubstituicaoNacionalProgramaApplicationService applicationService) {
        this.applicationService = Objects.requireNonNull(applicationService);
    }

    public PjbSubstituicaoNacionalProgramaResponse avaliar() {
        PjbSubstituicaoNacionalProgramaAggregate aggregate = applicationService.avaliar();
        return new PjbSubstituicaoNacionalProgramaResponse(
                aggregate.scoreGeral(),
                aggregate.prontoOperacaoAssistida(),
                aggregate.prontoCutoverNacional(),
                aggregate.buildGateAprovado(),
                aggregate.conectoresOperacionais(),
                aggregate.conectoresBloqueados(),
                aggregate.conectoresSaudaveis(),
                aggregate.sistemasProntosProducao(),
                aggregate.ondas().stream().map(this::mapOnda).toList(),
                aggregate.pendenciasCriticas(),
                aggregate.conclusaoTecnica(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    private PjbSubstituicaoNacionalOndaResponse mapOnda(PjbSubstituicaoNacionalOnda onda) {
        return new PjbSubstituicaoNacionalOndaResponse(
                onda.codigo(),
                onda.titulo(),
                onda.status().name(),
                onda.score(),
                onda.pronta(),
                onda.objetivo(),
                onda.criteriosEntrada(),
                onda.blocosExecucao(),
                onda.guardrails(),
                onda.rollback(),
                onda.sistemasAlvo(),
                onda.proximasAcoes()
        );
    }
}
