package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendAnalyticMetricCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendAvatarCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendChartPointView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendChartSeriesView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeCriticalDeadlineView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficePendingPetitionView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessTransferView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeQueueItemView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeTeamMemberView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceLegalCockpitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceMainDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessPageView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendVisualThemeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendWorkspaceBoardColumnView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendWorkspaceBoardItemView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceProcessQueryRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.identity.UsuarioAvatar;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.identity.UserAvatarService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeWorkspaceExecutiveDashboardService {

    private static final String BRAZIL_GREEN = "#009C3B";
    private static final String BRAZIL_GREEN_DARK = "#006B2D";
    private static final String BRAZIL_YELLOW = "#FFDF00";
    private static final String BRAZIL_YELLOW_SOFT = "#F6E27A";
    private static final String BRAZIL_BLUE = "#002776";
    private static final String BRAZIL_BLUE_ALT = "#1B4D9B";
    private static final String BRAZIL_WHITE = "#FFFFFF";
    private static final String BRAZIL_BACKGROUND = "#06162E";
    private static final String BRAZIL_SURFACE = "#0C2247";
    private static final String BRAZIL_SURFACE_ALT = "#12315E";
    private static final String BRAZIL_TEXT = "#F5F8FF";

    private final CurrentUserService currentUserService;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final OfficeWorkspaceDashboardService officeWorkspaceDashboardService;
    private final OfficeWorkspaceMainDashboardService officeWorkspaceMainDashboardService;
    private final OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final ProcessoRepository processoRepository;
    private final UserAvatarService userAvatarService;
    private final AuditLedgerService auditLedgerService;

    public OfficeWorkspaceExecutiveDashboardService(CurrentUserService currentUserService,
                                                    OfficeWorkspaceModeService officeWorkspaceModeService,
                                                    OfficeWorkspaceDashboardService officeWorkspaceDashboardService,
                                                    OfficeWorkspaceMainDashboardService officeWorkspaceMainDashboardService,
                                                    OfficeWorkspaceLegalCockpitService officeWorkspaceLegalCockpitService,
                                                    OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                                    ProcessoRepository processoRepository,
                                                    UserAvatarService userAvatarService,
                                                    AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.officeWorkspaceDashboardService = Objects.requireNonNull(officeWorkspaceDashboardService);
        this.officeWorkspaceMainDashboardService = Objects.requireNonNull(officeWorkspaceMainDashboardService);
        this.officeWorkspaceLegalCockpitService = Objects.requireNonNull(officeWorkspaceLegalCockpitService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.userAvatarService = Objects.requireNonNull(userAvatarService);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeWorkspaceExecutiveDashboardView dashboard(Authentication authentication,
                                                                      HttpServletRequest request,
                                                                      LocalDate from,
                                                                      LocalDate to) {
        Usuario usuario = currentUserService.getRequired();
        LocalDate safeFrom = from == null ? LocalDate.now() : from;
        LocalDate safeTo = to == null || to.isBefore(safeFrom) ? safeFrom.plusDays(31) : to;
        PjbFrontendOfficeModeView officeMode = officeWorkspaceModeService.current(request);
        PjbFrontendOfficeWorkspaceSummaryView officeSummary = officeWorkspaceDashboardService.currentSummary(request, officeMode.activeEquipeId());
        PjbFrontendOfficeWorkspaceMainDashboardView operationalSnapshot = officeWorkspaceMainDashboardService.dashboard(authentication, request, safeFrom, safeTo);
        PjbFrontendOfficeWorkspaceLegalCockpitView legalCockpit = officeWorkspaceLegalCockpitService.cockpit(authentication, request, safeFrom, safeTo, null);
        List<Processo> visibleProcesses = collectVisibleProcesses(request);
        PjbFrontendVisualThemeView visualTheme = buildBrazilExecutiveTheme();
        List<PjbFrontendChartSeriesView> charts = buildCharts(visibleProcesses, operationalSnapshot, legalCockpit, visualTheme);
        List<PjbFrontendAnalyticMetricCardView> headlineCards = buildHeadlineCards(officeMode, officeSummary, operationalSnapshot, visibleProcesses);
        List<PjbFrontendAvatarCardView> teamGallery = buildTeamGallery(officeSummary == null ? List.of() : officeSummary.members(), request);
        List<PjbFrontendAvatarCardView> profileGallery = buildProfileGallery(usuario, officeSummary, officeMode, request);
        List<PjbFrontendWorkspaceBoardColumnView> board = buildBoard(operationalSnapshot);
        LinkedHashSet<String> linkedModules = new LinkedHashSet<>();
        linkedModules.add("OFFICE_EXECUTIVE_DASHBOARD");
        linkedModules.add("OFFICE_MAIN_DASHBOARD");
        linkedModules.add("OFFICE_LEGAL_COCKPIT");
        linkedModules.add("PROCESS_RESULT_ANALYTICS");
        linkedModules.add("TEAM_GALLERY");
        linkedModules.add("BRAZIL_VISUAL_THEME");
        if (legalCockpit.linkedModules() != null) {
            linkedModules.addAll(legalCockpit.linkedModules());
        }
        LinkedHashSet<String> quickRoutes = new LinkedHashSet<>();
        quickRoutes.add("/api/v1/frontend/app/offices/workspace/executive-dashboard");
        if (operationalSnapshot.quickRoutes() != null) {
            quickRoutes.addAll(operationalSnapshot.quickRoutes());
        }
        quickRoutes.add("/api/v1/frontend/app/offices/workspace/main-dashboard");
        quickRoutes.add("/api/v1/frontend/app/offices/workspace/legal-cockpit");
        quickRoutes.add("/api/v1/frontend/app/offices/workspace/processes/query");
        LinkedHashSet<String> blockers = new LinkedHashSet<>(operationalSnapshot.blockers());
        LinkedHashSet<String> warnings = new LinkedHashSet<>(operationalSnapshot.warnings());
        if (visibleProcesses.isEmpty()) {
            warnings.add("SEM_PROCESSOS_SUFICIENTES_PARA_GRAFICOS_AVANCADOS");
        }
        if (teamGallery.isEmpty()) {
            warnings.add("GALERIA_DA_EQUIPE_SEM_MEMBROS_VISIVEIS");
        }
        auditLedgerService.appendSafely(
                "ADV_OFFICE_EXECUTIVE_DASHBOARD_QUERY",
                "FRONTEND",
                String.valueOf(usuario.getId()),
                "mode=" + officeMode.mode() + " equipe=" + officeMode.activeEquipeId() + " visible=" + visibleProcesses.size() + " theme=" + visualTheme.key()
        );
        return new PjbFrontendOfficeWorkspaceExecutiveDashboardView(
                Instant.now(),
                officeMode.mode(),
                officeMode.activeEquipeId(),
                officeMode.activeEquipeNome(),
                visualTheme,
                officeSummary,
                operationalSnapshot.kpis(),
                operationalSnapshot,
                legalCockpit,
                headlineCards,
                charts,
                teamGallery,
                profileGallery,
                board,
                List.copyOf(linkedModules),
                List.copyOf(quickRoutes),
                List.copyOf(blockers),
                List.copyOf(warnings)
        );
    }

    private List<Processo> collectVisibleProcesses(HttpServletRequest request) {
        LinkedHashMap<Long, Processo> byId = new LinkedHashMap<>();
        for (int page = 0; page < 4; page++) {
            PjbFrontendOfficeWorkspaceProcessPageView batch = officeProcessWorkspaceScopeService.currentWorkspaceProcesses(
                    new FrontendOfficeWorkspaceProcessQueryRequest(page, 24, null, null, null, null),
                    request
            );
            if (batch == null || batch.items() == null || batch.items().isEmpty()) {
                break;
            }
            List<Long> ids = batch.items().stream().map(item -> item == null ? null : item.processoId()).filter(Objects::nonNull).toList();
            if (ids.isEmpty()) {
                break;
            }
            processoRepository.findAllById(ids).forEach(processo -> byId.put(processo.getId(), processo));
            if (batch.items().size() < 24) {
                break;
            }
        }
        return List.copyOf(byId.values());
    }

    private PjbFrontendVisualThemeView buildBrazilExecutiveTheme() {
        return new PjbFrontendVisualThemeView(
                "BRAZIL_EXECUTIVE_2026",
                "Brasil Executivo",
                BRAZIL_GREEN,
                BRAZIL_YELLOW,
                BRAZIL_BLUE,
                BRAZIL_GREEN_DARK,
                BRAZIL_WHITE,
                BRAZIL_BACKGROUND,
                BRAZIL_SURFACE,
                BRAZIL_SURFACE_ALT,
                BRAZIL_GREEN,
                BRAZIL_BLUE,
                List.of(BRAZIL_GREEN, BRAZIL_YELLOW, BRAZIL_BLUE, BRAZIL_GREEN_DARK, BRAZIL_BLUE_ALT, BRAZIL_YELLOW_SOFT)
        );
    }

    private List<PjbFrontendAnalyticMetricCardView> buildHeadlineCards(PjbFrontendOfficeModeView officeMode,
                                                                       PjbFrontendOfficeWorkspaceSummaryView officeSummary,
                                                                       PjbFrontendOfficeWorkspaceMainDashboardView operationalSnapshot,
                                                                       List<Processo> visibleProcesses) {
        OutcomeSummary outcomes = summarizeOutcomes(visibleProcesses);
        long total = visibleProcesses.size();
        long publicos = visibleProcesses.stream().filter(item -> item.getNivelSigilo() == null || "PUBLICO".equalsIgnoreCase(item.getNivelSigilo().name())).count();
        ArrayList<PjbFrontendAnalyticMetricCardView> cards = new ArrayList<>();
        cards.add(metricCard("PROCESSOS", "Processos visíveis", Long.toString(total), officeMode.mode(), "PRIMARY", "/api/v1/frontend/app/offices/workspace/processes/query"));
        cards.add(metricCard("PROCEDENTES", "Procedentes", Long.toString(outcomes.procedentes()), percent(outcomes.procedentes(), total), "SUCCESS", "/api/v1/frontend/app/offices/workspace/executive-dashboard"));
        cards.add(metricCard("IMPROCEDENTES", "Improcedentes", Long.toString(outcomes.improcedentes()), percent(outcomes.improcedentes(), total), "INFO", "/api/v1/frontend/app/offices/workspace/executive-dashboard"));
        cards.add(metricCard("ACORDOS", "Acordos e homologações", Long.toString(outcomes.acordos()), percent(outcomes.acordos(), total), "WARNING", "/api/v1/frontend/app/offices/workspace/main-dashboard"));
        cards.add(metricCard("PRAZOS", "Prazos críticos", Long.toString(operationalSnapshot.criticalDeadlines().size()), Long.toString(operationalSnapshot.kpis().recursosVencendo()) + " recursos vencendo", "SECONDARY", "/api/v1/calendar/workspace"));
        cards.add(metricCard("EQUIPE", "Equipe online", Long.toString(officeSummary == null ? 0L : officeSummary.onlineMembers()), officeSummary == null ? "0 membros" : officeSummary.totalMembers() + " membros", "PRIMARY", "/api/v1/frontend/app/offices/workspace/summary"));
        cards.add(metricCard("PUBLICOS", "Autos públicos no radar", Long.toString(publicos), percent(publicos, total), "NEUTRAL", "/api/v1/professional/forensic-panel/workspace"));
        return List.copyOf(cards);
    }

    private PjbFrontendAnalyticMetricCardView metricCard(String key,
                                                         String label,
                                                         String value,
                                                         String secondaryValue,
                                                         String accentTone,
                                                         String route) {
        return new PjbFrontendAnalyticMetricCardView(
                key,
                label,
                value,
                secondaryValue,
                accentTone,
                accentHex(accentTone),
                surfaceHex(accentTone),
                BRAZIL_TEXT,
                route
        );
    }

    private List<PjbFrontendChartSeriesView> buildCharts(List<Processo> visibleProcesses,
                                                         PjbFrontendOfficeWorkspaceMainDashboardView operationalSnapshot,
                                                         PjbFrontendOfficeWorkspaceLegalCockpitView legalCockpit,
                                                         PjbFrontendVisualThemeView visualTheme) {
        OutcomeSummary outcomes = summarizeOutcomes(visibleProcesses);
        List<PjbFrontendChartSeriesView> charts = new ArrayList<>();
        charts.add(chartSeries(
                "OUTCOME_DISTRIBUTION",
                "Resultado das causas",
                "DONUT",
                "BRAZIL_RESULTS",
                List.of(
                        point("PROCEDENTE", "Procedentes", outcomes.procedentes(), "SUCCESS"),
                        point("IMPROCEDENTE", "Improcedentes", outcomes.improcedentes(), "INFO"),
                        point("PARCIAL", "Parcialmente procedentes", outcomes.parciais(), "WARNING"),
                        point("ACORDO", "Acordos", outcomes.acordos(), "PRIMARY"),
                        point("PENDENTE", "Sem resultado consolidado", outcomes.pendentes(), "NEUTRAL")
                ),
                visualTheme.chartPaletteHex()
        ));
        charts.add(chartSeries(
                "STATUS_DISTRIBUTION",
                "Status processual da carteira",
                "BAR",
                "BRAZIL_STATUS",
                toPoints(countByLabel(visibleProcesses, processo -> safeName(processo.getStatusProcesso())), "PRIMARY"),
                List.of(BRAZIL_GREEN, BRAZIL_BLUE, BRAZIL_YELLOW, BRAZIL_BLUE_ALT)
        ));
        charts.add(chartSeries(
                "RAMO_DISTRIBUTION",
                "Ramos do direito no workspace",
                "STACKED_BAR",
                "BRAZIL_BRANCHES",
                toPoints(countByLabel(visibleProcesses, processo -> safeName(processo.getRamoDireito())), "SECONDARY"),
                List.of(BRAZIL_BLUE, BRAZIL_GREEN, BRAZIL_YELLOW, BRAZIL_GREEN_DARK)
        ));
        charts.add(chartSeries(
                "PIPELINE_OPERACIONAL",
                "Pipeline operacional do escritório",
                "COLUMN",
                "BRAZIL_PIPELINE",
                List.of(
                        point("QUEUE", "Fila patronal", safeSize(operationalSnapshot.pendingQueueItems()), "WARNING"),
                        point("PETITIONS", "Petições pendentes", safeSize(operationalSnapshot.pendingPetitions()), "SECONDARY"),
                        point("TRANSFERS", "Transferências", safeSize(operationalSnapshot.pendingTransfers()), "INFO"),
                        point("HEARINGS", "Audiências próximas", operationalSnapshot.kpis().upcomingHearings(), "PRIMARY"),
                        point("TIMELINE", "Processos destacados", safeSize(legalCockpit.highlightedProcesses()), "SUCCESS")
                ),
                List.of(BRAZIL_YELLOW, BRAZIL_BLUE, BRAZIL_BLUE_ALT, BRAZIL_GREEN, BRAZIL_GREEN_DARK)
        ));
        charts.add(chartSeries(
                "CALENDAR_LOAD",
                "Carga de agenda e prazo",
                "LINE",
                "BRAZIL_CALENDAR",
                List.of(
                        point("PRAZOS_CRITICOS", "Prazos críticos", operationalSnapshot.kpis().criticalDeadlines(), "WARNING"),
                        point("INTIMACOES", "Intimações não lidas", operationalSnapshot.kpis().unreadIntimacoes(), "INFO"),
                        point("RECURSOS", "Recursos vencendo", operationalSnapshot.kpis().recursosVencendo(), "SECONDARY"),
                        point("AGENDA", "Audiências e agenda", operationalSnapshot.kpis().upcomingHearings(), "PRIMARY")
                ),
                List.of(BRAZIL_YELLOW, BRAZIL_BLUE, BRAZIL_BLUE_ALT, BRAZIL_GREEN)
        ));
        return List.copyOf(charts);
    }

    private PjbFrontendChartSeriesView chartSeries(String key,
                                                   String label,
                                                   String chartType,
                                                   String paletteKey,
                                                   List<PjbFrontendChartPointView> points,
                                                   List<String> paletteHex) {
        return new PjbFrontendChartSeriesView(
                key,
                label,
                chartType,
                paletteKey,
                paletteHex,
                points
        );
    }

    private PjbFrontendChartPointView point(String key,
                                            String label,
                                            long value,
                                            String accentTone) {
        return new PjbFrontendChartPointView(key, label, value, accentTone, accentHex(accentTone));
    }

    private List<PjbFrontendChartPointView> toPoints(Map<String, Long> counts, String accentTone) {
        if (counts.isEmpty()) {
            return List.of(point("SEM_DADOS", "Sem dados", 0L, accentTone));
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(8)
                .map(entry -> point(normalizeKey(entry.getKey()), entry.getKey(), entry.getValue(), accentTone))
                .toList();
    }

    private List<PjbFrontendAvatarCardView> buildTeamGallery(List<PjbFrontendOfficeTeamMemberView> members,
                                                             HttpServletRequest request) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        return members.stream()
                .sorted(Comparator.comparing(PjbFrontendOfficeTeamMemberView::online).reversed()
                        .thenComparing(PjbFrontendOfficeTeamMemberView::patrono).reversed()
                        .thenComparing(PjbFrontendOfficeTeamMemberView::fundador).reversed()
                        .thenComparing(PjbFrontendOfficeTeamMemberView::nome, String.CASE_INSENSITIVE_ORDER))
                .limit(10)
                .map(member -> toAvatarCard(member.userId(), member.nome(), subtitleForMember(member), member.online(), accentTagForMember(member), routeForMember(member, request)))
                .toList();
    }

    private List<PjbFrontendAvatarCardView> buildProfileGallery(Usuario usuario,
                                                                PjbFrontendOfficeWorkspaceSummaryView officeSummary,
                                                                PjbFrontendOfficeModeView officeMode,
                                                                HttpServletRequest request) {
        ArrayList<PjbFrontendAvatarCardView> cards = new ArrayList<>();
        cards.add(toAvatarCard(usuario.getId(), usuario.getNome(), profileSubtitle(usuario, officeMode), true, officeMode.mode(), "/api/v1/frontend/app/me"));
        if (officeSummary != null && officeSummary.members() != null) {
            officeSummary.members().stream()
                    .filter(item -> Objects.equals(item.userId(), officeSummary.patronoUserId()) || Objects.equals(item.userId(), officeSummary.founderUserId()))
                    .sorted(Comparator.comparing(PjbFrontendOfficeTeamMemberView::userId))
                    .forEach(item -> cards.add(toAvatarCard(item.userId(), item.nome(), subtitleForMember(item), item.online(), accentTagForMember(item), routeForMember(item, request))));
        }
        return cards.stream().distinct().limit(4).toList();
    }

    private PjbFrontendAvatarCardView toAvatarCard(Long userId,
                                                   String nome,
                                                   String subtitle,
                                                   boolean online,
                                                   String accentTag,
                                                   String route) {
        Optional<UsuarioAvatar> avatar = userId == null ? Optional.empty() : userAvatarService.find(userId);
        String avatarUrl = avatar.isPresent() ? "/api/v1/frontend/app/offices/team-members/" + userId + "/avatar" : null;
        String avatarEtag = avatar.map(UsuarioAvatar::getSha256).orElse(null);
        return new PjbFrontendAvatarCardView(
                userId,
                nome,
                subtitle,
                avatarUrl,
                avatarEtag,
                initials(nome),
                online,
                accentTag,
                accentHex(accentTag),
                badgeHex(accentTag),
                online ? BRAZIL_GREEN : BRAZIL_BLUE_ALT,
                route
        );
    }

    private List<PjbFrontendWorkspaceBoardColumnView> buildBoard(PjbFrontendOfficeWorkspaceMainDashboardView operationalSnapshot) {
        List<PjbFrontendWorkspaceBoardItemView> deadlines = safeList(operationalSnapshot.criticalDeadlines()).stream()
                .limit(6)
                .map(this::toDeadlineBoardItem)
                .toList();
        List<PjbFrontendWorkspaceBoardItemView> petitions = safeList(operationalSnapshot.pendingPetitions()).stream()
                .limit(6)
                .map(this::toPetitionBoardItem)
                .toList();
        List<PjbFrontendWorkspaceBoardItemView> queue = safeList(operationalSnapshot.pendingQueueItems()).stream()
                .limit(6)
                .map(this::toQueueBoardItem)
                .toList();
        List<PjbFrontendWorkspaceBoardItemView> transfers = safeList(operationalSnapshot.pendingTransfers()).stream()
                .limit(6)
                .map(this::toTransferBoardItem)
                .toList();
        return List.of(
                boardColumn("DEADLINES", "Prazos críticos", safeSize(operationalSnapshot.criticalDeadlines()), "WARNING", deadlines),
                boardColumn("PETITIONS", "Petições governadas", safeSize(operationalSnapshot.pendingPetitions()), "SECONDARY", petitions),
                boardColumn("QUEUE", "Fila patronal", safeSize(operationalSnapshot.pendingQueueItems()), "INFO", queue),
                boardColumn("TRANSFERS", "Transferências", safeSize(operationalSnapshot.pendingTransfers()), "PRIMARY", transfers)
        );
    }

    private PjbFrontendWorkspaceBoardColumnView boardColumn(String key,
                                                            String label,
                                                            long total,
                                                            String accentTone,
                                                            List<PjbFrontendWorkspaceBoardItemView> items) {
        return new PjbFrontendWorkspaceBoardColumnView(
                key,
                label,
                total,
                accentHex(accentTone),
                surfaceHex(accentTone),
                items
        );
    }

    private PjbFrontendWorkspaceBoardItemView toDeadlineBoardItem(PjbFrontendOfficeCriticalDeadlineView item) {
        long hours = item == null ? Long.MAX_VALUE : item.horasRestantes();
        String meta = item == null || item.dueAt() == null ? null : "vence em " + Math.max(hours, 0L) + "h";
        String tone = hours < 24 ? "WARNING" : "SECONDARY";
        return boardItem(
                item == null ? "DEADLINE" : "DEADLINE_" + item.workItemId(),
                item == null ? null : coalesce(item.numeroProcesso(), item.titulo()),
                item == null ? null : item.titulo(),
                meta,
                tone,
                item == null ? null : coalesce(item.readingModeRoute(), item.calendarRoute())
        );
    }

    private PjbFrontendWorkspaceBoardItemView toPetitionBoardItem(PjbFrontendOfficePendingPetitionView item) {
        return boardItem(
                item == null ? "PETITION" : "PETITION_" + item.operationId(),
                item == null ? null : coalesce(item.numeroProcesso(), item.actionType()),
                item == null ? null : coalesce(item.actionType(), item.status()),
                item == null ? null : coalesce(item.signerNome(), item.signerRegistration()),
                item != null && item.queueRequired() ? "WARNING" : "PRIMARY",
                item == null ? null : item.readingModeRoute()
        );
    }

    private PjbFrontendWorkspaceBoardItemView toQueueBoardItem(PjbFrontendOfficeQueueItemView item) {
        return boardItem(
                item == null ? "QUEUE" : "QUEUE_" + item.queueItemId(),
                item == null ? null : coalesce(item.actionType(), item.resourceType()),
                item == null ? null : item.summary(),
                item == null ? null : coalesce(item.status(), item.requestId()),
                "INFO",
                "/api/v1/frontend/app/offices/workspace/queue"
        );
    }

    private PjbFrontendWorkspaceBoardItemView toTransferBoardItem(PjbFrontendOfficeProcessTransferView item) {
        String tone = item != null && item.sensitiveProcessCount() > 0 ? "WARNING" : "PRIMARY";
        return boardItem(
                item == null ? "TRANSFER" : "TRANSFER_" + item.transferId(),
                item == null ? null : coalesce(item.targetEquipeNome(), item.sourceEquipeNome()),
                item == null ? null : item.previewSummary(),
                item == null ? null : item.processCount() + " processos",
                tone,
                item == null || item.targetEquipeId() == null ? "/api/v1/frontend/app/offices/transfers/incoming" : "/api/v1/frontend/app/offices/" + item.targetEquipeId() + "/transfers"
        );
    }

    private PjbFrontendWorkspaceBoardItemView boardItem(String key,
                                                        String title,
                                                        String subtitle,
                                                        String meta,
                                                        String accentTone,
                                                        String route) {
        return new PjbFrontendWorkspaceBoardItemView(
                key,
                title,
                subtitle,
                meta,
                accentTone,
                accentHex(accentTone),
                surfaceHex(accentTone),
                route
        );
    }

    private OutcomeSummary summarizeOutcomes(Collection<Processo> processes) {
        long procedentes = 0L;
        long improcedentes = 0L;
        long parciais = 0L;
        long acordos = 0L;
        long pendentes = 0L;
        for (Processo processo : processes) {
            String outcome = classifyOutcome(processo);
            switch (outcome) {
                case "PROCEDENTE" -> procedentes++;
                case "IMPROCEDENTE" -> improcedentes++;
                case "PARCIAL" -> parciais++;
                case "ACORDO" -> acordos++;
                default -> pendentes++;
            }
        }
        return new OutcomeSummary(procedentes, improcedentes, parciais, acordos, pendentes);
    }

    private String classifyOutcome(Processo processo) {
        String token = normalizeOutcomeToken(processo == null ? null : processo.getResultadoFinal());
        if (token.isBlank()) {
            return isClosedStatus(processo) ? "PENDENTE" : "PENDENTE";
        }
        if (containsAny(token, "PARCIALMENTE PROCEDENTE", "PROCEDENTE EM PARTE", "PARCIAL PROCEDENCIA", "PARCIAL PROCEDENTE")) {
            return "PARCIAL";
        }
        if (containsAny(token, "IMPROCEDENTE", "NAO PROCEDENTE", "NÃO PROCEDENTE", "REJEITADO")) {
            return "IMPROCEDENTE";
        }
        if (containsAny(token, "ACORDO", "CONCILIAC", "MEDIAC", "HOMOLOG") && !containsAny(token, "SEM ACORDO")) {
            return "ACORDO";
        }
        if (containsAny(token, "PROCEDENTE", "DEFIRO", "CONCEDO", "ACOLHO")) {
            return "PROCEDENTE";
        }
        return "PENDENTE";
    }

    private boolean isClosedStatus(Processo processo) {
        return processo != null
                && processo.getStatusProcesso() != null
                && containsAny(processo.getStatusProcesso().name().toUpperCase(Locale.ROOT), "JULGADO", "ARQUIVADO", "TRANSITO", "SENTENCA");
    }

    private Map<String, Long> countByLabel(Collection<Processo> processes,
                                           java.util.function.Function<Processo, String> extractor) {
        return processes.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.groupingBy(this::humanizeEnumLike, LinkedHashMap::new, Collectors.counting()));
    }

    private String humanizeEnumLike(String value) {
        String token = value == null ? "SEM_DADO" : value.trim();
        if (token.isBlank()) {
            token = "SEM_DADO";
        }
        return token.toUpperCase(Locale.ROOT).replace('_', ' ');
    }

    private String subtitleForMember(PjbFrontendOfficeTeamMemberView member) {
        if (member == null) {
            return null;
        }
        ArrayList<String> parts = new ArrayList<>();
        if (member.papelEquipe() != null && !member.papelEquipe().isBlank()) {
            parts.add(member.papelEquipe().replace('_', ' '));
        }
        if (member.registroProfissional() != null && !member.registroProfissional().isBlank()) {
            parts.add(member.registroProfissional());
        }
        if (member.lastSeenAt() != null) {
            long minutes = ChronoUnit.MINUTES.between(member.lastSeenAt(), Instant.now());
            parts.add(minutes <= 1 ? "agora" : "há " + minutes + " min");
        }
        return String.join(" • ", parts);
    }

    private String profileSubtitle(Usuario usuario, PjbFrontendOfficeModeView officeMode) {
        ArrayList<String> parts = new ArrayList<>();
        if (usuario != null && usuario.getTipoUsuario() != null) {
            parts.add(usuario.getTipoUsuario().name().replace('_', ' '));
        }
        if (usuario != null && usuario.getOab() != null && !usuario.getOab().isBlank()) {
            parts.add(usuario.getOab());
        }
        if (officeMode != null && officeMode.mode() != null) {
            parts.add(officeMode.mode());
        }
        return String.join(" • ", parts);
    }

    private String accentTagForMember(PjbFrontendOfficeTeamMemberView member) {
        if (member == null) {
            return "MEMBRO";
        }
        if (member.patrono()) {
            return "PATRONO";
        }
        if (member.fundador()) {
            return "FUNDADOR";
        }
        if (member.currentUser()) {
            return "VOCE";
        }
        return member.online() ? "ONLINE" : "MEMBRO";
    }

    private String routeForMember(PjbFrontendOfficeTeamMemberView member, HttpServletRequest request) {
        if (member == null || member.userId() == null) {
            return "/api/v1/frontend/app/offices/workspace/summary";
        }
        Long equipeId = officeWorkspaceModeService.current(request).activeEquipeId();
        return equipeId == null
                ? "/api/v1/frontend/app/offices/workspace/summary"
                : "/api/v1/frontend/app/offices/workspace/summary?equipeId=" + equipeId;
    }

    private String normalizeOutcomeToken(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT)
                .replace('Á', 'A')
                .replace('À', 'A')
                .replace('Ã', 'A')
                .replace('Â', 'A')
                .replace('É', 'E')
                .replace('Ê', 'E')
                .replace('Í', 'I')
                .replace('Ó', 'O')
                .replace('Ô', 'O')
                .replace('Õ', 'O')
                .replace('Ú', 'U')
                .replace('Ç', 'C');
    }

    private boolean containsAny(String token, String... probes) {
        if (token == null || token.isBlank()) {
            return false;
        }
        for (String probe : probes) {
            if (probe != null && !probe.isBlank() && token.contains(normalizeOutcomeToken(probe))) {
                return true;
            }
        }
        return false;
    }

    private long safeSize(List<?> items) {
        return items == null ? 0L : items.size();
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private String percent(long value, long total) {
        if (total <= 0L) {
            return "0%";
        }
        long rounded = Math.round((value * 100.0d) / total);
        return rounded + "%";
    }

    private String safeName(Enum<?> value) {
        return value == null ? "SEM_DADO" : value.name();
    }

    private String initials(String nome) {
        if (nome == null || nome.isBlank()) {
            return "PJ";
        }
        String[] parts = nome.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    private String normalizeKey(String value) {
        return value == null ? "SEM_DADO" : value.toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String coalesce(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private String accentHex(String accentTone) {
        if (accentTone == null || accentTone.isBlank()) {
            return BRAZIL_GREEN;
        }
        return switch (accentTone.toUpperCase(Locale.ROOT)) {
            case "SUCCESS", "PRIMARY", "ONLINE", "VOCE" -> BRAZIL_GREEN;
            case "WARNING", "FUNDADOR" -> BRAZIL_YELLOW;
            case "INFO", "SECONDARY", "PATRONO", "MEMBRO" -> BRAZIL_BLUE;
            case "NEUTRAL" -> BRAZIL_WHITE;
            default -> BRAZIL_GREEN_DARK;
        };
    }

    private String badgeHex(String accentTag) {
        if (accentTag == null || accentTag.isBlank()) {
            return BRAZIL_BLUE_ALT;
        }
        return switch (accentTag.toUpperCase(Locale.ROOT)) {
            case "PATRONO" -> BRAZIL_BLUE;
            case "FUNDADOR" -> BRAZIL_YELLOW;
            case "VOCE", "ONLINE" -> BRAZIL_GREEN;
            default -> BRAZIL_BLUE_ALT;
        };
    }

    private String surfaceHex(String accentTone) {
        if (accentTone == null || accentTone.isBlank()) {
            return BRAZIL_SURFACE;
        }
        return switch (accentTone.toUpperCase(Locale.ROOT)) {
            case "WARNING", "FUNDADOR" -> "#2C2A12";
            case "INFO", "SECONDARY", "PATRONO", "MEMBRO" -> BRAZIL_SURFACE_ALT;
            case "NEUTRAL" -> "#1C2741";
            default -> BRAZIL_SURFACE;
        };
    }

    private record OutcomeSummary(long procedentes,
                                  long improcedentes,
                                  long parciais,
                                  long acordos,
                                  long pendentes) {
    }
}
