package com.tcc.pjb.backend.service.competencia;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.competencia.TipoVaraDistribuicao;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.repository.ProcessoDistribuicaoCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.service.outbox.OutboxPublisher;
import com.tcc.pjb.backend.tribunal.distribuicao.ConfiguracaoDistribuicaoVaraService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MapaCompetenciaDinamicoEngineTest {

    @Test
    void deveReutilizarSnapshotCurtoDeUnidadesEntreChamadasConsecutivas() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        when(unidadeRepository.findAll()).thenReturn(List.of(unidade("VARA-01")));
        MapaCompetenciaDinamicoEngine engine = new MapaCompetenciaDinamicoEngine(
                unidadeRepository,
                mock(ProcessoDistribuicaoCompetenciaRepository.class),
                mock(ProcessoRepository.class),
                mock(AuditLedgerService.class),
                mock(OutboxPublisher.class),
                mock(CompetenceResolverService.class),
                mock(ConfiguracaoDistribuicaoVaraService.class),
                mock(ProceduralCanonicalResolver.class)
        );

        engine.analisarRedistribuicao(0.95d);
        engine.analisarRedistribuicao(0.95d);

        verify(unidadeRepository, times(1)).findAll();
    }

    private static UnidadeJudiciariaCompetencia unidade(String codigo) {
        UnidadeJudiciariaCompetencia unidade = new UnidadeJudiciariaCompetencia(
                codigo,
                "TJCE",
                "Fortaleza",
                "CE",
                TipoJustica.ESTADUAL,
                RamoDireito.CIVIL,
                TipoVaraDistribuicao.CIVEL_GERAL
        );
        unidade.setCapacidadeMaxima(100);
        unidade.setProcessosAtivos(10);
        unidade.setIndiceCongestionamento(new BigDecimal("0.40"));
        unidade.setAceitaDistribuicao(true);
        unidade.setPermiteDistribuicaoAutomatica(true);
        unidade.setPrioridadeEstrategica(10);
        return unidade;
    }
}
