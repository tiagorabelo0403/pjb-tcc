package com.tcc.pjb.backend.inovacao.atlas;

import com.tcc.pjb.backend.platform.runtime.execution.PjbTransactionalExecutionSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.atlas.AtlasAcessoMunicipio;
import com.tcc.pjb.backend.model.entity.atlas.ClassificacaoDesertoAtlas;
import com.tcc.pjb.backend.model.repository.AtlasAcessoMunicipioRepository;
import com.tcc.pjb.backend.model.repository.MunicipiosRepository;
import com.tcc.pjb.backend.model.repository.PainelTribunalMetricaRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AtlasAcessoJusticaServiceTest {

    @Test
    void deveReutilizarSnapshotCurtoNoResumoNacional() {
        AtlasAcessoMunicipioRepository atlasRepository = mock(AtlasAcessoMunicipioRepository.class);
        when(atlasRepository.findAll()).thenReturn(List.of(celula("2304400", "Fortaleza", "CE", 72.5)));
        AtlasAcessoJusticaService service = new AtlasAcessoJusticaService(
                atlasRepository,
                mock(MunicipiosRepository.class),
                mock(PainelTribunalMetricaRepository.class),
                mock(AuditLedgerService.class),
                mock(OutboxPublisher.class),
                mock(PjbTransactionalExecutionSupport.class)
        );

        AtlasAcessoJusticaService.ResumoNacionalAtlas first = service.resumoNacional();
        AtlasAcessoJusticaService.ResumoNacionalAtlas second = service.resumoNacional();

        assertThat(first.totalMunicipios()).isEqualTo(1);
        assertThat(second.totalMunicipios()).isEqualTo(1);
        verify(atlasRepository, times(1)).findAll();
    }

    private static AtlasAcessoMunicipio celula(String codigoIbge, String nome, String uf, double scoreTotal) {
        AtlasAcessoMunicipio entity = new AtlasAcessoMunicipio();
        entity.setCodigoIbge(codigoIbge);
        entity.setNomeMunicipio(nome);
        entity.setUf(uf);
        entity.setRegiao("NORDESTE");
        entity.setPopulacao(100000);
        entity.setVarasInstaladas(2);
        entity.setJuizesEmExercicio(3);
        entity.setDefensoriasPorMunicipio(1);
        entity.setAdvogadosOabAtivos(200);
        entity.setTemJuizadoEspecial(Boolean.TRUE);
        entity.setTemCejusc(Boolean.TRUE);
        entity.setProcessosPorMilHabitantes(50);
        entity.setNovosProcessosMes(400);
        entity.setTaxaResolutividadePct(BigDecimal.valueOf(66.0));
        entity.setTempoMedioResolucaoDias(BigDecimal.valueOf(320.0));
        entity.setIndiceCongestionamento(BigDecimal.valueOf(0.40));
        entity.setTaxaJusticaGratuitaPct(BigDecimal.valueOf(35.0));
        entity.setTaxaAutoRepresentacaoPct(BigDecimal.valueOf(10.0));
        entity.setTaxaPrescricaoAparentePct(BigDecimal.valueOf(4.0));
        entity.setScoreInfraestrutura(BigDecimal.valueOf(18.0));
        entity.setScoreRepresentacao(BigDecimal.valueOf(17.0));
        entity.setScoreCeleridade(BigDecimal.valueOf(19.0));
        entity.setScoreEfetividade(BigDecimal.valueOf(18.5));
        entity.setScoreTotal(BigDecimal.valueOf(scoreTotal));
        entity.setGrau("B");
        entity.setClassificacao(ClassificacaoDesertoAtlas.ADEQUADO);
        entity.setAtualizadoEm(Instant.now());
        return entity;
    }
}
