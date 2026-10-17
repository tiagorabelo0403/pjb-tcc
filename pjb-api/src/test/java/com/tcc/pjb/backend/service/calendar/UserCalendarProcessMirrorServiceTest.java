package com.tcc.pjb.backend.service.calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.security.abac.AccessDeniedPjbException;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.calendar.CalendarProcessEventMirrorResponse;
import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.service.timeline.surface.TimelineSurfaceFacadeService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UserCalendarProcessMirrorServiceTest {

    private final UserCalendarWorkspaceService workspaceService = mock(UserCalendarWorkspaceService.class);
    private final TimelineSurfaceFacadeService timelineService = mock(TimelineSurfaceFacadeService.class);
    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PjbAuthorizationService authorizationService = mock(PjbAuthorizationService.class);
    private final UserCalendarProcessMirrorService service = new UserCalendarProcessMirrorService(
            workspaceService, timelineService, processoRepository,
            mock(CalendarEventAttentionPolicyService.class), authorizationService);

    @Test
    void negativaDaGuardaDeParteInterrompeAntesDaAgendaEDaTimeline() {
        Processo processo = new Processo();
        processo.setId(42L);
        when(processoRepository.findById(42L)).thenReturn(Optional.of(processo));
        doThrow(new AccessDeniedPjbException("Cidadão não é parte do processo."))
                .when(authorizationService).requireReadProcessoAsCidadaoParte(processo);

        LocalDate hoje = LocalDate.now();
        assertThatThrownBy(() -> service.mirror(42L, hoje, hoje.plusDays(30)))
                .isInstanceOf(AccessDeniedPjbException.class);

        verify(authorizationService).requireReadProcessoAsCidadaoParte(processo);
        verify(workspaceService, never()).workspace(any(), any(), anyLong());
        verifyNoInteractions(timelineService);
    }

    @Test
    void usuarioAutorizadoPelaGuardaRecebeOEspelhoDaAgendaEDaTimeline() {
        Processo processo = new Processo();
        processo.setId(42L);
        processo.setTribunal("TJCE");
        when(processoRepository.findById(42L)).thenReturn(Optional.of(processo));
        LocalDate hoje = LocalDate.now();
        when(workspaceService.workspace(hoje, hoje.plusDays(30), 42L))
                .thenReturn(new CalendarWorkspaceResponse(hoje, hoje.plusDays(30), null, List.of(), List.of()));
        when(timelineService.timeline(42L)).thenReturn(List.of());

        CalendarProcessEventMirrorResponse out = service.mirror(42L, hoje, hoje.plusDays(30));

        assertThat(out.processoId()).isEqualTo(42L);
        assertThat(out.tribunal()).isEqualTo("TJCE");
        verify(authorizationService).requireReadProcessoAsCidadaoParte(processo);
        verify(timelineService).timeline(42L);
    }
}
