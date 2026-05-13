package com.tcc.pjb.backend.service.processual.painel.telemetria;

import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelTelemetriaConectorAggregate;
import com.tcc.pjb.backend.core.processo.painel.domain.ProcessoPainelTelemetriaConectorItem;
import com.tcc.pjb.backend.model.dto.processual.painel.telemetria.ProcessoPainelTelemetriaConectorItemResponse;
import com.tcc.pjb.backend.model.dto.processual.painel.telemetria.ProcessoPainelTelemetriaConectorResponse;
import org.springframework.stereotype.Service;

@Service
public class ProcessoPainelTelemetriaConectorAssembler {

    public ProcessoPainelTelemetriaConectorResponse toResponse(ProcessoPainelTelemetriaConectorAggregate aggregate) {
        return new ProcessoPainelTelemetriaConectorResponse(
                aggregate.processoId(),
                aggregate.numeroProcesso(),
                aggregate.tribunalCodigo(),
                aggregate.modoLeitura(),
                aggregate.conectores().stream().map(this::toItem).toList(),
                aggregate.alertas(),
                aggregate.geradoEm()
        );
    }

    private ProcessoPainelTelemetriaConectorItemResponse toItem(ProcessoPainelTelemetriaConectorItem item) {
        return new ProcessoPainelTelemetriaConectorItemResponse(
                item.connectorCode(),
                item.status(),
                item.accentColor(),
                item.successRate(),
                item.submissionReady(),
                item.syncReady(),
                item.fallbackMode(),
                item.cacheMode(),
                item.circuitMode(),
                item.latencyDescriptor(),
                item.latestEventAt(),
                item.latestSuccessAt(),
                item.sourceEndpoints(),
                item.blockers(),
                item.warnings()
        );
    }
}
