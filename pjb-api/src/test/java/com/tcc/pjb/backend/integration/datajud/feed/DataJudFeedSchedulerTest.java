package com.tcc.pjb.backend.integration.datajud.feed;

import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalRunCommand;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;

class DataJudFeedSchedulerTest {

    @Test
    void naoDeveExecutarQuandoDesabilitado() {
        DataJudFeedService service = mock(DataJudFeedService.class);
        DataJudFeedScheduler scheduler = new DataJudFeedScheduler(service, new DataJudFeedProperties(false, 100, 300_000, 2, List.of("CNJ")));

        scheduler.run();

        verify(service, never()).runIncremental(any(DataJudTribunalRunCommand.class));
    }

    @Test
    void deveExecutarCnjQuandoNaoHaLista() {
        DataJudFeedService service = mock(DataJudFeedService.class);
        DataJudFeedScheduler scheduler = new DataJudFeedScheduler(service, new DataJudFeedProperties(true, 100, 300_000, 3, List.of()));

        scheduler.run();

        verify(service, times(1)).runIncremental(any(DataJudTribunalRunCommand.class));
    }

    @Test
    void deveExecutarUmaVezPorTribunalValido() {
        DataJudFeedService service = mock(DataJudFeedService.class);
        DataJudFeedScheduler scheduler = new DataJudFeedScheduler(service, new DataJudFeedProperties(true, 100, 300_000, 3, List.of("TJCE", " ", "TRF5")));

        scheduler.run();

        verify(service, times(2)).runIncremental(any(DataJudTribunalRunCommand.class));
    }
}
