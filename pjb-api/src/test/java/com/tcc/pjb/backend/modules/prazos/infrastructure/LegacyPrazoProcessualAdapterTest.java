package com.tcc.pjb.backend.modules.prazos.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.modules.prazos.api.PrazoDiaForenseCommand;
import com.tcc.pjb.backend.modules.prazos.api.PrazoProcessualCalculoCommand;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.service.processual.prazo.PrazoProcessualNacionalService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class LegacyPrazoProcessualAdapterTest {

    @Test
    void converteComandoModularParaServicoLegadoSemExporEntity() {
        PrazoProcessualNacionalService legacy = Mockito.mock(PrazoProcessualNacionalService.class);
        when(legacy.calcular(any())).thenReturn(new PrazoProcessualNacionalService.PrazoProcessualResult(
                LocalDate.of(2026, 3, 17),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2),
                16,
                11,
                12,
                NationalPrazoEngine.TipoPrazo.APELACAO,
                RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "TJCE",
                "CE",
                "Quixada",
                true,
                "Dia util forense",
                List.of("Prazo padrao"),
                "CPC",
                "Calendario forense"
        ));
        LegacyPrazoProcessualAdapter adapter = new LegacyPrazoProcessualAdapter(legacy);

        var result = adapter.calcularPrazo(new PrazoProcessualCalculoCommand(
                LocalDate.of(2026, 3, 17),
                "APELACAO",
                "CIVIL",
                "PRIMEIRO_GRAU",
                "TJCE",
                "CE",
                "Quixada",
                null
        ));

        ArgumentCaptor<PrazoProcessualNacionalService.CalculoPrazoCommand> captor = ArgumentCaptor.forClass(PrazoProcessualNacionalService.CalculoPrazoCommand.class);
        verify(legacy).calcular(captor.capture());
        assertEquals(NationalPrazoEngine.TipoPrazo.APELACAO, captor.getValue().tipoPrazo());
        assertEquals(RamoDireito.CIVIL, captor.getValue().ramo());
        assertEquals(GrauJurisdicao.PRIMEIRO_GRAU, captor.getValue().grau());
        assertEquals(LocalDate.of(2026, 4, 2), result.vencimentoForense());
        assertEquals("APELACAO", result.tipoPrazo());
        assertFalse(result.conferenciaManualRecomendada());
    }

    @Test
    void converteAnaliseDeDiaForenseParaContratoModular() {
        PrazoProcessualNacionalService legacy = Mockito.mock(PrazoProcessualNacionalService.class);
        when(legacy.analisarDia(any(), any(), any(), any(), any(), any())).thenReturn(new PrazoProcessualNacionalService.DiaForenseResult(
                LocalDate.of(2026, 3, 18),
                true,
                "Dia util forense",
                "DIA_UTIL"
        ));
        LegacyPrazoProcessualAdapter adapter = new LegacyPrazoProcessualAdapter(legacy);

        var result = adapter.analisarDiaForense(new PrazoDiaForenseCommand(
                LocalDate.of(2026, 3, 18),
                "TJCE",
                "CE",
                "Quixada",
                "CIVIL",
                "PRIMEIRO_GRAU"
        ));

        assertEquals(LocalDate.of(2026, 3, 18), result.data());
        assertEquals("DIA_UTIL", result.tipoEntrada());
        assertFalse(result.conferenciaManualRecomendada());
    }

    @Test
    void rejeitaEnumLegadoInvalidoComErroClaro() {
        PrazoProcessualNacionalService legacy = Mockito.mock(PrazoProcessualNacionalService.class);
        LegacyPrazoProcessualAdapter adapter = new LegacyPrazoProcessualAdapter(legacy);

        assertThrows(IllegalArgumentException.class, () -> adapter.calcularPrazo(new PrazoProcessualCalculoCommand(
                LocalDate.of(2026, 3, 17),
                "INEXISTENTE",
                "CIVIL",
                "PRIMEIRO_GRAU",
                "TJCE",
                "CE",
                "Quixada",
                null
        )));
    }
}
