package com.tcc.pjb.backend.core.transito;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.executionmesh.ExecutionMeshState;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.executionmesh.ExecutionMeshStateRepository;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExecutionMeshStateService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final TypeReference<ArrayList<LinkedHashMap<String, Object>>> LIST_TYPE = new TypeReference<>() {};

    private final ExecutionMeshStateRepository repository;
    private final ObjectMapper objectMapper;

    public ExecutionMeshStateService(ExecutionMeshStateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.execution-start.persist", maxMillis = 2500, critical = true)
    public void recordExecutionStart(Processo processo,
                                     PostJudgmentOperationalProfile profile,
                                     double valorExequendo) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        snapshot.put("processoId", processo.getId());
        snapshot.put("numero", processo.getNumeroProcesso());
        snapshot.put("speciesCode", resolveSpeciesFromTrack(profile.executionTrack()));
        snapshot.put("currentStage", "CUMPRIMENTO_INICIADO");
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentImpact", profile.executionTrack());
        snapshot.put("satisfactionState", profile.satisfactionMode());
        snapshot.put("valorExequendo", valorExequendo);
        snapshot.put("profile", profile.toMap());
        snapshot.put("updatedAtEngine", Instant.now().toString());

        state.setSpeciesCode(stringValue(snapshot.get("speciesCode"), "QUANTIA"));
        state.setCurrentStage("CUMPRIMENTO_INICIADO");
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentImpact(profile.executionTrack());
        state.setSatisfactionState(profile.satisfactionMode());
        state.setSnapshotJson(writeJson(snapshot));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.incident.persist", maxMillis = 2500, critical = true)
    public void recordIncident(Processo processo,
                               ExecutionIncidentProfile profile,
                               WorkItem workItem) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> incidents = incidentLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("incidentType", profile.incidentType());
        entry.put("queueCode", profile.queueCode());
        entry.put("inboxKey", profile.inboxKey());
        entry.put("executionImpact", profile.executionImpact());
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("descriptor", profile.descriptor());
        entry.put("recordedAt", Instant.now().toString());
        incidents.add(entry);

        snapshot.put("currentStage", "INCIDENTE_EXECUTIVO_AUTUADO");
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentImpact", profile.executionImpact());
        snapshot.put("incidentProfile", profile.toMap());
        snapshot.put("lastIncidentType", profile.incidentType());

        state.setCurrentStage("INCIDENTE_EXECUTIVO_AUTUADO");
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentImpact(profile.executionImpact());
        state.setSpeciesCode(stringValue(snapshot.get("speciesCode"), resolveSpeciesFromQueue(profile.queueCode())));
        state.setIncidentCount(incidents.size());
        state.setSnapshotJson(writeJson(snapshot));
        state.setIncidentLedgerJson(writeJson(incidents));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.enforcement.persist", maxMillis = 2500, critical = true)
    public void recordEnforcementAct(Processo processo,
                                     ExecutionEnforcementProfile profile,
                                     WorkItem workItem,
                                     String detalhe,
                                     double valorOperacao) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = enforcementLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("actType", profile.actType());
        entry.put("speciesCode", profile.speciesCode());
        entry.put("descriptor", profile.descriptor());
        entry.put("queueCode", profile.queueCode());
        entry.put("inboxKey", profile.inboxKey());
        entry.put("detail", detalhe);
        entry.put("valorOperacao", valorOperacao);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", profile.actType());
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentImpact", profile.executionImpact());
        snapshot.put("speciesCode", profile.speciesCode());
        snapshot.put("satisfactionState", profile.satisfactionMode());
        snapshot.put("lastEnforcementAct", profile.actType());
        snapshot.put("enforcementProfile", profile.toMap());

        state.setSpeciesCode(profile.speciesCode());
        state.setCurrentStage(profile.actType());
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentImpact(profile.executionImpact());
        state.setSatisfactionState(profile.satisfactionMode());
        state.setEnforcementCount(ledger.size());
        state.setSnapshotJson(writeJson(snapshot));
        state.setEnforcementLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.patrimonial-constriction.persist", maxMillis = 2500, critical = true)
    public void recordPatrimonialConstriction(Processo processo,
                                              PatrimonialConstrictionProfile profile,
                                              WorkItem workItem,
                                              String detalhe,
                                              String convenio,
                                              double valorOperacao) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = patrimonialLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("actType", profile.actType());
        entry.put("assetKind", profile.assetKind());
        entry.put("descriptor", profile.descriptor());
        entry.put("queueCode", profile.queueCode());
        entry.put("inboxKey", profile.inboxKey());
        entry.put("convenio", convenio);
        entry.put("detail", detalhe);
        entry.put("valorOperacao", valorOperacao);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", profile.actType());
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentImpact", profile.satisfactionPriority());
        snapshot.put("currentAssetKind", profile.assetKind());
        snapshot.put("currentGateway", profile.metadata().get("convenioSugerido"));
        snapshot.put("patrimonialProfile", profile.toMap());
        snapshot.put("lastPatrimonialAct", profile.actType());

        state.setCurrentStage(profile.actType());
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentImpact(profile.satisfactionPriority());
        state.setCurrentAssetKind(profile.assetKind());
        state.setCurrentGateway(stringValue(profile.metadata().get("convenioSugerido"), state.getCurrentGateway()));
        state.setSnapshotJson(writeJson(snapshot));
        state.setPatrimonialLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.external-constriction.persist", maxMillis = 2500, critical = true)
    public void recordExternalConstriction(Processo processo,
                                           ExternalConstrictionProfile profile,
                                           WorkItem workItem,
                                           String referenciaExterna,
                                           double valorOperacao) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = externalLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("actType", profile.actType());
        entry.put("assetKind", profile.assetKind());
        entry.put("gatewayCode", profile.gatewayCode());
        entry.put("statusTarget", profile.statusTarget());
        entry.put("descriptor", profile.descriptor());
        entry.put("queueCode", profile.queueCode());
        entry.put("inboxKey", profile.inboxKey());
        entry.put("reference", referenciaExterna);
        entry.put("valorOperacao", valorOperacao);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", profile.actType());
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentAssetKind", profile.assetKind());
        snapshot.put("currentGateway", profile.gatewayCode());
        snapshot.put("externalStatus", profile.statusTarget());
        snapshot.put("externalProfile", profile.toMap());
        snapshot.put("lastExternalAct", profile.actType());

        state.setCurrentStage(profile.actType());
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentAssetKind(profile.assetKind());
        state.setCurrentGateway(profile.gatewayCode());
        state.setExternalStatus(profile.statusTarget());
        state.setSnapshotJson(writeJson(snapshot));
        state.setExternalLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.terminal-disposition.persist", maxMillis = 2500, critical = true)
    public void recordTerminalDisposition(Processo processo,
                                          ExecutionSatisfactionProfile profile,
                                          WorkItem workItem,
                                          double percentualSatisfeito,
                                          double saldoRemanescente,
                                          String fundamento) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = terminalLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("closureMode", profile.closureMode());
        entry.put("satisfactionMode", profile.satisfactionMode());
        entry.put("terminalDisposition", profile.terminalDisposition());
        entry.put("descriptor", profile.descriptor());
        entry.put("queueCode", profile.queueCode());
        entry.put("inboxKey", profile.inboxKey());
        entry.put("percentualSatisfeito", percentualSatisfeito);
        entry.put("saldoRemanescente", saldoRemanescente);
        entry.put("fundamento", fundamento);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", profile.closureMode());
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("satisfactionState", profile.satisfactionMode());
        snapshot.put("terminalDisposition", profile.terminalDisposition());
        snapshot.put("satisfactionPercent", percentualSatisfeito);
        snapshot.put("residualAmount", saldoRemanescente);
        snapshot.put("terminalProfile", profile.toMap());
        snapshot.put("lastTerminalDisposition", profile.terminalDisposition());

        state.setCurrentStage(profile.closureMode());
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setSatisfactionState(profile.satisfactionMode());
        state.setTerminalDisposition(profile.terminalDisposition());
        state.setSatisfactionPercent(scalePercent(percentualSatisfeito));
        state.setResidualAmount(scaleMoney(saldoRemanescente));
        state.setSnapshotJson(writeJson(snapshot));
        state.setTerminalLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.expropriation-governance.persist", maxMillis = 2500, critical = true)
    public void recordExpropriationGovernance(Processo processo,
                                              ExpropriationGovernanceProfile profile,
                                              WorkItem workItem,
                                              String modalidade,
                                              double valorReferencia) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = expropriationLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("actType", profile.actType());
        entry.put("assetKind", profile.assetKind());
        entry.put("expropriationMode", profile.expropriationMode());
        entry.put("modalidade", modalidade);
        entry.put("valorReferencia", valorReferencia);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("descriptor", profile.descriptor());
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", profile.actType());
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentImpact", profile.settlementMode());
        snapshot.put("currentAssetKind", profile.assetKind());
        snapshot.put("currentExpropriationMode", profile.expropriationMode());
        snapshot.put("expropriationProfile", profile.toMap());
        snapshot.put("lastExpropriationAct", profile.actType());

        state.setCurrentStage(profile.actType());
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentImpact(profile.settlementMode());
        state.setCurrentAssetKind(profile.assetKind());
        state.setCurrentExpropriationMode(profile.expropriationMode());
        state.setSnapshotJson(writeJson(snapshot));
        state.setExpropriationLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.auction-cycle.persist", maxMillis = 2500, critical = true)
    public void recordAuctionCycle(Processo processo,
                                   ExpropriationAuctionCycleProfile profile,
                                   WorkItem workItem,
                                   String modalidade,
                                   int tentativa,
                                   double valorReferencia) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = auctionCycleLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("actType", profile.actType());
        entry.put("assetKind", profile.assetKind());
        entry.put("cycleMode", profile.cycleMode());
        entry.put("modalidade", modalidade);
        entry.put("tentativa", tentativa);
        entry.put("valorReferencia", valorReferencia);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("descriptor", profile.descriptor());
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", profile.actType());
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentImpact", profile.homologationDesk());
        snapshot.put("currentAssetKind", profile.assetKind());
        snapshot.put("currentAuctionCycleMode", profile.cycleMode());
        snapshot.put("auctionCycleProfile", profile.toMap());
        snapshot.put("lastAuctionCycleAct", profile.actType());

        state.setCurrentStage(profile.actType());
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentImpact(profile.homologationDesk());
        state.setCurrentAssetKind(profile.assetKind());
        state.setCurrentAuctionCycleMode(profile.cycleMode());
        state.setSnapshotJson(writeJson(snapshot));
        state.setAuctionCycleLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.external-contingency.persist", maxMillis = 2500, critical = true)
    public void recordExternalContingency(Processo processo,
                                          ExternalConstrictionContingencyProfile profile,
                                          WorkItem workItem,
                                          String referenciaExterna,
                                          double valorOperacao) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = contingencyLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("gatewayCode", profile.gatewayCode());
        entry.put("assetKind", profile.assetKind());
        entry.put("contingencyMode", profile.contingencyMode());
        entry.put("reference", referenciaExterna);
        entry.put("valorOperacao", valorOperacao);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("descriptor", profile.descriptor());
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", "CONTINGENCIA_CONSTRICAO_EXTERNA");
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentAssetKind", profile.assetKind());
        snapshot.put("currentGateway", profile.gatewayCode());
        snapshot.put("currentContingencyMode", profile.contingencyMode());
        snapshot.put("contingencyProfile", profile.toMap());
        snapshot.put("lastContingencyGateway", profile.gatewayCode());

        state.setCurrentStage("CONTINGENCIA_CONSTRICAO_EXTERNA");
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentAssetKind(profile.assetKind());
        state.setCurrentGateway(profile.gatewayCode());
        state.setCurrentContingencyMode(profile.contingencyMode());
        state.setSnapshotJson(writeJson(snapshot));
        state.setContingencyLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.external-reconciliation.persist", maxMillis = 2500, critical = true)
    public void recordExternalReconciliation(Processo processo,
                                             ExternalConstrictionReconciliationProfile profile,
                                             WorkItem workItem,
                                             String referenciaExterna,
                                             double valorOperacao) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = reconciliationLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("gatewayCode", profile.gatewayCode());
        entry.put("assetKind", profile.assetKind());
        entry.put("externalStatus", profile.externalStatus());
        entry.put("reconciliationStatus", profile.reconciliationStatus());
        entry.put("reference", referenciaExterna);
        entry.put("valorOperacao", valorOperacao);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("descriptor", profile.descriptor());
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", profile.actType());
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentAssetKind", profile.assetKind());
        snapshot.put("currentGateway", profile.gatewayCode());
        snapshot.put("externalStatus", profile.externalStatus());
        snapshot.put("reconciliationStatus", profile.reconciliationStatus());
        snapshot.put("reconciliationProfile", profile.toMap());
        snapshot.put("lastReconciliationGateway", profile.gatewayCode());

        state.setCurrentStage(profile.actType());
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentAssetKind(profile.assetKind());
        state.setCurrentGateway(profile.gatewayCode());
        state.setExternalStatus(profile.externalStatus());
        state.setReconciliationStatus(profile.reconciliationStatus());
        state.setSnapshotJson(writeJson(snapshot));
        state.setReconciliationLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.archive-linkage.persist", maxMillis = 2500, critical = true)
    public void recordArchiveLinkage(Processo processo,
                                     TerminalArchiveLinkProfile profile,
                                     WorkItem workItem,
                                     String motivo) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = archiveLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("operationType", profile.operationType());
        entry.put("terminalDisposition", profile.terminalDisposition());
        entry.put("archiveEligibility", profile.archiveEligibility());
        entry.put("archiveLinkMode", profile.archiveLinkMode());
        entry.put("motivo", motivo);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("descriptor", profile.descriptor());
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", profile.operationType());
        snapshot.put("currentQueue", profile.archiveQueue());
        snapshot.put("currentInbox", profile.archiveInbox());
        snapshot.put("archiveLinkStatus", profile.archiveLinkMode());
        snapshot.put("archiveLinkProfile", profile.toMap());
        snapshot.put("lastArchiveOperation", profile.operationType());
        if (profile.terminalDisposition() != null) {
            snapshot.put("terminalDisposition", profile.terminalDisposition());
        }

        state.setCurrentStage(profile.operationType());
        state.setCurrentQueue(profile.archiveQueue());
        state.setCurrentInbox(profile.archiveInbox());
        state.setArchiveLinkStatus(profile.archiveLinkMode());
        if (profile.terminalDisposition() != null && !profile.terminalDisposition().isBlank()) {
            state.setTerminalDisposition(profile.terminalDisposition());
        }
        state.setSnapshotJson(writeJson(snapshot));
        state.setArchiveLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.expropriation-homologation.persist", maxMillis = 2500, critical = true)
    public void recordExpropriationHomologation(Processo processo,
                                                ExpropriationHomologationProfile profile,
                                                WorkItem workItem,
                                                String adquirente,
                                                double valorArrematacao) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = homologationLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("actType", profile.actType());
        entry.put("assetKind", profile.assetKind());
        entry.put("homologationMode", profile.homologationMode());
        entry.put("adquirente", adquirente);
        entry.put("valorArrematacao", valorArrematacao);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("descriptor", profile.descriptor());
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", "HOMOLOGACAO_EXPROPRIACAO_FINAL");
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentAssetKind", profile.assetKind());
        snapshot.put("currentHomologationMode", profile.homologationMode());
        snapshot.put("currentImpact", profile.settlementTriggerMode());
        snapshot.put("homologationProfile", profile.toMap());
        snapshot.put("lastHomologationAct", profile.actType());

        state.setCurrentStage("HOMOLOGACAO_EXPROPRIACAO_FINAL");
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentAssetKind(profile.assetKind());
        state.setCurrentHomologationMode(profile.homologationMode());
        state.setCurrentImpact(profile.settlementTriggerMode());
        state.setSnapshotJson(writeJson(snapshot));
        state.setHomologationLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.expropriation-settlement.persist", maxMillis = 2500, critical = true)
    public void recordExpropriationSettlement(Processo processo,
                                              ExpropriationSettlementProfile profile,
                                              WorkItem workItem,
                                              double valorProduto,
                                              double saldoExecutado,
                                              double saldoCredor) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = settlementLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("assetKind", profile.assetKind());
        entry.put("settlementMode", profile.settlementMode());
        entry.put("preferenceMode", profile.preferenceMode());
        entry.put("subrogationMode", profile.subrogationMode());
        entry.put("valorProduto", valorProduto);
        entry.put("saldoExecutado", saldoExecutado);
        entry.put("saldoCredor", saldoCredor);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("descriptor", profile.descriptor());
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        double residualAmount = Math.max(0D, saldoCredor - valorProduto);
        snapshot.put("currentStage", "LIQUIDACAO_PRODUTO_EXPROPRIACAO");
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentAssetKind", profile.assetKind());
        snapshot.put("currentPreferenceMode", profile.preferenceMode());
        snapshot.put("subrogationStatus", profile.subrogationMode());
        snapshot.put("currentImpact", profile.balanceMode());
        snapshot.put("terminalDisposition", profile.terminalDispositionHint());
        snapshot.put("residualAmount", residualAmount);
        snapshot.put("settlementProfile", profile.toMap());
        snapshot.put("lastSettlementAssetKind", profile.assetKind());

        state.setCurrentStage("LIQUIDACAO_PRODUTO_EXPROPRIACAO");
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentAssetKind(profile.assetKind());
        state.setCurrentPreferenceMode(profile.preferenceMode());
        state.setSubrogationStatus(profile.subrogationMode());
        state.setCurrentImpact(profile.balanceMode());
        state.setTerminalDisposition(profile.terminalDispositionHint());
        state.setResidualAmount(scaleMoney(residualAmount));
        state.setSnapshotJson(writeJson(snapshot));
        state.setSettlementLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "transito.mesh-state.closure-governance.persist", maxMillis = 2500, critical = true)
    public void recordClosureGovernance(Processo processo,
                                        ExecutionClosureGovernanceProfile profile,
                                        WorkItem workItem,
                                        String motivo,
                                        double percentualSatisfeito,
                                        double saldoRemanescente) {
        if (processo == null || processo.getId() == null || profile == null) {
            return;
        }
        ExecutionMeshState state = findOrCreate(processo);
        LinkedHashMap<String, Object> snapshot = snapshotFromState(state);
        List<LinkedHashMap<String, Object>> ledger = closureLedgerFromState(state);
        LinkedHashMap<String, Object> entry = new LinkedHashMap<>();
        entry.put("closureMode", profile.closureMode());
        entry.put("closureConsistencyStatus", profile.closureConsistencyStatus());
        entry.put("archiveReadiness", profile.archiveReadiness());
        entry.put("residualDispositionMode", profile.residualDispositionMode());
        entry.put("preferenceClosureMode", profile.preferenceClosureMode());
        entry.put("subrogationClosureMode", profile.subrogationClosureMode());
        entry.put("motivo", motivo);
        entry.put("percentualSatisfeito", percentualSatisfeito);
        entry.put("saldoRemanescente", saldoRemanescente);
        entry.put("workItemId", workItem != null ? workItem.getId() : null);
        entry.put("descriptor", profile.descriptor());
        entry.put("recordedAt", Instant.now().toString());
        ledger.add(entry);

        snapshot.put("currentStage", profile.closureMode());
        snapshot.put("currentQueue", profile.queueCode());
        snapshot.put("currentInbox", profile.inboxKey());
        snapshot.put("currentClosureMode", profile.closureMode());
        snapshot.put("closureConsistencyStatus", profile.closureConsistencyStatus());
        snapshot.put("archiveReadiness", profile.archiveReadiness());
        snapshot.put("terminalDisposition", profile.terminalDispositionHint());
        snapshot.put("satisfactionPercent", percentualSatisfeito);
        snapshot.put("residualAmount", saldoRemanescente);
        snapshot.put("closureGovernanceProfile", profile.toMap());
        snapshot.put("lastClosureGovernance", profile.closureMode());

        state.setCurrentStage(profile.closureMode());
        state.setCurrentQueue(profile.queueCode());
        state.setCurrentInbox(profile.inboxKey());
        state.setCurrentClosureMode(profile.closureMode());
        state.setClosureConsistencyStatus(profile.closureConsistencyStatus());
        state.setTerminalDisposition(profile.terminalDispositionHint());
        state.setSatisfactionPercent(scalePercent(percentualSatisfeito));
        state.setResidualAmount(scaleMoney(saldoRemanescente));
        state.setSnapshotJson(writeJson(snapshot));
        state.setClosureLedgerJson(writeJson(ledger));
        touchIntegrity(state);
        repository.save(state);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "transito.mesh-state.current-reference.read", maxMillis = 1200, critical = false)
    public Map<String, Object> currentTerminalReference(Processo processo) {
        if (processo == null || processo.getId() == null) {
            return Map.of();
        }
        ExecutionMeshState state = repository.findByProcesso_Id(processo.getId()).orElse(null);
        if (state == null) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("terminalDisposition", state.getTerminalDisposition());
        out.put("satisfactionState", state.getSatisfactionState());
        out.put("satisfactionPercent", state.getSatisfactionPercent());
        out.put("residualAmount", state.getResidualAmount());
        out.put("archiveLinkStatus", state.getArchiveLinkStatus());
        out.put("reconciliationStatus", state.getReconciliationStatus());
        out.put("currentClosureMode", state.getCurrentClosureMode());
        out.put("closureConsistencyStatus", state.getClosureConsistencyStatus());
        out.put("currentPreferenceMode", state.getCurrentPreferenceMode());
        out.put("subrogationStatus", state.getSubrogationStatus());
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    @Transactional(readOnly = true)
    @PjbTransactionalBudget(operation = "transito.mesh-state.snapshot.read", maxMillis = 1500, critical = false)
    public Map<String, Object> readSnapshot(Processo processo,
                                            PostJudgmentOperationalProfile operationalProfile,
                                            Map<String, Object> incidentMatrix,
                                            Map<String, Object> actMatrix,
                                            Map<String, Object> patrimonialMatrix,
                                            Map<String, Object> externalMatrix,
                                            Map<String, Object> expropriationMatrix,
                                            Map<String, Object> auctionCycleMatrix,
                                            Map<String, Object> contingencyMatrix,
                                            Map<String, Object> reconciliationMatrix,
                                            Map<String, Object> homologationMatrix,
                                            Map<String, Object> settlementMatrix,
                                            Map<String, Object> closureGovernanceMatrix,
                                            Map<String, Object> terminalMatrix,
                                            Map<String, Object> archiveLinkMatrix) {
        if (processo == null || processo.getId() == null) {
            return Map.of();
        }
        ExecutionMeshState state = repository.findByProcesso_Id(processo.getId()).orElse(null);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (state != null) {
            out.put("aggregateId", state.getAggregateId());
            out.put("speciesCode", state.getSpeciesCode());
            out.put("currentStage", state.getCurrentStage());
            out.put("currentQueue", state.getCurrentQueue());
            out.put("currentInbox", state.getCurrentInbox());
            out.put("currentImpact", state.getCurrentImpact());
            out.put("currentAssetKind", state.getCurrentAssetKind());
            out.put("currentGateway", state.getCurrentGateway());
            out.put("externalStatus", state.getExternalStatus());
            out.put("currentExpropriationMode", state.getCurrentExpropriationMode());
            out.put("currentAuctionCycleMode", state.getCurrentAuctionCycleMode());
            out.put("currentContingencyMode", state.getCurrentContingencyMode());
            out.put("currentHomologationMode", state.getCurrentHomologationMode());
            out.put("currentClosureMode", state.getCurrentClosureMode());
            out.put("closureConsistencyStatus", state.getClosureConsistencyStatus());
            out.put("currentPreferenceMode", state.getCurrentPreferenceMode());
            out.put("subrogationStatus", state.getSubrogationStatus());
            out.put("reconciliationStatus", state.getReconciliationStatus());
            out.put("archiveLinkStatus", state.getArchiveLinkStatus());
            out.put("terminalDisposition", state.getTerminalDisposition());
            out.put("satisfactionState", state.getSatisfactionState());
            out.put("satisfactionPercent", state.getSatisfactionPercent());
            out.put("residualAmount", state.getResidualAmount());
            out.put("incidentCount", state.getIncidentCount());
            out.put("enforcementCount", state.getEnforcementCount());
            out.put("snapshot", snapshotFromState(state));
            out.put("incidentLedger", incidentLedgerFromState(state));
            out.put("enforcementLedger", enforcementLedgerFromState(state));
            out.put("patrimonialLedger", patrimonialLedgerFromState(state));
            out.put("externalLedger", externalLedgerFromState(state));
            out.put("expropriationLedger", expropriationLedgerFromState(state));
            out.put("auctionCycleLedger", auctionCycleLedgerFromState(state));
            out.put("contingencyLedger", contingencyLedgerFromState(state));
            out.put("reconciliationLedger", reconciliationLedgerFromState(state));
            out.put("terminalLedger", terminalLedgerFromState(state));
            out.put("archiveLedger", archiveLedgerFromState(state));
            out.put("homologationLedger", homologationLedgerFromState(state));
            out.put("settlementLedger", settlementLedgerFromState(state));
            out.put("closureLedger", closureLedgerFromState(state));
            out.put("integrityFingerprint", state.getIntegrityFingerprint());
            out.put("updatedAt", state.getUpdatedAt());
        } else {
            out.put("aggregateId", aggregateId(processo));
            out.put("speciesCode", resolveSpeciesFromTrack(operationalProfile != null ? operationalProfile.executionTrack() : null));
            out.put("currentStage", "SEM_PERSISTENCIA_EXECUTIVA");
            out.put("incidentCount", 0);
            out.put("enforcementCount", 0);
            out.put("snapshot", Map.of());
            out.put("incidentLedger", List.of());
            out.put("enforcementLedger", List.of());
            out.put("patrimonialLedger", List.of());
            out.put("externalLedger", List.of());
            out.put("expropriationLedger", List.of());
            out.put("auctionCycleLedger", List.of());
            out.put("contingencyLedger", List.of());
            out.put("reconciliationLedger", List.of());
            out.put("terminalLedger", List.of());
            out.put("archiveLedger", List.of());
            out.put("homologationLedger", List.of());
            out.put("settlementLedger", List.of());
            out.put("closureLedger", List.of());
        }
        out.put("operationalProfile", operationalProfile != null ? operationalProfile.toMap() : Map.of());
        out.put("incidentMatrix", incidentMatrix == null ? Map.of() : Map.copyOf(incidentMatrix));
        out.put("actMatrix", actMatrix == null ? Map.of() : Map.copyOf(actMatrix));
        out.put("patrimonialMatrix", patrimonialMatrix == null ? Map.of() : Map.copyOf(patrimonialMatrix));
        out.put("externalConstrictionMatrix", externalMatrix == null ? Map.of() : Map.copyOf(externalMatrix));
        out.put("expropriationMatrix", expropriationMatrix == null ? Map.of() : Map.copyOf(expropriationMatrix));
        out.put("auctionCycleMatrix", auctionCycleMatrix == null ? Map.of() : Map.copyOf(auctionCycleMatrix));
        out.put("contingencyMatrix", contingencyMatrix == null ? Map.of() : Map.copyOf(contingencyMatrix));
        out.put("reconciliationMatrix", reconciliationMatrix == null ? Map.of() : Map.copyOf(reconciliationMatrix));
        out.put("homologationMatrix", homologationMatrix == null ? Map.of() : Map.copyOf(homologationMatrix));
        out.put("settlementMatrix", settlementMatrix == null ? Map.of() : Map.copyOf(settlementMatrix));
        out.put("closureGovernanceMatrix", closureGovernanceMatrix == null ? Map.of() : Map.copyOf(closureGovernanceMatrix));
        out.put("terminalMatrix", terminalMatrix == null ? Map.of() : Map.copyOf(terminalMatrix));
        out.put("archiveLinkMatrix", archiveLinkMatrix == null ? Map.of() : Map.copyOf(archiveLinkMatrix));
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    private ExecutionMeshState findOrCreate(Processo processo) {
        return repository.findForUpdateByProcessoId(processo.getId())
                .orElseGet(() -> {
                    ExecutionMeshState state = new ExecutionMeshState();
                    state.setAggregateId(aggregateId(processo));
                    state.setProcesso(processo);
                    state.setNumeroProcesso(processo.getNumeroProcesso());
                    state.setSpeciesCode("QUANTIA");
                    state.setCurrentStage("EXECUCAO_NAO_INICIADA");
                    state.setCurrentQueue(null);
                    state.setCurrentInbox(null);
                    state.setCurrentImpact(null);
                    state.setCurrentAssetKind(null);
                    state.setCurrentGateway(null);
                    state.setExternalStatus(null);
                    state.setCurrentExpropriationMode(null);
                    state.setCurrentAuctionCycleMode(null);
                    state.setCurrentContingencyMode(null);
                    state.setCurrentHomologationMode(null);
                    state.setCurrentClosureMode(null);
                    state.setClosureConsistencyStatus(null);
                    state.setCurrentPreferenceMode(null);
                    state.setSubrogationStatus(null);
                    state.setReconciliationStatus(null);
                    state.setArchiveLinkStatus(null);
                    state.setTerminalDisposition(null);
                    state.setSatisfactionState("SEM_SATISFACAO");
                    state.setSatisfactionPercent(scalePercent(0D));
                    state.setResidualAmount(scaleMoney(0D));
                    state.setIncidentCount(0);
                    state.setEnforcementCount(0);
                    state.setSnapshotJson("{}");
                    state.setIncidentLedgerJson("[]");
                    state.setEnforcementLedgerJson("[]");
                    state.setPatrimonialLedgerJson("[]");
                    state.setExternalLedgerJson("[]");
                    state.setTerminalLedgerJson("[]");
                    state.setExpropriationLedgerJson("[]");
                    state.setAuctionCycleLedgerJson("[]");
                    state.setContingencyLedgerJson("[]");
                    state.setReconciliationLedgerJson("[]");
                    state.setArchiveLedgerJson("[]");
                    state.setHomologationLedgerJson("[]");
                    state.setSettlementLedgerJson("[]");
                    state.setClosureLedgerJson("[]");
                    state.setIntegrityFingerprint(Hashes.sha256Hex(state.getAggregateId()));
                    return state;
                });
    }

    private LinkedHashMap<String, Object> snapshotFromState(ExecutionMeshState state) {
        if (state == null || state.getSnapshotJson() == null || state.getSnapshotJson().isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(state.getSnapshotJson(), MAP_TYPE);
        } catch (Exception ex) {
            return new LinkedHashMap<>();
        }
    }

    private List<LinkedHashMap<String, Object>> incidentLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getIncidentLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> enforcementLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getEnforcementLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> patrimonialLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getPatrimonialLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> externalLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getExternalLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> terminalLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getTerminalLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> expropriationLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getExpropriationLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> auctionCycleLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getAuctionCycleLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> contingencyLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getContingencyLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> reconciliationLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getReconciliationLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> archiveLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getArchiveLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> homologationLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getHomologationLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> settlementLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getSettlementLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> closureLedgerFromState(ExecutionMeshState state) {
        return readLedger(state == null ? null : state.getClosureLedgerJson());
    }

    private List<LinkedHashMap<String, Object>> readLedger(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, LIST_TYPE);
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private void touchIntegrity(ExecutionMeshState state) {
        String payload = String.join("|",
                stringValue(state.getAggregateId(), ""),
                stringValue(state.getSpeciesCode(), ""),
                stringValue(state.getCurrentStage(), ""),
                stringValue(state.getCurrentQueue(), ""),
                stringValue(state.getCurrentInbox(), ""),
                stringValue(state.getCurrentImpact(), ""),
                stringValue(state.getCurrentAssetKind(), ""),
                stringValue(state.getCurrentGateway(), ""),
                stringValue(state.getExternalStatus(), ""),
                stringValue(state.getCurrentExpropriationMode(), ""),
                stringValue(state.getCurrentAuctionCycleMode(), ""),
                stringValue(state.getCurrentContingencyMode(), ""),
                stringValue(state.getCurrentHomologationMode(), ""),
                stringValue(state.getCurrentClosureMode(), ""),
                stringValue(state.getClosureConsistencyStatus(), ""),
                stringValue(state.getCurrentPreferenceMode(), ""),
                stringValue(state.getSubrogationStatus(), ""),
                stringValue(state.getReconciliationStatus(), ""),
                stringValue(state.getArchiveLinkStatus(), ""),
                stringValue(state.getTerminalDisposition(), ""),
                stringValue(state.getSatisfactionState(), ""),
                decimalValue(state.getSatisfactionPercent()),
                decimalValue(state.getResidualAmount()),
                stringValue(state.getSnapshotJson(), "{}"),
                stringValue(state.getIncidentLedgerJson(), "[]"),
                stringValue(state.getEnforcementLedgerJson(), "[]"),
                stringValue(state.getPatrimonialLedgerJson(), "[]"),
                stringValue(state.getExternalLedgerJson(), "[]"),
                stringValue(state.getExpropriationLedgerJson(), "[]"),
                stringValue(state.getAuctionCycleLedgerJson(), "[]"),
                stringValue(state.getContingencyLedgerJson(), "[]"),
                stringValue(state.getReconciliationLedgerJson(), "[]"),
                stringValue(state.getTerminalLedgerJson(), "[]"),
                stringValue(state.getArchiveLedgerJson(), "[]"),
                stringValue(state.getHomologationLedgerJson(), "[]"),
                stringValue(state.getSettlementLedgerJson(), "[]"),
                stringValue(state.getClosureLedgerJson(), "[]"));
        state.setIntegrityFingerprint(Hashes.sha256Hex(payload));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new UncheckedIOException(new java.io.IOException(ex));
        }
    }

    private String aggregateId(Processo processo) {
        return UUID.nameUUIDFromBytes(("EXECUTION_MESH:" + processo.getId()).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String resolveSpeciesFromTrack(String executionTrack) {
        String track = stringValue(executionTrack, "").toUpperCase();
        if (track.contains("PENAL")) {
            return "EXECUCAO_PENAL";
        }
        if (track.contains("FISCAL") || track.contains("FAZENDA")) {
            return "EXECUCAO_FISCAL";
        }
        if (track.contains("TRABALHISTA")) {
            return "QUANTIA_TRABALHISTA";
        }
        if (track.contains("PROVISORIO")) {
            return "QUANTIA_PROVISORIA";
        }
        return "QUANTIA";
    }

    private String resolveSpeciesFromQueue(String queueCode) {
        String queue = stringValue(queueCode, "").toUpperCase();
        if (queue.contains("FISCAL")) {
            return "EXECUCAO_FISCAL";
        }
        if (queue.contains("PENAL")) {
            return "EXECUCAO_PENAL";
        }
        if (queue.contains("TRABALHISTA")) {
            return "QUANTIA_TRABALHISTA";
        }
        return "QUANTIA";
    }

    private BigDecimal scalePercent(double value) {
        return BigDecimal.valueOf(Math.max(value, 0D)).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal scaleMoney(double value) {
        return BigDecimal.valueOf(Math.max(value, 0D)).setScale(2, RoundingMode.HALF_UP);
    }

    private String decimalValue(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private String stringValue(Object value, String fallback) {
        return value == null ? fallback : String.valueOf(value);
    }
}