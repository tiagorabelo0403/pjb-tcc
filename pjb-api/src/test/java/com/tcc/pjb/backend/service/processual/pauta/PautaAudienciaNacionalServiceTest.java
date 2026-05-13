package com.tcc.pjb.backend.service.processual.pauta;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.model.entity.calendar.UserCalendarSystemEvent;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.calendar.UserCalendarSystemEventRepository;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;
import com.tcc.pjb.backend.tribunal.regras.TribunalRuleEngine;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PautaAudienciaNacionalServiceTest {

    @Test
    void shouldDetectConflictAndSuggestAlternative() {
        UserCalendarSystemEventRepository repository = Mockito.mock(UserCalendarSystemEventRepository.class);
        CalendarioForenseTribunalService calendario = Mockito.mock(CalendarioForenseTribunalService.class);
        TribunalRuleEngine ruleEngine = Mockito.mock(TribunalRuleEngine.class);
        when(calendario.analisarDia(any(), any())).thenReturn(
                new CalendarioForenseTribunalService.DiaForense(LocalDate.of(2026, 3, 18), true, "Dia útil forense", null)
        );
        when(ruleEngine.resolverPrazoDias(any(), any(), anyInt())).thenReturn(30);
        when(ruleEngine.resolverBooleano(any(), any(), anyBoolean())).thenReturn(true);
        UserCalendarSystemEvent conflict = UserCalendarSystemEvent.builder()
                .id(1L)
                .usuarioId(99L)
                .domainKey("X")
                .eventType("AUD")
                .title("Audiência anterior")
                .at(LocalDateTime.of(2026, 3, 18, 10, 0))
                .color("primary")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(repository.findByUsuarioIdBetween(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(conflict), List.<UserCalendarSystemEvent>of(), List.<UserCalendarSystemEvent>of());
        when(repository.findByUsuarioIdAndDomainKey(anyLong(), anyString())).thenReturn(Optional.empty());
        PautaAudienciaNacionalService service = new PautaAudienciaNacionalService(repository, calendario, ruleEngine);
        var result = service.avaliar(new PautaAudienciaNacionalService.PautaAudienciaCommand(
                99L,
                120L,
                "TJCE",
                "CE",
                "Quixadá",
                RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                LocalDateTime.of(2026, 3, 18, 10, 0),
                60,
                "Conciliação",
                "Sala 1",
                "/api/v1/processos/120"
        ));
        assertFalse(result.disponivel());
        assertTrue(result.sugestaoAlternativa() != null);
    }
}
