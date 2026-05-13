package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeCriticalDeadlineView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficePendingPetitionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeQueueItemView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeTeamMemberView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceLegalCockpitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceMainDashboardKpiView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceMainDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.advogado.AdvogadoDashboardDto;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.dto.OfficeQueueItemDto;
import com.tcc.pjb.backend.modules.advocacia.office.entity.AdvOfficeProcessOperation;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import com.tcc.pjb.backend.modules.advocacia.office.repository.AdvOfficeProcessOperationRepository;
import com.tcc.pjb.backend.service.advogado.AdvogadoDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeWorkspaceMainDashboardService {

    private static final List<OfficeActionType> PETITION_ACTIONS = List.of(OfficeActionType.PETICIONAR, OfficeActionType.RECORRER);
    private static final List<String> PENDING_OPERATION_STATUSES = List.of("CREATED", "PENDING_SIGNER");

    private final CurrentUserService currentUserService;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final OfficeWorkspaceDashboardService officeWorkspaceDashboardService;
    private final OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService;
    private final OfficeSignatureQueueService officeSignatureQueueService;
    private final OfficeProcessTransferService officeProcessTransferService;
    private final AdvogadoDashboardService advogadoDashboardService;
    private final AdvOfficeProcessOperationRepository processOperationRepository;
    private final AuditLedgerService auditLedgerService;

    public OfficeWorkspaceMainDashboardService(CurrentUserService currentUserService,
                                               OfficeWorkspaceModeService officeWorkspaceModeService,
                                               OfficeWorkspaceDashboardService officeWorkspaceDashboardService,
                                               OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService,
                                               OfficeSignatureQueueService officeSignatureQueueService,
                                               OfficeProcessTransferService officeProcessTransferService,
                                               AdvogadoDashboardService advogadoDashboardService,
                                               AdvOfficeProcessOperationRepository processOperationRepository,
                                               AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.officeWorkspaceDashboardService = Objects.requireNonNull(officeWorkspaceDashboardService);
        this.officeWorkspaceLegalCockpitService = Objects.requireNonNull(officeWorkspaceLegalCockpitService);
        this.officeSignatureQueueService = Objects.requireNonNull(officeSignatureQueueService);
        this.officeProcessTransferService = Objects.requireNonNull(officeProcessTransferService);
        this.advogadoDashboardService = Objects.requireNonNull(advogadoDashboardService);
        this.processOperationRepository = Objects.requireNonNull(processOperationRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeWorkspaceMainDashboardView dashboard(Authentication authentication,
                                                                 HttpServletRequest request,
                                                                 LocalDate from,
                                                                 LocalDate to) {
        Usuario usuario = currentUserService.getRequired();
        PjbFrontendOfficeModeView officeMode = officeWorkspaceModeService.current(request);
        PjbFrontendOfficeWorkspaceSummaryView officeSummary = officeWorkspaceDashboardService.currentSummary(request, officeMode.activeEquipeId());
        PjbFrontendOfficeWorkspaceLegalCockpitView legalCockpit = officeWorkspaceLegalCockpitService.cockpit(authentication, request, from, to, null);
        Map<Long, PjbFrontendOfficeWorkspaceProcessCardView> processCards = mapCardsByProcesso(legalCockpit.highlightedProcesses());
        var queuePage = officeSignatureQueueService.listarPorSigner(usuario.getId(), OfficeQueueStatus.PENDING, PageRequest.of(0, 8));
        List<PjbFrontendOfficeQueueItemView> pendingQueueItems = queuePage.getContent().stream().map(this::toQueueItem).toList();
        List<PjbFrontendOfficeProcessTransferView> transferViews = officeMode.activeEquipeId() == null
                ? List.of()
                : officeProcessTransferService.officeTransfers(officeMode.activeEquipeId()).stream()
                .filter(this::isPendingTransfer)
                .limit(8)
                .toList();
        AdvogadoDashboardDto.SummaryResponse lawyerSummary = advogadoDashboardService.summary(21, 12);
        List<PjbFrontendOfficeCriticalDeadlineView> criticalDeadlines = lawyerSummary.getPrazosCriticos() == null
                ? List.of()
                : lawyerSummary.getPrazosCriticos().stream().map(item -> toCriticalDeadline(item, processCards)).toList();
        List<AdvOfficeProcessOperation> operations = processOperationRepository.findDashboardPending(
                usuario.getId(),
                officeMode.activeEquipeId(),
                PETITION_ACTIONS,
                PENDING_OPERATION_STATUSES,
                PageRequest.of(0, 8)
        );
        List<PjbFrontendOfficePendingPetitionView> pendingPetitions = operations.stream().map(item -> toPendingPetition(item, processCards)).toList();
        long pendingPetitionCount = processOperationRepository.countDashboardPending(
                usuario.getId(),
                officeMode.activeEquipeId(),
                PETITION_ACTIONS,
                PENDING_OPERATION_STATUSES
        );
        long pendingTransferCount = officeMode.activeEquipeId() == null
                ? 0L
                : officeProcessTransferService.officeTransfers(officeMode.activeEquipeId()).stream().filter(this::isPendingTransfer).count();
        List<PjbFrontendOfficeTeamMemberView> onlineMembers = officeSummary == null || officeSummary.onlineTeamMembers() == null
                ? List.of()
                : officeSummary.onlineTeamMembers();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (officeSummary != null && officeSummary.blockers() != null) {
            blockers.addAll(officeSummary.blockers());
        }
        if (legalCockpit.blockers() != null) {
            blockers.addAll(legalCockpit.blockers());
        }
        if (legalCockpit.warnings() != null) {
            warnings.addAll(legalCockpit.warnings());
        }
        if (officeSummary != null && officeSummary.hints() != null && !officeSummary.hints().isEmpty()) {
            warnings.add("COCKPIT_INSTITUCIONAL_HABILITADO");
        }
        if (onlineMembers.isEmpty()) {
            warnings.add("EQUIPE_SEM_MEMBROS_ONLINE_NO_MOMENTO");
        }
        if (pendingQueueItems.isEmpty() && Boolean.TRUE.equals(officeMode.patronCertificateRequired())) {
            warnings.add("FILA_PATRONAL_SEM_ITENS_PENDENTES_NO_MOMENTO");
        }
        if (pendingPetitions.isEmpty()) {
            warnings.add("SEM_PETICOES_GOVERNADAS_PENDENTES_NO_WORKSPACE");
        }
        PjbFrontendOfficeWorkspaceMainDashboardKpiView kpis = new PjbFrontendOfficeWorkspaceMainDashboardKpiView(
                officeSummary == null ? 0L : officeSummary.totalMembers(),
                officeSummary == null ? 0L : officeSummary.onlineMembers(),
                legalCockpit.processPage() == null ? 0L : legalCockpit.processPage().totalVisible(),
                queuePage.getTotalElements(),
                pendingTransferCount,
                criticalDeadlines.size(),
                pendingPetitionCount,
                lawyerSummary.getAgendaProxima() == null ? 0L : lawyerSummary.getAgendaProxima().size(),
                countUnreadIntimacoes(lawyerSummary),
                countRecursosVencendo(lawyerSummary)
        );
        List<String> quickRoutes = buildQuickRoutes(officeMode.activeEquipeId());
        auditLedgerService.appendSafely(
                "ADV_OFFICE_MAIN_DASHBOARD_QUERY",
                "FRONTEND",
                String.valueOf(usuario.getId()),
                "mode=" + officeMode.mode() + " equipe=" + officeMode.activeEquipeId() + " queue=" + queuePage.getTotalElements() + " transfers=" + pendingTransferCount + " petitions=" + pendingPetitionCount
        );
        return new PjbFrontendOfficeWorkspaceMainDashboardView(
                Instant.now(),
                officeMode.mode(),
                officeMode.activeEquipeId(),
                officeMode.activeEquipeNome(),
                officeSummary,
                kpis,
                legalCockpit,
                onlineMembers,
                pendingQueueItems,
                transferViews,
                criticalDeadlines,
                pendingPetitions,
                quickRoutes,
                List.copyOf(blockers),
                List.copyOf(warnings)
        );
    }

    private Map<Long, PjbFrontendOfficeWorkspaceProcessCardView> mapCardsByProcesso(List<PjbFrontendOfficeWorkspaceProcessCardView> cards) {
        LinkedHashMap<Long, PjbFrontendOfficeWorkspaceProcessCardView> out = new LinkedHashMap<>();
        if (cards == null) {
            return out;
        }
        for (PjbFrontendOfficeWorkspaceProcessCardView card : cards) {
            if (card != null && card.processoId() != null) {
                out.put(card.processoId(), card);
            }
        }
        return out;
    }

    private PjbFrontendOfficeQueueItemView toQueueItem(OfficeQueueItemDto source) {
        return new PjbFrontendOfficeQueueItemView(
                source.getId(),
                source.getEquipeId(),
                source.getExecutorUserId(),
                source.getSignerUserId(),
                source.getActionType() == null ? null : source.getActionType().name(),
                source.getResourceType(),
                source.getResourceId(),
                source.getStatus() == null ? null : source.getStatus().name(),
                source.getCreatedAt(),
                source.getDecidedAt(),
                source.getDecidedByUserId(),
                source.getDecisionReason(),
                source.getRequestId(),
                source.getPayloadHash(),
                source.getSummary()
        );
    }

    private boolean isPendingTransfer(PjbFrontendOfficeProcessTransferView item) {
        return item != null && "PENDING_DESTINATION_ACCEPTANCE".equalsIgnoreCase(item.status());
    }

    private PjbFrontendOfficeCriticalDeadlineView toCriticalDeadline(AdvogadoDashboardDto.WorkItemLite item,
                                                                     Map<Long, PjbFrontendOfficeWorkspaceProcessCardView> cards) {
        PjbFrontendOfficeWorkspaceProcessCardView card = item == null ? null : cards.get(item.getProcessoId());
        Instant dueAt = item == null ? null : item.getDueAt();
        long hoursRemaining = dueAt == null ? Long.MAX_VALUE : ChronoUnit.HOURS.between(Instant.now(), dueAt);
        return new PjbFrontendOfficeCriticalDeadlineView(
                item == null ? null : item.getId(),
                item == null ? null : item.getProcessoId(),
                item == null ? null : item.getProcessoNumero(),
                item == null ? null : item.getTitulo(),
                dueAt,
                hoursRemaining,
                card == null ? "NEUTRAL" : card.accentColor(),
                card == null ? "NEUTRAL" : card.statusColor(),
                item == null || item.getProcessoId() == null ? null : "/api/v1/frontend/app/offices/workspace/processes/" + item.getProcessoId() + "/reading-mode",
                item == null || item.getProcessoId() == null ? null : "/api/v1/calendar/workspace?processoId=" + item.getProcessoId()
        );
    }

    private PjbFrontendOfficePendingPetitionView toPendingPetition(AdvOfficeProcessOperation item,
                                                                   Map<Long, PjbFrontendOfficeWorkspaceProcessCardView> cards) {
        Long processoId = item == null || item.getProcesso() == null ? null : item.getProcesso().getId();
        PjbFrontendOfficeWorkspaceProcessCardView card = processoId == null ? null : cards.get(processoId);
        return new PjbFrontendOfficePendingPetitionView(
                item == null ? null : item.getId(),
                processoId,
                item == null || item.getProcesso() == null ? null : firstNonBlank(item.getProcesso().getNumeroUnificado(), item.getProcesso().getNumeroProcesso(), item.getProcesso().getNumero()),
                item == null || item.getActionType() == null ? null : item.getActionType().name(),
                item == null ? null : item.getStatus(),
                item == null || item.getQueueItem() == null ? null : item.getQueueItem().getId(),
                item == null || item.getExecutor() == null ? null : item.getExecutor().getId(),
                item == null || item.getSigner() == null ? null : item.getSigner().getId(),
                item == null ? null : item.getSignerNameSnapshot(),
                item == null ? null : item.getSignerRegistrationSnapshot(),
                item != null && item.getQueueItem() != null,
                card == null ? "NEUTRAL" : card.accentColor(),
                item == null ? null : item.getCreatedAt(),
                processoId == null ? null : "/api/v1/frontend/app/offices/workspace/processes/" + processoId + "/reading-mode"
        );
    }

    private long countUnreadIntimacoes(AdvogadoDashboardDto.SummaryResponse summary) {
        if (summary == null || summary.getPrazosCriticos() == null) {
            return 0L;
        }
        return summary.getPrazosCriticos().stream()
                .filter(item -> item != null && containsAny(item.getTitulo(), "INTIMA", "CIENCIA", "PUBLICA"))
                .count();
    }

    private long countRecursosVencendo(AdvogadoDashboardDto.SummaryResponse summary) {
        if (summary == null || summary.getPrazosCriticos() == null) {
            return 0L;
        }
        return summary.getPrazosCriticos().stream()
                .filter(item -> item != null && containsAny(item.getTitulo(), "RECURSO", "APELA", "AGRAVO", "EMBARG"))
                .count();
    }

    private boolean containsAny(String value, String... tokens) {
        if (value == null || value.isBlank() || tokens == null || tokens.length == 0) {
            return false;
        }
        String normalized = value.toUpperCase();
        for (String token : tokens) {
            if (token != null && normalized.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private List<String> buildQuickRoutes(Long equipeId) {
        ArrayList<String> routes = new ArrayList<>();
        routes.add("/api/v1/frontend/app/offices/workspace/main-dashboard");
        routes.add("/api/v1/frontend/app/offices/workspace/executive-dashboard");
        routes.add("/api/v1/frontend/app/offices/workspace/summary");
        routes.add("/api/v1/frontend/app/offices/workspace/legal-cockpit");
        routes.add("/api/v1/frontend/app/offices/workspace/queue");
        routes.add("/api/v1/frontend/app/offices/transfers/incoming");
        routes.add("/api/v1/frontend/app/offices/workspace/processes/query");
        if (equipeId != null) {
            routes.add("/api/v1/frontend/app/offices/" + equipeId + "/transfers");
        }
        return List.copyOf(routes);
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
