package com.tcc.pjb.backend.service.recursal.mesh;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.NationalRecursalMeshEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalPlanningResult;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRevisionConflictException;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalSpecies;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionCommand;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionEvent;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalTransitionResult;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshAggregateView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshConsistencyView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshLedgerView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshPlanRequest;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshProcessLinkView;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshTransitionRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalTransitionLedgerEntry;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalAggregateStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalTransitionLedgerRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;

@Service
public class NationalRecursalMeshTransactionalService {

    private final NationalRecursalMeshEngine engine;
    private final RecursalMeshRequestMapper mapper;
    private final RecursalAggregateStateRepository aggregateRepository;
    private final RecursalTransitionLedgerRepository ledgerRepository;
    private final ProcessoRepository processoRepository;
    private final ObjectMapper objectMapper;
    private final OutboxPublisher outboxPublisher;
    private final RecursalMeshWorkflowIntegrationService workflowIntegrationService;
    private final RecursalMeshProjectionService projectionService;
    private final RecursalMeshConsistencyService consistencyService;
    private final RecursalMeshGuardService guardService;
    private final RecursalMeshFingerprintService fingerprintService;
    private final RecursalMeshSlaService slaService;
    private final RecursalMeshPartyNotificationService partyNotificationService;

    public NationalRecursalMeshTransactionalService(
            RecursalMeshRequestMapper mapper,
            RecursalAggregateStateRepository aggregateRepository,
            RecursalTransitionLedgerRepository ledgerRepository,
            ProcessoRepository processoRepository,
            ObjectMapper objectMapper,
            OutboxPublisher outboxPublisher,
            RecursalMeshWorkflowIntegrationService workflowIntegrationService,
            RecursalMeshProjectionService projectionService,
            RecursalMeshConsistencyService consistencyService,
            RecursalMeshGuardService guardService,
            RecursalMeshFingerprintService fingerprintService,
            RecursalMeshSlaService slaService,
            RecursalMeshPartyNotificationService partyNotificationService) {
        this.engine = new NationalRecursalMeshEngine();
        this.mapper = mapper;
        this.aggregateRepository = aggregateRepository;
        this.ledgerRepository = ledgerRepository;
        this.processoRepository = processoRepository;
        this.objectMapper = objectMapper;
        this.outboxPublisher = outboxPublisher;
        this.workflowIntegrationService = workflowIntegrationService;
        this.projectionService = projectionService;
        this.consistencyService = consistencyService;
        this.guardService = guardService;
        this.fingerprintService = fingerprintService;
        this.slaService = slaService;
        this.partyNotificationService = partyNotificationService;
    }

    @Transactional
    public RecursalMeshAggregateView openAggregate(RecursalMeshPlanRequest request, String actor) {
        var context = mapper.toContext(request.context());
        var species = mapper.toSpecies(request.species());
        guardService.validateContext(context);
        guardService.validateSpecies(species);
        RecursalPlanningResult planning = engine.plan(context, species, request.recursoId());
        RecursalAggregateState aggregate = aggregateRepository.findForUpdateByRecursoId(planning.initialSnapshot().recursoId())
                .orElseGet(RecursalAggregateState::new);
        boolean created = false;
        if (aggregate.getRecursoId() == null) {
            hydrateAggregate(aggregate, planning.context().processoId(), planning.species(), planning.routePlan(), planning.initialSnapshot(), planning.context());
            guardService.validateAggregate(aggregate, planning.initialSnapshot(), planning.routePlan());
            aggregateRepository.save(aggregate);
            registerLedger(aggregate, planning.initialSnapshot(), planning.initialSnapshot(), actor, null, null, planning.context(), planning.routePlan());
            syncMainProcess(aggregate.getProcesso(), planning.species(), planning.initialSnapshot().state());
            workflowIntegrationService.onAggregateOpened(aggregate.getProcesso(), planning, actor, aggregate.getSnapshotJson());
            enqueueOutbox(aggregate, planning.initialSnapshot(), planning.initialSnapshot(), RecursalTransitionEvent.PROTOCOLAR, actor, "RECURSAL_MESH_AGGREGATE_OPENED", null);
            created = true;
        }
        RecursalStateSnapshot projectionSnapshot = snapshotOf(aggregate);
        projectionService.sync(aggregate, created ? RecursalTransitionEvent.PROTOCOLAR : null, actor, projectionSnapshot.atualizadoEm(), projectionSnapshot.revision() + 1);
        return viewOf(aggregate);
    }

