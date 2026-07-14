package com.tcc.pjb.backend.service.casefile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventStore;
import com.tcc.pjb.backend.core.kernel.casefile.CaseFileEventType;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.core.kernel.recursal.ProceedingKeyFactory;
import com.tcc.pjb.backend.core.kernel.recursal.RecursalRelationType;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.casefile.CaseFile;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceeding;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingRole;
import com.tcc.pjb.backend.model.entity.casefile.CaseProceedingStatus;
import com.tcc.pjb.backend.model.entity.casefile.CaseEdge;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.VinculoProcessualTipo;
import com.tcc.pjb.backend.model.repository.CaseEdgeRepository;
import com.tcc.pjb.backend.model.repository.CaseFileRepository;
import com.tcc.pjb.backend.model.repository.CaseProceedingRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;

@Service
public class CaseContinuityOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(CaseContinuityOrchestratorService.class);

    private final ProcessoRepository processoRepository;
    private final CaseFileResolverService caseFileResolverService;
    private final CaseFileRepository caseFileRepository;
    private final CaseProceedingRepository caseProceedingRepository;
    private final CaseEdgeRepository caseEdgeRepository;
    private final CaseFileEventStore eventStore;
    private final PjbAuthorizationService authorizationService;
    private final AuditLedgerService auditLedgerService;
    private final CaseContinuityObservabilityMetrics observabilityMetrics;

    public CaseContinuityOrchestratorService(ProcessoRepository processoRepository,
                                             CaseFileResolverService caseFileResolverService,
                                             CaseFileRepository caseFileRepository,
                                             CaseProceedingRepository caseProceedingRepository,
                                             CaseEdgeRepository caseEdgeRepository,
                                             CaseFileEventStore eventStore,
                                             PjbAuthorizationService authorizationService,
                                             AuditLedgerService auditLedgerService,
                                             CaseContinuityObservabilityMetrics observabilityMetrics) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.caseFileResolverService = Objects.requireNonNull(caseFileResolverService);
        this.caseFileRepository = Objects.requireNonNull(caseFileRepository);
        this.caseProceedingRepository = Objects.requireNonNull(caseProceedingRepository);
        this.caseEdgeRepository = Objects.requireNonNull(caseEdgeRepository);
        this.eventStore = Objects.requireNonNull(eventStore);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
        this.observabilityMetrics = Objects.requireNonNull(observabilityMetrics);
    }

    @Transactional
    @PjbTransactionalBudget(operation = "case-continuity.ensure-root.persist", maxMillis = 2500, critical = true)
    public void ensureRootCase(Long processoId, String reason) {
        if (processoId == null) {
            return;
        }
        Processo processo = loadProcesso(processoId);
        CaseFileResolution resolution = caseFileResolverService.resolveForProcesso(processoId, LegalIntegrationSystem.OTHER);
        CaseProceeding anchor = resolution.anchorProceeding();
        CaseProceedingRole role = Objects.equals(resolution.caseFile().getRootProcessoId(), processoId) ? CaseProceedingRole.ROOT : CaseProceedingRole.VINCULADO;
        boolean dirty = syncAnchor(anchor, processo, role, null, resolveTrack(processo, null));
        if (dirty) {
            caseProceedingRepository.save(anchor);
        }
        eventStore.append(resolution.caseFile().getId(), CaseFileEventType.CASE_CONTINUITY_SYNCED,
                new CaseContinuityPayload(processoId, "ENSURE_ROOT", resolveTrack(processo, null).name(), safe(reason)));
        observabilityMetrics.recordLifecycleTransition(resolveTrack(processo, null), processo.getFaseAtual(), processo.getStatusProcesso(), false);
        auditContinuity("CASE_CONTINUITY_ROOT_ASSURED", resolution.caseFile().getId(), processoId, anchor.getProceedingKey(), resolveTrack(processo, null), safe(reason));
        log.info("case_continuity_root_assured caseFileId={} processoId={} proceedingKey={} track={}", resolution.caseFile().getId(), processoId, anchor.getProceedingKey(), resolveTrack(processo, null));
    }

    @Transactional
    @PjbTransactionalBudget(operation = "case-continuity.lifecycle-sync.persist", maxMillis = 3000, critical = true)
    public void syncFromLifecycle(Processo processo, ProcessoLifecycleAction action) {
        if (processo == null || processo.getId() == null || action == null) {
            return;
        }
        CaseFileResolution resolution = caseFileResolverService.resolveForProcesso(processo.getId(), LegalIntegrationSystem.OTHER);
        CaseProceeding anchor = resolution.anchorProceeding();
        CaseContinuityTrack anchorTrack = resolveTrack(processo, action);
        CaseProceedingRole role = Objects.equals(resolution.caseFile().getRootProcessoId(), processo.getId()) ? CaseProceedingRole.ROOT : CaseProceedingRole.VINCULADO;
        boolean dirty = syncAnchor(anchor, processo, role, null, anchorTrack);
        if (action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO && anchor.getStatus() == CaseProceedingStatus.RECONCILED) {
            anchor.setStatus(CaseProceedingStatus.ACTIVE);
            dirty = true;
        }
        if (dirty) {
            caseProceedingRepository.save(anchor);
        }
        if (action == ProcessoLifecycleAction.INICIAR_CUMPRIMENTO) {
            assureExecutionProceeding(resolution.caseFile(), anchor, processo);
        }
        if (action == ProcessoLifecycleAction.ARQUIVAR) {
            closeCaseProceedings(resolution.caseFile().getId(), anchor.getProceedingKey(), processo);
        }
        if (action == ProcessoLifecycleAction.DESARQUIVAR) {
            reopenCaseProceedings(resolution.caseFile().getId(), anchor.getProceedingKey(), processo);
        }
        eventStore.append(resolution.caseFile().getId(), CaseFileEventType.CASE_CONTINUITY_SYNCED,
                new CaseContinuityPayload(processo.getId(), action.name(), anchorTrack.name(), processDescriptor(processo)));
        observabilityMetrics.recordLifecycleTransition(anchorTrack, processo.getFaseAtual(), processo.getStatusProcesso(), action == ProcessoLifecycleAction.ARQUIVAR || action == ProcessoLifecycleAction.DESARQUIVAR);
        auditContinuity("CASE_CONTINUITY_SYNC", resolution.caseFile().getId(), processo.getId(), anchor.getProceedingKey(), anchorTrack, action.name());
        log.info("case_continuity_sync caseFileId={} processoId={} proceedingKey={} action={} track={} fase={} status={}", resolution.caseFile().getId(), processo.getId(), anchor.getProceedingKey(), action, anchorTrack, processo.getFaseAtual(), processo.getStatusProcesso());
    }

    @Transactional
    @PjbTransactionalBudget(operation = "case-continuity.unify-linked-cases.persist", maxMillis = 4000, critical = true)
    public void unifyLinkedCases(Long baseProcessoId, Long relatedProcessoId, VinculoProcessualTipo vinculoTipo, String reason) {
        if (baseProcessoId == null || relatedProcessoId == null || Objects.equals(baseProcessoId, relatedProcessoId) || vinculoTipo == null || vinculoTipo == VinculoProcessualTipo.NENHUM) {
            return;
        }
        Processo base = loadProcesso(baseProcessoId);
        Processo related = loadProcesso(relatedProcessoId);
        CaseFileResolution baseResolution = caseFileResolverService.resolveForProcesso(baseProcessoId, LegalIntegrationSystem.OTHER);
        CaseFileResolution relatedResolution = caseFileResolverService.resolveForProcesso(relatedProcessoId, LegalIntegrationSystem.OTHER);
        CaseFile target = chooseCanonical(baseResolution.caseFile(), relatedResolution.caseFile());
        CaseFile source = Objects.equals(target.getId(), baseResolution.caseFile().getId()) ? relatedResolution.caseFile() : baseResolution.caseFile();
        if (!Objects.equals(target.getId(), source.getId())) {
            mergeCaseFiles(target, source);
        }
        CaseProceeding baseAnchor = refreshAnchor(target.getId(), base.getId());
        CaseProceeding relatedAnchor = refreshAnchor(target.getId(), related.getId());
        if (baseAnchor != null) {
            syncAnchor(baseAnchor, base, Objects.equals(target.getRootProcessoId(), base.getId()) ? CaseProceedingRole.ROOT : CaseProceedingRole.VINCULADO, null, resolveTrack(base, null));
            caseProceedingRepository.save(baseAnchor);
        }
        if (relatedAnchor != null) {
            syncAnchor(relatedAnchor, related, Objects.equals(target.getRootProcessoId(), related.getId()) ? CaseProceedingRole.ROOT : CaseProceedingRole.VINCULADO, baseAnchor == null ? null : baseAnchor.getProceedingKey(), resolveTrack(related, null));
            caseProceedingRepository.save(relatedAnchor);
        }
        if (baseAnchor != null && relatedAnchor != null) {
            upsertEdge(target.getId(), baseAnchor.getProceedingKey(), relatedAnchor.getProceedingKey(), mapRelation(vinculoTipo), LegalAppealType.OUTRO);
        }
        Long canonicalRoot = minRoot(baseResolution.caseFile(), relatedResolution.caseFile());
        if (!Objects.equals(target.getRootProcessoId(), canonicalRoot)) {
            target.setRootProcessoId(canonicalRoot);
            caseFileRepository.save(target);
        }
        eventStore.append(target.getId(), CaseFileEventType.CASE_FILES_MERGED,
                new CaseMergePayload(baseProcessoId, relatedProcessoId, vinculoTipo.name(), canonicalRoot, safe(reason)));
        observabilityMetrics.recordMerge(vinculoTipo.name());
        auditLedgerService.appendSafely("CASE_CONTINUITY_MERGE", "CASE_FILE", String.valueOf(target.getId()), baseProcessoId + "|" + relatedProcessoId + "|" + vinculoTipo.name() + "|" + canonicalRoot, safe(reason));
        log.info("case_continuity_merge caseFileId={} baseProcessoId={} relatedProcessoId={} linkageType={} canonicalRoot={}", target.getId(), baseProcessoId, relatedProcessoId, vinculoTipo, canonicalRoot);
    }

    @Transactional(readOnly = true)
    public CaseContinuitySnapshot inspect(Long processoId) {
        Processo processo = loadProcesso(processoId);
        authorizationService.requireReadProcesso(processo);
        CaseFileResolution resolution = caseFileResolverService.resolveForProcesso(processoId, LegalIntegrationSystem.OTHER);
        List<CaseProceeding> proceedings = new ArrayList<>(caseProceedingRepository.findAllByCaseFileId(resolution.caseFile().getId()));
        proceedings.sort(Comparator.comparing((CaseProceeding item) -> item.getProceedingRole() == null ? CaseProceedingRole.VINCULADO : item.getProceedingRole())
                .thenComparing(item -> item.getCreatedAt() == null ? Instant.EPOCH : item.getCreatedAt()));
        List<CaseContinuityProceedingNode> nodes = proceedings.stream()
                .map(item -> new CaseContinuityProceedingNode(
                        item.getProceedingKey(),
                        item.getLinkedProcessoId(),
                        item.isShadow(),
                        item.getStatus(),
                        item.getContinuityTrack(),
                        item.getProceedingRole(),
                        item.getInstanceLevel(),
                        item.getCourt(),
                        item.getNumeroUnificado(),
                        item.getParentProceedingKey(),
                        item.getSourceFaseProcessual(),
                        item.getSourceStatusProcesso(),
                        item.getSecrecy(),
                        item.getLastSyncAt()
                ))
                .toList();
        List<CaseContinuityEdgeLink> edges = caseEdgeRepository.findAllByCaseFileId(resolution.caseFile().getId()).stream()
                .map(edge -> new CaseContinuityEdgeLink(edge.getFromProceedingKey(), edge.getToProceedingKey(), edge.getRelationType(), edge.getAppealType()))
                .toList();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (nodes.size() == 1) {
            warnings.add("Caso ainda não possui desdobramentos internos, recursais ou executivos materializados no grafo unificado.");
        }
        if (edges.stream().noneMatch(edge -> edge.relationType() == RecursalRelationType.PROCESSUAL_LINKAGE || edge.relationType() == RecursalRelationType.EXECUTION_CONTINUATION)) {
            warnings.add("Não há continuidade executiva ou vínculo estrutural adicional consolidado para este caso raiz.");
        }
        return new CaseContinuitySnapshot(
                resolution.caseFile().getId(),
                resolution.caseFile().getRootProcessoId(),
                processoId,
                resolution.anchorProceeding().getProceedingKey(),
                dominantTrack(proceedings),
                nodes,
                edges,
                List.copyOf(warnings)
        );
    }

    private void assureExecutionProceeding(CaseFile caseFile, CaseProceeding anchor, Processo processo) {
        String numero = firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), anchor.getNumeroUnificado(), "EXECUCAO");
        String court = firstNonBlank(anchor.getCourt(), processo.getTribunal(), "EXECUCAO");
        String execKey = ProceedingKeyFactory.keyOf(caseFile.getId(), anchor.getInstanceLevel() == null ? InstanceLevel.FIRST_INSTANCE : anchor.getInstanceLevel(), court, numero, "EXECUCAO");
        CaseProceeding proceeding = caseProceedingRepository.findByProceedingKey(execKey).orElse(null);
        if (proceeding == null) {
            proceeding = CaseProceeding.builder()
                    .caseFileId(caseFile.getId())
                    .proceedingKey(execKey)
                    .shadow(false)
                    .status(CaseProceedingStatus.ACTIVE)
                    .instanceLevel(anchor.getInstanceLevel() == null ? InstanceLevel.FIRST_INSTANCE : anchor.getInstanceLevel())
                    .court(court)
                    .numeroUnificado(numero)
                    .linkedProcessoId(processo.getId())
                    .continuityTrack(resolveTrack(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO))
                    .proceedingRole(CaseProceedingRole.CUMPRIMENTO)
                    .parentProceedingKey(anchor.getProceedingKey())
                    .sourceFaseProcessual(processo.getFaseAtual())
                    .sourceStatusProcesso(processo.getStatusProcesso())
                    .lastSyncAt(Instant.now())
                    .secrecy(maxSigilo(anchor.getSecrecy(), processo.getNivelSigilo()))
                    .sourceSystem(anchor.getSourceSystem() == null ? LegalIntegrationSystem.OTHER : anchor.getSourceSystem())
                    .build();
            caseProceedingRepository.save(proceeding);
        } else {
            syncAnchor(proceeding, processo, CaseProceedingRole.CUMPRIMENTO, anchor.getProceedingKey(), resolveTrack(processo, ProcessoLifecycleAction.INICIAR_CUMPRIMENTO));
            if (proceeding.getStatus() == CaseProceedingStatus.CLOSED) {
                proceeding.setStatus(CaseProceedingStatus.ACTIVE);
            }
            caseProceedingRepository.save(proceeding);
        }
        upsertEdge(caseFile.getId(), anchor.getProceedingKey(), execKey, RecursalRelationType.EXECUTION_CONTINUATION, LegalAppealType.OUTRO);
    }

    private void closeCaseProceedings(Long caseFileId, String anchorProceedingKey, Processo processo) {
        for (CaseProceeding proceeding : caseProceedingRepository.findAllByCaseFileId(caseFileId)) {
            if (Objects.equals(proceeding.getLinkedProcessoId(), processo.getId()) || Objects.equals(proceeding.getParentProceedingKey(), anchorProceedingKey) || Objects.equals(proceeding.getProceedingKey(), anchorProceedingKey)) {
                proceeding.setStatus(CaseProceedingStatus.CLOSED);
                proceeding.setContinuityTrack(CaseContinuityTrack.ARQUIVADO);
                proceeding.setProceedingRole(Objects.equals(proceeding.getProceedingKey(), anchorProceedingKey) ? CaseProceedingRole.TERMINAL : proceeding.getProceedingRole());
                proceeding.setSourceFaseProcessual(processo.getFaseAtual());
                proceeding.setSourceStatusProcesso(processo.getStatusProcesso());
                proceeding.setLastSyncAt(Instant.now());
                caseProceedingRepository.save(proceeding);
            }
        }
    }

    private void reopenCaseProceedings(Long caseFileId, String anchorProceedingKey, Processo processo) {
        for (CaseProceeding proceeding : caseProceedingRepository.findAllByCaseFileId(caseFileId)) {
            if (Objects.equals(proceeding.getLinkedProcessoId(), processo.getId()) || Objects.equals(proceeding.getParentProceedingKey(), anchorProceedingKey) || Objects.equals(proceeding.getProceedingKey(), anchorProceedingKey)) {
                proceeding.setStatus(CaseProceedingStatus.ACTIVE);
                proceeding.setContinuityTrack(resolveTrack(processo, ProcessoLifecycleAction.DESARQUIVAR));
                if (Objects.equals(proceeding.getProceedingKey(), anchorProceedingKey)) {
                    proceeding.setProceedingRole(CaseProceedingRole.ROOT);
                }
                proceeding.setSourceFaseProcessual(processo.getFaseAtual());
                proceeding.setSourceStatusProcesso(processo.getStatusProcesso());
                proceeding.setLastSyncAt(Instant.now());
                caseProceedingRepository.save(proceeding);
            }
        }
        upsertEdge(caseFileId, anchorProceedingKey, anchorProceedingKey, RecursalRelationType.ARCHIVAL_CONTINUATION, LegalAppealType.OUTRO);
    }

    private CaseFile chooseCanonical(CaseFile base, CaseFile related) {
        if (base == null) return related;
        if (related == null) return base;
        long baseRoot = base.getRootProcessoId() == null ? Long.MAX_VALUE : base.getRootProcessoId();
        long relatedRoot = related.getRootProcessoId() == null ? Long.MAX_VALUE : related.getRootProcessoId();
        return baseRoot <= relatedRoot ? base : related;
    }

    private Long minRoot(CaseFile base, CaseFile related) {
        long baseRoot = base == null || base.getRootProcessoId() == null ? Long.MAX_VALUE : base.getRootProcessoId();
        long relatedRoot = related == null || related.getRootProcessoId() == null ? Long.MAX_VALUE : related.getRootProcessoId();
        long min = Math.min(baseRoot, relatedRoot);
        return min == Long.MAX_VALUE ? null : min;
    }

    private void mergeCaseFiles(CaseFile target, CaseFile source) {
        if (target == null || source == null || Objects.equals(target.getId(), source.getId())) {
            return;
        }
        List<CaseProceeding> sourceProceedings = new ArrayList<>(caseProceedingRepository.findAllByCaseFileId(source.getId()));
        for (CaseProceeding sourceProceeding : sourceProceedings) {
            CaseProceeding existing = caseProceedingRepository.findByProceedingKey(sourceProceeding.getProceedingKey()).orElse(null);
            if (existing != null && !Objects.equals(existing.getId(), sourceProceeding.getId()) && Objects.equals(existing.getCaseFileId(), target.getId())) {
                mergeProceeding(existing, sourceProceeding);
                caseProceedingRepository.save(existing);
                caseProceedingRepository.delete(sourceProceeding);
                continue;
            }
            sourceProceeding.setCaseFileId(target.getId());
            sourceProceeding.setLastSyncAt(Instant.now());
            caseProceedingRepository.save(sourceProceeding);
        }
        Set<String> targetEdgeKeys = new LinkedHashSet<>();
        for (CaseEdge edge : caseEdgeRepository.findAllByCaseFileId(target.getId())) {
            targetEdgeKeys.add(edgeKey(edge.getFromProceedingKey(), edge.getToProceedingKey(), edge.getRelationType(), edge.getAppealType()));
        }
        for (CaseEdge edge : new ArrayList<>(caseEdgeRepository.findAllByCaseFileId(source.getId()))) {
            String key = edgeKey(edge.getFromProceedingKey(), edge.getToProceedingKey(), edge.getRelationType(), edge.getAppealType());
            if (targetEdgeKeys.contains(key)) {
                caseEdgeRepository.delete(edge);
                continue;
            }
            edge.setCaseFileId(target.getId());
            caseEdgeRepository.save(edge);
            targetEdgeKeys.add(key);
        }
        caseFileRepository.delete(source);
    }

    private void mergeProceeding(CaseProceeding target, CaseProceeding source) {
        if (target.getLinkedProcessoId() == null) target.setLinkedProcessoId(source.getLinkedProcessoId());
        if (target.getCourt() == null || target.getCourt().isBlank()) target.setCourt(source.getCourt());
        if (target.getNumeroUnificado() == null || target.getNumeroUnificado().isBlank()) target.setNumeroUnificado(source.getNumeroUnificado());
        if (target.getParentProceedingKey() == null || target.getParentProceedingKey().isBlank()) target.setParentProceedingKey(source.getParentProceedingKey());
        if (target.getSourceFaseProcessual() == null) target.setSourceFaseProcessual(source.getSourceFaseProcessual());
        if (target.getSourceStatusProcesso() == null) target.setSourceStatusProcesso(source.getSourceStatusProcesso());
        if (statusRank(source.getStatus()) > statusRank(target.getStatus())) target.setStatus(source.getStatus());
        target.setContinuityTrack(maxTrack(target.getContinuityTrack(), source.getContinuityTrack()));
        target.setProceedingRole(maxRole(target.getProceedingRole(), source.getProceedingRole()));
        target.setSecrecy(maxSigilo(target.getSecrecy(), source.getSecrecy()));
        target.setLastSyncAt(Instant.now());
    }

    private CaseProceeding refreshAnchor(Long caseFileId, Long processoId) {
        return caseProceedingRepository.findFirstByCaseFileIdAndLinkedProcessoIdAndParentProceedingKeyIsNull(caseFileId, processoId).orElse(null);
    }

    private boolean syncAnchor(CaseProceeding anchor, Processo processo, CaseProceedingRole role, String parentProceedingKey, CaseContinuityTrack track) {
        boolean dirty = false;
        if (anchor.getLinkedProcessoId() == null && processo.getId() != null) {
            anchor.setLinkedProcessoId(processo.getId());
            dirty = true;
        }
        if (anchor.getStatus() == null || statusRank(mapProceedingStatus(processo)) > statusRank(anchor.getStatus())) {
            anchor.setStatus(mapProceedingStatus(processo));
            dirty = true;
        }
        if (!Objects.equals(anchor.getContinuityTrack(), track)) {
            anchor.setContinuityTrack(track);
            dirty = true;
        }
        if (!Objects.equals(anchor.getProceedingRole(), role)) {
            anchor.setProceedingRole(role);
            dirty = true;
        }
        if (!Objects.equals(anchor.getParentProceedingKey(), parentProceedingKey)) {
            anchor.setParentProceedingKey(parentProceedingKey);
            dirty = true;
        }
        if (!Objects.equals(anchor.getSourceFaseProcessual(), processo.getFaseAtual())) {
            anchor.setSourceFaseProcessual(processo.getFaseAtual());
            dirty = true;
        }
        if (!Objects.equals(anchor.getSourceStatusProcesso(), processo.getStatusProcesso())) {
            anchor.setSourceStatusProcesso(processo.getStatusProcesso());
            dirty = true;
        }
        String nextCourt = firstNonBlank(anchor.getCourt(), processo.getTribunal(), processo.getTribunalCodigoRoteado());
        if (!Objects.equals(anchor.getCourt(), nextCourt)) {
            anchor.setCourt(nextCourt);
            dirty = true;
        }
        String nextNumero = firstNonBlank(anchor.getNumeroUnificado(), processo.getNumeroUnificado(), processo.getNumeroProcesso());
        if (!Objects.equals(anchor.getNumeroUnificado(), nextNumero)) {
            anchor.setNumeroUnificado(nextNumero);
            dirty = true;
        }
        NivelSigilo nextSigilo = maxSigilo(anchor.getSecrecy(), processo.getNivelSigilo());
        if (nextSigilo != anchor.getSecrecy()) {
            anchor.setSecrecy(nextSigilo);
            dirty = true;
        }
        anchor.setLastSyncAt(Instant.now());
        return dirty;
    }

    private void upsertEdge(Long caseFileId, String fromKey, String toKey, RecursalRelationType relationType, LegalAppealType appealType) {
        String key = edgeKey(fromKey, toKey, relationType, appealType);
        boolean exists = caseEdgeRepository.findAllByCaseFileId(caseFileId).stream()
                .anyMatch(edge -> edgeKey(edge.getFromProceedingKey(), edge.getToProceedingKey(), edge.getRelationType(), edge.getAppealType()).equals(key));
        if (exists) {
            return;
        }
        caseEdgeRepository.save(CaseEdge.builder()
                .caseFileId(caseFileId)
                .fromProceedingKey(fromKey)
                .toProceedingKey(toKey)
                .relationType(relationType)
                .appealType(appealType == null ? LegalAppealType.OUTRO : appealType)
                .build());
    }

    private Processo loadProcesso(Long processoId) {
        return processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado: " + processoId));
    }

    private static String edgeKey(String fromKey, String toKey, RecursalRelationType relationType, LegalAppealType appealType) {
        return fromKey + '|' + toKey + '|' + relationType.name() + '|' + (appealType == null ? LegalAppealType.OUTRO.name() : appealType.name());
    }

    private static RecursalRelationType mapRelation(VinculoProcessualTipo vinculoTipo) {
        return switch (vinculoTipo) {
            case APENSAMENTO, DEPENDENCIA -> RecursalRelationType.INCIDENT_WITHIN;
            case PREVENCAO, CONEXAO, CONTINENCIA, REFERENCIA -> RecursalRelationType.PROCESSUAL_LINKAGE;
            case NENHUM -> RecursalRelationType.RELATED_AUTONOMOUS_ACTION;
        };
    }

    private static CaseProceedingStatus mapProceedingStatus(Processo processo) {
        if (processo == null) return CaseProceedingStatus.ACTIVE;
        if (processo.getStatusProcesso() == StatusProcesso.ARQUIVADO || processo.getStatusProcesso() == StatusProcesso.BAIXADO) return CaseProceedingStatus.CLOSED;
        if (processo.getStatusProcesso() == StatusProcesso.TRANSITO_EM_JULGADO) return CaseProceedingStatus.RECONCILED;
        return CaseProceedingStatus.ACTIVE;
    }

    private static CaseContinuityTrack resolveTrack(Processo processo, ProcessoLifecycleAction action) {
        return CaseContinuityTrack.resolve(action,
                processo == null ? null : processo.getFaseAtual(),
                processo == null ? null : processo.getStatusProcesso());
    }

    private static int statusRank(CaseProceedingStatus status) {
        if (status == null) return 0;
        return switch (status) {
            case PREDICTED -> 10;
            case ACTIVE -> 30;
            case RECONCILED -> 40;
            case MERGED -> 45;
            case CLOSED -> 50;
        };
    }

    private static NivelSigilo maxSigilo(NivelSigilo current, NivelSigilo next) {
        NivelSigilo a = current == null ? NivelSigilo.PUBLICO : current;
        NivelSigilo b = next == null ? NivelSigilo.PUBLICO : next;
        return a.getNivel() >= b.getNivel() ? a : b;
    }

    private static CaseContinuityTrack maxTrack(CaseContinuityTrack current, CaseContinuityTrack next) {
        return CaseContinuityTrack.dominant(current, next);
    }

    private static CaseProceedingRole maxRole(CaseProceedingRole current, CaseProceedingRole next) {
        return CaseProceedingRole.dominant(current, next);
    }

    private static int trackRank(CaseContinuityTrack track) {
        return track == null ? 0 : track.priorityRank();
    }

    private static int roleRank(CaseProceedingRole role) {
        return role == null ? 0 : role.priorityRank();
    }

    private static CaseContinuityTrack dominantTrack(List<CaseProceeding> proceedings) {
        return proceedings.stream()
                .map(CaseProceeding::getContinuityTrack)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(CaseContinuityOrchestratorService::trackRank))
                .orElse(CaseContinuityTrack.CONHECIMENTO);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void auditContinuity(String action, Long caseFileId, Long processoId, String proceedingKey, CaseContinuityTrack track, String detail) {
        auditLedgerService.appendSafely(action, "CASE_FILE", String.valueOf(caseFileId), String.join("|",
                caseFileId == null ? "-" : String.valueOf(caseFileId),
                processoId == null ? "-" : String.valueOf(processoId),
                proceedingKey == null ? "-" : proceedingKey,
                track == null ? "-" : track.name(),
                detail == null ? "-" : detail));
    }

    private static String processDescriptor(Processo processo) {
        if (processo == null) return null;
        return firstNonBlank(processo.getNumeroUnificado(), processo.getNumeroProcesso(), processo.getId() == null ? null : String.valueOf(processo.getId()));
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) return null;
        return value.length() <= 240 ? value : value.substring(0, 240);
    }

    private record CaseContinuityPayload(Long processoId, String trigger, String track, String descriptor) {}

    private record CaseMergePayload(Long baseProcessoId, Long relatedProcessoId, String linkageType, Long canonicalRootProcessoId, String reason) {}
}
