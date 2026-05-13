package com.tcc.pjb.backend.core.prazos.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.auditoria.domain.PrazoAuditQuery;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazoAuditTrailServiceViewsTest {

    @Test
    void shouldExposeQueryHealthAndTimelineViews() {
        CalendarioForenseRepository repository = mock(CalendarioForenseRepository.class);
        when(repository.findApplicableBetween(any(), any(), any(), any()))
                .thenReturn(List.of(
                        CalendarioForenseEntry.builder().dia(LocalDate.of(2026, 4, 21)).uf("SP").tipo("FERIADO").build(),
                        CalendarioForenseEntry.builder().dia(LocalDate.of(2026, 4, 22)).uf("SP").tipo("FERIADO").build()));
        PrazoAuditTrailService service = new PrazoAuditTrailService(repository);
        var query = new PrazoAuditQuery(1L, "INTIMACAO", 5, PrazoRegime.UTEIS, LocalDateTime.of(2026, 4, 20, 9, 0), LocalDateTime.of(2026, 4, 28, 9, 0), "SP", "Fortaleza");

        var result = service.query(query);
        var health = service.healthView(result.trail());
        var timeline = service.timeline(result.trail());

        assertThat(result.trail().calendarioVersaoHash()).isNotBlank();
        assertThat(health.totalBloqueios()).isEqualTo(2L);
        assertThat(timeline.entries()).hasSize(2);
    }
}
