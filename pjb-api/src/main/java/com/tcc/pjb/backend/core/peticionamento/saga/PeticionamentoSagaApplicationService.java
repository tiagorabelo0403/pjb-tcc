package com.tcc.pjb.backend.core.peticionamento.saga;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.CompensarSagaPeticionamentoCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.DispararTriagemSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.GerarProtocoloSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.NotificarPartesSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.RegistrarNoProcessoSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaAuditQuery;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCompensationQuery;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaHealthQuery;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepQuery;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ValidarSagaPeticionamentoCommand;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PeticionamentoSagaApplicationService {

    private final PeticionamentoSagaOrchestrator orchestrator;
    private final AuditLedgerService auditLedgerService;

    public PeticionamentoSagaApplicationService(PeticionamentoSagaOrchestrator orchestrator,
                                                AuditLedgerService auditLedgerService) {
        this.orchestrator = Objects.requireNonNull(orchestrator);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.ValidacaoSagaPeticionamentoResult validar(Long rascunhoId) {
        return orchestrator.validar(new ValidarSagaPeticionamentoCommand(requireId(rascunhoId)));
    }

    @Transactional
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.ProtocoloSagaPeticionamentoResult gerarProtocolo(Long rascunhoId) {
        var result = orchestrator.gerarProtocolo(new GerarProtocoloSagaCommand(requireId(rascunhoId)));
        auditLedgerService.appendSafely("SAGA_PROTOCOLO_MANUAL", "PETICAO", String.valueOf(rascunhoId), null, result.numeroProtocolo());
        return result;
    }

    @Transactional
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.RegistrarNoProcessoSagaResult registrarNoProcesso(Long rascunhoId) {
        return orchestrator.registrarNoProcesso(new RegistrarNoProcessoSagaCommand(requireId(rascunhoId)));
    }

    @Transactional
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.DispararTriagemSagaResult dispararTriagem(Long rascunhoId) {
        return orchestrator.dispararTriagem(new DispararTriagemSagaCommand(requireId(rascunhoId)));
    }

    @Transactional
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.NotificarPartesSagaResult notificarPartes(Long rascunhoId) {
        return orchestrator.notificarPartes(new NotificarPartesSagaCommand(requireId(rascunhoId)));
    }

    @Transactional
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.CompensacaoSagaPeticionamentoResult compensar(Long rascunhoId) {
        return orchestrator.compensar(new CompensarSagaPeticionamentoCommand(requireId(rascunhoId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaHealthResult health(Long rascunhoId) {
        return orchestrator.health(new SagaHealthQuery(requireId(rascunhoId)));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionTimeline timeline(Long rascunhoId) {
        Long requiredId = requireId(rascunhoId);
        var timeline = orchestrator.executionTimeline(requiredId);
        auditLedgerService.appendSafely("SAGA_TIMELINE_QUERY", "PETICAO", String.valueOf(requiredId), null, "steps=" + timeline.steps().size());
        return timeline;
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionHealthSnapshot executionHealth(Long rascunhoId) {
        return orchestrator.executionHealth(requireId(rascunhoId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionAuditView audit(Long rascunhoId) {
        return orchestrator.executionAuditView(requireId(rascunhoId));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepResult step(Long rascunhoId, String etapa) {
        return orchestrator.stepResult(new SagaStepQuery(requireId(rascunhoId), etapa));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCompensationAuditSnapshot compensation(Long rascunhoId) {
        return orchestrator.compensation(new SagaCompensationQuery(requireId(rascunhoId), null));
    }

    private Long requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("rascunhoId obrigatorio");
        }
        return id;
    }
}
