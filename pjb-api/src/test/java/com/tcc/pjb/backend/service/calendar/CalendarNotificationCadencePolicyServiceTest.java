package com.tcc.pjb.backend.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import com.tcc.pjb.backend.model.dto.calendar.UserCalendarPreferenceResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalendarNotificationCadencePolicyServiceTest {

    @Test
    void resolvesMandadoCadenceForOfficialJustice() {
        CalendarNotificationCadencePolicyService service = new CalendarNotificationCadencePolicyService();
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(TipoUsuario.OFICIAL_JUSTICA);
        UserCalendarPreferenceResponse preference = new UserCalendarPreferenceResponse(
                1L,
                List.of("AGENDA_PROCESSUAL"),
                List.of("AGENDA_PROCESSUAL"),
                List.of(),
                "WEEK",
                false,
                true,
                true,
                "INSTITUCIONAL",
                null,
                "CENTRAL_MANDADOS",
                "STRICT",
                List.of("AGENDA_PROCESSUAL"),
                List.of(),
                List.of(),
                Instant.now()
        );
        CalendarWorkspaceEventDto event = new CalendarWorkspaceEventDto(
                "AGENDA_PROCESSUAL",
                "AGENDA_MANDADOS",
                "Mandados e diligências",
                "MANDADO",
                10L,
                100L,
                "0001",
                "Mandado pendente",
                "Central de mandados",
                LocalDateTime.now().plusHours(10),
                "BLUE",
                false,
                "/api/v1/processos/100",
                null,
                "OFICIAL_JUSTICA"
        );

        var decision = service.resolve(usuario, "OFICIAL_JUSTICA", preference, event, Processo.builder().id(100L).build(), LocalDateTime.now());

        assertNotNull(decision);
        assertTrue(decision.stageCode().startsWith("MANDADO"));
        assertEquals("STRICT", decision.cadenceMode());
    }
}
