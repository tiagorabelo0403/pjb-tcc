package com.tcc.pjb.backend.service.processual.painel.previdenciario;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelPrevidenciarioFonte;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelPrevidenciarioTrilhoAggregate;
import com.tcc.pjb.backend.model.dto.processual.painel.previdenciario.ProcessoPainelPrevidenciarioFonteResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.previdenciario.ProcessoPainelPrevidenciarioTrilhoResponse;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelPrevidenciarioTrilhoAssembler {

    public ProcessoPainelPrevidenciarioTrilhoResponse toResponse(ProcessoPainelPrevidenciarioTrilhoAggregate aggregate) {
        return new ProcessoPainelPrevidenciarioTrilhoResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.aplicavel(),
                aggregate.statusGeral(),
                aggregate.recomendacaoCnis(),
                aggregate.filaPericialStatus(),
                aggregate.pagamentoStatus(),
                aggregate.fontes().stream().map(this::toFonte).toList(),
                aggregate.alertas(),
                aggregate.proximosPassos(),
                aggregate.geradoEm()
        );
    }

    private ProcessoPainelPrevidenciarioFonteResponse toFonte(ProcessoPainelPrevidenciarioFonte fonte) {
        return new ProcessoPainelPrevidenciarioFonteResponse(
                fonte.code(),
                fonte.title(),
                fonte.status(),
                fonte.readyForUse(),
                fonte.fallbackMode(),
                fonte.signal()
        );
    }
}
