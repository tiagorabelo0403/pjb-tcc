package com.tcc.pjb.backend.service.calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.calendar.CalendarWorkspaceEventDto;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CalendarEventAttentionPolicyServiceTest {

    @Test
    void classifiesMandadoRetornoAsCriticalOperationalPresentation() {
        CalendarEventAttentionPolicyService service = new CalendarEventAttentionPolicyService();
        CalendarWorkspaceEventDto event = new CalendarWorkspaceEventDto(
                "AGENDA_PROCESSUAL",
                "AGENDA_MANDADOS",
                "Mandados",
                "MANDADO_RETORNO",
                10L,
                20L,
                "0001",
                "Retorno de diligência",
                "Frustrado",
                LocalDateTime.now().plusHours(3),
                "RED",
                false,
                "/api/v1/oficial/20",
                null,
                "OFICIAL"
        );

        CalendarEventAttentionPolicyService.AttentionDescriptor descriptor = service.describe(event, LocalDateTime.now());

        assertEquals("MANDADO_RETORNO", descriptor.presentationCode());
        assertEquals("RETORNO", descriptor.detailCode());
        assertEquals("RETURN", descriptor.iconCode());
        assertTrue(descriptor.attentionScore() >= 90);
    }
}
