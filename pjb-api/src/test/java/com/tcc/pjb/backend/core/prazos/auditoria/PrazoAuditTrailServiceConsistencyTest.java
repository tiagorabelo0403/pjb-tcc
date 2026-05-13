package com.tcc.pjb.backend.core.prazos.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditQuery;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazoAuditTrailServiceConsistencyTest {

    @Test
    void shouldKeepStableHashForEquivalentCalendarioEntries() {
        CalendarioForenseRepository calendario = mock(CalendarioForenseRepository.class);
        List<CalendarioForenseEntry> entries = List.of(
                new CalendarioForenseEntry(LocalDate.of(2026, 4, 14), "CE", "Fortaleza", "FERIADO"),
                new CalendarioForenseEntry(LocalDate.of(2026, 4, 15), "CE", "Fortaleza", "SUSPENSAO")
        );
        when(calendario.findApplicableBetween("CE", "Fortaleza", LocalDate.of(2026, 4, 9), LocalDate.of(2026, 4, 17))).thenReturn(entries);
        PrazoAuditTrailService service = new PrazoAuditTrailService(calendario);

        var first = service.query(new PrazoAuditQuery(1L, "INTIMACAO", 2, PrazoRegime.UTEIS, LocalDateTime.of(2026, 4, 11, 9, 0), LocalDateTime.of(2026, 4, 15, 9, 0), "CE", "Fortaleza"));
        var second = service.query(new PrazoAuditQuery(1L, "INTIMACAO", 2, PrazoRegime.UTEIS, LocalDateTime.of(2026, 4, 11, 9, 0), LocalDateTime.of(2026, 4, 15, 9, 0), "CE", "Fortaleza"));
        var health = service.healthView(first.trail());
        var timeline = service.timeline(first.trail());

        assertThat(first.trail().calendarioVersaoHash()).isEqualTo(second.trail().calendarioVersaoHash());
        assertThat(health.totalFeriadosBloqueados()).isEqualTo(2);
        assertThat(timeline.entries()).hasSize(2);
    }
}
