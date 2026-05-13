package com.tcc.pjb.backend.service.recursal.mesh;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalRoutePlan;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalStateSnapshot;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshConsistencyView;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalAggregateState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalProcessIntegrationState;
import com.tcc.pjb.backend.model.entity.recursalmesh.RecursalTransitionLedgerEntry;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalAggregateStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalProcessIntegrationStateRepository;
import com.tcc.pjb.backend.model.repository.recursalmesh.RecursalTransitionLedgerRepository;

@Service
public class RecursalMeshConsistencyService {

    private final RecursalAggregateStateRepository aggregateRepository;
    private final RecursalProcessIntegrationStateRepository projectionRepository;
    private final RecursalTransitionLedgerRepository ledgerRepository;
    private final ObjectMapper objectMapper;
    private final RecursalMeshFingerprintService fingerprintService;

    public RecursalMeshConsistencyService(
            RecursalAggregateStateRepository aggregateRepository,
            RecursalProcessIntegrationStateRepository projectionRepository,
            RecursalTransitionLedgerRepository ledgerRepository,
            ObjectMapper objectMapper,
            RecursalMeshFingerprintService fingerprintService) {
        this.aggregateRepository = aggregateRepository;
        this.projectionRepository = projectionRepository;
        this.ledgerRepository = ledgerRepository;
        this.objectMapper = objectMapper;
        this.fingerprintService = fingerprintService;
    }

    @Transactional(readOnly = true)
    public Optional<RecursalMeshConsistencyView> verify(String recursoId) {
        return aggregateRepository.findById(recursoId).map(this::verifyAggregate);
    }

    @Transactional(readOnly = true)
    public RecursalMeshConsistencyView verifyAggregate(RecursalAggregateState aggregate) {
        RecursalStateSnapshot snapshot = parseSnapshot(aggregate.getSnapshotJson());
        RecursalRoutePlan routePlan = tryParseRoutePlan(aggregate.getRoutePlanJson());
        RecursalProcessIntegrationState projection = projectionRepository.findById(aggregate.getRecursoId()).orElse(null);
        List<RecursalTransitionLedgerEntry> ledger = ledgerRepository.findTop100ByRecursoIdOrderByToRevisionDesc(aggregate.getRecursoId());
        List<String> inconsistencies = new ArrayList<>();
        boolean projectionConsistent = projectionConsistent(aggregate, projection, snapshot, inconsistencies);
        boolean ledgerConsistent = ledgerConsistent(snapshot, ledger, inconsistencies);
        boolean processConsistent = processConsistent(aggregate.getProcesso(), snapshot.state(), inconsistencies);
        boolean routePlanConsistent = routePlanConsistent(aggregate, snapshot, routePlan, inconsistencies);
        boolean aggregateFingerprintConsistent = aggregateFingerprintConsistent(aggregate, inconsistencies);
        boolean projectionFingerprintConsistent = projectionFingerprintConsistent(projection, inconsistencies);
        boolean ledgerFingerprintConsistent = ledgerFingerprintConsistent(ledger, inconsistencies);
        return buildView(aggregate, snapshot, ledger, projectionConsistent, ledgerConsistent, processConsistent, routePlanConsistent, aggregateFingerprintConsistent, projectionFingerprintConsistent, ledgerFingerprintConsistent, inconsistencies);
    }

