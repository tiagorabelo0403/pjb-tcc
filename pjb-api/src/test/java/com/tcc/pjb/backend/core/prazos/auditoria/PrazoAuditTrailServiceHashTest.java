package com.tcc.pjb.backend.core.prazos.auditoria;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.prazos.PrazoRegime;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseEntry;
import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrazoAuditTrailServiceHashTest {

    @Test
    void shouldBuildStableHashAndDistinctBlockedDays() {
        CalendarioForenseRepository repository = mock(CalendarioForenseRepository.class);
        CalendarioForenseEntry one = CalendarioForenseEntry.builder().build();
        one.setDia(LocalDate.of(2026,4,21));
        one.setUf("CE");
        one.setTipo("FERIADO");
        CalendarioForenseEntry two = CalendarioForenseEntry.builder().build();
        two.setDia(LocalDate.of(2026,4,21));
        two.setUf("CE");
        two.setTipo("SUSPENSAO");
        when(repository.findApplicableBetween(any(), any(), any(), any())).thenReturn(List.of(two, one));
        PrazoAuditTrailService service = new PrazoAuditTrailService(repository);
        var trail = service.build(1L, "EVT", 5, PrazoRegime.UTEIS, LocalDateTime.of(2026,4,20,8,0), LocalDateTime.of(2026,4,28,8,0), "CE", null);
        assertThat(trail.totalFeriadosBloqueados()).isEqualTo(1);
        assertThat(trail.calendarioVersaoHash()).isNotBlank();
        assertThat(service.healthView(trail).totalFeriadosBloqueados()).isEqualTo(1);
        assertThat(service.timeline(trail).entries()).hasSize(2);
    }
}
