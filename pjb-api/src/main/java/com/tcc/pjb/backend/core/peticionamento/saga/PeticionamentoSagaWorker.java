package com.tcc.pjb.backend.core.peticionamento.saga;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.CompensarSagaPeticionamentoCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.RegistrarNoProcessoSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.NotificarPartesSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.DispararTriagemSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.GerarProtocoloSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ValidarSagaPeticionamentoCommand;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.spring.client.annotation.JobWorker;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings({"removal", "deprecation"})
public class PeticionamentoSagaWorker {

    private final PeticionamentoSagaOrchestrator orchestrator;
    private final AuditLedgerService auditLedger;

    public PeticionamentoSagaWorker(PeticionamentoSagaOrchestrator orchestrator,
                                    AuditLedgerService auditLedger) {
        this.orchestrator = Objects.requireNonNull(orchestrator);
        this.auditLedger = Objects.requireNonNull(auditLedger);
    }

    @SuppressWarnings({"removal", "deprecation"})
    @JobWorker(type = "pjb-validar-peticao")
    public Map<String, Object> validarPeticao(ActivatedJob job) {
        Long rascunhoId = longValue(job.getVariablesAsMap().get("rascunhoId"));
        var result = orchestrator.validar(new ValidarSagaPeticionamentoCommand(rascunhoId));
        auditLedger.appendSafely("SAGA_VALIDAR_OK", "PETICAO", String.valueOf(rascunhoId), "ok=" + result.ok());
        return Map.of("validacaoOk", result.ok(), "erros", result.erros());
    }

    @SuppressWarnings({"removal", "deprecation"})
    @JobWorker(type = "pjb-gerar-protocolo")
    public Map<String, Object> gerarProtocolo(ActivatedJob job) {
        Long rascunhoId = longValue(job.getVariablesAsMap().get("rascunhoId"));
        var result = orchestrator.gerarProtocolo(new GerarProtocoloSagaCommand(rascunhoId));
        auditLedger.appendSafely("SAGA_PROTOCOLO_OK", "PETICAO", String.valueOf(rascunhoId), "protocolo=" + result.numeroProtocolo());
        return Map.of(
                "numeroProtocolo", result.numeroProtocolo(),
                "dataProtocolo", result.dataProtocolo().toString(),
                "processoId", result.processoId() == null ? -1L : result.processoId(),
                "referenciaConector", result.referenciaConector() == null ? "" : result.referenciaConector()
        );
    }


    @SuppressWarnings({"removal", "deprecation"})
    @JobWorker(type = "pjb-registrar-no-processo")
    public Map<String, Object> registrarNoProcesso(ActivatedJob job) {
        Long rascunhoId = longValue(job.getVariablesAsMap().get("rascunhoId"));
        var result = orchestrator.registrarNoProcesso(new RegistrarNoProcessoSagaCommand(rascunhoId));
        auditLedger.appendSafely("SAGA_REGISTRO_OK", "PETICAO", String.valueOf(rascunhoId), "status=" + result.status());
        return Map.of("registrado", result.registrado(), "statusRegistro", result.status());
    }

    @SuppressWarnings({"removal", "deprecation"})
    @JobWorker(type = "pjb-disparar-triagem")
    public Map<String, Object> dispararTriagem(ActivatedJob job) {
        Long rascunhoId = longValue(job.getVariablesAsMap().get("rascunhoId"));
        var result = orchestrator.dispararTriagem(new DispararTriagemSagaCommand(rascunhoId));
        auditLedger.appendSafely("SAGA_TRIAGEM_OK", "PETICAO", String.valueOf(rascunhoId), "status=" + result.status());
        return Map.of("triagemDisparada", result.disparada(), "statusTriagem", result.status());
    }

    @SuppressWarnings({"removal", "deprecation"})
    @JobWorker(type = "pjb-notificar-partes")
    public Map<String, Object> notificarPartes(ActivatedJob job) {
        Long rascunhoId = longValue(job.getVariablesAsMap().get("rascunhoId"));
        var result = orchestrator.notificarPartes(new NotificarPartesSagaCommand(rascunhoId));
        auditLedger.appendSafely("SAGA_NOTIFICACAO_OK", "PETICAO", String.valueOf(rascunhoId), "status=" + result.status());
        return Map.of("partesNotificadas", result.notificadas(), "statusNotificacao", result.status());
    }

    @SuppressWarnings({"removal", "deprecation"})
    @JobWorker(type = "pjb-compensar-peticao")
    public void compensarPeticao(ActivatedJob job) {
        Long rascunhoId = longValue(job.getVariablesAsMap().get("rascunhoId"));
        orchestrator.compensar(new CompensarSagaPeticionamentoCommand(rascunhoId));
        auditLedger.appendSafely("SAGA_COMPENSACAO", "PETICAO", String.valueOf(rascunhoId));
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text.trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
