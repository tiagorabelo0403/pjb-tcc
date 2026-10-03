package com.tcc.pjb.backend.service.processo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.distribuicao.ProcessoInitialDistributionSnapshotService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessoDistribuicaoInicialReforcoServiceTest {

    private final MapaCompetenciaDinamicoEngine engine = mock(MapaCompetenciaDinamicoEngine.class);
    private final ProcessoInitialDistributionSnapshotService snapshotService = mock(ProcessoInitialDistributionSnapshotService.class);
    private final ProcessoDistribuicaoInicialReforcoService service = new ProcessoDistribuicaoInicialReforcoService(engine, snapshotService);

    private Processo processoCompleto() {
        Processo p = new Processo();
        p.setId(1L);
        p.setUnidadeJudiciariaCodigo("UJ1");
        p.setTribunalCodigoRoteado("TRT7");
        p.setPreProtocoloStatus("APTA");
        p.setCompetenciaTerritorialModo("FIXO");
        p.setPreventionMode("NENHUM");
        p.setLinkageMode("NENHUM");
        return p;
    }

    @Test
    void reforcaDistribuicaoQuandoSnapshotAusente() {
        Processo p = new Processo();
        p.setId(1L);
        DynamicCompetenceDistributionResponse resp = mock(DynamicCompetenceDistributionResponse.class);
        when(engine.registrarDistribuicaoInicial(p)).thenReturn(Optional.of(resp));

        DynamicCompetenceDistributionResponse out = service.ensureSnapshot(p);

        assertThat(out).isSameAs(resp);
        verify(snapshotService).consolidar(p);
    }

    @Test
    void naoReforcaQuandoSnapshotCompleto() {
        Processo p = processoCompleto();

        DynamicCompetenceDistributionResponse out = service.ensureSnapshot(p);

        assertThat(out).isNull();
        verify(engine, never()).registrarDistribuicaoInicial(any());
        verify(snapshotService, never()).consolidar(any());
    }

    @Test
    void falhaNoEngineNaoPropaga() {
        Processo p = new Processo();
        p.setId(1L);
        when(engine.registrarDistribuicaoInicial(p)).thenThrow(new RuntimeException("boom"));

        assertThatCode(() -> assertThat(service.ensureSnapshot(p)).isNull()).doesNotThrowAnyException();
    }
}
