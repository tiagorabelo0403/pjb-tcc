package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarEventsResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarSystemEvent;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarSystemEventRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCalendarWorkspaceService {

    private final UserCalendarService userCalendarService;
    private final CurrentUserService currentUserService;
    private final UserCalendarSystemEventRepository systemEventRepository;
    private final CalendarAudienceProfileService audienceProfileService;
    private final UserCalendarPreferenceService preferenceService;
    private final ProcessoRepository processoRepository;
    private final CalendarInstitutionalScopeService scopeService;
    private final CalendarInstitutionalContextService contextService;

    public UserCalendarWorkspaceService(UserCalendarService userCalendarService,
                                        CurrentUserService currentUserService,
                                        UserCalendarSystemEventRepository systemEventRepository,
                                        CalendarAudienceProfileService audienceProfileService,
                                        UserCalendarPreferenceService preferenceService,
                                        ProcessoRepository processoRepository,
                                        CalendarInstitutionalScopeService scopeService,
                                        CalendarInstitutionalContextService contextService) {
        this.userCalendarService = Objects.requireNonNull(userCalendarService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.systemEventRepository = Objects.requireNonNull(systemEventRepository);
        this.audienceProfileService = Objects.requireNonNull(audienceProfileService);
        this.preferenceService = Objects.requireNonNull(preferenceService);
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.scopeService = Objects.requireNonNull(scopeService);
        this.contextService = Objects.requireNonNull(contextService);
    }

    @Transactional(readOnly = true)
    public CalendarWorkspaceResponse workspace(LocalDate from, LocalDate to, Long processoId) {
        return workspaceForUser(currentUserService.getRequired(), from, to, processoId);
    }

    @Transactional(readOnly = true)
    public CalendarWorkspaceResponse workspaceForUser(Usuario usuario, LocalDate from, LocalDate to, Long processoId) {
        CalendarAudienceProfileService.CalendarProfile defaultProfile = audienceProfileService.resolve(usuario);
        var preference = preferenceService.currentOrDefault(usuario);
        CalendarAudienceProfileService.CalendarProfile profile = new CalendarAudienceProfileService.CalendarProfile(
                defaultProfile.profileCode(),
                defaultProfile.profileTitle(),
                defaultProfile.highlightLaneCode(),
                preference.visibleLaneCodes(),
                preference.pinnedLaneCodes(),
                defaultProfile.prazoTracks(),
                defaultProfile.colorLegend(),
                preference.includePersonalCalendar()
        );
        CalendarEventsResponse base = processoId == null
                ? userCalendarService.listForUser(usuario, from, to)
                : userCalendarService.listForProcessoForUser(usuario, from, to, processoId);
        Map<Long, UserCalendarSystemEvent> systemIndex = indexSystemEvents(usuario, from, to);
        Map<Long, ProcessoContext> processoContext = indexProcessos(base);
        List<CalendarInstitutionalScopeService.ScopeOption> scopes = scopeService.availableScopes(usuario, preference.includePersonalCalendar(), preference.includeInstitutionalCalendar(), processoId);
        String activeScopeCode = scopeService.normalizeActiveScope(preference.selectedScopeCode(), scopes, preference.includePersonalCalendar(), preference.includeInstitutionalCalendar());
        Long selectedTeamId = preference.selectedTeamId() != null ? preference.selectedTeamId() : scopeService.parseTeamId(activeScopeCode);
        List<CalendarInstitutionalContextService.InstitutionalContextOption> contexts = contextService.availableContexts(usuario, activeScopeCode, processoId, selectedTeamId);
        String activeInstitutionContextCode = contextService.normalizeActiveContext(preference.selectedInstitutionContextCode(), contexts, activeScopeCode);
        EnumMap<Lane, WorkspaceLaneAccumulator> grouped = new EnumMap<>(Lane.class);
        for (Lane lane : Lane.values()) {
            grouped.put(lane, new WorkspaceLaneAccumulator());
        }
        for (CalendarEventsResponse.CalendarDayDto day : base.days()) {
            for (CalendarEventDto event : day.events()) {
                UserCalendarSystemEvent systemEvent = "CUSTOM".equals(event.eventType()) || event.eventId() == null
                        ? null
                        : systemIndex.get(event.eventId());
                ProcessoContext context = event.processoId() == null ? null : processoContext.get(event.processoId());
                Lane lane = resolveLane(event, systemEvent);
                if (!scopeService.scopeAllows(activeScopeCode, new CalendarInstitutionalScopeService.CalendarContext(lane == Lane.PESSOAL, context == null ? null : context.equipeId()), preference.includePersonalCalendar())) {
                    continue;
                }
                Segment segment = resolveSegment(lane, event, systemEvent, context);
                if (!contextService.allows(activeInstitutionContextCode, lane.code, segment.code, profile.profileCode())) {
                    continue;
                }
                CalendarWorkspaceEventDto workspaceEvent = new CalendarWorkspaceEventDto(
                        lane.code,
                        segment.code,
                        segment.title,
                        event.eventType(),
                        event.eventId(),
                        event.processoId(),
                        event.processoNumero(),
                        event.title(),
                        resolveSubtitle(event, systemEvent, context),
                        event.at(),
                        normalizeColor(event.color(), lane.accentColor),
                        event.marked(),
                        event.detailsUrl(),
                        resolveDeadlineRuleSummary(lane, segment, context),
                        resolveAudienceCode(usuario, profile.profileCode(), activeInstitutionContextCode, segment)
                );
                WorkspaceLaneAccumulator accumulator = grouped.get(lane);
                accumulator.days.computeIfAbsent(day.day(), ignored -> new ArrayList<>()).add(workspaceEvent);
                accumulator.segmentCounts.merge(segment.code, new SegmentCounter(segment.code, segment.title, 1), SegmentCounter::merge);
            }
        }
        List<CalendarWorkspaceResponse.CalendarLaneDto> lanes = new ArrayList<>(Lane.values().length);
        Set<String> visibleCodes = new LinkedHashSet<>(profile.visibleLaneCodes());
        Set<String> pinnedCodes = new LinkedHashSet<>(profile.pinnedLaneCodes());
        for (Lane lane : Lane.values()) {
            WorkspaceLaneAccumulator accumulator = grouped.get(lane);
            List<CalendarWorkspaceResponse.CalendarDayDto> laneDays = accumulator.days.entrySet().stream()
                    .map(entry -> new CalendarWorkspaceResponse.CalendarDayDto(
                            entry.getKey(),
                            entry.getValue().stream()
                                    .sorted(Comparator.comparing(CalendarWorkspaceEventDto::at).thenComparing(CalendarWorkspaceEventDto::title))
                                    .toList()
                    ))
                    .toList();
            int totalEvents = laneDays.stream().mapToInt(item -> item.events().size()).sum();
            List<CalendarWorkspaceResponse.CalendarSegmentDto> segments = accumulator.segmentCounts.values().stream()
                    .sorted(Comparator.comparing(SegmentCounter::title))
                    .map(SegmentCounter::toDto)
                    .toList();
            lanes.add(new CalendarWorkspaceResponse.CalendarLaneDto(
                    lane.code,
                    lane.title,
                    lane.accentColor,
                    visibleCodes.contains(lane.code),
                    pinnedCodes.contains(lane.code),
                    totalEvents,
                    segments,
                    laneDays
            ));
        }
        return new CalendarWorkspaceResponse(
                from,
                to,
                new CalendarWorkspaceResponse.CalendarProfileDto(
                        profile.profileCode(),
                        profile.profileTitle(),
                        profile.highlightLaneCode(),
                        profile.visibleLaneCodes(),
                        profile.pinnedLaneCodes(),
                        profile.prazoTracks().stream()
                                .map(track -> new CalendarWorkspaceResponse.CalendarPrazoTrackDto(
                                        track.trackCode(),
                                        track.trackTitle(),
                                        track.regime(),
                                        track.summary(),
                                        track.highlighted()
                                )).toList(),
                        profile.personalEventsEnabled()
                ),
                profile.colorLegend().stream()
                        .map(item -> new CalendarWorkspaceResponse.CalendarColorLegendDto(item.colorCode(), item.label(), item.meaning()))
                        .toList(),
                List.copyOf(lanes)
        );
    }

    private Map<Long, UserCalendarSystemEvent> indexSystemEvents(Usuario usuario, LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from.atStartOfDay();
        LocalDateTime toDateTime = to.plusDays(1).atStartOfDay().minusSeconds(1);
        Map<Long, UserCalendarSystemEvent> index = new LinkedHashMap<>();
        for (UserCalendarSystemEvent event : systemEventRepository.findByUsuarioIdBetween(usuario.getId(), fromDateTime, toDateTime)) {
            if (event.getId() != null) {
                index.put(event.getId(), event);
            }
        }
        return index;
    }

    private Map<Long, ProcessoContext> indexProcessos(CalendarEventsResponse base) {
        LinkedHashSet<Long> processoIds = new LinkedHashSet<>();
        for (CalendarEventsResponse.CalendarDayDto day : base.days()) {
            for (CalendarEventDto event : day.events()) {
                if (event.processoId() != null) {
                    processoIds.add(event.processoId());
                }
            }
        }
        if (processoIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, ProcessoContext> index = new LinkedHashMap<>();
        for (Processo processo : processoRepository.findAllById(processoIds)) {
            index.put(processo.getId(), new ProcessoContext(processo.getRamoDireito(), processo.getRito(), processo.getTribunal(), processo.getUf(), processo.getEquipe() != null ? processo.getEquipe().getId() : null));
        }
        return index;
    }

    private Lane resolveLane(CalendarEventDto event, UserCalendarSystemEvent systemEvent) {
        if ("CUSTOM".equals(event.eventType())) {
            return Lane.PESSOAL;
        }
        if ("AUDIENCIA".equals(event.eventType())
                || "JULGAMENTO".equals(event.eventType())
                || "AUDIENCIA_PROCESSUAL".equals(event.eventType())
                || "AUDIENCIA_RECURSO_SECRETARIA".equals(event.eventType())
                || "AUDIENCIA_PRESENCA_SECRETARIA".equals(event.eventType())) {
            return Lane.AGENDA_PROCESSUAL;
        }
        String raw = normalize(event.domainKey(),
                event.sourceCode(),
                event.body(),
                systemEvent == null ? null : systemEvent.getDomainKey(),
                systemEvent == null ? null : systemEvent.getEventType(),
                systemEvent == null ? null : systemEvent.getTitle(),
                systemEvent == null ? null : systemEvent.getBody(),
                event.eventType(),
                event.title());
        if (containsAny(raw, "PRECAT", "RPV", "REQUISICAO DE PAGAMENTO", "REQUISIÇÃO DE PAGAMENTO", "ALVARA", "ALVARÁ")) {
            return Lane.PRECATORIOS;
        }
        if (containsAny(raw, ":PRAZO", ":LEMBRETE:", " PRAZO ", "PRAZO:", "VENC", "DECURSO", "INTIMACAO", "INTIMAÇÃO", "CITACAO", "CITAÇÃO", "EMBARG", "APELA", "AGRAVO", "RECURSO", "RESP", "RE ")) {
            return Lane.PRAZOS;
        }
        return Lane.AGENDA_PROCESSUAL;
    }

    private Segment resolveSegment(Lane lane, CalendarEventDto event, UserCalendarSystemEvent systemEvent, ProcessoContext context) {
        String raw = normalize(event.domainKey(),
                event.sourceCode(),
                event.body(),
                systemEvent == null ? null : systemEvent.getDomainKey(),
                systemEvent == null ? null : systemEvent.getTitle(),
                systemEvent == null ? null : systemEvent.getBody(),
                event.eventType(),
                event.title());
        if (lane == Lane.PESSOAL) {
            return Segment.PESSOAL_GERAL;
        }
        if (lane == Lane.AGENDA_PROCESSUAL) {
            if (containsAny(raw, "MANDADO", "DILIGEN", "AVALIADOR", "CUMPRIMENTO")) {
                return Segment.AGENDA_MANDADOS;
            }
            if (containsAny(raw, "PERICIA", "PERÍCIA", "LAUDO", "VISTORIA", "EXAME TECNICO", "EXAME TÉCNICO")) {
                return Segment.AGENDA_PERICIAS;
            }
            if (containsAny(raw, "GABINETE", "CONCLUSAO", "CONCLUSÃO", "MINUTA", "VOTO")) {
                return Segment.AGENDA_GABINETE;
            }
            if (containsAny(raw, "SECRETARIA", "CARTORIO", "CARTÓRIO", "EXPEDIENTE", "INTIMACAO", "INTIMAÇÃO", "REMESSA")) {
                return Segment.AGENDA_SECRETARIA;
            }
            if (containsAny(raw, "JULGAMENTO", "PAUTA", "COLEGIADO", "SESSAO", "SESSÃO")) {
                return Segment.AGENDA_JULGAMENTOS;
            }
            if (containsAny(raw, "AUDIENCIA", "AUDIÊNCIA", "PRESENCA", "PRESENÇA")) {
                return Segment.AGENDA_AUDIENCIAS;
            }
            return Segment.AGENDA_OPERACIONAL;
        }
        if (lane == Lane.PRECATORIOS) {
            if (containsAny(raw, "RPV", "PEQUENO VALOR")) {
                return Segment.PRECATORIO_RPV;
            }
            if (containsAny(raw, "ALVARA", "ALVARÁ", "LIBERACAO", "LIBERAÇÃO")) {
                return Segment.PRECATORIO_LIBERACAO;
            }
            return Segment.PRECATORIO_ORDEM;
        }
        if (context != null) {
            if (context.rito != null && (context.rito == RitoProcessual.JUIZADO_ESPECIAL
                    || context.rito == RitoProcessual.JUIZADO_ESPECIAL_CIVEL
                    || context.rito == RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA
                    || context.rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL
                    || context.rito == RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL)) {
                return Segment.PRAZO_JUIZADO;
            }
            if (context.ramo == RamoDireito.TRABALHISTA || (context.rito != null && context.rito.isTrabalhista())) {
                return containsAny(raw, "RECURSO", "AGRAVO", "REVISTA", "EMBARGO") ? Segment.PRAZO_TRABALHISTA_RECURSAL : Segment.PRAZO_TRABALHISTA_GERAL;
            }
            if (context.ramo == RamoDireito.ELEITORAL || (context.rito != null && context.rito.isEleitoral())) {
                return Segment.PRAZO_ELEITORAL;
            }
            if (context.ramo == RamoDireito.PENAL || context.ramo == RamoDireito.MILITAR || (context.rito != null && context.rito.isPenal())) {
                return Segment.PRAZO_PENAL;
            }
        }
        if (containsAny(raw, "EMBARG")) {
            return Segment.PRAZO_EMBARGOS;
        }
        if (containsAny(raw, "APELA", "AGRAVO", "RESP", "RECURSO ESPECIAL", "RECURSO EXTRAORDINARIO", "RECURSO EXTRAORDINÁRIO", "CONTRARRAZOES", "CONTRARRAZÕES", "CONFLITO DE COMPETENCIA", "CONFLITO DE COMPETÊNCIA")) {
            return Segment.PRAZO_RECURSAL_CPC;
        }
        return Segment.PRAZO_CPC_GERAL;
    }

    private String resolveSubtitle(CalendarEventDto event, UserCalendarSystemEvent systemEvent, ProcessoContext context) {
        if (event.body() != null && !event.body().isBlank()) {
            String body = event.body().trim();
            return body.length() > 220 ? body.substring(0, 220) : body;
        }
        if (systemEvent != null && systemEvent.getBody() != null && !systemEvent.getBody().isBlank()) {
            String body = systemEvent.getBody().trim();
            return body.length() > 220 ? body.substring(0, 220) : body;
        }
        if (event.processoNumero() != null && !event.processoNumero().isBlank()) {
            StringBuilder builder = new StringBuilder("Processo ").append(event.processoNumero());
            if (context != null && context.ramo != null) {
                builder.append(" · ").append(context.ramo.getDescricao());
            }
            if (context != null && context.rito != null) {
                builder.append(" · ").append(normalizeRito(context.rito));
            }
            return builder.toString();
        }
        return titleForEventType(event.eventType());
    }

    private String resolveDeadlineRuleSummary(Lane lane, Segment segment, ProcessoContext context) {
        if (lane != Lane.PRAZOS) {
            return lane == Lane.PRECATORIOS ? "Agenda financeira segregada da agenda processual." : null;
        }
        return switch (segment) {
            case PRAZO_CPC_GERAL -> "CPC • dias úteis • ajuste para o próximo dia útil forense.";
            case PRAZO_RECURSAL_CPC -> "CPC recursal • recursos e contrarrazões com calendário próprio do tribunal.";
            case PRAZO_EMBARGOS -> "Embargos • janela curta e controle reforçado de termo inicial e termo final.";
            case PRAZO_JUIZADO -> "Juizados • microssistema com contagem e recorribilidade próprias.";
            case PRAZO_TRABALHISTA_GERAL -> "CLT • trilha trabalhista ordinária, audiência e execução com regime próprio.";
            case PRAZO_TRABALHISTA_RECURSAL -> "CLT recursal • recursos trabalhistas com prazos curtos e rito especializado.";
            case PRAZO_ELEITORAL -> "Eleitoral • trilha contínua e peremptória nos períodos críticos do calendário eleitoral.";
            case PRAZO_PENAL -> context != null && context.ramo == RamoDireito.MILITAR
                    ? "Penal militar • contagem e intimação com regime castrense próprio."
                    : "Penal • resposta, alegações e recursos com marcos próprios de intimação.";
            default -> "Prazo processual com trilha segmentada por ramo, rito e tipo de ato.";
        };
    }

    private String resolveAudienceCode(Usuario usuario,
                                      String profileCode,
                                      String activeInstitutionContextCode,
                                      Segment segment) {
        String profile = profileCode == null || profileCode.isBlank() ? "USER" : profileCode.trim().toUpperCase(Locale.ROOT);
        String context = activeInstitutionContextCode == null || activeInstitutionContextCode.isBlank() ? "GERAL" : activeInstitutionContextCode.trim().toUpperCase(Locale.ROOT);
        if (usuario != null && usuario.getTipoUsuario() != null) {
            if (segment == Segment.AGENDA_MANDADOS && (usuario.getTipoUsuario() == com.tcc.pjb.backend.model.entity.enums.TipoUsuario.OFICIAL_JUSTICA || usuario.getTipoUsuario() == com.tcc.pjb.backend.model.entity.enums.TipoUsuario.OFICIAL_JUSTICA_AVALIADOR)) {
                return "OFICIAL_JUSTICA";
            }
            if (segment == Segment.AGENDA_PERICIAS && usuario.getTipoUsuario().isPerito()) {
                return "PERITO";
            }
        }
        return profile + ":" + context;
    }

    private String titleForEventType(String eventType) {
        if (eventType == null) {
            return "Evento do calendário";
        }
        return switch (eventType) {
            case "AUDIENCIA", "AUDIENCIA_PROCESSUAL", "AUDIENCIA_RECURSO_SECRETARIA", "AUDIENCIA_PRESENCA_SECRETARIA" -> "Agenda processual";
            case "JULGAMENTO", "PAUTA_COLEGIADA", "PAUTA_SUSTENTACAO" -> "Sessão colegiada";
            case "MANDADO_DILIGENCIA", "MANDADO_ROTA", "MANDADO_TENTATIVA", "MANDADO_RETORNO", "MANDADO_CERTIDAO", "MANDADO_MULTI_TENTATIVA", "MANDADO_JANELA_RETORNO" -> "Mandado e diligência";
            case "PERICIA_OPERACIONAL", "PERICIA_NOMEACAO", "PERICIA_ACEITE", "PERICIA_HONORARIOS", "PERICIA_LAUDO", "PERICIA_LAUDO_PENDENTE", "PERICIA_ENTREGA_TECNICA" -> "Perícia";
            case "SECRETARIA_OPERACIONAL", "SECRETARIA_AUDIENCIA", "SECRETARIA_FILA_AUDIENCIA", "SECRETARIA_PAUTA_INTERNA", "SECRETARIA_SLA" -> "Secretaria e cartório";
            case "GABINETE_DECISORIO", "GABINETE_CONCLUSAO", "GABINETE_MINUTA", "GABINETE_VOTO", "GABINETE_PAUTA" -> "Gabinete";
            case "PRAZO_INSTITUCIONAL", "PRAZO_RECURSAL_OPERACIONAL", "PRAZO_EMBARGOS_OPERACIONAL" -> "Prazo institucional";
            case "PRECATORIO_OPERACIONAL", "PRECATORIO_RPV_OPERACIONAL", "PRECATORIO_LIBERACAO_OPERACIONAL" -> "Precatório e RPV";
            case "CUSTOM" -> "Lembrete pessoal";
            default -> "Evento do fluxo";
        };
    }

    private String normalize(String... values) {
        StringBuilder builder = new StringBuilder();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isBlank()) {
                    if (!builder.isEmpty()) {
                        builder.append(' ');
                    }
                    builder.append(value.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        return builder.toString();
    }

    private boolean containsAny(String raw, String... tokens) {
        if (raw == null || raw.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && raw.contains(token.toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String normalizeColor(String color, String fallback) {
        if (color == null || color.isBlank()) {
            return fallback;
        }
        String normalized = color.trim().toUpperCase(Locale.ROOT);
        return normalized.length() > 16 ? normalized.substring(0, 16) : normalized;
    }

    private String normalizeRito(RitoProcessual rito) {
        String[] parts = rito.name().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            builder.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return builder.toString();
    }

    private record ProcessoContext(
            RamoDireito ramo,
            RitoProcessual rito,
            String tribunal,
            String uf,
            Long equipeId
    ) {
    }

    private record SegmentCounter(
            String code,
            String title,
            int totalEvents
    ) {
        SegmentCounter merge(SegmentCounter other) {
            return new SegmentCounter(code, title, totalEvents + other.totalEvents);
        }

        CalendarWorkspaceResponse.CalendarSegmentDto toDto() {
            return new CalendarWorkspaceResponse.CalendarSegmentDto(code, title, totalEvents);
        }
    }

    private static final class WorkspaceLaneAccumulator {
        private final Map<LocalDate, List<CalendarWorkspaceEventDto>> days = new LinkedHashMap<>();
        private final Map<String, SegmentCounter> segmentCounts = new LinkedHashMap<>();
    }

    private enum Lane {
        PRAZOS("PRAZOS", "Prazos", "AMBER"),
        PRECATORIOS("PRECATORIOS", "Precatórios e RPV", "PURPLE"),
        AGENDA_PROCESSUAL("AGENDA_PROCESSUAL", "Agenda processual", "BLUE"),
        PESSOAL("PESSOAL", "Pessoal", "GREEN");

        private final String code;
        private final String title;
        private final String accentColor;

        Lane(String code, String title, String accentColor) {
            this.code = code;
            this.title = title;
            this.accentColor = accentColor;
        }
    }

    private enum Segment {
        PRAZO_CPC_GERAL("PRAZO_CPC_GERAL", "Prazos cíveis e fazendários"),
        PRAZO_RECURSAL_CPC("PRAZO_RECURSAL_CPC", "Recursos do CPC"),
        PRAZO_EMBARGOS("PRAZO_EMBARGOS", "Embargos e retificação"),
        PRAZO_JUIZADO("PRAZO_JUIZADO", "Juizados especiais"),
        PRAZO_TRABALHISTA_GERAL("PRAZO_TRABALHISTA_GERAL", "Trabalho e execução"),
        PRAZO_TRABALHISTA_RECURSAL("PRAZO_TRABALHISTA_RECURSAL", "Recursos trabalhistas"),
        PRAZO_ELEITORAL("PRAZO_ELEITORAL", "Calendário eleitoral"),
        PRAZO_PENAL("PRAZO_PENAL", "Penal e militar"),
        PRECATORIO_ORDEM("PRECATORIO_ORDEM", "Ordem cronológica"),
        PRECATORIO_RPV("PRECATORIO_RPV", "RPV e pequeno valor"),
        PRECATORIO_LIBERACAO("PRECATORIO_LIBERACAO", "Alvará e liberação"),
        AGENDA_AUDIENCIAS("AGENDA_AUDIENCIAS", "Audiências e presença"),
        AGENDA_JULGAMENTOS("AGENDA_JULGAMENTOS", "Julgamentos e sessões"),
        AGENDA_MANDADOS("AGENDA_MANDADOS", "Mandados e diligências"),
        AGENDA_PERICIAS("AGENDA_PERICIAS", "Perícias e janelas técnicas"),
        AGENDA_SECRETARIA("AGENDA_SECRETARIA", "Secretaria e cartório"),
        AGENDA_GABINETE("AGENDA_GABINETE", "Gabinete e apoio decisório"),
        AGENDA_OPERACIONAL("AGENDA_OPERACIONAL", "Agenda institucional"),
        PESSOAL_GERAL("PESSOAL_GERAL", "Lembretes pessoais");

        private final String code;
        private final String title;

        Segment(String code, String title) {
            this.code = code;
            this.title = title;
        }
    }
}
