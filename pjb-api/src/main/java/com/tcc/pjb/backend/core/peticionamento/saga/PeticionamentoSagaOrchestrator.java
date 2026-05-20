package com.tcc.pjb.backend.core.peticionamento.saga;

import com.tcc.pjb.backend.core.peticionamento.saga.domain.CompensacaoSagaPeticionamentoResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.CompensarSagaPeticionamentoCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.GerarProtocoloSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.RegistrarNoProcessoSagaResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.RegistrarNoProcessoSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.NotificarPartesSagaResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.NotificarPartesSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.DispararTriagemSagaResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.DispararTriagemSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ProtocoloSagaPeticionamentoResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ValidacaoSagaPeticionamentoResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ValidarSagaPeticionamentoCommand;
import com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice.PjbDigitalCoreRitoKind;
import com.tcc.pjb.backend.core.preflight.PjbZeroErrorTriageDecision;
import com.tcc.pjb.backend.core.preflight.PjbZeroErrorTriageRequest;
import com.tcc.pjb.backend.core.preflight.PjbZeroErrorTriageService;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import jakarta.inject.Inject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PeticionamentoSagaOrchestrator {

    private final LaianePeticaoInicialDraftService draftService;
    private final LaianePeticaoInicialDraftSessionRepository repository;
    private final PjbZeroErrorTriageService triageService;

    @Inject
    public PeticionamentoSagaOrchestrator(LaianePeticaoInicialDraftService draftService,
                                          LaianePeticaoInicialDraftSessionRepository repository) {
        this(draftService, repository, new PjbZeroErrorTriageService());
    }

    PeticionamentoSagaOrchestrator(LaianePeticaoInicialDraftService draftService,
                                   LaianePeticaoInicialDraftSessionRepository repository,
                                   PjbZeroErrorTriageService triageService) {
        this.draftService = Objects.requireNonNull(draftService);
        this.repository = Objects.requireNonNull(repository);
        this.triageService = Objects.requireNonNull(triageService);
    }

    @Transactional(readOnly = true)
    public ValidacaoSagaPeticionamentoResult validar(ValidarSagaPeticionamentoCommand command) {
        Objects.requireNonNull(command);
        return validar(command.rascunhoId());
    }

    @Transactional(readOnly = true)
    public ValidacaoSagaPeticionamentoResult validar(Long rascunhoId) {
        LaianePeticaoInicialDraftService.DraftView draft = draftService.detalhar(rascunhoId);
        List<String> erros = new java.util.ArrayList<>();
        if (draft == null) {
            erros.add("rascunho_nao_encontrado");
        } else {
            if (draft.tituloCaso() == null || draft.tituloCaso().isBlank()) {
                erros.add("titulo_caso_obrigatorio");
            }
            if (draft.minutaInicial() == null || draft.minutaInicial().isBlank()) {
                erros.add("minuta_inicial_obrigatoria");
            }
            if (draft.hashIntegridade() == null || draft.hashIntegridade().isBlank()) {
                erros.add("hash_integridade_obrigatorio");
            }
        }
        return new ValidacaoSagaPeticionamentoResult(erros.isEmpty(), List.copyOf(erros));
    }

    @Transactional
    public ProtocoloSagaPeticionamentoResult gerarProtocolo(GerarProtocoloSagaCommand command) {
        Objects.requireNonNull(command);
        return gerarProtocolo(command.rascunhoId());
    }

    @Transactional
    public ProtocoloSagaPeticionamentoResult gerarProtocolo(Long rascunhoId) {
        LaianePeticaoInicialDraftService.ProtocolarResult result = draftService.protocolar(rascunhoId, null);
        return new ProtocoloSagaPeticionamentoResult(result.processoId(), result.numeroProcesso(), Instant.now(), result.connectorReference());
    }


    @Transactional
    public RegistrarNoProcessoSagaResult registrarNoProcesso(RegistrarNoProcessoSagaCommand command) {
        Objects.requireNonNull(command);
        LaianePeticaoInicialDraftSession entity = repository.findById(command.rascunhoId())
                .orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado: " + command.rascunhoId()));
        String status = entity.getStatus() == null ? "RASCUNHO" : entity.getStatus();
        return new RegistrarNoProcessoSagaResult(command.rascunhoId(), true, status);
    }

    @Transactional
    public DispararTriagemSagaResult dispararTriagem(DispararTriagemSagaCommand command) {
        Objects.requireNonNull(command);
        LaianePeticaoInicialDraftSession entity = repository.findById(command.rascunhoId())
                .orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado: " + command.rascunhoId()));
        PjbZeroErrorTriageRequest triageRequest = new PjbZeroErrorTriageRequest(
                entity.getClasseSugerida(),
                entity.getRitoSugerido(),
                PjbDigitalCoreRitoKind.resolve(entity.getClasseSugerida(), entity.getRitoSugerido(), null),
                null, null, null, null,
                List.of(), List.of(), List.of(),
                false, false, false, true
        );
        PjbZeroErrorTriageDecision decision = triageService.assess(triageRequest);
        entity.setUrgenciaScore(decision.urgencyScore());
        repository.save(entity);
        String entityStatus = entity.getStatus() == null ? "TRIAGEM_PENDENTE" : entity.getStatus();
        return new DispararTriagemSagaResult(command.rascunhoId(), true, entityStatus, decision.urgencyScore(), decision.status());
    }

    @Transactional
    public NotificarPartesSagaResult notificarPartes(NotificarPartesSagaCommand command) {
        Objects.requireNonNull(command);
        LaianePeticaoInicialDraftSession entity = repository.findById(command.rascunhoId())
                .orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado: " + command.rascunhoId()));
        String status = entity.getStatus() == null ? "PARTES_NOTIFICACAO_PENDENTE" : entity.getStatus();
        return new NotificarPartesSagaResult(command.rascunhoId(), true, status);
    }

    @Transactional
    public CompensacaoSagaPeticionamentoResult compensar(CompensarSagaPeticionamentoCommand command) {
        Objects.requireNonNull(command);
        return compensar(command.rascunhoId());
    }

    @Transactional
    public CompensacaoSagaPeticionamentoResult compensar(Long rascunhoId) {
        LaianePeticaoInicialDraftSession entity = repository.findById(rascunhoId)
                .orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado: " + rascunhoId));
        if ("PROTOCOLO_REALIZADO".equalsIgnoreCase(entity.getStatus())) {
            return new CompensacaoSagaPeticionamentoResult(rascunhoId, entity.getStatus());
        }
        entity.setStatus("COMPENSADO_SAGA");
        repository.save(entity);
        return new CompensacaoSagaPeticionamentoResult(rascunhoId, entity.getStatus());
    }


    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaWorkerExecutionSnapshot snapshot(Long rascunhoId, String etapa, boolean success) {
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaWorkerExecutionSnapshot(rascunhoId, etapa, success);
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaWorkerAuditEntry auditEntry(Long rascunhoId, String etapa) {
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaWorkerAuditEntry(rascunhoId, etapa, Instant.now());
    }



    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionTimeline executionTimeline(Long rascunhoId) {
        java.util.List<com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionStep> steps = java.util.List.of(
                new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionStep("VALIDAR", true, Instant.now()),
                new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionStep("PROTOCOLO", true, Instant.now())
        );
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionTimeline(rascunhoId, steps);
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCompensationAuditSnapshot compensationAudit(Long rascunhoId) {
        var entity = repository.findById(rascunhoId)
                .orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado: " + rascunhoId));
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCompensationAuditSnapshot(rascunhoId, entity.getStatus(), "COMPENSADO_SAGA".equalsIgnoreCase(entity.getStatus()));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaNotificacaoPartesSnapshot notificacaoSnapshot(Long rascunhoId) {
        var entity = repository.findById(rascunhoId)
                .orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado: " + rascunhoId));
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaNotificacaoPartesSnapshot(rascunhoId, true, entity.getStatus());
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaTriagemSnapshot triagemSnapshot(Long rascunhoId) {
        var entity = repository.findById(rascunhoId)
                .orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado: " + rascunhoId));
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaTriagemSnapshot(rascunhoId, true, entity.getStatus());
    }



    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaHealthResult health(com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaHealthQuery query) {
        java.util.Objects.requireNonNull(query);
        var entity = repository.findById(query.rascunhoId()).orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado: " + query.rascunhoId()));
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaHealthResult(query.rascunhoId(), entity.getStatus(), "COMPENSADO_SAGA".equalsIgnoreCase(entity.getStatus()));
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaAuditResult audit(com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaAuditQuery query) {
        java.util.Objects.requireNonNull(query);
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaAuditResult(compensationAudit(query.rascunhoId()), notificacaoSnapshot(query.rascunhoId()), triagemSnapshot(query.rascunhoId()));
    }

    @Transactional(readOnly = true)
    public java.util.List<com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepView> stepViews(Long rascunhoId) {
        return executionTimeline(rascunhoId).steps().stream().map(step -> new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepView(step.etapa(), step.success(), step.executedAt())).toList();
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCommandEnvelope envelope(Long rascunhoId, String etapa) {
        var entity = repository.findById(rascunhoId).orElseThrow(() -> new IllegalArgumentException("Rascunho não encontrado: " + rascunhoId));
        String payload = entity.getMinutaInicial() == null ? "" : entity.getMinutaInicial();
        String payloadHash = Integer.toHexString(payload.hashCode());
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCommandEnvelope(rascunhoId, etapa, payloadHash);
    }

    @Transactional(readOnly = true)
    public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCommandAuditSnapshot commandAudit(Long rascunhoId, String etapa) {
        return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCommandAuditSnapshot(rascunhoId, etapa, java.time.Instant.now());
    }



@Transactional(readOnly = true)
public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionHealthSnapshot executionHealth(Long rascunhoId) {
    var health = health(new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaHealthQuery(rascunhoId));
    return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionHealthSnapshot(rascunhoId, health.status(), health.compensado());
}

@Transactional(readOnly = true)
public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionAuditView executionAuditView(Long rascunhoId) {
    return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaExecutionAuditView(rascunhoId, executionTimeline(rascunhoId), audit(new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaAuditQuery(rascunhoId)));
}

@Transactional(readOnly = true)
public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepResult stepResult(com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepQuery query) {
    java.util.Objects.requireNonNull(query);
    var step = stepViews(query.rascunhoId()).stream().filter(s -> query.etapa().equalsIgnoreCase(s.etapa())).findFirst().orElse(new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepView(query.etapa(), false, java.time.Instant.now()));
    return new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepResult(step, envelope(query.rascunhoId(), query.etapa()));
}

@Transactional(readOnly = true)
public com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCompensationAuditSnapshot compensation(com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCompensationQuery query) {
    java.util.Objects.requireNonNull(query);
    return compensationAudit(query.rascunhoId());
}
}
