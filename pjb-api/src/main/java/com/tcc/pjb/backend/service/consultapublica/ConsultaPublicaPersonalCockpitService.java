package com.tcc.pjb.backend.service.consultapublica;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarPanelResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.cidadao.CidadaoProcessoOverviewResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalAiAssistDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalCalculatorHintDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalCalendarDigestDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalCockpitResponse;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalCockpitSpotlightDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalMovementDigestDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalProcessCardDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaPersonalProcessTagDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceActionDto;
import com.tcc.pjb.backend.model.dto.consultapublica.ConsultaPublicaWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialExperienceContext;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialWorkspaceCardResponse;
import com.tcc.pjb.backend.model.dto.publico.PrazoRealPredictionResponse;
import com.tcc.pjb.backend.model.dto.timeline.TimelineItemResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.workspace.WorkspaceProcessoEtiqueta;
import com.tcc.pjb.backend.model.repository.processo.ProcessoNoteRepository;
import com.tcc.pjb.backend.model.repository.workspace.WorkspaceProcessoEtiquetaRepository;
import com.tcc.pjb.backend.service.calendar.UserCalendarPanelService;
import com.tcc.pjb.backend.service.calendar.UserCalendarWorkspaceService;
import com.tcc.pjb.backend.service.cidadao.CidadaoProcessoOverviewService;
import com.tcc.pjb.backend.service.prazo.CongestionScoreService;
import com.tcc.pjb.backend.service.processual.calculo.CalculoJudicialWorkspaceService;
import com.tcc.pjb.backend.service.security.access.PersonalProcessAccessGuardService;
import com.tcc.pjb.backend.service.timeline.surface.TimelineSurfaceFacadeService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaPublicaPersonalCockpitService {

    private final CurrentUserService currentUserService;
    private final PersonalProcessAccessGuardService personalProcessAccessGuardService;
    private final ConsultaPublicaWorkspaceService consultaPublicaWorkspaceService;
    private final CidadaoProcessoOverviewService processoOverviewService;
    private final UserCalendarWorkspaceService userCalendarWorkspaceService;
    private final UserCalendarPanelService userCalendarPanelService;
    private final CongestionScoreService congestionScoreService;
    private final TimelineSurfaceFacadeService timelineSurfaceFacadeService;
    private final WorkspaceProcessoEtiquetaRepository workspaceProcessoEtiquetaRepository;
    private final ProcessoNoteRepository processoNoteRepository;
    private final CalculoJudicialWorkspaceService calculoJudicialWorkspaceService;

    public ConsultaPublicaPersonalCockpitService(CurrentUserService currentUserService,
                                                 PersonalProcessAccessGuardService personalProcessAccessGuardService,
                                                 ConsultaPublicaWorkspaceService consultaPublicaWorkspaceService,
                                                 CidadaoProcessoOverviewService processoOverviewService,
                                                 UserCalendarWorkspaceService userCalendarWorkspaceService,
                                                 UserCalendarPanelService userCalendarPanelService,
                                                 CongestionScoreService congestionScoreService,
                                                 TimelineSurfaceFacadeService timelineSurfaceFacadeService,
                                                 WorkspaceProcessoEtiquetaRepository workspaceProcessoEtiquetaRepository,
                                                 ProcessoNoteRepository processoNoteRepository,
                                                 CalculoJudicialWorkspaceService calculoJudicialWorkspaceService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.personalProcessAccessGuardService = Objects.requireNonNull(personalProcessAccessGuardService);
        this.consultaPublicaWorkspaceService = Objects.requireNonNull(consultaPublicaWorkspaceService);
        this.processoOverviewService = Objects.requireNonNull(processoOverviewService);
        this.userCalendarWorkspaceService = Objects.requireNonNull(userCalendarWorkspaceService);
        this.userCalendarPanelService = Objects.requireNonNull(userCalendarPanelService);
        this.congestionScoreService = Objects.requireNonNull(congestionScoreService);
        this.timelineSurfaceFacadeService = Objects.requireNonNull(timelineSurfaceFacadeService);
        this.workspaceProcessoEtiquetaRepository = Objects.requireNonNull(workspaceProcessoEtiquetaRepository);
        this.processoNoteRepository = Objects.requireNonNull(processoNoteRepository);
        this.calculoJudicialWorkspaceService = Objects.requireNonNull(calculoJudicialWorkspaceService);
    }

    @Transactional(readOnly = true)
    public ConsultaPublicaPersonalCockpitResponse cockpit(Authentication authentication,
                                                          Long processoId,
                                                          LocalDate from,
                                                          LocalDate to) {
        PersonalProcessAccessGuardService.PersonalProcessAccessEnvelope envelope = personalProcessAccessGuardService.requireOwnProcessAccess("CONSULTA_PUBLICA_PERSONAL_COCKPIT");
        Usuario usuario = currentUserService.getRequired();
        LocalDate safeFrom = from == null ? LocalDate.now() : from;
        LocalDate safeTo = to == null || to.isBefore(safeFrom) ? safeFrom.plusDays(31) : to;
        ConsultaPublicaWorkspaceResponse workspace = consultaPublicaWorkspaceService.workspace();
        List<ConsultaPublicaPersonalProcessCardDto> cards = workspace.meusProcessos() == null ? List.of() : workspace.meusProcessos();
        Long spotlightProcessId = resolveSpotlightProcessId(processoId, cards);
        CalendarPanelResponse portfolioPanel = userCalendarPanelService.panelForUser(usuario, safeFrom, safeTo, null);
        ConsultaPublicaPersonalCockpitSpotlightDto spotlight = spotlightProcessId == null
                ? null
                : buildSpotlight(usuario, spotlightProcessId, safeFrom, safeTo, cards, workspace);
        ConsultaPublicaPersonalAiAssistDto aiAssist = buildAiAssist(spotlight, workspace);
        List<String> warnings = new ArrayList<>();
        if (cards.isEmpty()) {
            warnings.add("Nenhum processo pessoal elegível apareceu no cockpit autenticado; a superfície pública continua disponível, mas a malha pessoal exige vínculo civil direto com o feito.");
        }
        if (spotlight == null) {
            warnings.add("Sem processo em foco, o cockpit expõe portfólio, calendário agregado e rotas rápidas, mas não injeta leitura contextual, IA processual ou prazo real de um feito específico.");
        }
        warnings.addAll(envelope.nextActions());
        return new ConsultaPublicaPersonalCockpitResponse(
                LocalDateTime.now(),
                usuario.getId(),
                "PERSONAL_COCKPIT",
                envelope.accessMode(),
                "Cockpit pessoal integrado de processos, prazos, calendário, calculadora e IA contextual.",
                workspace.routes(),
                workspace.personalHub() == null ? null : workspace.personalHub().metrics(),
                toPortfolioCalendarDigest(portfolioPanel),
                toPortfolioMovementDigest(cards, spotlight),
                spotlight,
                buildCalculatorHints(authentication, usuario, spotlight, workspace),
                aiAssist,
                buildQuickActions(spotlightProcessId, workspace),
                capabilityFlags(spotlight != null),
                List.copyOf(new LinkedHashSet<>(warnings))
        );
    }

    private ConsultaPublicaPersonalCockpitSpotlightDto buildSpotlight(Usuario usuario,
                                                                      Long processoId,
                                                                      LocalDate from,
                                                                      LocalDate to,
                                                                      List<ConsultaPublicaPersonalProcessCardDto> cards,
                                                                      ConsultaPublicaWorkspaceResponse workspace) {
        CidadaoProcessoOverviewResponse overview = processoOverviewService.overview(processoId);
        PrazoRealPredictionResponse prazoReal = congestionScoreService.predizer(processoId, "ATO_PROCESSUAL");
        List<TimelineItemResponse> timeline = timelineSurfaceFacadeService.timeline(processoId).stream().limit(12).toList();
        List<ConsultaPublicaPersonalProcessTagDto> tags = workspaceProcessoEtiquetaRepository.findAllByProcessoId(processoId).stream()
                .map(this::toTag)
                .sorted(Comparator.comparing(ConsultaPublicaPersonalProcessTagDto::nome, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
        long notesCount = processoNoteRepository.countByProcessoId(processoId);
        CalendarWorkspaceResponse processCalendar = userCalendarWorkspaceService.workspaceForUser(usuario, from, to, processoId);
        ConsultaPublicaPersonalProcessCardDto card = cards.stream().filter(item -> Objects.equals(item.processoId(), processoId)).findFirst().orElse(null);
        List<String> warnings = new ArrayList<>();
        if (timeline.isEmpty()) {
            warnings.add("A timeline autenticada não retornou movimentações recentes para o processo em foco no intervalo visível.");
        }
        if (notesCount == 0) {
            warnings.add("Este processo ainda não tem notas privadas registradas no workspace pessoal.");
        }
        if (tags.isEmpty()) {
            warnings.add("Este processo ainda não recebeu etiquetas do workspace; a cor processual depende sobretudo de prazo e movimentação.");
        }
        return new ConsultaPublicaPersonalCockpitSpotlightDto(
                processoId,
                overview.card() == null ? null : overview.card().numeroUnificado(),
                card == null ? normalizePrazoBand(prazoReal.uiBand()) : card.colorBand(),
                overview,
                prazoReal,
                toProcessCalendarDigest(processCalendar, from, to),
                timeline,
                tags,
                notesCount,
                buildSpotlightActions(processoId, workspace),
                List.copyOf(warnings)
        );
    }

    private List<ConsultaPublicaWorkspaceActionDto> buildQuickActions(Long processoId,
                                                                      ConsultaPublicaWorkspaceResponse workspace) {
        List<ConsultaPublicaWorkspaceActionDto> actions = new ArrayList<>();
        if (workspace.personalHub() != null && workspace.personalHub().quickActions() != null) {
            actions.addAll(workspace.personalHub().quickActions());
        }
        actions.add(new ConsultaPublicaWorkspaceActionDto("COCKPIT", "Abrir cockpit pessoal", "/api/v1/processos/pessoais/cockpit", "PRIMARY"));
        if (processoId != null) {
            actions.add(new ConsultaPublicaWorkspaceActionDto("PROCESSO_EM_FOCO", "Fixar processo em foco", "/api/v1/processos/pessoais/cockpit?processoId=" + processoId, "PRIMARY"));
        }
        return List.copyOf(actions);
    }

    private List<ConsultaPublicaWorkspaceActionDto> buildSpotlightActions(Long processoId,
                                                                          ConsultaPublicaWorkspaceResponse workspace) {
        List<ConsultaPublicaWorkspaceActionDto> actions = new ArrayList<>();
        actions.add(new ConsultaPublicaWorkspaceActionDto("OVERVIEW", "Abrir visão autenticada", "/api/v1/processos/pessoais/" + processoId + "/overview", "PRIMARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("COCKPIT", "Atualizar cockpit neste processo", "/api/v1/processos/pessoais/cockpit?processoId=" + processoId, "PRIMARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("CALENDARIO", "Abrir calendário do processo", "/api/v1/calendar/workspace?from={from}&to={to}&processoId=" + processoId, "SECONDARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("TIMELINE", "Abrir timeline", "/api/v1/timeline/processo/" + processoId, "SECONDARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("PRAZO_REAL", "Abrir prazo real", "/api/v1/processos/" + processoId + "/prazo-real?tipoAto=ATO_PROCESSUAL", "SECONDARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("CALCULADORA", "Abrir calculadora judicial", "/api/v1/processual/calculos/workspace", "SECONDARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("IA_PROCESSUAL", "Abrir IA processual", "/api/v1/chat/processo/" + processoId, "SECONDARY"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("NOTAS", "Abrir notas privadas", "/api/v1/processos/" + processoId + "/notes", "NEUTRAL"));
        actions.add(new ConsultaPublicaWorkspaceActionDto("ETIQUETAS", "Abrir etiquetas e cores", "/api/v1/workspace/processos/" + processoId + "/etiquetas", "NEUTRAL"));
        if (workspace.personalHub() != null && workspace.personalHub().quickActions() != null) {
            actions.addAll(workspace.personalHub().quickActions().stream()
                    .filter(item -> !"MEUS_PROCESSOS".equals(item.code()))
                    .toList());
        }
        return List.copyOf(actions);
    }

    private ConsultaPublicaPersonalCalendarDigestDto toPortfolioCalendarDigest(CalendarPanelResponse panel) {
        int criticalDays = panel.dayCells() == null ? 0 : (int) panel.dayCells().stream().filter(item -> item.criticalEvents() > 0).count();
        String dominantColor = panel.dayCells() == null
                ? null
                : panel.dayCells().stream()
                .filter(item -> item.dominantColor() != null && !item.dominantColor().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(CalendarPanelResponse.PanelDayCellDto::dominantColor, LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        String dominantLane = panel.highlightedEvents() == null
                ? null
                : panel.highlightedEvents().stream()
                .filter(item -> item.laneCode() != null && !item.laneCode().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(CalendarPanelResponse.HighlightedEventDto::laneCode, LinkedHashMap::new, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        List<String> focusLabels = panel.highlightedEvents() == null
                ? List.of()
                : panel.highlightedEvents().stream().map(CalendarPanelResponse.HighlightedEventDto::title).filter(Objects::nonNull).distinct().limit(6).toList();
        return new ConsultaPublicaPersonalCalendarDigestDto(
                panel.from(),
                panel.to(),
                panel.highlightedEvents() == null ? 0 : panel.highlightedEvents().size(),
                panel.processSpotlights() == null ? 0 : panel.processSpotlights().size(),
                criticalDays,
                dominantLane,
                dominantColor,
                focusLabels
        );
    }

    private ConsultaPublicaPersonalCalendarDigestDto toProcessCalendarDigest(CalendarWorkspaceResponse workspace,
                                                                             LocalDate from,
                                                                             LocalDate to) {
        int totalEvents = workspace.lanes() == null ? 0 : workspace.lanes().stream().mapToInt(CalendarWorkspaceResponse.CalendarLaneDto::totalEvents).sum();
        int criticalDays = 0;
        Map<String, Integer> laneCounts = new LinkedHashMap<>();
        LinkedHashSet<String> focusLabels = new LinkedHashSet<>();
        String dominantColor = null;
        for (CalendarWorkspaceResponse.CalendarLaneDto lane : workspace.lanes()) {
            laneCounts.put(lane.laneCode(), lane.totalEvents());
            if (dominantColor == null && lane.totalEvents() > 0) {
                dominantColor = lane.accentColor();
            }
            for (CalendarWorkspaceResponse.CalendarDayDto day : lane.days()) {
                boolean criticalDay = false;
                for (CalendarWorkspaceEventDto event : day.events()) {
                    if (event.title() != null && !event.title().isBlank()) {
                        focusLabels.add(event.title().trim());
                    }
                    if (isCriticalColor(event.color())) {
                        criticalDay = true;
                    }
                }
                if (criticalDay) {
                    criticalDays++;
                }
            }
        }
        String dominantLane = laneCounts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
        return new ConsultaPublicaPersonalCalendarDigestDto(
                from,
                to,
                totalEvents,
                Math.min(focusLabels.size(), 6),
                criticalDays,
                dominantLane,
                dominantColor,
                focusLabels.stream().limit(6).toList()
        );
    }

    private ConsultaPublicaPersonalMovementDigestDto toPortfolioMovementDigest(List<ConsultaPublicaPersonalProcessCardDto> cards,
                                                                               ConsultaPublicaPersonalCockpitSpotlightDto spotlight) {
        long recent48h = cards.stream()
                .filter(item -> item.ultimaMovimentacao() != null && item.ultimaMovimentacao().data() != null)
                .filter(item -> item.ultimaMovimentacao().data().isAfter(LocalDateTime.now().minusHours(48)))
                .count();
        long withOpenDeadline = cards.stream().filter(item -> item.proximoPrazo() != null).count();
        long blockedItems = spotlight == null || spotlight.timeline() == null
                ? 0L
                : spotlight.timeline().stream().filter(TimelineItemResponse::bloqueioOperacional).count();
        Map<String, Long> statuses = cards.stream()
                .map(ConsultaPublicaPersonalProcessCardDto::colorBand)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.groupingBy(v -> v, LinkedHashMap::new, java.util.stream.Collectors.counting()));
        String dominantStatus = statuses.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("STABLE_NEUTRAL");
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        signals.add("processos_no_portfolio=" + cards.size());
        signals.add("movimentacao_48h=" + recent48h);
        signals.add("com_prazo_monitorado=" + withOpenDeadline);
        if (spotlight != null && spotlight.prazoReal() != null) {
            signals.add("prazo_real_risco=" + safe(spotlight.prazoReal().riskLevel()));
        }
        if (spotlight != null && spotlight.notesCount() > 0) {
            signals.add("notas_privadas=" + spotlight.notesCount());
        }
        return new ConsultaPublicaPersonalMovementDigestDto(
                cards.size() + (spotlight == null || spotlight.timeline() == null ? 0 : spotlight.timeline().size()),
                recent48h,
                withOpenDeadline,
                blockedItems,
                dominantStatus,
                List.copyOf(signals)
        );
    }

    private List<ConsultaPublicaPersonalCalculatorHintDto> buildCalculatorHints(Authentication authentication,
                                                                                Usuario usuario,
                                                                                ConsultaPublicaPersonalCockpitSpotlightDto spotlight,
                                                                                ConsultaPublicaWorkspaceResponse workspace) {
        List<String> domainCodes = recommendedDomains(spotlight);
        CalculoJudicialSolicitantePerfil perfil = resolvePerfilCalculadora(usuario);
        String workspaceRoute = workspace.routes().judicialCalculatorWorkspace();
        return domainCodes.stream().distinct().limit(3).map(domainCode -> {
            CalculoJudicialWorkspaceCardResponse card = calculoJudicialWorkspaceService.workspaceCard(
                    authentication,
                    perfil,
                    domainCode,
                    new CalculoJudicialExperienceContext(resolveRamoDireito(spotlight), resolveClasseProcessual(spotlight), null, "PESSOAL", null, "CONSULTA_PUBLICA_PERSONAL_COCKPIT")
            );
            String title = card == null ? fallbackCalculatorTitle(domainCode) : card.titulo();
            String resolvedMode = card == null ? "assistida_ia" : String.valueOf(card.design().getOrDefault("resolvedExperienceMode", "assistida_ia"));
            return new ConsultaPublicaPersonalCalculatorHintDto(
                    domainCode,
                    title,
                    calculatorRationale(domainCode, spotlight),
                    workspaceRoute,
                    workspaceRoute + "/" + domainCode,
                    !"manual_tradicional".equalsIgnoreCase(resolvedMode),
                    resolvedMode
            );
        }).toList();
    }

    private ConsultaPublicaPersonalAiAssistDto buildAiAssist(ConsultaPublicaPersonalCockpitSpotlightDto spotlight,
                                                             ConsultaPublicaWorkspaceResponse workspace) {
        if (spotlight == null) {
            return new ConsultaPublicaPersonalAiAssistDto(
                    workspace.routes().processChatHistory().replace("{processoId}", "{processoId}"),
                    "/api/v1/chat",
                    "Assistência geral autenticada sem processo em foco.",
                    false,
                    List.of(
                            "Quais processos do meu portfólio exigem atuação esta semana?",
                            "Resuma minhas frentes urgentes com base em prazo, movimentação e etiquetas.",
                            "Mostre como devo priorizar calendário, cálculo e leitura orientada hoje."
                    ),
                    List.of(
                            "A IA do cockpit não amplia sigilo nem cria acesso a autos de terceiros.",
                            "Sugestões devem ser validadas pelo operador antes de peticionar ou praticar ato processual."
                    )
            );
        }
        String processoNumero = Optional.ofNullable(spotlight.processoNumero()).orElse("processo em foco");
        return new ConsultaPublicaPersonalAiAssistDto(
                "/api/v1/chat/processo/" + spotlight.processoId(),
                "/api/v1/chat",
                "Assistência contextual autenticada para " + processoNumero + ".",
                true,
                List.of(
                        "Explique a última movimentação e o próximo risco operacional deste processo.",
                        "Monte um resumo técnico do processo com foco em prazo, rito e providência sugerida.",
                        "Diga se devo abrir calendário, calculadora judicial ou leitura orientada primeiro neste feito."
                ),
                List.of(
                        "A IA deve operar com base no contexto autenticado do titular ou operador autorizado.",
                        "Nenhuma resposta da IA substitui conferência jurídica, cálculo auditável ou leitura integral do ato oficial."
                )
        );
    }

    private Long resolveSpotlightProcessId(Long explicitProcessId,
                                           List<ConsultaPublicaPersonalProcessCardDto> cards) {
        if (explicitProcessId != null) {
            return explicitProcessId;
        }
        return cards.isEmpty() ? null : cards.getFirst().processoId();
    }

    private ConsultaPublicaPersonalProcessTagDto toTag(WorkspaceProcessoEtiqueta item) {
        return new ConsultaPublicaPersonalProcessTagDto(
                item.getEtiqueta().getId(),
                item.getEtiqueta().getNome(),
                item.getEtiqueta().getCorHex(),
                item.getEtiqueta().isSistema(),
                item.getEtiqueta().getAtualizadoEm()
        );
    }

    private boolean isCriticalColor(String color) {
        if (color == null || color.isBlank()) {
            return false;
        }
        String normalized = color.trim().toUpperCase(Locale.ROOT);
        return normalized.contains("RED") || normalized.contains("ORANGE") || normalized.contains("CRITICAL") || normalized.contains("ATTENTION");
    }

    private String normalizePrazoBand(String uiBand) {
        if (uiBand == null || uiBand.isBlank()) {
            return "STABLE_NEUTRAL";
        }
        String normalized = uiBand.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("RED") || normalized.contains("CRITICAL")) {
            return "CRITICAL_RED";
        }
        if (normalized.contains("ORANGE") || normalized.contains("ATTENTION")) {
            return "ATTENTION_ORANGE";
        }
        if (normalized.contains("BLUE") || normalized.contains("ACTIVE")) {
            return "ACTIVE_BLUE";
        }
        return normalized;
    }

    private List<String> capabilityFlags(boolean spotlightPresent) {
        List<String> flags = new ArrayList<>();
        flags.add("PERSONAL_PROCESS_ACCESS_GUARDED");
        flags.add("CALENDAR_INTEGRATED");
        flags.add("DEADLINE_REALTIME_INTEGRATED");
        flags.add("PROCESS_COLORS_ACTIVE");
        flags.add("JUDICIAL_CALCULATOR_LINKED");
        flags.add("AI_CONTEXTUAL_ASSIST_LINKED");
        if (spotlightPresent) {
            flags.add("PROCESS_SPOTLIGHT_ACTIVE");
            flags.add("TIMELINE_PROCESSUAL_LINKED");
            flags.add("NOTES_AND_TAGS_LINKED");
        }
        return List.copyOf(flags);
    }

    private List<String> recommendedDomains(ConsultaPublicaPersonalCockpitSpotlightDto spotlight) {
        List<String> domains = new ArrayList<>();
        domains.add("CUSTAS_PROCESSUAIS");
        RamoDireito ramo = RamoDireito.fromString(resolveRamoDireito(spotlight));
        if (ramo == null) {
            domains.add("FAZENDA_TRIBUTARIO");
            return domains;
        }
        if (ramo == RamoDireito.TRABALHISTA || ramo == RamoDireito.PROCESSUAL_TRABALHISTA || ramo == RamoDireito.ACIDENTARIO) {
            domains.add(0, "TRABALHISTA_CLT");
            return domains;
        }
        if (ramo == RamoDireito.PREVIDENCIARIO) {
            domains.add(0, "FEDERAL_PREVIDENCIARIO_CJF");
            return domains;
        }
        if (ramo.isFazendaLike()) {
            domains.add(0, "FAZENDA_TRIBUTARIO");
            return domains;
        }
        domains.add(0, "FAZENDA_TRIBUTARIO");
        return domains;
    }

    private String calculatorRationale(String domainCode,
                                       ConsultaPublicaPersonalCockpitSpotlightDto spotlight) {
        String processNumber = spotlight == null || spotlight.processoNumero() == null ? "o processo em foco" : spotlight.processoNumero();
        return switch (domainCode) {
            case "TRABALHISTA_CLT" -> "O ramo processual e o perfil do feito sugerem cálculo trabalhista auditável para " + processNumber + ".";
            case "FEDERAL_PREVIDENCIARIO_CJF" -> "O processo em foco aponta aderência previdenciária, então a calculadora federal/previdenciária acelera estimativa e conferência para " + processNumber + ".";
            case "FAZENDA_TRIBUTARIO" -> "A natureza fazendária ou tributária do caso recomenda trilha de cálculo com correção, juros e memória técnica para " + processNumber + ".";
            case "CUSTAS_PROCESSUAIS" -> "Custas e despesas permanecem relevantes no cockpit pessoal para evitar cegueira operacional em " + processNumber + ".";
            default -> "A calculadora judicial pode ser aberta sem duplicar regra negocial dentro do cockpit pessoal.";
        };
    }

    private String fallbackCalculatorTitle(String domainCode) {
        return switch (domainCode) {
            case "TRABALHISTA_CLT" -> "Calculadora trabalhista CLT";
            case "FAZENDA_TRIBUTARIO" -> "Calculadora fazenda e tributário";
            case "FEDERAL_PREVIDENCIARIO_CJF" -> "Calculadora federal e previdenciária";
            case "CUSTAS_PROCESSUAIS" -> "Calculadora de custas processuais";
            default -> "Calculadora judicial";
        };
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

    private String resolveRamoDireito(ConsultaPublicaPersonalCockpitSpotlightDto spotlight) {
        if (spotlight == null || spotlight.overview() == null || spotlight.overview().card() == null) {
            return null;
        }
        return spotlight.overview().card().ramoSugerido();
    }

    private String resolveClasseProcessual(ConsultaPublicaPersonalCockpitSpotlightDto spotlight) {
        if (spotlight == null || spotlight.overview() == null || spotlight.overview().card() == null) {
            return null;
        }
        return spotlight.overview().card().classeProcessual();
    }

    private String safe(String value) {
        return value == null ? "NAO_INFORMADO" : value;
    }
}
