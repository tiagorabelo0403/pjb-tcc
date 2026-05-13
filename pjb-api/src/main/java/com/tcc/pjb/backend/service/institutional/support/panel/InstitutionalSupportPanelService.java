package com.tcc.pjb.backend.service.institutional.support.panel;

import com.tcc.pjb.backend.model.dto.institutional.support.panel.InstitutionalSupportPanelGroupResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.panel.InstitutionalSupportPanelItemResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.panel.InstitutionalSupportPanelSnapshotResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.operations.InstitutionalSupportCompetenceSnapshotResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.operations.InstitutionalSupportCoverageSnapshotResponse;
import com.tcc.pjb.backend.model.dto.institutional.support.operations.InstitutionalSupportPrepautaSnapshotResponse;
import com.tcc.pjb.backend.model.dto.security.operational.OperationalCredentialSnapshotResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.WorkItemStatus;
import com.tcc.pjb.backend.model.entity.workflow.WorkItem;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatProcessContactEnvelopeResolver;
import com.tcc.pjb.backend.service.institutional.support.lane.InstitutionalSupportLaneResolver;
import com.tcc.pjb.backend.service.institutional.support.operations.InstitutionalSupportOperationsService;
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitutionalSupportPanelService {

    private static final List<WorkItemStatus> ACTIVE_STATUSES = List.of(WorkItemStatus.PENDENTE, WorkItemStatus.EM_EXECUCAO);
    private static final int PANEL_LIMIT = 120;

    private final InstitutionalSupportLaneResolver laneResolver;
    private final WorkItemRepository workItemRepository;
    private final SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver;
    private final OperationalFunctionCredentialService credentialService;
    private final InstitutionalSupportOperationsService operationsService;

    public InstitutionalSupportPanelService(InstitutionalSupportLaneResolver laneResolver,
                                            WorkItemRepository workItemRepository,
                                            SecretariatProcessContactEnvelopeResolver contactEnvelopeResolver,
                                            OperationalFunctionCredentialService credentialService,
                                            InstitutionalSupportOperationsService operationsService) {
        this.laneResolver = Objects.requireNonNull(laneResolver);
        this.workItemRepository = Objects.requireNonNull(workItemRepository);
        this.contactEnvelopeResolver = Objects.requireNonNull(contactEnvelopeResolver);
        this.credentialService = Objects.requireNonNull(credentialService);
        this.operationsService = Objects.requireNonNull(operationsService);
    }


    @Transactional(readOnly = true)
    public InstitutionalSupportCompetenceSnapshotResponse competenceMatrix(String branchCode) {
        return operationsService.competenceMatrix(branchCode);
    }

    @Transactional(readOnly = true)
    public InstitutionalSupportCoverageSnapshotResponse coverage(String branchCode) {
        return operationsService.coverage(branchCode);
    }

    @Transactional(readOnly = true)
    public InstitutionalSupportPrepautaSnapshotResponse prePauta(String branchCode, Long processoId) {
        return operationsService.prePauta(branchCode, processoId);
    }

    @Transactional(readOnly = true)
    public InstitutionalSupportPanelSnapshotResponse snapshot(String branchCode) {
        InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane = requireBranch(branchCode);
        List<WorkItem> items = workItemRepository.findActiveByInboxPrefixAndTerritory(
                lane.inboxPrefix(),
                lane.uf(),
                denormalizeComarca(lane.comarca()),
                ACTIVE_STATUSES,
                PageRequest.of(0, PANEL_LIMIT)
        );
        long total = workItemRepository.countActiveByInboxPrefixAndTerritory(
                lane.inboxPrefix(),
                lane.uf(),
                denormalizeComarca(lane.comarca()),
                ACTIVE_STATUSES
        );
        List<InstitutionalSupportPanelItemResponse> projected = items.stream().map(item -> projectItem(item, lane)).toList();
        OperationalCredentialSnapshotResponse credential = credentialService.snapshotForCurrentUser("INSTITUTIONAL_SUPPORT");
        LinkedHashMap<String, Object> laneMap = new LinkedHashMap<>();
        laneMap.put("branchCode", lane.branchCode());
        laneMap.put("branchLabel", lane.branchLabel());
        laneMap.put("scope", lane.scope());
        laneMap.put("federativeAxis", lane.federativeAxis());
        laneMap.put("tribunalCodigo", lane.tribunalCodigo());
        laneMap.put("uf", lane.uf());
        laneMap.put("comarca", lane.comarca());
        laneMap.put("inboxPrefix", lane.inboxPrefix());
        laneMap.put("forumAnchor", lane.forumAnchor());
        laneMap.put("actorRoles", lane.actorRoles());
        laneMap.put("capabilities", lane.capabilities());
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("visibleItems", projected.size());
        metrics.put("totalActiveItems", total);
        metrics.put("audienciaItems", projected.stream().filter(this::looksLikeAudiencia).count());
        metrics.put("sessaoItems", projected.stream().filter(this::looksLikeSessao).count());
        metrics.put("intimacaoItems", projected.stream().filter(this::looksLikeIntimacao).count());
        metrics.put("processosUnicos", projected.stream().map(InstitutionalSupportPanelItemResponse::processoId).filter(Objects::nonNull).distinct().count());
        metrics.put("ritosUnicos", projected.stream().map(InstitutionalSupportPanelItemResponse::ritoProcessual).filter(Objects::nonNull).distinct().count());
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("snapshotPath", lane.snapshotPath());
        routes.put("agendaPath", lane.snapshotPath().replace("/snapshot", "/agenda"));
        routes.put("credentialBasePath", lane.credentialBasePath());
        routes.put("memberPanelPath", lane.memberPanelPath());
        routes.put("frontMode", "INSTITUTIONAL_SUPPORT_DESK");
        ArrayList<String> warnings = new ArrayList<>(lane.warnings());
        if (projected.isEmpty()) {
            warnings.add("Nenhum item ativo caiu na malha institucional filtrada para a secretaria deste órgão.");
        }
        return new InstitutionalSupportPanelSnapshotResponse(
                Instant.now(),
                compactMap(laneMap),
                compactMap(metrics),
                List.copyOf(projected),
                groupByProcesso(projected),
                groupByRito(projected),
                groupByData(projected),
                credentialPayload(lane, credential),
                compactMap(routes),
                List.copyOf(new LinkedHashSet<>(warnings))
        );
    }

    @Transactional(readOnly = true)
    public InstitutionalSupportPanelSnapshotResponse agenda(String branchCode) {
        InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane = requireBranch(branchCode);
        Instant from = Instant.now().minusSeconds(12 * 3600L);
        Instant to = Instant.now().plusSeconds(14L * 24L * 3600L);
        List<WorkItem> items = workItemRepository.findCalendarWindowByInboxPrefixAndTerritory(
                lane.inboxPrefix(),
                lane.uf(),
                denormalizeComarca(lane.comarca()),
                from,
                to,
                ACTIVE_STATUSES,
                PageRequest.of(0, PANEL_LIMIT)
        );
        List<InstitutionalSupportPanelItemResponse> projected = items.stream().map(item -> projectItem(item, lane)).toList();
        OperationalCredentialSnapshotResponse credential = credentialService.snapshotForCurrentUser("INSTITUTIONAL_SUPPORT");
        LinkedHashMap<String, Object> laneMap = new LinkedHashMap<>();
        laneMap.put("branchCode", lane.branchCode());
        laneMap.put("branchLabel", lane.branchLabel());
        laneMap.put("scope", lane.scope());
        laneMap.put("tribunalCodigo", lane.tribunalCodigo());
        laneMap.put("uf", lane.uf());
        laneMap.put("comarca", lane.comarca());
        LinkedHashMap<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("agendaWindowFrom", from);
        metrics.put("agendaWindowTo", to);
        metrics.put("visibleItems", projected.size());
        metrics.put("audienciaItems", projected.stream().filter(this::looksLikeAudiencia).count());
        metrics.put("sessaoItems", projected.stream().filter(this::looksLikeSessao).count());
        ArrayList<String> warnings = new ArrayList<>(lane.warnings());
        if (projected.isEmpty()) {
            warnings.add("A agenda institucional não encontrou audiência, sessão ou prazo operacional na janela consultada.");
        }
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("snapshotPath", lane.snapshotPath());
        routes.put("agendaPath", lane.snapshotPath().replace("/snapshot", "/agenda"));
        routes.put("credentialBasePath", lane.credentialBasePath());
        routes.put("memberPanelPath", lane.memberPanelPath());
        return new InstitutionalSupportPanelSnapshotResponse(
                Instant.now(),
                compactMap(laneMap),
                compactMap(metrics),
                List.copyOf(projected),
                groupByProcesso(projected),
                groupByRito(projected),
                groupByData(projected),
                credentialPayload(lane, credential),
                compactMap(routes),
                List.copyOf(new LinkedHashSet<>(warnings))
        );
    }

    private InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot requireBranch(String branchCode) {
        InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane = laneResolver.requireCurrentUser();
        if (branchCode != null && !branchCode.isBlank() && !lane.branchCode().equalsIgnoreCase(branchCode)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "branchCode fora do escopo institucional do usuário");
        }
        return lane;
    }

    private InstitutionalSupportPanelItemResponse projectItem(WorkItem item,
                                                              InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane) {
        Processo processo = item.getProcesso();
        Map<String, Object> envelope = contactEnvelopeResolver.buildEnvelope(processo);
        Map<String, Object> principal = principalContact(envelope);
        ArrayList<String> tags = new ArrayList<>();
        tags.add(lane.branchCode());
        if (processo != null && processo.getRito() != null) {
            tags.add(processo.getRito().group());
        }
        if (looksLikeAudiencia(item)) {
            tags.add("AUDIENCIA");
        }
        if (looksLikeSessao(item)) {
            tags.add("SESSAO");
        }
        if (looksLikeIntimacao(item)) {
            tags.add("INTIMACAO");
        }
        LinkedHashMap<String, Object> routes = new LinkedHashMap<>();
        routes.put("memberPanelPath", lane.memberPanelPath());
        routes.put("supportSnapshotPath", lane.snapshotPath());
        routes.put("supportAgendaPath", lane.snapshotPath().replace("/snapshot", "/agenda"));
        if (processo != null && processo.getId() != null) {
            routes.put("processoPath", lane.memberPanelPath());
            routes.put("processoId", processo.getId());
        }
        return new InstitutionalSupportPanelItemResponse(
                item.getId(),
                processo == null ? null : processo.getId(),
                processo == null ? null : processo.getNumeroProcesso(),
                item.getTitulo(),
                item.getStatus() == null ? null : item.getStatus().name(),
                item.getQueueCode(),
                item.getInboxKey(),
                processo == null || processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                processo == null || processo.getRito() == null ? null : processo.getRito().name(),
                processo == null ? null : processo.getClasseProcessual(),
                processo == null ? null : processo.getVara(),
                firstNonBlank(item.getComarca(), processo == null ? null : processo.getComarca()),
                firstNonBlank(item.getUf(), processo == null ? null : processo.getUf()),
                item.getDueAt(),
                firstNonNull(item.getUpdatedAt(), item.getCreatedAt()),
                stringOf(principal.get("nome")),
                stringOf(principal.get("email")),
                compactMap(envelope),
                List.copyOf(new LinkedHashSet<>(tags)),
                compactMap(routes)
        );
    }

    private Map<String, Object> credentialPayload(InstitutionalSupportLaneResolver.InstitutionalSupportLaneSnapshot lane,
                                                  OperationalCredentialSnapshotResponse snapshot) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("laneCode", snapshot.laneCode());
        out.put("entries", snapshot.entries().stream().map(entry -> {
            LinkedHashMap<String, Object> item = new LinkedHashMap<>();
            item.put("functionCode", entry.functionCode());
            item.put("label", entry.label());
            item.put("status", entry.status());
            item.put("active", entry.active());
            item.put("resetRequired", entry.resetRequired());
            item.put("locked", entry.locked());
            item.put("routes", compactMap(java.util.Map.of(
                    "challengePath", com.tcc.pjb.backend.core.operational.OperationalApiRoutes.institutionalSupportCredentialChallenge(lane.branchCode(), entry.functionCode()),
                    "setPasswordPath", com.tcc.pjb.backend.core.operational.OperationalApiRoutes.institutionalSupportCredentialPassword(lane.branchCode(), entry.functionCode()),
                    "unlockPath", com.tcc.pjb.backend.core.operational.OperationalApiRoutes.institutionalSupportCredentialUnlock(lane.branchCode(), entry.functionCode()),
                    "credentialBasePath", lane.credentialBasePath(),
                    "branchBound", Boolean.TRUE
            )));
            return compactMap(item);
        }).toList());
        if (snapshot.directorGovernance() != null && !snapshot.directorGovernance().isEmpty()) {
            out.put("directorGovernance", compactMap(snapshot.directorGovernance()));
        }
        out.put("routes", compactMap(java.util.Map.of(
                "credentialBasePath", lane.credentialBasePath(),
                "branchBound", Boolean.TRUE
        )));
        return compactMap(out);
    }

    private List<InstitutionalSupportPanelGroupResponse> groupByProcesso(List<InstitutionalSupportPanelItemResponse> items) {
        return group(items,
                item -> firstNonBlank(item.numeroProcesso(), item.processoId() == null ? null : String.valueOf(item.processoId()), "SEM_PROCESSO"),
                item -> firstNonBlank(item.numeroProcesso(), item.titulo(), "Processo"));
    }

    private List<InstitutionalSupportPanelGroupResponse> groupByRito(List<InstitutionalSupportPanelItemResponse> items) {
        return group(items,
                item -> firstNonBlank(item.ritoProcessual(), "SEM_RITO"),
                item -> firstNonBlank(item.ritoProcessual(), "Sem rito"));
    }

    private List<InstitutionalSupportPanelGroupResponse> groupByData(List<InstitutionalSupportPanelItemResponse> items) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("pt-BR"));
        return group(items,
                item -> dateBucket(item.dueAt(), item.updatedAt()),
                item -> {
                    Instant instant = firstNonNull(item.dueAt(), item.updatedAt());
                    if (instant == null) {
                        return "Sem data";
                    }
                    return formatter.format(LocalDate.ofInstant(instant, ZoneId.systemDefault()));
                });
    }

    private List<InstitutionalSupportPanelGroupResponse> group(Collection<InstitutionalSupportPanelItemResponse> items,
                                                               java.util.function.Function<InstitutionalSupportPanelItemResponse, String> keyFn,
                                                               java.util.function.Function<InstitutionalSupportPanelItemResponse, String> labelFn) {
        LinkedHashMap<String, List<InstitutionalSupportPanelItemResponse>> grouped = new LinkedHashMap<>();
        for (InstitutionalSupportPanelItemResponse item : items) {
            String key = firstNonBlank(keyFn.apply(item), "SEM_GRUPO");
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(item);
        }
        return grouped.entrySet().stream()
                .map(entry -> new InstitutionalSupportPanelGroupResponse(
                        entry.getKey(),
                        labelFn.apply(entry.getValue().get(0)),
                        entry.getValue().size(),
                        List.copyOf(entry.getValue().stream().sorted(Comparator.comparing(InstitutionalSupportPanelItemResponse::dueAt, Comparator.nullsLast(Comparator.naturalOrder()))).toList())
                ))
                .toList();
    }

    private boolean looksLikeAudiencia(InstitutionalSupportPanelItemResponse item) {
        return containsToken(item.titulo(), "AUDIENCIA", "PAUTA") || containsToken(item.queueCode(), "AUDIENCIA");
    }

    private boolean looksLikeSessao(InstitutionalSupportPanelItemResponse item) {
        return containsToken(item.titulo(), "SESSAO", "ACORDAO", "COLEGIADO") || containsToken(item.queueCode(), "SESSAO", "COLEGIADO");
    }

    private boolean looksLikeIntimacao(InstitutionalSupportPanelItemResponse item) {
        return containsToken(item.titulo(), "INTIM", "CITAC", "VISTA") || containsToken(item.queueCode(), "INTIM", "CITAC", "VISTA");
    }

    private boolean looksLikeAudiencia(WorkItem item) {
        return containsToken(item.getTitulo(), "AUDIENCIA", "PAUTA") || containsToken(item.getQueueCode(), "AUDIENCIA");
    }

    private boolean looksLikeSessao(WorkItem item) {
        return containsToken(item.getTitulo(), "SESSAO", "ACORDAO", "COLEGIADO") || containsToken(item.getQueueCode(), "SESSAO", "COLEGIADO");
    }

    private boolean looksLikeIntimacao(WorkItem item) {
        return containsToken(item.getTitulo(), "INTIM", "CITAC", "VISTA") || containsToken(item.getQueueCode(), "INTIM", "CITAC", "VISTA");
    }

    private boolean containsToken(String value, String... tokens) {
        if (value == null || value.isBlank() || tokens == null) {
            return false;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        for (String token : tokens) {
            if (token != null && !token.isBlank() && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String dateBucket(Instant dueAt, Instant updatedAt) {
        Instant instant = firstNonNull(dueAt, updatedAt);
        if (instant == null) {
            return "SEM_DATA";
        }
        return LocalDate.ofInstant(instant, ZoneId.systemDefault()).toString();
    }

    private Map<String, Object> principalContact(Map<String, Object> envelope) {
        if (envelope == null || envelope.isEmpty()) {
            return Map.of();
        }
        Object autor = envelope.get("autor");
        if (autor instanceof Map<?, ?> map && map.get("nome") != null) {
            return castMap(map);
        }
        Object reu = envelope.get("reu");
        if (reu instanceof Map<?, ?> map && map.get("nome") != null) {
            return castMap(map);
        }
        Object advogados = envelope.get("advogados");
        if (advogados instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> map && map.get("nome") != null) {
                    return castMap(map);
                }
            }
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> raw) {
        return (Map<String, Object>) raw;
    }


    private Map<String, Object> compactMap(Map<String, ?> raw) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (raw != null) {
            raw.forEach((key, value) -> {
                if (key != null && value != null) {
                    out.put(key, value);
                }
            });
        }
        return Collections.unmodifiableMap(out);
    }

    private String denormalizeComarca(String comarca) {
        return comarca == null ? null : comarca.replace('_', ' ');
    }

    private String stringOf(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String... values) {
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