    @Transactional
    public RecursalTransitionResult transition(RecursalMeshTransitionRequest request) {
        var context = mapper.toContext(request.context());
        var species = mapper.toSpecies(request.species());
        guardService.validateContext(context);
        guardService.validateSpecies(species);
        RecursalTransitionCommand draftCommand = mapper.toCommand(request, request.snapshot() == null
                ? RecursalStateSnapshot.newDraft(request.recursoId(), context)
                : request.snapshot());
        RecursalPlanningResult planning = engine.plan(draftCommand.context(), draftCommand.species(), request.recursoId());
        RecursalAggregateState aggregate = aggregateRepository.findForUpdateByRecursoId(planning.initialSnapshot().recursoId())
                .orElseGet(RecursalAggregateState::new);
        if (aggregate.getRecursoId() == null) {
            hydrateAggregate(aggregate, draftCommand.context().processoId(), draftCommand.species(), planning.routePlan(), planning.initialSnapshot(), draftCommand.context());
        }
        RecursalStateSnapshot currentSnapshot = snapshotOf(aggregate);
        if (hasText(request.commandId())) {
            Optional<RecursalTransitionLedgerEntry> existing = ledgerRepository.findTopByRecursoIdAndCommandIdOrderByToRevisionDesc(planning.initialSnapshot().recursoId(), request.commandId());
            if (existing.isPresent()) {
                return replay(existing.get(), currentSnapshot, draftCommand.species());
            }
        }
        validateExpectedRevision(request, currentSnapshot);
        validateSpeciesConsistency(aggregate, draftCommand.species());
        RecursalTransitionCommand command = mapper.toCommand(request, currentSnapshot);
        RecursalTransitionResult result = engine.transition(command);
        hydrateAggregate(aggregate, command.context().processoId(), command.species(), result.routePlan(), result.current(), command.context());
        guardService.validateAggregate(aggregate, result.current(), result.routePlan());
        aggregateRepository.save(aggregate);
        registerLedger(aggregate, result.previous(), result.current(), request.actor(), request.event(), request.commandId(), command.context(), result.routePlan());
        syncMainProcess(aggregate.getProcesso(), command.species(), result.current().state());
        workflowIntegrationService.onTransition(aggregate.getProcesso(), command.species(), request.event(), result, request.actor(), aggregate.getSnapshotJson());
        enqueueOutbox(aggregate, result.previous(), result.current(), request.event(), request.actor(), "RECURSAL_MESH_TRANSITIONED", request.commandId());
        projectionService.sync(aggregate, request.event(), request.actor(), result.current().atualizadoEm(), result.current().revision() + 1);
        partyNotificationService.onTransition(aggregate, request.event(), result.previous(), result.current(), request.actor(), request.commandId());
        return result;
    }

    @Transactional(readOnly = true)
    public Optional<RecursalMeshAggregateView> findAggregate(String recursoId) {
        return aggregateRepository.findById(recursoId).map(this::viewOf);
    }

    @Transactional(readOnly = true)
    public List<RecursalMeshLedgerView> findLedger(String recursoId) {
        return ledgerOf(recursoId);
    }

    @Transactional(readOnly = true)
    public List<RecursalMeshProcessLinkView> findByProcesso(Long processoId) {
        return projectionService.findByProcesso(processoId);
    }

    @Transactional(readOnly = true)
    public Optional<RecursalMeshConsistencyView> verifyConsistency(String recursoId) {
        return consistencyService.verify(recursoId);
    }

