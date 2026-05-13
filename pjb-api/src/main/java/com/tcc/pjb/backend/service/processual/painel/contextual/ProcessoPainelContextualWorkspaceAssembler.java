package com.tcc.pjb.backend.service.processual.painel.contextual;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelConectorSaude;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelContextualAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelContextualWidget;
import com.tcc.pjb.backend.model.dto.processual.painel.contextual.ProcessoPainelConectorSaudeResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.contextual.ProcessoPainelContextualResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.contextual.ProcessoPainelContextualWidgetResponse;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelContextualWorkspaceAssembler {

    public ProcessoPainelContextualResponse toResponse(ProcessoPainelContextualAggregate aggregate) {
        return new ProcessoPainelContextualResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.ramoDireito(),
                aggregate.painelCodigo(),
                aggregate.painelTitulo(),
                aggregate.perfilCodigo(),
                aggregate.widgets().stream().map(this::toWidget).toList(),
                aggregate.conectores().stream().map(this::toConectorSaude).toList(),
                aggregate.fundamentos(),
                aggregate.geradoEm()
        );
    }

    private ProcessoPainelContextualWidgetResponse toWidget(ProcessoPainelContextualWidget widget) {
        return new ProcessoPainelContextualWidgetResponse(
                widget.code(),
                widget.title(),
                widget.kind(),
                widget.status(),
                widget.accentColor(),
                widget.headline(),
                widget.subtitle(),
                widget.insights(),
                widget.navigationPath()
        );
    }

    private ProcessoPainelConectorSaudeResponse toConectorSaude(ProcessoPainelConectorSaude conector) {
        return new ProcessoPainelConectorSaudeResponse(
                conector.systemCode(),
                conector.status(),
                conector.accentColor(),
                conector.successRate(),
                conector.submissionReady(),
                conector.syncReady(),
                conector.latestEventAt(),
                conector.blockers(),
                conector.warnings()
        );
    }
}
