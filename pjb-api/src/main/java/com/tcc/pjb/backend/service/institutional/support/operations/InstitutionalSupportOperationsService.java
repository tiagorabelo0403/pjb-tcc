package com.tcc.pjb.backend.service.institutional.support.operations;

import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import com.tcc.pjb.backend.model.dto.institutional.support.operations.InstitutionalSupportCompetenceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.operations.InstitutionalSupportCoverageSnapshotResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.operations.InstitutionalSupportPrepautaSnapshotResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.operational.catalog.OperationalCompetenceMatrixService;
import com.tcc.pjb.backend.service.operational.catalog.OperationalCoveragePlannerService;
import com.tcc.pjb.backend.service.operational.catalog.ProceduralFormalCatalogService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatProcessContactEnvelopeResolver;
import com.tcc.pjb.backend.service.institutional.support.lane.InstitutionalSupportLaneResolver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalSupportOperationsService {

    private static final List<WorkItemStatus> ACTIVE_STATUSES = List.of(WorkItemStatus.PENDENTE, WorkItemStatus.EM_EXECUCAO);

    private final InstitutionalSupportLaneResolver laneResolver;
    private final WorkItemRepository workItemRepository;
    private final ProcessoRepository processoRepository;
    private final SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver;
    private final OperationalCompetenceMatrixService competenceMatrixService;
    private final OperationalCoveragePlannerService coveragePlannerService;
    private final ProceduralFormalCatalogService formalCatalogService;

    public InstitutionalSupportOperationsService(InstitutionalSupportLaneResolver laneResolver,
                                                 WorkItemRepository workItemRepository,
                                                 ProcessoRepository processoRepository,
                                                 SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver,
                                                 OperationalCompetenceMatrixService competenceMatrixService,
                                                 OperationalCoveragePlannerService coveragePlannerService,
                                                 ProceduralFormalCatalogService formalCatalogService) {
        this.laneResolver = Objects.requireNonNull(laneResolver);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.contactEnvelopeResolver = Objects.requireNonNull(contactEnvelopeResolver);
        this.competenceMatrixService = Objects.requireNonNull(competenceMatrixService);
        this.coveragePlannerService = Objects.requireNonNull(coveragePlannerService);
        this.formalCatalogService = Objects.requireNonNull(formalCatalogService);
    }

    @Transactional(readOnly = true)
    public InstitutionalSupportCompetenceSnapshotResponse competenceMatrix(String branchCode) {
        InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane = requireLane(branchCode);
        List<WorkItem> items = activeItems(lane, 160);
        OperationalCompetenceMatrixService.MatrixProjection projection = competenceMatrixService.resolveInstitutional(branchCode, lane, null, items);
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("snapshotPath", OperationalApiRoutes.institutionalSupportSnapshot(lane.branchCode()));
        routes.put("agendaPath", OperationalApiRoutes.institutionalSupportAgenda(lane.branchCode()));
        routes.put("competenceMatrixPath", OperationalApiRoutes.institutionalSupportCompetenceMatrix(lane.branchCode()));
        routes.put("coveragePath", OperationalApiRoutes.institutionalSupportCoverage(lane.branchCode()));
        return new InstitutionalSupportCompetenceSnapshotResponse(
                Instant.now(),
                laneMap(lane),
                projection.metrics(),
                projection.rules().stream().map(rule -> new InstitutionalSupportCompetenceSnapshotResponse.Rule(
                        rule.actCode(),
                        rule.actLabel(),
                        rule.actAxis(),
                        rule.minimumRole(),
                        rule.delegatedFunction(),
                        rule.ramoAxis(),
                        rule.ritoAxis(),
                        rule.phaseAxis(),
                        rule.secrecyAxis(),
                        rule.functionalCredentialRequired(),
                        rule.institutionalScopes(),
                        rule.signals()
                )).toList(),
                projection.warnings(),
                Map.copyOf(routes)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalSupportCoverageSnapshotResponse coverage(String branchCode) {
        InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane = requireLane(branchCode);
        List<WorkItem> items = activeItems(lane, 220);
        OperationalCoveragePlannerService.CoverageProjection projection = coveragePlannerService.resolveInstitutional(branchCode, items);
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("coveragePath", OperationalApiRoutes.institutionalSupportCoverage(lane.branchCode()));
        routes.put("snapshotPath", OperationalApiRoutes.institutionalSupportSnapshot(lane.branchCode()));
        return new InstitutionalSupportCoverageSnapshotResponse(
                Instant.now(),
                laneMap(lane),
                projection.metrics(),
                projection.slices().stream().map(slice -> new InstitutionalSupportCoverageSnapshotResponse.Cell(
                        slice.sliceKey(),
                        slice.label(),
                        slice.totalItems(),
                        slice.overdueItems(),
                        slice.blockingItems(),
                        slice.unassignedItems(),
                        slice.loadBand(),
                        slice.nextDueAt(),
                        slice.substitutePool(),
                        slice.redistributionSuggestions(),
                        slice.metrics()
                )).toList(),
                projection.gaps(),
                projection.warnings(),
                Map.copyOf(routes)
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalSupportPrepautaSnapshotResponse prePauta(String branchCode, Long processoId) {
        InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane = requireLane(branchCode);
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new IllegalArgumentException("Processo não encontrado para pré-pauta institucional."));
        List<WorkItem> timeline = workItemRepository.findAllByProcesso(processoId);
        ProceduralFormalCatalogService.FormalCatalogProjection formalCatalog = formalCatalogService.resolveInstitutionalCatalog(branchCode, lane, processo, timeline);
        Map<String, Object> contactEnvelope = contactEnvelopeResolver.buildEnvelope(processo);
        LinkedHashMap<String, Object> processoMap = new LinkedHashMap<>();
        processoMap.put("processoId", processo.getId());
        processoMap.put("numeroProcesso", firstNonBlank(processo.getNumeroCNJ(), processo.getNumeroProcesso(), processo.getNumero()));
        processoMap.put("ramoDireito", processo.getRamoDireito() == null ? null : processo.getRamoDireito().name());
        processoMap.put("ritoProcessual", processo.getRito() == null ? null : processo.getRito().name());
        processoMap.put("faseAtual", processo.getFaseAtual() == null ? null : processo.getFaseAtual().name());
        processoMap.put("classeProcessual", processo.getClasseProcessual());
        processoMap.put("assunto", processo.getAssunto());
        processoMap.put("nivelSigilo", processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name());
        processoMap.put("vara", processo.getVara());
        processoMap.put("tribunal", processo.getTribunal());
        processoMap.put("uf", processo.getUf());
        processoMap.put("comarca", processo.getComarca());
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("timelineSize", timeline.size());
        metrics.put("pendingActs", timeline.stream().filter(item -> ACTIVE_STATUSES.contains(item.getStatus())).count());
        metrics.put("blockingActs", timeline.stream().filter(WorkItem::isBlocking).count());
        metrics.put("formalDocuments", formalCatalog.documents().size());
        List<InstitutionalSupportPrepautaSnapshotResponse.TimelineItem> timelineItems = timeline.stream()
                .sorted(Comparator.comparing(WorkItem::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(item -> new InstitutionalSupportPrepautaSnapshotResponse.TimelineItem(
                        item.getId(),
                        item.getTitulo(),
                        item.getStatus() == null ? null : item.getStatus().name(),
                        item.getQueueCode(),
                        firstNonNull(item.getDueAt(), item.getUpdatedAt(), item.getCreatedAt()),
                        item.isBlocking(),
                        List.of(item.getAssignedRole() == null ? "SEM_PAPEL" : item.getAssignedRole().name())
                )).toList();
        List<InstitutionalSupportPrepautaSnapshotResponse.PendingAct> pendingActs = timeline.stream()
                .filter(item -> ACTIVE_STATUSES.contains(item.getStatus()))
                .map(item -> new InstitutionalSupportPrepautaSnapshotResponse.PendingAct(
                        firstNonBlank(item.getQueueCode(), item.getTemplateCode(), "ATO_PENDENTE"),
                        item.getTitulo(),
                        item.isBlocking() ? "CRITICO" : item.getDueAt() != null && item.getDueAt().isBefore(Instant.now()) ? "ALTO" : "MODERADO",
                        item.getDueAt(),
                        item.isBlocking(),
                        buildSignals(item)
                )).toList();
        List<InstitutionalSupportPrepautaSnapshotResponse.ChecklistItem> checklist = buildChecklist(processo, timeline, contactEnvelope, formalCatalog);
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (timeline.isEmpty()) {
            warnings.add("Pré-pauta sem trilha viva de work items; revisar distribuição institucional por especialidade.");
        }
        if (contactEnvelope.isEmpty()) {
            warnings.add("Envelope de contatos ausente; o pacote do membro exige saneamento mínimo antes do ato.");
        }
        if (processo.getNivelSigilo() != null && processo.getNivelSigilo().name().startsWith("SIGILO")) {
            warnings.add("Processo sensível: restringir dossiê, contatos e peças à célula institucional autorizada.");
        }
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("snapshotPath", OperationalApiRoutes.institutionalSupportSnapshot(lane.branchCode()));
        routes.put("prePautaPath", OperationalApiRoutes.institutionalSupportProcessPrePauta(lane.branchCode(), processoId));
        routes.put("coveragePath", OperationalApiRoutes.institutionalSupportCoverage(lane.branchCode()));
        return new InstitutionalSupportPrepautaSnapshotResponse(
                Instant.now(),
                laneMap(lane),
                Map.copyOf(processoMap),
                Map.copyOf(metrics),
                timelineItems,
                checklist,
                pendingActs,
                formalCatalog.documents().stream().map(document -> new InstitutionalSupportPrepautaSnapshotResponse.DocumentTemplate(
                        document.documentCode(),
                        document.title(),
                        document.actAxis(),
                        document.targetPhase(),
                        document.sensitive(),
                        document.tags()
                )).toList(),
                contactEnvelope,
                List.copyOf(warnings),
                Map.copyOf(routes)
        );
    }

    private List<InstitutionalSupportPrepautaSnapshotResponse.ChecklistItem> buildChecklist(Processo processo,
                                                                                            List<WorkItem> timeline,
                                                                                            Map<String, Object> contactEnvelope,
                                                                                            ProceduralFormalCatalogService.FormalCatalogProjection formalCatalog) {
        List<InstitutionalSupportPrepautaSnapshotResponse.ChecklistItem> items = new ArrayList<>();
        items.add(check("RESUMO_CASO", "Resumo do caso materializado", processo.getResumoIA() != null || processo.getAnaliseTriagemV1() != null, "ALTO",
                processo.getResumoIA() != null ? "Resumo IA disponível." : "Resumo ausente; preparar síntese institucional."));
        items.add(check("CONTATOS", "Contatos essenciais disponíveis", !contactEnvelope.isEmpty(), "CRITICO",
                contactEnvelope.isEmpty() ? "Falta envelope de contatos do processo." : "Envelope de contatos pronto para o membro."));
        items.add(check("TIMELINE", "Linha do tempo operacional viva", !timeline.isEmpty(), "ALTO",
                timeline.isEmpty() ? "Sem work items vivos no processo." : "Trilha operacional presente."));
        items.add(check("FORMAL_CATALOG", "Pacote formal projetado", !formalCatalog.documents().isEmpty(), "MEDIO",
                formalCatalog.documents().isEmpty() ? "Sem documentos formais projetados." : formalCatalog.documents().size() + " modelos prontos para o caso."));
        items.add(check("RISCO_ATO", "Risco do ato identificado", timeline.stream().anyMatch(WorkItem::isBlocking), "ALTO",
                timeline.stream().anyMatch(WorkItem::isBlocking) ? "Há diligência bloqueante ou dependência crítica." : "Sem bloqueio crítico imediato."));
        return List.copyOf(items);
    }

    private InstitutionalSupportPrepautaSnapshotResponse.ChecklistItem check(String code,
                                                                             String label,
                                                                             boolean ok,
                                                                             String severity,
                                                                             String detail) {
        return new InstitutionalSupportPrepautaSnapshotResponse.ChecklistItem(code, label, ok ? "OK" : "PENDENTE", severity, detail);
    }

    private List<String> buildSignals(WorkItem item) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        if (item.isBlocking()) {
            signals.add("BLOCKING");
        }
        if (item.getDueAt() != null && item.getDueAt().isBefore(Instant.now())) {
            signals.add("OVERDUE");
        }
        if (item.getAssignedRole() != null) {
            signals.add(item.getAssignedRole().name());
        }
        if (item.getFaseOrigem() != null) {
            signals.add(item.getFaseOrigem().name());
        }
        return List.copyOf(signals);
    }

    private List<WorkItem> activeItems(InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane, int limit) {
        return workItemRepository.findActiveByInboxPrefixAndTerritory(
                lane.inboxPrefix(),
                lane.uf(),
                denormalizeComarca(lane.comarca()),
                ACTIVE_STATUSES,
                PageRequest.of(0, limit)
        );
    }

    private InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot requireLane(String branchCode) {
        InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane = laneResolver.requireCurrentUser();
        if (branchCode != null && !branchCode.isBlank() && !lane.branchCode().equalsIgnoreCase(branchCode)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "branchCode fora do escopo institucional do usuário");
        }
        return lane;
    }

    private Map<String, Object> laneMap(InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("branchCode", lane.branchCode());
        out.put("branchLabel", lane.branchLabel());
        out.put("scope", lane.scope());
        out.put("federativeAxis", lane.federativeAxis());
        out.put("tribunalCodigo", lane.tribunalCodigo());
        out.put("uf", lane.uf());
        out.put("comarca", lane.comarca());
        out.put("inboxPrefix", lane.inboxPrefix());
        out.put("memberPanelPath", lane.memberPanelPath());
        return Collections.unmodifiableMap(out);
    }

    private String denormalizeComarca(String comarca) {
        return comarca == null ? null : comarca.replace('_', ' ');
    }

    private static <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
