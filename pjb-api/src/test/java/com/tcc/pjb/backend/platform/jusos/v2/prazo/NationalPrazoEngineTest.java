package com.tcc.pjb.backend.platform.jusos.v2.prazo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.prazos.calendario.CalendarioForenseRepository;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.JurisdicaoRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NationalPrazoEngineTest {

    @Test
    void adicionarFeriadoTribunalComCacheFrioNaoFazRecursiveUpdateEInfluenciaCalculo() {
        CalendarioForenseRepository calendarioRepository = mock(CalendarioForenseRepository.class);
        JurisdicaoRepository jurisdicaoRepository = mock(JurisdicaoRepository.class);
        when(jurisdicaoRepository.findByCodigo(any())).thenReturn(Optional.empty());
        when(jurisdicaoRepository.findBySiglaIgnoreCase(any())).thenReturn(Optional.empty());
        when(calendarioRepository.findApplicableBetween(any(), any(), any(), any())).thenReturn(List.of());

        NationalPrazoEngine engine = new NationalPrazoEngine(calendarioRepository, jurisdicaoRepository);
        LocalDate feriado = LocalDate.of(2026, 5, 21);

        assertThatCode(() -> engine.adicionarFeriadoTribunal("TJCE", 2026, feriado))
                .doesNotThrowAnyException();

        NationalPrazoEngine.PrazoCalculado prazo = engine.calcular(
                LocalDate.of(2026, 5, 20),
                NationalPrazoEngine.TipoPrazo.EMBARGOS_DECLARACAO,
                RamoDireito.CIVIL,
                null,
                "TJCE"
        );

        assertThat(prazo.vencimento()).isEqualTo(LocalDate.of(2026, 5, 28));
    }
}