    private void validateExpectedRevision(RecursalMeshTransitionRequest request, RecursalStateSnapshot currentSnapshot) {
        if (request.expectedRevision() == null) {
            return;
        }
        if (request.expectedRevision().intValue() != currentSnapshot.revision()) {
            throw new RecursalRevisionConflictException(
                    "Conflito de revisão recursal para recurso " + request.recursoId() + ": esperado=" + request.expectedRevision() + ", atual=" + currentSnapshot.revision()
            );
        }
    }

    private void validateSpeciesConsistency(RecursalAggregateState aggregate, RecursalSpecies species) {
        if (aggregate.getSpeciesCode() == null) {
            return;
        }
        if (!aggregate.getSpeciesCode().equals(species.code())) {
            throw new IllegalArgumentException("Espécie recursal divergente do agregado persistido: esperado=" + aggregate.getSpeciesCode() + ", recebido=" + species.code());
        }
    }

    private RecursalTransitionResult replay(RecursalTransitionLedgerEntry entry, RecursalStateSnapshot persistedSnapshot, RecursalSpecies species) {
        RecursalStateSnapshot current = snapshotFromLedger(entry);
        RecursalRoutePlan routePlan = routePlanFromLedger(entry);
        RecursalStateSnapshot previous = current.revision() == persistedSnapshot.revision() ? persistedSnapshot : previousSnapshot(entry, persistedSnapshot);
        return new RecursalTransitionResult(previous, current, species, routePlan, engine.planForSnapshot(current, species, routePlan));
    }

    private RecursalStateSnapshot previousSnapshot(RecursalTransitionLedgerEntry entry, RecursalStateSnapshot persistedSnapshot) {
        return ledgerRepository.findTop100ByRecursoIdOrderByToRevisionDesc(entry.getRecursoId()).stream()
                .filter(candidate -> candidate.getToRevision() == entry.getFromRevision())
                .findFirst()
                .map(this::snapshotFromLedger)
                .orElse(persistedSnapshot);
    }

    private void hydrateAggregate(RecursalAggregateState aggregate, Long processoId, RecursalSpecies species, RecursalRoutePlan routePlan, RecursalStateSnapshot snapshot, Object context) {
        aggregate.setRecursoId(snapshot.recursoId());
        aggregate.setProcesso(resolveProcesso(processoId));
        aggregate.setNumeroProcesso(aggregate.getProcesso() != null ? aggregate.getProcesso().getNumeroProcesso() : null);
        aggregate.setSpeciesCode(species.code());
        aggregate.setSpeciesName(species.formalName());
        aggregate.setProfileName(routePlan.profileName());
        aggregate.setCurrentState(snapshot.state());
        aggregate.setTribunalAtual(snapshot.tribunalAtual());
        aggregate.setTribunalDetalhadoAtual(snapshot.tribunalDetalhadoAtual());
        aggregate.setInstanciaAtual(snapshot.instanciaAtual());
        aggregate.setAutoridadeAtual(snapshot.autoridadeAtual());
        aggregate.setPreparoSatisfeito(snapshot.preparoSatisfeito());
        aggregate.setAdmissibilidadePositiva(snapshot.admissibilidadePositiva());
        aggregate.setRemetido(snapshot.remetido());
        aggregate.setAutuadoDestino(snapshot.autuadoDestino());
        aggregate.setDistribuidoDestino(snapshot.distribuidoDestino());
        aggregate.setPreparoEmComplementacao(snapshot.preparoEmComplementacao());
        aggregate.setDiligenciaPendente(snapshot.diligenciaPendente());
        aggregate.setMultaEmbargos(snapshot.multaEmbargosProtelatoriosAplicada());
        aggregate.setSobrestadoPrecedente(snapshot.sobrestadoPorPrecedente());
        aggregate.setEfeitoSuspensivoAtivo(snapshot.efeitoSuspensivoAtivo());
        aggregate.setEfeitoAtivoConcedido(snapshot.efeitoAtivoConcedido());
        aggregate.setConhecimentoParcial(snapshot.conhecimentoParcial());
        aggregate.setIteracoesEmbargos(snapshot.iteracoesEmbargosDeclaracao());
        aggregate.setSnapshotJson(writeJson(snapshot));
        aggregate.setRoutePlanJson(writeJson(routePlan));
        aggregate.setContextJson(writeJson(context));
        aggregate.setIntegrityFingerprint(fingerprintService.aggregateFingerprint(aggregate));
    }

