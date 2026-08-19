package com.tcc.pjb.backend.service.prazo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoMarco;
import com.tcc.pjb.backend.model.dto.prazo.PrazoCertidaoTempestividadeResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PrazoCartorioPainelServiceTempestividadeTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final ProcessoPrazoApplicationService processoPrazoApplicationService = mock(ProcessoPrazoApplicationService.class);
    private final PrazoCartorioPainelService service = new PrazoCartorioPainelService(
            mock(CienciaProcessualRepository.class), processoRepository, processoPrazoApplicationService);

    private ProcessoPrazoMarco marco(LocalDate vencimento) {
        return new ProcessoPrazoMarco(
                "TRILHA_PRINCIPAL", "Prazo principal do rito", "CONTESTACAO", "TRILHA",
                vencimento.minusDays(15), vencimento, 15, 15, 0, false, false, true,
                "MANTEM_TRILHA_REGULAR", List.of(), List.of("CPC art. 335"));
    }

    @Test
    void certificaTempestivoQuandoPraticadoAteOVencimento() {
        Processo processo = new Processo();
        processo.setId(70L);
        processo.setNumeroProcesso("0002222-33.2026.8.06.0001");
        when(processoRepository.findById(70L)).thenReturn(Optional.of(processo));
        LocalDate vencimento = LocalDate.of(2027, 3, 20);
        when(processoPrazoApplicationService.calcular(70L, NationalPrazoEngine.TipoPrazo.CONTESTACAO))
                .thenReturn(marco(vencimento));

        PrazoCertidaoTempestividadeResponse resultado = service.certificarTempestividade(
                70L, NationalPrazoEngine.TipoPrazo.CONTESTACAO, vencimento);

        assertThat(resultado.tempestivo()).isTrue();
        assertThat(resultado.numeroProcesso()).isEqualTo("0002222-33.2026.8.06.0001");
        assertThat(resultado.textoCertidao()).contains("CERTIDÃO DE TEMPESTIVIDADE").contains("CPC art. 335");
    }

    @Test
    void certificaIntempestivoQuandoPraticadoAposOVencimento() {
        Processo processo = new Processo();
        processo.setId(71L);
        processo.setNumeroProcesso("0003333-44.2026.8.06.0001");
        when(processoRepository.findById(71L)).thenReturn(Optional.of(processo));
        LocalDate vencimento = LocalDate.of(2027, 3, 20);
        when(processoPrazoApplicationService.calcular(71L, NationalPrazoEngine.TipoPrazo.CONTESTACAO))
                .thenReturn(marco(vencimento));

        PrazoCertidaoTempestividadeResponse resultado = service.certificarTempestividade(
                71L, NationalPrazoEngine.TipoPrazo.CONTESTACAO, vencimento.plusDays(1));

        assertThat(resultado.tempestivo()).isFalse();
        assertThat(resultado.textoCertidao()).contains("CERTIDÃO DE INTEMPESTIVIDADE");
    }

    @Test
    void rejeitaProcessoInexistenteSemChamarMotorDePrazo() {
        when(processoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.certificarTempestividade(999L, NationalPrazoEngine.TipoPrazo.CONTESTACAO, LocalDate.now()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
