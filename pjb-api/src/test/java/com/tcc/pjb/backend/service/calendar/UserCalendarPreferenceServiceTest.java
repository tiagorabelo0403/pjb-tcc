package com.tcc.pjb.backend.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceRequest;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarPreference;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.MembroEquipeRepository;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarPreferenceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserCalendarPreferenceServiceTest {

    @Test
    void defaultsFollowAudienceProfile() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserCalendarPreferenceRepository repository = mock(UserCalendarPreferenceRepository.class);
        CalendarAudienceProfileService audienceProfileService = new CalendarAudienceProfileService();
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        when(membroEquipeRepository.carregarComEquipe(10L)).thenReturn(List.of());
        CalendarInstitutionalScopeService scopeService = new CalendarInstitutionalScopeService(membroEquipeRepository);
        CalendarInstitutionalContextService contextService = new CalendarInstitutionalContextService();
        UserCalendarPreferenceService service = new UserCalendarPreferenceService(currentUserService, repository, audienceProfileService, scopeService, contextService);

        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setTipoUsuario(TipoUsuario.CIDADAO);
        when(repository.findByUsuarioId(10L)).thenReturn(Optional.empty());

        var response = service.currentOrDefault(usuario);

        assertEquals("MONTH", response.defaultView());
        assertTrue(response.includePersonalCalendar());
        assertFalse(response.includeInstitutionalCalendar());
        assertTrue(response.visibleLaneCodes().contains("AGENDA_PROCESSUAL"));
        assertEquals("PESSOAL", response.selectedScopeCode());
        assertEquals("PESSOAL", response.selectedInstitutionContextCode());
        assertEquals("SMART", response.notificationCadenceMode());
    }

    @Test
    void saveFiltersHiddenAndPersonalLane() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        UserCalendarPreferenceRepository repository = mock(UserCalendarPreferenceRepository.class);
        CalendarAudienceProfileService audienceProfileService = new CalendarAudienceProfileService();
        MembroEquipeRepository membroEquipeRepository = mock(MembroEquipeRepository.class);
        when(membroEquipeRepository.carregarComEquipe(20L)).thenReturn(List.of());
        CalendarInstitutionalScopeService scopeService = new CalendarInstitutionalScopeService(membroEquipeRepository);
        CalendarInstitutionalContextService contextService = new CalendarInstitutionalContextService();
        UserCalendarPreferenceService service = new UserCalendarPreferenceService(currentUserService, repository, audienceProfileService, scopeService, contextService);

        Usuario usuario = new Usuario();
        usuario.setId(20L);
        usuario.setTipoUsuario(TipoUsuario.ADVOGADO);
        when(currentUserService.getRequired()).thenReturn(usuario);
        when(repository.findByUsuarioId(20L)).thenReturn(Optional.of(new UserCalendarPreference()));

        var response = service.save(new UserCalendarPreferenceRequest(
                List.of("PRAZOS", "AGENDA_PROCESSUAL", "PESSOAL"),
                List.of("PRAZOS"),
                List.of("PESSOAL"),
                "week",
                false,
                true,
                true,
                "institucional",
                null,
                "recursal_escritorio",
                "strict",
                List.of("PRAZOS", "AGENDA_PROCESSUAL")
        ));

        assertEquals("WEEK", response.defaultView());
        assertFalse(response.includePersonalCalendar());
        assertFalse(response.visibleLaneCodes().contains("PESSOAL"));
        assertEquals(List.of("PRAZOS"), response.pinnedLaneCodes());
        assertEquals("INSTITUCIONAL", response.selectedScopeCode());
        assertEquals("RECURSAL_ESCRITORIO", response.selectedInstitutionContextCode());
        assertEquals("STRICT", response.notificationCadenceMode());
        assertEquals(List.of("PRAZOS", "AGENDA_PROCESSUAL"), response.notificationLaneCodes());
    }
}
