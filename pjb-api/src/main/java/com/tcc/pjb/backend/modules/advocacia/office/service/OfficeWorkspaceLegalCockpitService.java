package com.tcc.pjb.backend.modules.advocacia.office.service;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessAccessView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeProcessReadingModeView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceLegalCockpitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessCardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessPageView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceProcessView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendOfficeWorkspaceSummaryView;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperienceContext;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import com.tcc.pjb.backend.model.dto.ui.frontend.FrontendOfficeWorkspaceProcessQueryRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.calendar.UserCalendarWorkspaceService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialWorkspaceService;
import com.tcc.pjb.backend.service.timeline.surface.TimelineSurfaceFacadeService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfficeWorkspaceLegalCockpitService {

    private final CurrentUserService currentUserService;
    private final OfficeWorkspaceModeService officeWorkspaceModeService;
    private final OfficeWorkspaceDashboardService officeWorkspaceDashboardService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;
    private final UserCalendarWorkspaceService userCalendarWorkspaceService;
    private final CalculoJudicialWorkspaceService calculoJudicialWorkspaceService;
    private final TimelineSurfaceFacadeService timelineSurfaceFacadeService;
    private final ProcessoRepository processoRepository;
    private final AuditLedgerService auditLedgerService;

    public OfficeWorkspaceLegalCockpitService(CurrentUserService currentUserService,
                                              OfficeWorkspaceModeService officeWorkspaceModeService,
                                              OfficeWorkspaceDashboardService officeWorkspaceDashboardService,
                                              OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService,
                                              UserCalendarWorkspaceService userCalendarWorkspaceService,
                                              CalculoJudicialWorkspaceService calculoJudicialWorkspaceService,
                                              TimelineSurfaceFacadeService timelineSurfaceFacadeService,
                                              ProcessoRepository processoRepository,
                                              AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.officeWorkspaceModeService = Objects.requireNonNull(officeWorkspaceModeService);
        this.officeWorkspaceDashboardService = Objects.requireNonNull(officeWorkspaceDashboardService);
        this.officeProcessWorkspaceScopeService = Objects.requireNonNull(officeProcessWorkspaceScopeService);
        this.userCalendarWorkspaceService = Objects.requireNonNull(userCalendarWorkspaceService);
        this.calculoJudicialWorkspaceService = Objects.requireNonNull(calculoJudicialWorkspaceService);
        this.timelineSurfaceFacadeService = Objects.requireNonNull(timelineSurfaceFacadeService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeWorkspaceLegalCockpitView cockpit(Authentication authentication,
                                                              HttpServletRequest request,
                                                              LocalDate from,
                                                              LocalDate to,
                                                              Long processoId) {
        Usuario usuario = currentUserService.getRequired();
        LocalDate safeFrom = from == null ? LocalDate.now() : from;
        LocalDate safeTo = to == null || to.isBefore(safeFrom) ? safeFrom.plusDays(31) : to;
        PjbFrontendOfficeModeView officeMode = officeWorkspaceModeService.current(request);
        PjbFrontendOfficeWorkspaceSummaryView summary = officeWorkspaceDashboardService.currentSummary(request, officeMode.activeEquipeId());
        PjbFrontendOfficeWorkspaceProcessPageView processPage = officeProcessWorkspaceScopeService.currentWorkspaceProcesses(
                new FrontendOfficeWorkspaceProcessQueryRequest(0, 12, null, null, null, null),
                request);
        List<PjbFrontendOfficeWorkspaceProcessCardView> cards = processPage.items().stream().map(this::toProcessCard).toList();
        CalendarWorkspaceResponse calendarWorkspace = userCalendarWorkspaceService.workspace(safeFrom, safeTo, processoId);
        CalculoJudicialWorkspaceResponse calculadora = calculoJudicialWorkspaceService.workspace(
                authentication,
                resolvePerfilCalculadora(usuario),
                null,
                new CalculoJudicialExperienceContext(firstAllowedRamo(summary, processPage), null, null, resolvePerfilEquipe(summary), null, "OFFICE_WORKSPACE")
        );
        PjbFrontendOfficeProcessReadingModeView reading = processoId == null ? null : readingMode(processoId, request, safeFrom, safeTo);
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (summary == null) {
            blockers.add("WORKSPACE_INSTITUCIONAL_INDISPONIVEL");
        }
        if (cards.isEmpty()) {
            warnings.add("CARTEIRA_VISIVEL_VAZIA_NO_WORKSPACE");
        }
        if (calendarWorkspace.lanes() == null || calendarWorkspace.lanes().isEmpty()) {
            warnings.add("CALENDARIO_SEM_EVENTOS_NO_INTERVALO");
        }
        if (officeMode.patronCertificateRequired()) {
            warnings.add("ASSINATURA_PATRONAL_VINCULADA_AO_WORKSPACE");
        }
        auditLedgerService.appendSafely("ADV_OFFICE_LEGAL_COCKPIT_QUERY", "FRONTEND", String.valueOf(usuario.getId()), "mode=" + officeMode.mode() + " equipe=" + officeMode.activeEquipeId() + " processo=" + processoId);
        return new PjbFrontendOfficeWorkspaceLegalCockpitView(
                officeMode.mode(),
                officeMode.activeEquipeId(),
                officeMode.activeEquipeNome(),
                summary,
                processPage,
                cards,
                calendarWorkspace,
                calculadora,
                reading,
                List.of("CALCULADORA_JUDICIAL", "CALENDARIO_PRAZOS", "CORES_PROCESSUAIS", "MOVIMENTACAO_MODO_LEITURA", "PRAZO_REAL", "PETICIONAMENTO_GOVERNADO"),
                buildQuickRoutes(processoId),
                List.copyOf(blockers),
                List.copyOf(warnings)
        );
    }

    @Transactional(readOnly = true)
    public PjbFrontendOfficeProcessReadingModeView readingMode(Long processoId,
                                                               HttpServletRequest request,
                                                               LocalDate from,
                                                               LocalDate to) {
        LocalDate safeFrom = from == null ? LocalDate.now() : from;
        LocalDate safeTo = to == null || to.isBefore(safeFrom) ? safeFrom.plusDays(31) : to;
        PjbFrontendOfficeModeView officeMode = officeWorkspaceModeService.current(request);
        PjbFrontendOfficeWorkspaceSummaryView summary = officeWorkspaceDashboardService.currentSummary(request, officeMode.activeEquipeId());
        PjbFrontendOfficeProcessAccessView access = officeProcessWorkspaceScopeService.access(processoId, null, request);
        Processo processo = processoRepository.findById(processoId).orElseThrow(() -> new EntityNotFoundException("Processo nao encontrado: " + processoId));
        List<TimelineItemResponse> timeline = access.visibleInWorkspace() ? timelineSurfaceFacadeService.timeline(processoId) : List.of();
        CalendarWorkspaceResponse calendarWorkspace = access.visibleInWorkspace() ? userCalendarWorkspaceService.workspace(safeFrom, safeTo, processoId) : emptyCalendar(safeFrom, safeTo);
        LinkedHashSet<String> blockers = new LinkedHashSet<>(access.blockers());
        LinkedHashSet<String> warnings = new LinkedHashSet<>(access.warnings());
        if (!access.visibleInWorkspace()) {
            blockers.add("PROCESSO_FORA_DO_WORKSPACE_LEGAL");
        }
        if (timeline.isEmpty() && access.visibleInWorkspace()) {
            warnings.add("TIMELINE_SEM_MOVIMENTACAO_RELEVANTE");
        }
        return new PjbFrontendOfficeProcessReadingModeView(
                processoId,
                processNumber(processo),
                officeMode.mode(),
                officeMode.activeEquipeId(),
                summary == null ? officeMode.activeEquipeNome() : summary.officeName(),
                true,
                resolveAccentColor(processo),
                resolveStatusColor(processo.getStatusProcesso()),
                resolveRamoColor(processo.getRamoDireito()),
                resolveSigiloColor(processo.getNivelSigilo()),
                access,
                calendarWorkspace,
                timeline,
                List.of("TIMELINE_PROCESSUAL", "CALENDARIO_DE_PRAZOS", "PRAZO_REAL", "CALCULADORA_JUDICIAL"),
                buildQuickRoutes(processoId),
                List.copyOf(blockers),
                List.copyOf(warnings)
        );
    }

    private PjbFrontendOfficeWorkspaceProcessCardView toProcessCard(PjbFrontendOfficeWorkspaceProcessView item) {
        return new PjbFrontendOfficeWorkspaceProcessCardView(
                item.processoId(),
                item.numeroProcesso(),
                item.ramoDireito(),
                item.statusProcesso(),
                item.nivelSigilo(),
                resolveAccentColor(item.ramoDireito(), item.statusProcesso(), item.nivelSigilo()),
                resolveStatusColor(parseStatus(item.statusProcesso())),
                resolveRamoColor(parseRamo(item.ramoDireito())),
                resolveSigiloColor(parseSigilo(item.nivelSigilo())),
                item.visibleInWorkspace(),
                item.sensitive(),
                item.ownPersonalCase(),
                item.patronCertificateRequired(),
                item.blockers(),
                item.warnings(),
                "/api/v1/frontend/app/offices/workspace/processes/" + item.processoId() + "/reading-mode",
                "/api/v1/timeline/processo/" + item.processoId(),
                "/api/v1/calendar/workspace?processoId=" + item.processoId(),
                "/api/v1/processual/calculos/workspace",
                "/api/v1/processo/" + item.processoId() + "/prazo-real"
        );
    }

    private List<String> buildQuickRoutes(Long processoId) {
        ArrayList<String> routes = new ArrayList<>();
        routes.add("/api/v1/frontend/app/offices/workspace/summary");
        routes.add("/api/v1/frontend/app/offices/workspace/processes/query");
        routes.add("/api/v1/frontend/app/offices/workspace/legal-cockpit");
        routes.add("/api/v1/processual/calculos/workspace");
        routes.add("/api/v1/calendar/workspace");
        if (processoId != null) {
            routes.add("/api/v1/frontend/app/offices/workspace/processes/" + processoId + "/reading-mode");
            routes.add("/api/v1/timeline/processo/" + processoId);
            routes.add("/api/v1/calendar/workspace?processoId=" + processoId);
            routes.add("/api/v1/processo/" + processoId + "/prazo-real");
        }
        return List.copyOf(routes);
    }

    private CalculoJudicialSolicitantePerfil resolvePerfilCalculadora(Usuario usuario) {
        TipoUsuario tipoUsuario = usuario == null ? null : usuario.getTipoUsuario();
        if (tipoUsuario == null) {
            return CalculoJudicialSolicitantePerfil.CIDADAO;
        }
        if (tipoUsuario.isMagistratura()) {
            return CalculoJudicialSolicitantePerfil.MAGISTRATURA;
        }
        if (tipoUsuario.isProcuradoria() || tipoUsuario.isMinisterioPublico()) {
            return CalculoJudicialSolicitantePerfil.PROCURADORIA;
        }
        if (tipoUsuario.isAdvocacia() || tipoUsuario.isDefensoriaPublica()) {
            return CalculoJudicialSolicitantePerfil.ADVOGADO;
        }
        return CalculoJudicialSolicitantePerfil.CIDADAO;
    }

    private String firstAllowedRamo(PjbFrontendOfficeWorkspaceSummaryView summary, PjbFrontendOfficeWorkspaceProcessPageView processPage) {
        if (summary != null && summary.allowedRamos() != null && !summary.allowedRamos().isEmpty()) {
            return summary.allowedRamos().get(0);
        }
        if (processPage != null && processPage.effectiveAllowedRamos() != null && !processPage.effectiveAllowedRamos().isEmpty()) {
            return processPage.effectiveAllowedRamos().get(0);
        }
        return null;
    }

    private String resolvePerfilEquipe(PjbFrontendOfficeWorkspaceSummaryView summary) {
        if (summary == null) {
            return "AUTONOMO";
        }
        if (summary.currentUserFounder()) {
            return "PATRONO";
        }
        if (summary.currentUserAffiliated()) {
            return "AFILIADO";
        }
        return "AUTONOMO";
    }

    private String processNumber(Processo processo) {
        if (processo == null) {
            return null;
        }
        if (processo.getNumeroUnificado() != null && !processo.getNumeroUnificado().isBlank()) {
            return processo.getNumeroUnificado();
        }
        if (processo.getNumeroProcesso() != null && !processo.getNumeroProcesso().isBlank()) {
            return processo.getNumeroProcesso();
        }
        return processo.getNumero();
    }

    private CalendarWorkspaceResponse emptyCalendar(LocalDate from, LocalDate to) {
        return new CalendarWorkspaceResponse(
                from,
                to,
                new CalendarWorkspaceResponse.CalendarProfileDto("OFFICE_SCOPE", "Workspace jurídico", "PRAZOS", List.of(), List.of(), List.of(), false),
                List.of(),
                List.of()
        );
    }

    private String resolveAccentColor(Processo processo) {
        return resolveAccentColor(
                processo == null || processo.getRamoDireito() == null ? null : processo.getRamoDireito().name(),
                processo == null || processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name(),
                processo == null || processo.getNivelSigilo() == null ? null : processo.getNivelSigilo().name()
        );
    }

    private String resolveAccentColor(String ramo, String status, String sigilo) {
        if (sigilo != null && !sigilo.equalsIgnoreCase("PUBLICO")) {
            return "AMBER";
        }
        if (ramo != null) {
            String normalized = ramo.toUpperCase(Locale.ROOT);
            if (normalized.contains("PENAL")) {
                return "RED";
            }
            if (normalized.contains("TRABALH")) {
                return "ORANGE";
            }
            if (normalized.contains("TRIBUT")) {
                return "GOLD";
            }
            if (normalized.contains("PREVID")) {
                return "TEAL";
            }
            if (normalized.contains("FAMILIA")) {
                return "PURPLE";
            }
        }
        if (status != null && (status.equalsIgnoreCase("ARQUIVADO") || status.equalsIgnoreCase("JULGADO") || status.equalsIgnoreCase("TRANSITO_EM_JULGADO"))) {
            return "GREEN";
        }
        return "BLUE";
    }

    private String resolveStatusColor(StatusProcesso status) {
        if (status == null) {
            return "BLUE";
        }
        return switch (status) {
            case ARQUIVADO, JULGADO, TRANSITO_EM_JULGADO, SENTENCA_PROFERIDA -> "GREEN";
            case SUSPENSO_POR_OBITO -> "AMBER";
            default -> "BLUE";
        };
    }

    private String resolveRamoColor(RamoDireito ramoDireito) {
        if (ramoDireito == null) {
            return "BLUE";
        }
        return switch (ramoDireito) {
            case PENAL, PROCESSUAL_PENAL, EXECUCAO_PENAL, MILITAR -> "RED";
            case TRABALHISTA, PROCESSUAL_TRABALHISTA -> "ORANGE";
            case TRIBUTARIO, EXECUCAO_FISCAL -> "GOLD";
            case PREVIDENCIARIO -> "TEAL";
            case FAMILIA, INFANCIA_JUVENTUDE -> "PURPLE";
            default -> "BLUE";
        };
    }

    private String resolveSigiloColor(NivelSigilo nivelSigilo) {
        if (nivelSigilo == null || nivelSigilo == NivelSigilo.PUBLICO) {
            return "GREEN";
        }
        return switch (nivelSigilo) {
            case SEGREDO_JUSTICA -> "AMBER";
            default -> "RED";
        };
    }

    private StatusProcesso parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return StatusProcesso.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private RamoDireito parseRamo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return RamoDireito.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private NivelSigilo parseSigilo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return NivelSigilo.valueOf(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
