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
import org.mockito.Mockito;

class PrazoAuditTrailServiceTest {

    private final CalendarioForenseRepository calendarioRepository = Mockito.mock(CalendarioForenseRepository.class);
    private final PrazoAuditTrailService service = new PrazoAuditTrailService(calendarioRepository);

    @Test
    void deveGerarHashEContarBloqueios() {
        when(calendarioRepository.findApplicableBetween(Mockito.eq("CE"), Mockito.eq("Fortaleza"), Mockito.any(), Mockito.any()))
                .thenReturn(List.of(entry(LocalDate.of(2026, 4, 21), "FERIADO"), entry(LocalDate.of(2026, 4, 22), "SUSPENSAO")));
        PrazoAuditTrail trail = service.build(10L, "DJE", 2, PrazoRegime.UTEIS,
                LocalDateTime.of(2026, 4, 20, 8, 0), LocalDateTime.of(2026, 4, 24, 8, 0), "ce", "Fortaleza");
        assertThat(trail.totalFeriadosBloqueados()).isEqualTo(2);
        assertThat(trail.calendarioVersaoHash()).isNotBlank();
        assertThat(trail.uf()).isEqualTo("CE");
    }

    private static CalendarioForenseEntry entry(LocalDate dia, String tipo) {
        CalendarioForenseEntry entry = CalendarioForenseEntry.builder().build();
        entry.setDia(dia);
        entry.setTipo(tipo);
        entry.setUf("CE");
        entry.setComarca("Fortaleza");
        return entry;
    }
}
