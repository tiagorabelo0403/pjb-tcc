package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalBridgeResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarInstitutionalFocusResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CalendarInstitutionalBridgeService {

    private final CurrentUserService currentUserService;
    private final UserCalendarWorkspaceService workspaceService;
    private final UserCalendarPreferenceService preferenceService;
    private final CalendarInstitutionalScopeService scopeService;
    private final CalendarInstitutionalContextService contextService;
    private final CalendarEventAttentionPolicyService attentionPolicyService;

    public CalendarInstitutionalBridgeService(CurrentUserService currentUserService,
                                              UserCalendarWorkspaceService workspaceService,
                                              UserCalendarPreferenceService preferenceService,
                                              CalendarInstitutionalScopeService scopeService,
                                              CalendarInstitutionalContextService contextService,
                                              CalendarEventAttentionPolicyService attentionPolicyService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.workspaceService = Objects.requireNonNull(workspaceService);
        this.preferenceService = Objects.requireNonNull(preferenceService);
        this.scopeService = Objects.requireNonNull(scopeService);
        this.contextService = Objects.requireNonNull(contextService);
        this.attentionPolicyService = Objects.requireNonNull(attentionPolicyService);
    }

    @Transactional(readOnly = true)
    public CalendarInstitutionalBridgeResponse bridge(LocalDate from, LocalDate to, Long processoId) {
        return bridgeForUser(currentUserService.getRequired(), from, to, processoId);
    }

    @Transactional(readOnly = true)
    public CalendarInstitutionalBridgeResponse bridgeForUser(Usuario usuario, LocalDate from, LocalDate to, Long processoId) {
        UserCalendarPreferenceResponse preference = preferenceService.currentOrDefault(usuario);
        CalendarWorkspaceResponse workspace = workspaceService.workspaceForUser(usuario, from, to, processoId);
        List<CalendarInstitutionalScopeService.ScopeOption> scopes = scopeService.availableScopes(usuario, preference.includePersonalCalendar(), preference.includeInstitutionalCalendar(), processoId);
        String activeScopeCode = scopeService.normalizeActiveScope(preference.selectedScopeCode(), scopes, preference.includePersonalCalendar(), preference.includeInstitutionalCalendar());
        Long selectedTeamId = preference.selectedTeamId() != null ? preference.selectedTeamId() : scopeService.parseTeamId(activeScopeCode);
        List<CalendarInstitutionalContextService.InstitutionalContextOption> contexts = contextService.availableContexts(usuario, activeScopeCode, processoId, selectedTeamId);
        String normalizedActiveContextCode = contextService.normalizeActiveContext(preference.selectedInstitutionContextCode(), contexts, activeScopeCode);
        String activeContextCode = firstNonBlank(
                normalizedActiveContextCode,
                preference.selectedInstitutionContextCode(),
                contexts.stream().map(CalendarInstitutionalContextService.InstitutionalContextOption::contextCode).filter(Objects::nonNull).findFirst().orElse(null),
                activeScopeCode
        );
        String contextTitle = contexts.stream()
                .filter(item -> Objects.equals(item.contextCode(), activeContextCode))
                .map(CalendarInstitutionalContextService.InstitutionalContextOption::contextTitle)
                .findFirst()
                .orElse("Calendário institucional");
        String unitLabel = resolveUnitLabel(scopes, contexts, activeScopeCode, activeContextCode);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LinkedHashMap<CardKey, CardAccumulator> grouped = new LinkedHashMap<>();
        for (CalendarWorkspaceResponse.CalendarLaneDto lane : workspace.lanes()) {
            if (!lane.visible()) {
                continue;
            }
            for (CalendarWorkspaceResponse.CalendarDayDto day : lane.days()) {
                for (CalendarWorkspaceEventDto event : day.events()) {
                    String windowCode = resolveWindowCode(event.at(), now);
                    String windowLabel = resolveWindowLabel(windowCode);
                    Priority priority = resolvePriority(event.color(), event.at(), now);
                    CalendarEventAttentionPolicyService.AttentionDescriptor attention = attentionPolicyService.describe(event, now);
                    CardKey key = new CardKey(lane.laneCode(), event.segmentCode());
                    grouped.computeIfAbsent(key, ignored -> new CardAccumulator(
                            unitLabel,
                            activeContextCode,
                            contextTitle,
                            windowCode,
                            windowLabel,
                            priority,
                            lane.laneCode(),
                            safeText(lane.laneTitle(), lane.laneCode()),
                            event.segmentCode(),
                            safeText(event.segmentTitle(), lane.laneTitle()),
                            dominantColor(lane.accentColor(), event.color())
                    )).add(event, attention, now);
                }
            }
        }
        List<CalendarInstitutionalBridgeResponse.CardDto> cards = grouped.values().stream()
                .map(CardAccumulator::toDto)
                .sorted(Comparator
                        .comparingInt(CalendarInstitutionalBridgeResponse.CardDto::attentionScore).reversed()
                        .thenComparing((CalendarInstitutionalBridgeResponse.CardDto item) -> priorityRank(item.priorityCode()))
                        .thenComparing(item -> item.nextAt() == null ? LocalDateTime.MAX : item.nextAt())
                        .thenComparing((CalendarInstitutionalBridgeResponse.CardDto item) -> -item.totalEvents())
                        .thenComparing(CalendarInstitutionalBridgeResponse.CardDto::title))
                .limit(12)
                .toList();
        int totalEvents = cards.stream().mapToInt(CalendarInstitutionalBridgeResponse.CardDto::totalEvents).sum();
        int criticalEvents = cards.stream().mapToInt(CalendarInstitutionalBridgeResponse.CardDto::criticalEvents).sum();
        int overdueEvents = cards.stream().mapToInt(CalendarInstitutionalBridgeResponse.CardDto::overdueEvents).sum();
        int processCount = cards.stream().mapToInt(CalendarInstitutionalBridgeResponse.CardDto::processCount).sum();
        return new CalendarInstitutionalBridgeResponse(
                Instant.now(),
                usuario == null ? null : usuario.getId(),
                workspace.profile().profileCode(),
                activeScopeCode,
                activeContextCode,
                unitLabel,
                new CalendarInstitutionalBridgeResponse.Summary(cards.size(), totalEvents, criticalEvents, overdueEvents, processCount),
                cards
        );
    }

    public Map<String, Object> toPanelMap(CalendarInstitutionalBridgeResponse response) {
        if (response == null) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfNotNull(out, "generatedAt", response.generatedAt());
        putIfNotNull(out, "usuarioId", response.usuarioId());
        putIfNotNull(out, "profileCode", response.profileCode());
        putIfNotNull(out, "activeScopeCode", response.activeScopeCode());
        putIfNotNull(out, "activeInstitutionContextCode", response.activeInstitutionContextCode());
        putIfNotNull(out, "unitLabel", response.unitLabel());
        if (response.summary() != null) {
            out.put("summary", Map.of(
                    "totalCards", response.summary().totalCards(),
                    "totalEvents", response.summary().totalEvents(),
                    "criticalEvents", response.summary().criticalEvents(),
                    "overdueEvents", response.summary().overdueEvents(),
                    "processCount", response.summary().processCount()
            ));
        }
        out.put("cards", response.cards() == null ? List.of() : response.cards());
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }


    public CalendarInstitutionalFocusResponse focus(CalendarInstitutionalBridgeResponse response) {
        if (response == null) {
            return new CalendarInstitutionalFocusResponse(Instant.now(), null, null, null, List.of(), List.of(), List.of(), List.of());
        }
        List<CalendarInstitutionalBridgeResponse.CardDto> cards = response.cards() == null ? List.of() : response.cards();
        LinkedHashMap<String, FocusSliceAccumulator> slices = new LinkedHashMap<>();
        LinkedHashMap<String, WindowAccumulator> windows = new LinkedHashMap<>();
        LinkedHashMap<String, PriorityAccumulator> priorities = new LinkedHashMap<>();
        for (CalendarInstitutionalBridgeResponse.CardDto card : cards) {
            FocusSlice slice = resolveFocusSlice(card);
            slices.computeIfAbsent(slice.code(), ignored -> new FocusSliceAccumulator(slice)).add(card);
            windows.computeIfAbsent(card.windowCode(), ignored -> new WindowAccumulator(card.windowCode(), card.windowLabel())).add(card);
            priorities.computeIfAbsent(card.priorityCode(), ignored -> new PriorityAccumulator(card.priorityCode(), card.priorityLabel())).add(card);
        }
        List<CalendarInstitutionalFocusResponse.FocusSliceDto> focusSlices = slices.values().stream()
                .map(FocusSliceAccumulator::toDto)
                .sorted(Comparator
                        .comparingInt(CalendarInstitutionalFocusResponse.FocusSliceDto::attentionScore).reversed()
                        .thenComparing((CalendarInstitutionalFocusResponse.FocusSliceDto item) -> priorityRankForSlice(item.color(), item.overdueEvents(), item.criticalEvents()))
                        .thenComparing(item -> item.nextAt() == null ? LocalDateTime.MAX : item.nextAt())
                        .thenComparing((CalendarInstitutionalFocusResponse.FocusSliceDto item) -> -item.totalEvents())
                        .thenComparing(CalendarInstitutionalFocusResponse.FocusSliceDto::sliceTitle))
                .toList();
        List<CalendarInstitutionalFocusResponse.WindowBucketDto> windowBuckets = windows.values().stream()
                .map(WindowAccumulator::toDto)
                .sorted(Comparator.comparing((CalendarInstitutionalFocusResponse.WindowBucketDto item) -> windowRank(item.windowCode())))
                .toList();
        List<CalendarInstitutionalFocusResponse.PriorityBucketDto> priorityBuckets = priorities.values().stream()
                .map(PriorityAccumulator::toDto)
                .sorted(Comparator.comparing((CalendarInstitutionalFocusResponse.PriorityBucketDto item) -> priorityRank(item.priorityCode())))
                .toList();
        List<CalendarInstitutionalFocusResponse.MilestoneDto> milestones = cards.stream()
                .filter(card -> card.nextAt() != null)
                .sorted(Comparator
                        .comparingInt(CalendarInstitutionalBridgeResponse.CardDto::attentionScore).reversed()
                        .thenComparing(CalendarInstitutionalBridgeResponse.CardDto::nextAt)
                        .thenComparing(CalendarInstitutionalBridgeResponse.CardDto::title))
                .limit(8)
                .map(card -> new CalendarInstitutionalFocusResponse.MilestoneDto(
                        card.cardKey(),
                        card.title(),
                        card.subtitle(),
                        card.nextAt(),
                        card.color(),
                        card.detailsUrl(),
                        card.presentationCode(),
                        card.iconCode(),
                        card.attentionScore()
                ))
                .toList();
        return new CalendarInstitutionalFocusResponse(
                response.generatedAt(),
                response.profileCode(),
                response.activeScopeCode(),
                response.activeInstitutionContextCode(),
                focusSlices,
                windowBuckets,
                priorityBuckets,
                milestones
        );
    }

    public Map<String, Object> toFocusPanelMap(CalendarInstitutionalFocusResponse response) {
        if (response == null) {
            return Map.of();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        putIfNotNull(out, "generatedAt", response.generatedAt());
        putIfNotNull(out, "profileCode", response.profileCode());
        putIfNotNull(out, "activeScopeCode", response.activeScopeCode());
        putIfNotNull(out, "activeInstitutionContextCode", response.activeInstitutionContextCode());
        out.put("focusSlices", response.focusSlices() == null ? List.of() : response.focusSlices());
        out.put("windows", response.windows() == null ? List.of() : response.windows());
        out.put("priorities", response.priorities() == null ? List.of() : response.priorities());
        out.put("milestones", response.milestones() == null ? List.of() : response.milestones());
        return out.isEmpty() ? Map.of() : Collections.unmodifiableMap(out);
    }

    private static FocusSlice resolveFocusSlice(CalendarInstitutionalBridgeResponse.CardDto card) {
        String laneCode = normalize(card.laneCode());
        String segmentCode = normalize(card.segmentCode());
        if (containsAny(segmentCode, "MANDADO", "DILIGENCIA") || containsAny(card.contextCode(), "CENTRAL_MANDADOS")) {
            return new FocusSlice("MANDADOS", "Central de mandados", dominantColor("BLUE", card.color()));
        }
        if (containsAny(segmentCode, "SECRETARIA", "CARTORIO") || containsAny(card.contextCode(), "SECRETARIA_UNIDADE")) {
            return new FocusSlice("SECRETARIA", "Secretaria e cartório", dominantColor("AMBER", card.color()));
        }
        if (containsAny(segmentCode, "GABINETE", "PAUTA") || containsAny(card.contextCode(), "GABINETE", "ORGAO_JULGADOR", "ASSESSORIA_GABINETE", "PAUTA_COLEGIADO")) {
            return new FocusSlice("GABINETE", "Gabinete e colegiado", dominantColor("PURPLE", card.color()));
        }
        if (containsAny(segmentCode, "PERICIA") || containsAny(card.contextCode(), "NUCLEO_PERICIAL")) {
            return new FocusSlice("PERICIA", "Núcleo pericial", dominantColor("GREEN", card.color()));
        }
        if (containsAny(segmentCode, "PRECATORIO", "RPV") || containsAny(laneCode, "PRECATORIOS")) {
            return new FocusSlice("PRECATORIOS", "Precatórios e RPV", dominantColor("GREEN", card.color()));
        }
        if (containsAny(segmentCode, "PRAZO", "RECURSAL", "EMBARGOS") || containsAny(laneCode, "PRAZOS")) {
            return new FocusSlice("PRAZOS", "Prazos processuais", dominantColor("RED", card.color()));
        }
        if (containsAny(laneCode, "AGENDA_PROCESSUAL")) {
            return new FocusSlice("AGENDA_PROCESSUAL", "Agenda processual", dominantColor("BLUE", card.color()));
        }
        if (containsAny(laneCode, "PESSOAL")) {
            return new FocusSlice("PESSOAL", "Agenda pessoal", dominantColor("BLUE", card.color()));
        }
        return new FocusSlice("OPERACIONAL", "Operação institucional", dominantColor("BLUE", card.color()));
    }

    private static List<FocusDetail> resolveFocusDetails(String sliceCode, CalendarInstitutionalBridgeResponse.CardDto card) {
        if (!"MANDADOS".equals(normalize(sliceCode))) {
            return List.of(resolveFocusDetail(sliceCode, card));
        }
        LinkedHashMap<String, FocusDetail> details = new LinkedHashMap<>();
        String source = normalize(String.join(" ", card.highlights() == null ? List.<String>of() : card.highlights()));
        if (containsAny(card.segmentCode(), "TENTATIVA") || containsAny(card.presentationCode(), "TENTATIVA") || containsAny(card.detailCode(), "TENTATIVA") || containsAny(source, "TENTATIVA")) {
            details.put("TENTATIVAS", new FocusDetail("TENTATIVAS", "Tentativas de diligência", dominantColor("AMBER", card.color())));
        }
        if (containsAny(card.segmentCode(), "RETORNO") || containsAny(card.presentationCode(), "RETORNO") || containsAny(card.detailCode(), "RETORNO") || containsAny(source, "RETORNO")) {
            details.put("RETORNO", new FocusDetail("RETORNO", "Retorno e reexpedição", dominantColor("RED", card.color())));
        }
        if (containsAny(card.segmentCode(), "CERTIDAO", "CERTIDÃO") || containsAny(card.presentationCode(), "CERTIDAO", "CERTIDÃO") || containsAny(card.detailCode(), "CERTIDAO", "CERTIDÃO") || containsAny(source, "CERTIDAO", "CERTIDÃO")) {
            details.put("CERTIDAO", new FocusDetail("CERTIDAO", "Certidões", dominantColor("GREEN", card.color())));
        }
        if (containsAny(card.segmentCode(), "ROTA") || containsAny(card.presentationCode(), "ROTA") || containsAny(card.detailCode(), "ROTA") || containsAny(source, "ROTA")) {
            details.put("ROTA", new FocusDetail("ROTA", "Rota e deslocamento", dominantColor("BLUE", card.color())));
        }
        return details.isEmpty() ? List.of(resolveFocusDetail(sliceCode, card)) : List.copyOf(details.values());
    }

    private static int detailWeight(String detailCode, CalendarInstitutionalBridgeResponse.CardDto card) {
        if (card == null || card.totalEvents() <= 1) {
            return Math.max(1, card == null ? 1 : card.totalEvents());
        }
        String normalizedDetail = normalize(detailCode);
        int hits = 0;
        for (String highlight : card.highlights() == null ? List.<String>of() : card.highlights()) {
            String normalizedHighlight = normalize(highlight);
            if ("TENTATIVAS".equals(normalizedDetail) && containsAny(normalizedHighlight, "TENTATIVA")) {
                hits++;
            } else if ("RETORNO".equals(normalizedDetail) && containsAny(normalizedHighlight, "RETORNO")) {
                hits++;
            } else if ("CERTIDAO".equals(normalizedDetail) && containsAny(normalizedHighlight, "CERTIDAO", "CERTIDÃO")) {
                hits++;
            } else if ("ROTA".equals(normalizedDetail) && containsAny(normalizedHighlight, "ROTA")) {
                hits++;
            }
        }
        return Math.max(1, hits);
    }

    private static FocusDetail resolveFocusDetail(String sliceCode, CalendarInstitutionalBridgeResponse.CardDto card) {
        String segmentCode = normalize(card.segmentCode());
        String laneCode = normalize(card.laneCode());
        String color = card.color();
        return switch (normalize(sliceCode)) {
            case "MANDADOS" -> {
                if (containsAny(segmentCode, "ROTA")) {
                    yield new FocusDetail("ROTA", "Rota e deslocamento", dominantColor("BLUE", color));
                }
                if (containsAny(segmentCode, "TENTATIVA")) {
                    yield new FocusDetail("TENTATIVAS", "Tentativas de diligência", dominantColor("AMBER", color));
                }
                if (containsAny(segmentCode, "CERTIDAO", "CERTIDÃO")) {
                    yield new FocusDetail("CERTIDAO", "Certidões", dominantColor("GREEN", color));
                }
                if (containsAny(segmentCode, "RETORNO")) {
                    yield new FocusDetail("RETORNO", "Retorno e reexpedição", dominantColor("RED", color));
                }
                yield new FocusDetail("OPERACAO_MANDADO", "Operação de mandados", dominantColor("BLUE", color));
            }
            case "SECRETARIA" -> {
                if (containsAny(segmentCode, "AUDIENCIA", "AUDIÊNCIA", "FILA_AUDIENCIA")) {
                    yield new FocusDetail("AUDIENCIA", "Audiência e fila", dominantColor("RED", color));
                }
                if (containsAny(segmentCode, "PAUTA_INTERNA", "PAUTA")) {
                    yield new FocusDetail("PAUTA_INTERNA", "Pauta interna", dominantColor("AMBER", color));
                }
                if (containsAny(segmentCode, "SLA")) {
                    yield new FocusDetail("SLA", "SLA e expediente", dominantColor("AMBER", color));
                }
                yield new FocusDetail("OPERACIONAL_SECRETARIA", "Operação cartorária", dominantColor("BLUE", color));
            }
            case "GABINETE" -> {
                if (containsAny(segmentCode, "CONCLUSAO", "CONCLUSÃO")) {
                    yield new FocusDetail("CONCLUSAO", "Conclusões", dominantColor("BLUE", color));
                }
                if (containsAny(segmentCode, "MINUTA")) {
                    yield new FocusDetail("MINUTA", "Minutas", dominantColor("PURPLE", color));
                }
                if (containsAny(segmentCode, "VOTO")) {
                    yield new FocusDetail("VOTO", "Votos", dominantColor("PURPLE", color));
                }
                if (containsAny(segmentCode, "PAUTA", "SUSTENTACAO", "SUSTENTAÇÃO") || containsAny(laneCode, "AGENDA_PROCESSUAL")) {
                    yield new FocusDetail("PAUTA", "Pauta e sessão", dominantColor("AMBER", color));
                }
                yield new FocusDetail("OPERACIONAL_GABINETE", "Operação de gabinete", dominantColor("PURPLE", color));
            }
            case "PERICIA" -> {
                if (containsAny(segmentCode, "ACEITE")) {
                    yield new FocusDetail("ACEITE", "Aceites", dominantColor("BLUE", color));
                }
                if (containsAny(segmentCode, "HONORARIOS", "HONORÁRIOS")) {
                    yield new FocusDetail("HONORARIOS", "Honorários", dominantColor("AMBER", color));
                }
                if (containsAny(segmentCode, "LAUDO_PENDENTE")) {
                    yield new FocusDetail("LAUDO_PENDENTE", "Laudos pendentes", dominantColor("RED", color));
                }
                if (containsAny(segmentCode, "ENTREGA_TECNICA", "ENTREGA_TÉCNICA", "LAUDO")) {
                    yield new FocusDetail("ENTREGA_TECNICA", "Entrega e laudo", dominantColor("GREEN", color));
                }
                yield new FocusDetail("OPERACIONAL_PERICIA", "Operação pericial", dominantColor("GREEN", color));
            }
            case "PRECATORIOS" -> {
                if (containsAny(segmentCode, "RPV")) {
                    yield new FocusDetail("RPV", "RPV", dominantColor("GREEN", color));
                }
                if (containsAny(segmentCode, "LIBERACAO", "LIBERAÇÃO")) {
                    yield new FocusDetail("LIBERACAO", "Liberação", dominantColor("GREEN", color));
                }
                yield new FocusDetail("PRECATORIO", "Precatórios", dominantColor("GREEN", color));
            }
            case "PRAZOS" -> {
                if (containsAny(segmentCode, "EMBARGOS")) {
                    yield new FocusDetail("EMBARGOS", "Embargos", dominantColor("RED", color));
                }
                if (containsAny(segmentCode, "RECURSAL")) {
                    yield new FocusDetail("RECURSAL", "Recursal", dominantColor("RED", color));
                }
                yield new FocusDetail("PRAZO_GERAL", "Prazos gerais", dominantColor("AMBER", color));
            }
            case "AGENDA_PROCESSUAL" -> {
                if (containsAny(segmentCode, "AUDIENCIA", "AUDIÊNCIA")) {
                    yield new FocusDetail("AUDIENCIAS", "Audiências", dominantColor("RED", color));
                }
                if (containsAny(segmentCode, "PAUTA", "SESSAO", "SESSÃO")) {
                    yield new FocusDetail("SESSOES", "Sessões e pauta", dominantColor("PURPLE", color));
                }
                yield new FocusDetail("ATOS", "Atos processuais", dominantColor("BLUE", color));
            }
            case "PESSOAL" -> new FocusDetail("PESSOAL", "Agenda pessoal", dominantColor("BLUE", color));
            default -> new FocusDetail("OPERACIONAL", "Operação institucional", dominantColor("BLUE", color));
        };
    }

    private static boolean containsAny(String value, String... parts) {
        String normalized = normalize(value);
        if (normalized == null) {
            return false;
        }
        for (String part : parts) {
            String probe = normalize(part);
            if (probe != null && normalized.contains(probe)) {
                return true;
            }
        }
        return false;
    }

    private static int priorityRankForSlice(String color, int overdueEvents, int criticalEvents) {
        if (overdueEvents > 0) {
            return -1;
        }
        if (criticalEvents > 0) {
            return 0;
        }
        return priorityRank(resolvePriority(color, null, LocalDateTime.now(ZoneOffset.UTC)).code());
    }

    private static int windowRank(String code) {
        return switch (normalize(code)) {
            case "VENCIDO" -> 0;
            case "HOJE" -> 1;
            case "ATE_24H" -> 2;
            case "ATE_48H" -> 3;
            case "ATE_7D" -> 4;
            default -> 5;
        };
    }

    private String resolveUnitLabel(List<CalendarInstitutionalScopeService.ScopeOption> scopes,
                                    List<CalendarInstitutionalContextService.InstitutionalContextOption> contexts,
                                    String activeScopeCode,
                                    String activeContextCode) {
        String contextLabel = contexts.stream()
                .filter(item -> Objects.equals(item.contextCode(), activeContextCode))
                .map(CalendarInstitutionalContextService.InstitutionalContextOption::contextLabel)
                .findFirst()
                .orElse(null);
        if (contextLabel != null && !contextLabel.isBlank()) {
            return contextLabel;
        }
        return scopes.stream()
                .filter(item -> Objects.equals(item.scopeCode(), activeScopeCode))
                .map(CalendarInstitutionalScopeService.ScopeOption::institutionLabel)
                .findFirst()
                .orElse("Calendário institucional");
    }

    private static String resolveWindowCode(LocalDateTime at, LocalDateTime now) {
        if (at == null) {
            return "FUTURO";
        }
        if (at.isBefore(now)) {
            return "VENCIDO";
        }
        long hours = ChronoUnit.HOURS.between(now, at);
        if (at.toLocalDate().isEqual(now.toLocalDate())) {
            return "HOJE";
        }
        if (hours <= 24) {
            return "ATE_24H";
        }
        if (hours <= 48) {
            return "ATE_48H";
        }
        if (hours <= 24 * 7) {
            return "ATE_7D";
        }
        return "FUTURO";
    }

    private static String resolveWindowLabel(String code) {
        return switch (normalize(code)) {
            case "VENCIDO" -> "Janela vencida";
            case "HOJE" -> "Hoje";
            case "ATE_24H" -> "Próximas 24h";
            case "ATE_48H" -> "Próximas 48h";
            case "ATE_7D" -> "Próximos 7 dias";
            default -> "Janela futura";
        };
    }

    private static Priority resolvePriority(String color, LocalDateTime at, LocalDateTime now) {
        if (at != null && at.isBefore(now)) {
            return new Priority("CRITICA", "Crítica", dominantColor("RED", color));
        }
        return switch (severity(color)) {
            case 5 -> new Priority("CRITICA", "Crítica", dominantColor("RED", color));
            case 4 -> new Priority("ALTA", "Alta", dominantColor("AMBER", color));
            case 3 -> new Priority("MEDIA", "Média", dominantColor("BLUE", color));
            default -> new Priority("NORMAL", "Normal", dominantColor("GREEN", color));
        };
    }

    private static int severity(String color) {
        return switch (normalize(color)) {
            case "RED", "VERMELHO" -> 5;
            case "AMBER", "LARANJA", "ORANGE", "PURPLE", "ROXO" -> 4;
            case "BLUE", "AZUL" -> 3;
            case "GREEN", "VERDE" -> 2;
            default -> 1;
        };
    }

    private static int priorityRank(String code) {
        return switch (normalize(code)) {
            case "CRITICA" -> 0;
            case "ALTA" -> 1;
            case "MEDIA" -> 2;
            default -> 3;
        };
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String dominantColor(String preferred, String fallback) {
        String normalizedFallback = normalize(fallback);
        if (normalizedFallback != null) {
            return normalizedFallback;
        }
        String normalizedPreferred = normalize(preferred);
        return normalizedPreferred == null ? "BLUE" : normalizedPreferred;
    }

    private static String safeText(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary.trim();
        }
        return fallback == null ? "Agenda institucional" : fallback.trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (target != null && key != null && value != null) {
            target.put(key, value);
        }
    }

    private record FocusSlice(String code, String title, String color) {
    }

    private record FocusDetail(String code, String title, String color) {
    }


    private static final class FocusSliceAccumulator {
        private final FocusSlice slice;
        private int totalCards;
        private int totalEvents;
        private int criticalEvents;
        private int overdueEvents;
        private int processCount;
        private LocalDateTime nextAt;
        private final LinkedHashSet<String> highlights = new LinkedHashSet<>();
        private int attentionScore;
        private String presentationCode;
        private String iconCode;
        private final LinkedHashMap<String, FocusDetailAccumulator> details = new LinkedHashMap<>();

        private FocusSliceAccumulator(FocusSlice slice) {
            this.slice = slice;
        }

        private void add(CalendarInstitutionalBridgeResponse.CardDto card) {
            add(card, Math.max(1, card.totalEvents()));
        }

        private void add(CalendarInstitutionalBridgeResponse.CardDto card, int eventWeight) {
            int weight = Math.max(1, eventWeight);
            totalCards++;
            totalEvents += weight;
            criticalEvents += card.criticalEvents() > 0 ? Math.min(weight, card.criticalEvents()) : 0;
            overdueEvents += card.overdueEvents() > 0 ? Math.min(weight, card.overdueEvents()) : 0;
            processCount += card.processCount();
            if (nextAt == null || (card.nextAt() != null && card.nextAt().isBefore(nextAt))) {
                nextAt = card.nextAt();
            }
            if (highlights.size() < 4) {
                highlights.addAll(card.highlights());
            }
            if (card.attentionScore() >= attentionScore) {
                attentionScore = card.attentionScore();
                presentationCode = card.presentationCode();
                iconCode = card.iconCode();
            }
            for (FocusDetail detail : resolveFocusDetails(slice.code(), card)) {
                details.computeIfAbsent(detail.code(), ignored -> new FocusDetailAccumulator(detail)).add(card, detailWeight(detail.code(), card));
            }
        }

        private CalendarInstitutionalFocusResponse.FocusSliceDto toDto() {
            List<CalendarInstitutionalFocusResponse.FocusDetailDto> detailBuckets = details.values().stream()
                    .map(FocusDetailAccumulator::toDto)
                    .sorted(Comparator
                            .comparingInt(CalendarInstitutionalFocusResponse.FocusDetailDto::attentionScore).reversed()
                            .thenComparing((CalendarInstitutionalFocusResponse.FocusDetailDto item) -> priorityRankForSlice(item.color(), item.overdueEvents(), item.criticalEvents()))
                            .thenComparing(item -> item.nextAt() == null ? LocalDateTime.MAX : item.nextAt())
                            .thenComparing((CalendarInstitutionalFocusResponse.FocusDetailDto item) -> -item.totalEvents())
                            .thenComparing(CalendarInstitutionalFocusResponse.FocusDetailDto::detailTitle))
                    .limit(6)
                    .toList();
            return new CalendarInstitutionalFocusResponse.FocusSliceDto(
                    slice.code(),
                    slice.title(),
                    slice.color(),
                    totalCards,
                    totalEvents,
                    criticalEvents,
                    overdueEvents,
                    processCount,
                    nextAt,
                    List.copyOf(highlights),
                    detailBuckets,
                    presentationCode,
                    iconCode,
                    attentionScore
            );
        }
    }

    private static final class FocusDetailAccumulator {
        private final FocusDetail detail;
        private int totalCards;
        private int totalEvents;
        private int criticalEvents;
        private int overdueEvents;
        private int processCount;
        private LocalDateTime nextAt;
        private final LinkedHashSet<String> highlights = new LinkedHashSet<>();
        private int attentionScore;
        private String presentationCode;
        private String iconCode;

        private FocusDetailAccumulator(FocusDetail detail) {
            this.detail = detail;
        }

        private void add(CalendarInstitutionalBridgeResponse.CardDto card) {
            add(card, Math.max(1, card.totalEvents()));
        }

        private void add(CalendarInstitutionalBridgeResponse.CardDto card, int eventWeight) {
            int weight = Math.max(1, eventWeight);
            totalCards++;
            totalEvents += weight;
            criticalEvents += card.criticalEvents() > 0 ? Math.min(weight, card.criticalEvents()) : 0;
            overdueEvents += card.overdueEvents() > 0 ? Math.min(weight, card.overdueEvents()) : 0;
            processCount += card.processCount();
            if (nextAt == null || (card.nextAt() != null && card.nextAt().isBefore(nextAt))) {
                nextAt = card.nextAt();
            }
            if (highlights.size() < 3) {
                highlights.addAll(card.highlights());
            }
            if (card.attentionScore() >= attentionScore) {
                attentionScore = card.attentionScore();
                presentationCode = card.presentationCode();
                iconCode = card.iconCode();
            }
        }

        private CalendarInstitutionalFocusResponse.FocusDetailDto toDto() {
            return new CalendarInstitutionalFocusResponse.FocusDetailDto(
                    detail.code(),
                    detail.title(),
                    detail.color(),
                    totalCards,
                    totalEvents,
                    criticalEvents,
                    overdueEvents,
                    processCount,
                    nextAt,
                    List.copyOf(highlights),
                    presentationCode,
                    iconCode,
                    attentionScore
            );
        }
    }

    private static final class WindowAccumulator {

        private final String windowCode;
        private final String windowLabel;
        private int totalCards;
        private int totalEvents;
        private int criticalEvents;
        private int overdueEvents;

        private WindowAccumulator(String windowCode, String windowLabel) {
            this.windowCode = windowCode;
            this.windowLabel = windowLabel;
        }

        private void add(CalendarInstitutionalBridgeResponse.CardDto card) {
            totalCards++;
            totalEvents += card.totalEvents();
            criticalEvents += card.criticalEvents();
            overdueEvents += card.overdueEvents();
        }

        private CalendarInstitutionalFocusResponse.WindowBucketDto toDto() {
            return new CalendarInstitutionalFocusResponse.WindowBucketDto(windowCode, windowLabel, totalCards, totalEvents, criticalEvents, overdueEvents);
        }
    }

    private static final class PriorityAccumulator {
        private final String priorityCode;
        private final String priorityLabel;
        private int totalCards;
        private int totalEvents;

        private PriorityAccumulator(String priorityCode, String priorityLabel) {
            this.priorityCode = priorityCode;
            this.priorityLabel = priorityLabel;
        }

        private void add(CalendarInstitutionalBridgeResponse.CardDto card) {
            totalCards++;
            totalEvents += card.totalEvents();
        }

        private CalendarInstitutionalFocusResponse.PriorityBucketDto toDto() {
            return new CalendarInstitutionalFocusResponse.PriorityBucketDto(priorityCode, priorityLabel, totalCards, totalEvents);
        }
    }

    private static final class CardAccumulator {
        private final String unitLabel;
        private final String contextCode;
        private final String contextTitle;
        private final String windowCode;
        private final String windowLabel;
        private Priority priority;
        private final String laneCode;
        private final String laneTitle;
        private final String segmentCode;
        private final String segmentTitle;
        private String color;
        private int totalEvents;
        private int criticalEvents;
        private int overdueEvents;
        private final LinkedHashSet<Long> processIds = new LinkedHashSet<>();
        private LocalDateTime nextAt;
        private String detailsUrl;
        private final List<String> highlights = new ArrayList<>();
        private int attentionScore;
        private String presentationCode;
        private String presentationTitle;
        private String detailCode;
        private String detailTitle;
        private String iconCode;

        private CardAccumulator(String unitLabel,
                                String contextCode,
                                String contextTitle,
                                String windowCode,
                                String windowLabel,
                                Priority priority,
                                String laneCode,
                                String laneTitle,
                                String segmentCode,
                                String segmentTitle,
                                String color) {
            this.unitLabel = unitLabel;
            this.contextCode = contextCode;
            this.contextTitle = contextTitle;
            this.windowCode = windowCode;
            this.windowLabel = windowLabel;
            this.priority = priority;
            this.laneCode = laneCode;
            this.laneTitle = laneTitle;
            this.segmentCode = segmentCode;
            this.segmentTitle = segmentTitle;
            this.color = color;
        }

        private void add(CalendarWorkspaceEventDto event, CalendarEventAttentionPolicyService.AttentionDescriptor attention, LocalDateTime now) {
            boolean becomesNext = nextAt == null || (event.at() != null && event.at().isBefore(nextAt));
            totalEvents++;
            Priority eventPriority = resolvePriority(event.color(), event.at(), now);
            if (priorityRank(eventPriority.code()) < priorityRank(priority.code())) {
                priority = eventPriority;
            }
            if ("CRITICA".equals(eventPriority.code()) || "ALTA".equals(eventPriority.code())) {
                criticalEvents++;
            }
            if (event.at() != null && event.at().isBefore(now)) {
                overdueEvents++;
            }
            if (event.processoId() != null) {
                processIds.add(event.processoId());
            }
            if (becomesNext) {
                nextAt = event.at();
                if (event.detailsUrl() != null && !event.detailsUrl().isBlank()) {
                    detailsUrl = event.detailsUrl();
                }
            }
            if (detailsUrl == null && event.detailsUrl() != null && !event.detailsUrl().isBlank()) {
                detailsUrl = event.detailsUrl();
            }
            if (severity(event.color()) > severity(color)) {
                color = event.color();
            }
            if (attention != null) {
                if (presentationCode == null || becomesNext) {
                    presentationCode = attention.presentationCode();
                    presentationTitle = attention.presentationTitle();
                    detailCode = attention.detailCode();
                    detailTitle = attention.detailTitle();
                    iconCode = attention.iconCode();
                }
                attentionScore = Math.max(attentionScore, attention.attentionScore());
            }
            if (highlights.size() < 3) {
                String processoNumero = event.processoNumero() == null || event.processoNumero().isBlank() ? "sem número" : event.processoNumero();
                highlights.add(processoNumero + " • " + safeText(event.title(), segmentTitle));
            }
        }

        private CalendarInstitutionalBridgeResponse.CardDto toDto() {
            return new CalendarInstitutionalBridgeResponse.CardDto(
                    String.join(":", normalize(contextCode), normalize(laneCode), normalize(segmentCode), normalize(windowCode), normalize(priority.code())),
                    unitLabel,
                    contextCode,
                    contextTitle,
                    windowCode,
                    windowLabel,
                    priority.code(),
                    priority.label(),
                    laneCode,
                    laneTitle,
                    segmentCode,
                    segmentTitle,
                    segmentTitle + " • " + windowLabel,
                    contextTitle + " • " + laneTitle,
                    dominantColor(priority.color(), color),
                    totalEvents,
                    criticalEvents,
                    overdueEvents,
                    processIds.size(),
                    nextAt,
                    detailsUrl,
                    List.copyOf(highlights),
                    presentationCode,
                    presentationTitle,
                    detailCode,
                    detailTitle,
                    iconCode,
                    attentionScore
            );
        }
    }

    private record CardKey(String laneCode, String segmentCode) {
    }

    private record Priority(String code, String label, String color) {
    }
}
