package com.tcc.pjb.backend.service.processual.prazo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.tribunal.calendario.CalendarioForenseTribunalService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PrazoProcessualNacionalServiceTest {

    @Test
    void shouldMergeNationalAndForenseDeadline() {
        NationalPrazoEngine national = Mockito.mock(NationalPrazoEngine.class);
        CalendarioForenseTribunalService calendario = Mockito.mock(CalendarioForenseTribunalService.class);
        when(national.calcular(any(), any(), any(), any(), any())).thenReturn(
                new NationalPrazoEngine.PrazoCalculado(
                        LocalDate.of(2026, 3, 17),
                        LocalDate.of(2026, 4, 1),
                        15,
                        11,
                        NationalPrazoEngine.TipoPrazo.APELACAO,
                        RamoDireito.CIVIL,
                        GrauJurisdicao.PRIMEIRO_GRAU,
                        false,
                        java.util.List.of("Prazo padrão"),
                        "CPC"
                )
        );
        when(calendario.calcularPrazo(any(), anyInt(), any(), any(), any())).thenReturn(
                new CalendarioForenseTribunalService.PrazoCalculado(
                        LocalDate.of(2026, 3, 17),
                        LocalDate.of(2026, 3, 18),
                        11,
                        LocalDate.of(2026, 4, 1),
                        "TJCE",
                        "CE",
                        "Quixadá",
                        java.util.List.of(),
                        "Prazo calculado",
                        "Calendário forense"
                )
        );
        when(calendario.analisarDia(any(), any())).thenReturn(
                new CalendarioForenseTribunalService.DiaForense(LocalDate.of(2026, 3, 17), true, "Dia útil forense", null)
        );
        PrazoProcessualNacionalService service = new PrazoProcessualNacionalService(national, calendario);
        var result = service.calcular(new PrazoProcessualNacionalService.CalculoPrazoCommand(
                LocalDate.of(2026, 3, 17),
                NationalPrazoEngine.TipoPrazo.APELACAO,
                RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "TJCE",
                "CE",
                "Quixadá",
                null
        ));
        assertEquals(LocalDate.of(2026, 4, 1), result.vencimentoForense());
        assertFalse(result.advertencias().isEmpty());
        assertNotNull(result.fundamentoForense());
    }
}