    private RecursalMeshConsistencyView buildView(
            RecursalAggregateState aggregate,
            RecursalStateSnapshot snapshot,
            List<RecursalTransitionLedgerEntry> ledger,
            boolean projectionConsistent,
            boolean ledgerConsistent,
            boolean processConsistent,
            boolean routePlanConsistent,
            boolean aggregateFingerprintConsistent,
            boolean projectionFingerprintConsistent,
            boolean ledgerFingerprintConsistent,
            List<String> inconsistencies) {
        RecursalTransitionLedgerEntry lastLedger = ledger.stream().max(Comparator.comparingInt(RecursalTransitionLedgerEntry::getToRevision)).orElse(null);
        Processo processo = aggregate.getProcesso();
        boolean overallConsistent = projectionConsistent && ledgerConsistent && processConsistent && routePlanConsistent && aggregateFingerprintConsistent && projectionFingerprintConsistent && ledgerFingerprintConsistent && inconsistencies.isEmpty();
        return new RecursalMeshConsistencyView(
                aggregate.getRecursoId(),
                processo == null ? null : processo.getId(),
                aggregate.getSpeciesCode(),
                aggregate.getProfileName(),
                snapshot.state(),
                snapshot.revision(),
                processo == null ? null : processo.getFaseAtual(),
                processo == null ? null : processo.getStatusProcesso(),
                projectionConsistent,
                ledgerConsistent,
                processConsistent,
                routePlanConsistent,
                aggregateFingerprintConsistent,
                projectionFingerprintConsistent,
                ledgerFingerprintConsistent,
                overallConsistent,
                ledger.size(),
                lastLedger == null ? null : lastLedger.getToRevision(),
                lastLedger == null ? null : lastLedger.getEventCode(),
                lastLedger == null ? null : lastLedger.getToState(),
                Instant.now(),
                List.copyOf(inconsistencies)
        );
    }

    private boolean projectionConsistent(RecursalAggregateState aggregate, RecursalProcessIntegrationState projection, RecursalStateSnapshot snapshot, List<String> inconsistencies) {
        if (projection == null) {
            inconsistencies.add("Projection recursal ausente");
            return false;
        }
        boolean consistent = true;
        Long aggregateProcessoId = aggregate.getProcesso() == null ? null : aggregate.getProcesso().getId();
        Long projectionProcessoId = projection.getProcesso() == null ? null : projection.getProcesso().getId();
        consistent &= same(aggregateProcessoId, projectionProcessoId, inconsistencies, "Processo da projection divergente do agregado");
        consistent &= same(aggregate.getCurrentState(), snapshot.state(), inconsistencies, "Estado do agregado divergente do snapshot");
        consistent &= same(aggregate.getCurrentState(), projection.getCurrentState(), inconsistencies, "Estado da projection divergente do agregado");
        consistent &= same(aggregate.getTribunalAtual(), snapshot.tribunalAtual(), inconsistencies, "Tribunal do agregado divergente do snapshot");
        consistent &= same(aggregate.getTribunalDetalhadoAtual(), snapshot.tribunalDetalhadoAtual(), inconsistencies, "Tribunal detalhado do agregado divergente do snapshot");
        consistent &= same(snapshot.revision(), projection.getCurrentRevision(), inconsistencies, "Revisão da projection divergente do snapshot");
        if (projection.getTotalTransitions() < snapshot.revision() + 1) {
            inconsistencies.add("Projection com total de transições inferior à revisão do snapshot");
            consistent = false;
        }
        if (projection.isTransitadoEmJulgado() != (snapshot.state() == RecursalLifecycleState.TRANSITADO_EM_JULGADO)) {
            inconsistencies.add("Flag transitadoEmJulgado divergente da máquina de estado");
            consistent = false;
        }
        return consistent;
    }

    private boolean ledgerConsistent(RecursalStateSnapshot snapshot, List<RecursalTransitionLedgerEntry> ledger, List<String> inconsistencies) {
        if (ledger.isEmpty()) {
            inconsistencies.add("Ledger recursal ausente");
            return false;
        }
        RecursalTransitionLedgerEntry last = ledger.stream().max(Comparator.comparingInt(RecursalTransitionLedgerEntry::getToRevision)).orElseThrow();
        boolean consistent = true;
        consistent &= same(snapshot.revision(), last.getToRevision(), inconsistencies, "Revisão final do ledger divergente do snapshot");
        consistent &= same(snapshot.state(), last.getToState(), inconsistencies, "Estado final do ledger divergente do snapshot");
        long revisionMatches = ledger.stream().filter(entry -> entry.getToRevision() == snapshot.revision()).count();
        if (revisionMatches != 1L) {
            inconsistencies.add("Ledger contém multiplicidade inválida para a revisão atual");
            consistent = false;
        }
        return consistent;
    }

