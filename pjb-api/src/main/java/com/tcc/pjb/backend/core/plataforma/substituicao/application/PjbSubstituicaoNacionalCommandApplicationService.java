package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.governance.idempotency.IdempotencyInProgressException;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyBeginResult;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.jobs.domain.JobType;
import com.tcc.pjb.backend.core.jobs.runtime.JobAdminService;
import com.tcc.pjb.backend.core.jobs.runtime.JobCommandService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoControleAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoModo;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalExecucaoAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEventoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoEventoRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoCommandRequest;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoCommandResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoControleRequest;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
public class PjbSubstituicaoNacionalCommandApplicationService {

    private final PjbSubstituicaoNacionalExecucaoRepository repository;
    private final PjbSubstituicaoNacionalExecucaoEventoRepository eventoRepository;
    private final PjbSubstituicaoNacionalExecutionQueryApplicationService queryApplicationService;
    private final RequestIdempotencyService requestIdempotencyService;
    private final CanonicalJsonHasher canonicalJsonHasher;
    private final JobCommandService jobCommandService;
    private final JobAdminService jobAdminService;
    private final ObjectMapper objectMapper;

    public PjbSubstituicaoNacionalCommandApplicationService(PjbSubstituicaoNacionalExecucaoRepository repository,
                                                            PjbSubstituicaoNacionalExecucaoEventoRepository eventoRepository,
                                                            PjbSubstituicaoNacionalExecutionQueryApplicationService queryApplicationService,
                                                            RequestIdempotencyService requestIdempotencyService,
                                                            CanonicalJsonHasher canonicalJsonHasher,
                                                            JobCommandService jobCommandService,
                                                            JobAdminService jobAdminService,
                                                            ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.eventoRepository = Objects.requireNonNull(eventoRepository);
        this.queryApplicationService = Objects.requireNonNull(queryApplicationService);
        this.requestIdempotencyService = Objects.requireNonNull(requestIdempotencyService);
        this.canonicalJsonHasher = Objects.requireNonNull(canonicalJsonHasher);
        this.jobCommandService = Objects.requireNonNull(jobCommandService);
        this.jobAdminService = Objects.requireNonNull(jobAdminService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional
    public PjbSubstituicaoNacionalExecucaoCommandResponse submeter(PjbSubstituicaoNacionalExecucaoCommandRequest request,
                                                                   String requestedBy,
                                                                   String externalIdempotencyKey) {
        SubmissionEnvelope envelope = normalize(request, requestedBy);
        String requestHash = canonicalJsonHasher.fingerprint(envelope).sha256();
        RequestIdempotencyBeginResult begin;
        try {
            begin = requestIdempotencyService.begin("PJB_SUBSTITUICAO_NACIONAL_EXECUCAO", requestHash, Duration.ofMinutes(5));
        } catch (IdempotencyInProgressException ex) {
            PjbSubstituicaoNacionalExecucaoEntity existing = repository.findByRequestHash(requestHash).orElse(null);
            if (existing != null) {
                return new PjbSubstituicaoNacionalExecucaoCommandResponse(existing.getId(), existing.getTribunalCodigo(), existing.getAcao(), existing.getSituacao(), existing.getJobId(), false, true, existing.getCorrelationId(), Instant.now());
            }
            throw ex;
        }
        if (begin.isCompleted() && begin.resourceId() != null) {
            Long execucaoId = Long.parseLong(begin.resourceId());
            PjbSubstituicaoNacionalExecucaoAggregate aggregate = queryApplicationService.detalhar(execucaoId);
            return new PjbSubstituicaoNacionalExecucaoCommandResponse(aggregate.execucaoId(), aggregate.tribunalCodigo(), aggregate.acao(), aggregate.situacao(), aggregate.jobId(), true, false, aggregate.correlationId(), Instant.now());
        }
        PjbSubstituicaoNacionalExecucaoEntity existing = repository.findByRequestHash(requestHash).orElse(null);
        if (existing != null) {
            return new PjbSubstituicaoNacionalExecucaoCommandResponse(existing.getId(), existing.getTribunalCodigo(), existing.getAcao(), existing.getSituacao(), existing.getJobId(), !begin.created(), begin.isInProgress(), existing.getCorrelationId(), Instant.now());
        }
        try {
            String correlationId = RequestContext.getRequestId().filter(value -> !value.isBlank()).orElseGet(() -> UUID.randomUUID().toString());
            PjbSubstituicaoNacionalExecucaoEntity entity = new PjbSubstituicaoNacionalExecucaoEntity(
                    envelope.tribunal().codigo(),
                    envelope.tribunal().nome(),
                    envelope.tribunal().ramo().name(),
                    envelope.request().acao(),
                    envelope.modoExecucao(),
                    envelope.dryRun(),
                    requestHash,
                    envelope.requestedBy(),
                    envelope.request().justificativa(),
                    envelope.request().ondaAlvo(),
                    toJson(payloadMap(envelope, externalIdempotencyKey))
            );
            repository.save(entity);
            eventoRepository.save(new PjbSubstituicaoNacionalExecucaoEventoEntity(entity, "COMANDO_RECEBIDO", "INFO", PjbSubstituicaoExecucaoFase.RECEPCAO, "Comando de substituição nacional recebido.", toJson(payloadMap(envelope, externalIdempotencyKey))));
            String jobIdempotencyKey = externalIdempotencyKey == null || externalIdempotencyKey.isBlank() ? requestHash : externalIdempotencyKey.trim() + ":" + requestHash;
            JobCommandService.JobCreateResult jobCreateResult = jobCommandService.createIdempotent(
                    JobType.PJB_SUBSTITUICAO_NACIONAL_EXECUCAO,
                    "pjb-substituicao-nacional:" + envelope.tribunal().codigo(),
                    envelope.requestedBy(),
                    jobIdempotencyKey,
                    new PjbSubstituicaoNacionalExecutionJobHandler.JobInput(entity.getId()),
                    priorityFor(envelope),
                    5
            );
            entity.enfileirar(jobCreateResult.jobId(), correlationId);
            repository.save(entity);
            eventoRepository.save(new PjbSubstituicaoNacionalExecucaoEventoEntity(entity, "JOB_ENFILEIRADO", "INFO", PjbSubstituicaoExecucaoFase.RECEPCAO, "Execução enfileirada para orquestração governada.", toJson(Map.of("jobId", jobCreateResult.jobId(), "replay", jobCreateResult.replay(), "inProgress", jobCreateResult.inProgress()))));
            Map<String, Object> responsePayload = new LinkedHashMap<>();
            responsePayload.put("execucaoId", entity.getId());
            responsePayload.put("tribunalCodigo", entity.getTribunalCodigo());
            responsePayload.put("acao", entity.getAcao().name());
            responsePayload.put("situacao", entity.getSituacao().name());
            responsePayload.put("jobId", entity.getJobId() != null ? entity.getJobId().toString() : null);
            responsePayload.put("correlationId", correlationId);
            requestIdempotencyService.complete(requestHash, "PJB_SUBSTITUICAO_EXECUCAO", String.valueOf(entity.getId()), canonicalJsonHasher.fingerprint(responsePayload).sha256(), toJson(responsePayload));
            return new PjbSubstituicaoNacionalExecucaoCommandResponse(entity.getId(), entity.getTribunalCodigo(), entity.getAcao(), entity.getSituacao(), entity.getJobId(), jobCreateResult.replay(), jobCreateResult.inProgress(), correlationId, Instant.now());
        } catch (RuntimeException ex) {
            requestIdempotencyService.fail(requestHash);
            throw ex;
        }
    }

    @Transactional
    public PjbSubstituicaoNacionalExecucaoAggregate controlar(Long execucaoId, PjbSubstituicaoNacionalExecucaoControleRequest request) {
        PjbSubstituicaoNacionalExecucaoEntity entity = repository.findLockedById(execucaoId)
                .orElseThrow(() -> new IllegalArgumentException("Execução de substituição nacional não encontrada: " + execucaoId));
        if (entity.getJobId() == null) {
            throw new IllegalStateException("Execução sem job associado para controle operacional.");
        }
        PjbSubstituicaoExecucaoControleAcao controleAcao = request.acao();
        switch (controleAcao) {
            case PAUSAR_JOB -> jobAdminService.pause(entity.getJobId(), request.motivo());
            case RETOMAR_JOB -> jobAdminService.resume(entity.getJobId());
            case FORCAR_REPROCESSAMENTO -> {
                jobAdminService.forceRetry(entity.getJobId());
                entity.reencaminhar();
                repository.save(entity);
            }
        }
        eventoRepository.save(new PjbSubstituicaoNacionalExecucaoEventoEntity(entity, "CONTROLE_APLICADO", "WARN", entity.getFaseAtual(), "Controle operacional aplicado à execução.", toJson(Map.of("acao", controleAcao.name(), "motivo", Objects.toString(request.motivo(), ""), "jobId", entity.getJobId()))));
        return queryApplicationService.detalhar(execucaoId);
    }

    private SubmissionEnvelope normalize(PjbSubstituicaoNacionalExecucaoCommandRequest request, String requestedBy) {
        String tribunalCodigo = Objects.toString(request.tribunalCodigo(), "").trim().toUpperCase();
        NationalCompetenceMatrix tribunal = NationalCompetenceMatrix.porCodigo(tribunalCodigo)
                .orElseThrow(() -> new IllegalArgumentException("Tribunal não mapeado para substituição nacional: " + tribunalCodigo));
        PjbSubstituicaoExecucaoModo modoExecucao = request.modoExecucao() == null ? PjbSubstituicaoExecucaoModo.ASSISTIDA : request.modoExecucao();
        boolean dryRun = request.dryRun() == null || request.dryRun();
        String normalizedRequestedBy = requestedBy == null || requestedBy.isBlank() ? "sistema" : requestedBy.trim();
        return new SubmissionEnvelope(request, tribunal, modoExecucao, dryRun, normalizedRequestedBy);
    }

    private Map<String, Object> payloadMap(SubmissionEnvelope envelope, String externalIdempotencyKey) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("tribunalCodigo", envelope.tribunal().codigo());
        payload.put("tribunalNome", envelope.tribunal().nome());
        payload.put("ramoJustica", envelope.tribunal().ramo().name());
        payload.put("acao", envelope.request().acao().name());
        payload.put("modoExecucao", envelope.modoExecucao().name());
        payload.put("dryRun", envelope.dryRun());
        if (envelope.request().ondaAlvo() != null && !envelope.request().ondaAlvo().isBlank()) {
            payload.put("ondaAlvo", envelope.request().ondaAlvo().trim());
        }
        if (envelope.request().justificativa() != null && !envelope.request().justificativa().isBlank()) {
            payload.put("justificativa", envelope.request().justificativa().trim());
        }
        payload.put("metadados", envelope.request().metadados());
        if (externalIdempotencyKey != null && !externalIdempotencyKey.isBlank()) {
            payload.put("idempotencyKeyExterno", externalIdempotencyKey.trim());
        }
        payload.put("requestedBy", envelope.requestedBy());
        return java.util.Collections.unmodifiableMap(payload);
    }

    private int priorityFor(SubmissionEnvelope envelope) {
        return switch (envelope.request().acao()) {
            case CONFIRMAR_CUTOVER -> 96;
            case ACIONAR_ROLLBACK -> 98;
            case HOMOLOGAR_TRIBUNAL -> 90;
            case SINCRONIZAR_COMUNICACOES_NACIONAIS -> 88;
            case INICIAR_MIGRACAO_SOMBRA -> 86;
        };
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private record SubmissionEnvelope(PjbSubstituicaoNacionalExecucaoCommandRequest request,
                                      NationalCompetenceMatrix tribunal,
                                      PjbSubstituicaoExecucaoModo modoExecucao,
                                      boolean dryRun,
                                      String requestedBy) {
    }
}
