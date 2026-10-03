package com.tcc.pjb.backend.service.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class CidadaoProcessoDetalheLoaderServiceTest {

    private final MovimentacaoProcessualRepository movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);
    private final DocumentoProcessualRepository documentoRepository = mock(DocumentoProcessualRepository.class);
    private final AudienciaRepository audienciaRepository = mock(AudienciaRepository.class);
    private final JulgamentoColegiadoRepository julgamentoRepository = mock(JulgamentoColegiadoRepository.class);
    private final CidadaoProcessoDetalheLoaderService service = new CidadaoProcessoDetalheLoaderService(
            movimentacaoRepository, documentoRepository, audienciaRepository, julgamentoRepository);

    @Test
    void loadersVaziosQuandoSemIds() {
        assertThat(service.loadLatestMovements(List.of())).isEmpty();
        assertThat(service.loadDocCounts(null)).isEmpty();
        assertThat(service.loadNextAudiencias(List.of())).isEmpty();
        assertThat(service.loadNextJulgamentos(null)).isEmpty();
    }

    @Test
    void loadLatestMovementsIndexaPorProcesso() {
        Processo p = mock(Processo.class);
        when(p.getId()).thenReturn(7L);
        MovimentacaoProcessual mov = mock(MovimentacaoProcessual.class);
        when(mov.getProcesso()).thenReturn(p);
        when(movimentacaoRepository.findLatestByProcessoIds(any())).thenReturn(List.of(mov));

        assertThat(service.loadLatestMovements(List.of(7L))).containsEntry(7L, mov);
    }

    @Test
    void loadNextAudienciasIndexaPorProcesso() {
        Processo p = mock(Processo.class);
        when(p.getId()).thenReturn(9L);
        Audiencia audiencia = mock(Audiencia.class);
        when(audiencia.getProcesso()).thenReturn(p);
        when(audienciaRepository.findNextUpcomingByProcessoIds(any(), any())).thenReturn(List.of(audiencia));

        assertThat(service.loadNextAudiencias(List.of(9L))).containsEntry(9L, audiencia);
    }

    @Test
    void loadDocCountsIndexaPorProcesso() {
        DocumentoProcessualRepository.ProcessoDocCount row = mock(DocumentoProcessualRepository.ProcessoDocCount.class);
        when(row.getProcessoId()).thenReturn(5L);
        when(row.getCnt()).thenReturn(3L);
        when(documentoRepository.countDocsByProcessoIds(any())).thenReturn(List.of(row));

        assertThat(service.loadDocCounts(List.of(5L))).containsEntry(5L, 3L);
    }
}
