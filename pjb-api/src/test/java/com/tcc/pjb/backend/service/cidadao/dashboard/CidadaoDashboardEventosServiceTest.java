package com.tcc.pjb.backend.service.cidadao.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.service.ui.UiHintService;
import java.util.List;
import org.junit.jupiter.api.Test;

class CidadaoDashboardEventosServiceTest {

    private final AudienciaRepository audienciaRepo = mock(AudienciaRepository.class);
    private final JulgamentoColegiadoRepository julgamentoRepo = mock(JulgamentoColegiadoRepository.class);
    private final UiHintService ui = mock(UiHintService.class);
    private final CidadaoDashboardEventosService service = new CidadaoDashboardEventosService(
            audienciaRepo, julgamentoRepo, ui);

    private Processo processo(Long id) {
        Processo p = mock(Processo.class);
        when(p.getId()).thenReturn(id);
        return p;
    }

    @Test
    void nextAudIndexaPorIdDoProcesso() {
        Processo p = processo(7L);
        Audiencia a = mock(Audiencia.class);
        when(a.getProcesso()).thenReturn(p);
        when(audienciaRepo.findNextUpcomingByProcessoIds(any(), any())).thenReturn(List.of(a));

        assertThat(service.nextAud(List.of(7L))).containsEntry(7L, a);
    }

    @Test
    void nextJulgIndexaPorIdDoProcesso() {
        Processo p = processo(9L);
        JulgamentoColegiado j = mock(JulgamentoColegiado.class);
        when(j.getProcesso()).thenReturn(p);
        when(julgamentoRepo.findNextPautaByProcessoIds(any(), any())).thenReturn(List.of(j));

        assertThat(service.nextJulg(List.of(9L))).containsEntry(9L, j);
    }

    @Test
    void nextAudVazioQuandoNaoHaIds() {
        assertThat(service.nextAud(List.of())).isEmpty();
        assertThat(service.nextAud(null)).isEmpty();
    }

    @Test
    void proximosEventosVazioQuandoNaoHaProcessos() {
        assertThat(service.proximosEventos(List.of())).isEmpty();
        assertThat(service.proximosEventos(null)).isEmpty();
    }
}
