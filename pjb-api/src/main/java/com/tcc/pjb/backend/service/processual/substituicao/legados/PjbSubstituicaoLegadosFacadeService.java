package com.tcc.pjb.backend.service.processual.substituicao.legados;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoLegadosApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosProva;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoLegadosSistema;
import com.tcc.pjb.backend.model.dto.processual.substituicao.legados.PjbSubstituicaoLegadosProvaResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.legados.PjbSubstituicaoLegadosResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.legados.PjbSubstituicaoLegadosSistemaResponse;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class PjbSubstituicaoLegadosFacadeService {

    private final PjbSubstituicaoLegadosApplicationService pjbSubstituicaoLegadosApplicationService;

    public PjbSubstituicaoLegadosFacadeService(PjbSubstituicaoLegadosApplicationService pjbSubstituicaoLegadosApplicationService) {
        this.pjbSubstituicaoLegadosApplicationService = Objects.requireNonNull(pjbSubstituicaoLegadosApplicationService);
    }

    public PjbSubstituicaoLegadosResponse avaliar(Long processoId) {
        PjbSubstituicaoLegadosAggregate aggregate = pjbSubstituicaoLegadosApplicationService.avaliar(processoId);
        return new PjbSubstituicaoLegadosResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.scoreGeral(),
                aggregate.prontoSubstituicaoImediata(),
                aggregate.conclusaoTecnica(),
                aggregate.provas().stream().map(this::mapProva).toList(),
                aggregate.sistemas().stream().map(this::mapSistema).toList(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    private PjbSubstituicaoLegadosProvaResponse mapProva(PjbSubstituicaoLegadosProva prova) {
        return new PjbSubstituicaoLegadosProvaResponse(prova.codigo(), prova.titulo(), prova.status().name(), prova.score(), prova.concluida(), prova.fundamentos(), prova.bloqueios());
    }

    private PjbSubstituicaoLegadosSistemaResponse mapSistema(PjbSubstituicaoLegadosSistema sistema) {
        return new PjbSubstituicaoLegadosSistemaResponse(sistema.sistema(), sistema.status().name(), sistema.scoreAderencia(), sistema.conclusao(), sistema.pendencias());
    }
}