    private Processo resolveProcesso(Long processoId) {
        return processoId == null ? null : processoRepository.findById(processoId).orElse(null);
    }

    private RecursalStateSnapshot snapshotOf(RecursalAggregateState aggregate) {
        try {
            return objectMapper.readValue(aggregate.getSnapshotJson(), RecursalStateSnapshot.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao ler snapshot recursal persistido", ex);
        }
    }

    private RecursalStateSnapshot snapshotFromLedger(RecursalTransitionLedgerEntry entry) {
        try {
            return objectMapper.readValue(entry.getSnapshotJson(), RecursalStateSnapshot.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao ler snapshot recursal do ledger", ex);
        }
    }

    private RecursalRoutePlan routePlanOf(RecursalAggregateState aggregate) {
        try {
            return objectMapper.readValue(aggregate.getRoutePlanJson(), RecursalRoutePlan.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao ler plano recursal persistido", ex);
        }
    }

    private RecursalRoutePlan routePlanFromLedger(RecursalTransitionLedgerEntry entry) {
        try {
            return objectMapper.readValue(entry.getRoutePlanJson(), RecursalRoutePlan.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao ler plano recursal do ledger", ex);
        }
    }

    private void registerLedger(RecursalAggregateState aggregate, RecursalStateSnapshot previous, RecursalStateSnapshot current, String actor, RecursalTransitionEvent event, String commandId, Object context, RecursalRoutePlan routePlan) {
        RecursalTransitionLedgerEntry entry = new RecursalTransitionLedgerEntry();
        entry.setRecursoId(aggregate.getRecursoId());
        entry.setProcessoId(aggregate.getProcesso() == null ? null : aggregate.getProcesso().getId());
        entry.setSpeciesCode(aggregate.getSpeciesCode());
        entry.setProfileName(routePlan.profileName());
        entry.setCommandId(trimToNull(commandId));
        entry.setEventCode(event == null ? RecursalTransitionEvent.PROTOCOLAR : event);
        entry.setFromState(previous.state());
        entry.setToState(current.state());
        entry.setFromRevision(previous.revision());
        entry.setToRevision(current.revision());
        entry.setActor(actor);
        entry.setOccurredAt(current.atualizadoEm() == null ? Instant.now() : current.atualizadoEm());
        entry.setSnapshotJson(writeJson(current));
        entry.setRoutePlanJson(writeJson(routePlan));
        entry.setContextJson(writeJson(context));
        entry.setIntegrityFingerprint(fingerprintService.ledgerFingerprint(entry));
        ledgerRepository.save(entry);
    }

    private void syncMainProcess(Processo processo, RecursalSpecies species, RecursalLifecycleState state) {
        if (processo == null) {
            return;
        }
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setDataUltimaMovimentacao(java.time.LocalDateTime.now());
        processo.setStatusProcesso(mapStatus(species, state));
        processoRepository.save(processo);
    }

    private StatusProcesso mapStatus(RecursalSpecies species, RecursalLifecycleState state) {
        if (state == RecursalLifecycleState.TRANSITADO_EM_JULGADO) {
            return StatusProcesso.TRANSITO_EM_JULGADO;
        }
        if (state == RecursalLifecycleState.BAIXADO) {
            return StatusProcesso.BAIXADO;
        }
        if (species instanceof com.tcc.pjb.backend.core.kernel.recursal.mesh.embargos.EmbargosDeclaracao) {
            return StatusProcesso.EMBARGOS_DECLARACAO;
        }
        return StatusProcesso.RECURSO_INTERPOSTO;
    }

    private void enqueueOutbox(RecursalAggregateState aggregate, RecursalStateSnapshot previous, RecursalStateSnapshot current, RecursalTransitionEvent event, String actor, String eventType, String commandId) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("recursoId", aggregate.getRecursoId());
        if (aggregate.getProcesso() != null) {
            payload.put("processoId", aggregate.getProcesso().getId());
        }
        payload.put("speciesCode", aggregate.getSpeciesCode());
        payload.put("profileName", aggregate.getProfileName());
        payload.put("tribunalAtual", aggregate.getTribunalAtual() == null ? null : aggregate.getTribunalAtual().name());
        payload.put("tribunalDetalhadoAtual", aggregate.getTribunalDetalhadoAtual() == null ? null : aggregate.getTribunalDetalhadoAtual().name());
        payload.put("state", current == null || current.state() == null ? null : current.state().name());
        payload.put("previousState", previous == null || previous.state() == null ? null : previous.state().name());
        payload.put("transitionEvent", event == null ? null : event.name());
        payload.put("efeitoSuspensivoAtivo", aggregate.isEfeitoSuspensivoAtivo());
        payload.put("efeitoAtivoConcedido", aggregate.isEfeitoAtivoConcedido());
        payload.put("conhecimentoParcial", aggregate.isConhecimentoParcial());
        payload.put("integrityFingerprint", aggregate.getIntegrityFingerprint());
        if (actor != null && !actor.isBlank()) {
            payload.put("actor", actor);
        }
        slaService.snapshot(aggregate).ifPresent(snapshot -> {
            payload.put("slaSeveridade", snapshot.severidade());
            payload.put("slaPrevistaSaida", snapshot.dataPrevistaSaida().toString());
            payload.put("slaVencido", snapshot.vencido());
            payload.put("slaDiasUteisExcedidos", snapshot.diasUteisExcedidos());
            payload.put("slaFatalParaPartes", snapshot.fatalParaPartes());
        });
        if (hasText(commandId)) {
            payload.put("commandId", commandId);
        }
        outboxPublisher.enqueue(
                "recursal.mesh." + aggregate.getRecursoId(),
                eventType,
                payload,
                Map.of("module", "recursal-mesh"),
                hasText(commandId)
                        ? "recursal-mesh:" + eventType + ":" + aggregate.getRecursoId() + ":" + commandId
                        : "recursal-mesh:" + eventType + ":" + aggregate.getRecursoId() + ":" + (current == null || current.state() == null ? aggregate.getCurrentState().name() : current.state().name()),
                "RECURSAL_MESH",
                aggregate.getRecursoId()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar payload recursal", ex);
        }
    }

    private RecursalMeshAggregateView viewOf(RecursalAggregateState aggregate) {
        List<RecursalMeshLedgerView> ledger = ledgerOf(aggregate.getRecursoId());
        return new RecursalMeshAggregateView(
                aggregate.getRecursoId(),
                aggregate.getProcesso() == null ? null : aggregate.getProcesso().getId(),
                aggregate.getSpeciesCode(),
                aggregate.getSpeciesName(),
                aggregate.getProfileName(),
                snapshotOf(aggregate),
                routePlanOf(aggregate),
                slaService.snapshot(aggregate).orElse(null),
                ledger,
                aggregate.getCreatedAt(),
                aggregate.getUpdatedAt()
        );
    }

    private List<RecursalMeshLedgerView> ledgerOf(String recursoId) {
        return ledgerRepository.findTop100ByRecursoIdOrderByToRevisionDesc(recursoId).stream()
                .sorted(Comparator.comparingInt(RecursalTransitionLedgerEntry::getToRevision))
                .map(entry -> new RecursalMeshLedgerView(
                        entry.getId(),
                        entry.getCommandId(),
                        entry.getEventCode(),
                        entry.getFromState(),
                        entry.getToState(),
                        entry.getFromRevision(),
                        entry.getToRevision(),
                        entry.getActor(),
                        entry.getOccurredAt()
                ))
                .toList();
    }
}
