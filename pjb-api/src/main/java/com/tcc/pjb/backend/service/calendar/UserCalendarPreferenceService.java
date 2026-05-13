package com.tcc.pjb.backend.service.calendar;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceRequest;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarPreference;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarPreferenceRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCalendarPreferenceService {

    private final CurrentUserService currentUserService;
    private final UserCalendarPreferenceRepository repository;
    private final CalendarAudienceProfileService audienceProfileService;
    private final CalendarInstitutionalScopeService scopeService;
    private final CalendarInstitutionalContextService contextService;

    public UserCalendarPreferenceService(CurrentUserService currentUserService,
                                         UserCalendarPreferenceRepository repository,
                                         CalendarAudienceProfileService audienceProfileService,
                                         CalendarInstitutionalScopeService scopeService,
                                         CalendarInstitutionalContextService contextService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.repository = Objects.requireNonNull(repository);
        this.audienceProfileService = Objects.requireNonNull(audienceProfileService);
        this.scopeService = Objects.requireNonNull(scopeService);
        this.contextService = Objects.requireNonNull(contextService);
    }

    @Transactional(readOnly = true)
    public UserCalendarPreferenceResponse current() {
        Usuario usuario = currentUserService.getRequired();
        UserCalendarPreference preference = repository.findByUsuarioId(usuario.getId()).orElseGet(() -> defaultEntity(usuario));
        return toResponse(usuario, preference, audienceProfileService.resolve(usuario));
    }

    @Transactional(readOnly = true)
    public UserCalendarPreferenceResponse currentOrDefault(Usuario usuario) {
        UserCalendarPreference preference = repository.findByUsuarioId(usuario.getId()).orElseGet(() -> defaultEntity(usuario));
        return toResponse(usuario, preference, audienceProfileService.resolve(usuario));
    }

    @Transactional
    public UserCalendarPreferenceResponse save(UserCalendarPreferenceRequest request) {
        Usuario usuario = currentUserService.getRequired();
        CalendarAudienceProfileService.CalendarProfile profile = audienceProfileService.resolve(usuario);
        UserCalendarPreference entity = repository.findByUsuarioId(usuario.getId()).orElseGet(() -> defaultEntity(usuario));
        Instant now = Instant.now();
        entity.setUsuarioId(usuario.getId());
        entity.setVisibleLaneCodesRaw(joinCodes(request.visibleLaneCodes(), profile.visibleLaneCodes()));
        entity.setPinnedLaneCodesRaw(joinCodes(request.pinnedLaneCodes(), profile.pinnedLaneCodes()));
        entity.setHiddenLaneCodesRaw(joinCodes(request.hiddenLaneCodes(), List.of()));
        entity.setDefaultView(normalizeView(request.defaultView()));
        entity.setIncludePersonalCalendar(request.includePersonalCalendar() == null ? entity.isIncludePersonalCalendar() : request.includePersonalCalendar());
        entity.setIncludeInstitutionalCalendar(request.includeInstitutionalCalendar() == null ? entity.isIncludeInstitutionalCalendar() : request.includeInstitutionalCalendar());
        entity.setHighlightUrgentDays(request.highlightUrgentDays() == null ? entity.isHighlightUrgentDays() : request.highlightUrgentDays());
        entity.setSelectedScopeCode(normalizeCode(request.selectedScopeCode()));
        entity.setSelectedTeamId(request.selectedTeamId());
        entity.setSelectedInstitutionContextCode(normalizeCode(request.selectedInstitutionContextCode()));
        entity.setNotificationCadenceMode(normalizeCadenceMode(request.notificationCadenceMode()));
        entity.setNotificationLaneCodesRaw(joinCodes(request.notificationLaneCodes(), profile.visibleLaneCodes()));
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        repository.save(entity);
        return toResponse(usuario, entity, profile);
    }

    private UserCalendarPreference defaultEntity(Usuario usuario) {
        CalendarAudienceProfileService.CalendarProfile profile = audienceProfileService.resolve(usuario);
        UserCalendarPreference entity = new UserCalendarPreference();
        entity.setUsuarioId(usuario != null ? usuario.getId() : null);
        entity.setVisibleLaneCodesRaw(joinCodes(profile.visibleLaneCodes(), List.of()));
        entity.setPinnedLaneCodesRaw(joinCodes(profile.pinnedLaneCodes(), List.of()));
        entity.setHiddenLaneCodesRaw(null);
        entity.setDefaultView(usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isInstitucional() ? "WEEK" : "MONTH");
        entity.setIncludePersonalCalendar(profile.personalEventsEnabled());
        entity.setIncludeInstitutionalCalendar(usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isInstitucional());
        entity.setHighlightUrgentDays(true);
        entity.setSelectedScopeCode(usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isInstitucional() ? "INSTITUCIONAL" : profile.personalEventsEnabled() ? "PESSOAL" : "PROCESSUAL");
        entity.setSelectedTeamId(null);
        entity.setSelectedInstitutionContextCode(defaultContext(usuario, entity.getSelectedScopeCode(), null));
        entity.setNotificationCadenceMode("SMART");
        entity.setNotificationLaneCodesRaw(joinCodes(profile.visibleLaneCodes(), List.of()));
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    private UserCalendarPreferenceResponse toResponse(Usuario usuario,
                                                      UserCalendarPreference entity,
                                                      CalendarAudienceProfileService.CalendarProfile profile) {
        List<String> hidden = splitCodes(entity.getHiddenLaneCodesRaw());
        List<String> visible = mergedVisible(entity.getVisibleLaneCodesRaw(), profile.visibleLaneCodes(), hidden, entity.isIncludePersonalCalendar());
        List<String> pinned = mergedPinned(entity.getPinnedLaneCodesRaw(), profile.pinnedLaneCodes(), visible);
        List<String> notificationLanes = splitCodes(entity.getNotificationLaneCodesRaw()).isEmpty() ? visible : splitCodes(entity.getNotificationLaneCodesRaw());
        List<CalendarInstitutionalScopeService.ScopeOption> availableScopes = scopeService.availableScopes(usuario, entity.isIncludePersonalCalendar(), entity.isIncludeInstitutionalCalendar(), null);
        String activeScopeCode = scopeService.normalizeActiveScope(entity.getSelectedScopeCode(), availableScopes, entity.isIncludePersonalCalendar(), entity.isIncludeInstitutionalCalendar());
        Long selectedTeamId = entity.getSelectedTeamId() != null ? entity.getSelectedTeamId() : scopeService.parseTeamId(activeScopeCode);
        List<CalendarInstitutionalContextService.InstitutionalContextOption> availableContexts = contextService.availableContexts(usuario, activeScopeCode, null, selectedTeamId);
        String activeInstitutionContextCode = contextService.normalizeActiveContext(entity.getSelectedInstitutionContextCode(), availableContexts, activeScopeCode);
        return new UserCalendarPreferenceResponse(
                usuario != null ? usuario.getId() : null,
                visible,
                pinned,
                hidden,
                normalizeView(entity.getDefaultView()),
                entity.isIncludePersonalCalendar(),
                entity.isIncludeInstitutionalCalendar(),
                entity.isHighlightUrgentDays(),
                activeScopeCode,
                selectedTeamId,
                activeInstitutionContextCode,
                normalizeCadenceMode(entity.getNotificationCadenceMode()),
                notificationLanes,
                availableScopes.stream()
                        .map(option -> new UserCalendarPreferenceResponse.ScopeOptionDto(
                                option.scopeCode(),
                                option.scopeTitle(),
                                option.institutionLabel(),
                                option.scopeKind(),
                                option.scopeCode().equalsIgnoreCase(activeScopeCode)
                        ))
                        .toList(),
                availableContexts.stream()
                        .map(option -> new UserCalendarPreferenceResponse.InstitutionalContextOptionDto(
                                option.contextCode(),
                                option.contextTitle(),
                                option.contextLabel(),
                                option.contextKind(),
                                option.contextCode().equalsIgnoreCase(activeInstitutionContextCode)
                        ))
                        .toList(),
                entity.getUpdatedAt()
        );
    }

    public List<String> visibleLanes(UserCalendarPreferenceResponse preference, List<String> defaults) {
        if (preference == null) {
            return defaults;
        }
        return preference.visibleLaneCodes().isEmpty() ? defaults : preference.visibleLaneCodes();
    }

    public List<String> pinnedLanes(UserCalendarPreferenceResponse preference, List<String> defaults) {
        if (preference == null) {
            return defaults;
        }
        return preference.pinnedLaneCodes().isEmpty() ? defaults : preference.pinnedLaneCodes();
    }

    private String defaultContext(Usuario usuario, String selectedScopeCode, Long selectedTeamId) {
        List<CalendarInstitutionalContextService.InstitutionalContextOption> contexts = contextService.availableContexts(usuario, selectedScopeCode, null, selectedTeamId);
        return contextService.normalizeActiveContext(null, contexts, selectedScopeCode);
    }

    private static List<String> mergedVisible(String raw, List<String> defaults, List<String> hidden, boolean includePersonal) {
        LinkedHashSet<String> out = new LinkedHashSet<>(splitCodes(raw));
        if (out.isEmpty()) {
            out.addAll(defaults);
        }
        out.removeAll(hidden);
        if (!includePersonal) {
            out.remove("PESSOAL");
        }
        return List.copyOf(out);
    }

    private static List<String> mergedPinned(String raw, List<String> defaults, List<String> visible) {
        LinkedHashSet<String> out = new LinkedHashSet<>(splitCodes(raw));
        if (out.isEmpty()) {
            out.addAll(defaults);
        }
        out.retainAll(new LinkedHashSet<>(visible));
        return List.copyOf(out);
    }

    private static String joinCodes(List<String> requested, List<String> fallback) {
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        List<String> source = requested == null || requested.isEmpty() ? fallback : requested;
        for (String item : source) {
            String code = normalizeCode(item);
            if (code != null) {
                normalized.add(code);
            }
        }
        return normalized.isEmpty() ? null : String.join(",", normalized);
    }

    private static List<String> splitCodes(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String item : raw.split("[,;|]")) {
            String normalized = normalizeCode(item);
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return List.copyOf(out);
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeView(String value) {
        if (value == null || value.isBlank()) {
            return "MONTH";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Set.of("MONTH", "WEEK", "DAY").contains(normalized) ? normalized : "MONTH";
    }

    private static String normalizeCadenceMode(String value) {
        if (value == null || value.isBlank()) {
            return "SMART";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Set.of("SMART", "STRICT", "ESSENTIAL").contains(normalized) ? normalized : "SMART";
    }
}