    private boolean processConsistent(Processo processo, RecursalLifecycleState state, List<String> inconsistencies) {
        if (processo == null) {
            return true;
        }
        boolean consistent = true;
        FaseProcessual faseAtual = processo.getFaseAtual();
        StatusProcesso statusAtual = processo.getStatusProcesso();
        if (state == RecursalLifecycleState.TRANSITADO_EM_JULGADO) {
            if (statusAtual != StatusProcesso.TRANSITO_EM_JULGADO) {
                inconsistencies.add("Status do processo principal divergente do trânsito em julgado recursal");
                consistent = false;
            }
            if (faseAtual == null) {
                inconsistencies.add("Fase processual principal ausente após trânsito em julgado recursal");
                consistent = false;
            }
            return consistent;
        }
        if (faseAtual != FaseProcessual.RECURSAL) {
            inconsistencies.add("Fase processual principal divergente da malha recursal");
            consistent = false;
        }
        if (statusAtual != StatusProcesso.RECURSO_INTERPOSTO && statusAtual != StatusProcesso.EMBARGOS_DECLARACAO && statusAtual != StatusProcesso.BAIXADO) {
            inconsistencies.add("Status do processo principal fora do domínio recursal esperado");
            consistent = false;
        }
        return consistent;
    }

    private boolean routePlanConsistent(RecursalAggregateState aggregate, RecursalStateSnapshot snapshot, RecursalRoutePlan routePlan, List<String> inconsistencies) {
        if (routePlan == null) {
            inconsistencies.add("Route plan recursal inválido ou ausente");
            return false;
        }
        boolean consistent = true;
        if (routePlan.tribunalDetalhadoOrigem().tribunal() != routePlan.tribunalOrigem()) {
            inconsistencies.add("Route plan com tribunal detalhado de origem incompatível");
            consistent = false;
        }
        if (routePlan.tribunalDetalhadoDestino().tribunal() != routePlan.tribunalDestino()) {
            inconsistencies.add("Route plan com tribunal detalhado de destino incompatível");
            consistent = false;
        }
        if (aggregate.getTribunalAtual() != snapshot.tribunalAtual()) {
            inconsistencies.add("Route plan não corresponde ao estado atual do agregado");
            consistent = false;
        }
        return consistent;
    }

    private boolean aggregateFingerprintConsistent(RecursalAggregateState aggregate, List<String> inconsistencies) {
        String expected = fingerprintService.aggregateFingerprint(aggregate);
        if (expected.equals(aggregate.getIntegrityFingerprint())) {
            return true;
        }
        inconsistencies.add("Fingerprint do agregado recursal divergente");
        return false;
    }

    private boolean projectionFingerprintConsistent(RecursalProcessIntegrationState projection, List<String> inconsistencies) {
        if (projection == null) {
            return false;
        }
        String expected = fingerprintService.projectionFingerprint(projection);
        if (expected.equals(projection.getIntegrityFingerprint())) {
            return true;
        }
        inconsistencies.add("Fingerprint da projection recursal divergente");
        return false;
    }

    private boolean ledgerFingerprintConsistent(List<RecursalTransitionLedgerEntry> ledger, List<String> inconsistencies) {
        if (ledger.isEmpty()) {
            return false;
        }
        boolean consistent = true;
        for (RecursalTransitionLedgerEntry entry : ledger) {
            String expected = fingerprintService.ledgerFingerprint(entry);
            if (!expected.equals(entry.getIntegrityFingerprint())) {
                inconsistencies.add("Fingerprint do ledger recursal divergente na revisão " + entry.getToRevision());
                consistent = false;
            }
        }
        return consistent;
    }

    private <T> boolean same(T expected, T actual, List<String> inconsistencies, String message) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            return true;
        }
        inconsistencies.add(message);
        return false;
    }

    private RecursalStateSnapshot parseSnapshot(String snapshotJson) {
        try {
            return objectMapper.readValue(snapshotJson, RecursalStateSnapshot.class);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao desserializar snapshot recursal", ex);
        }
    }

    private RecursalRoutePlan tryParseRoutePlan(String routePlanJson) {
        try {
            return objectMapper.readValue(routePlanJson, RecursalRoutePlan.class);
        } catch (IOException ex) {
            return null;
        }
    }
}
