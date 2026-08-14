package com.tcc.pjb.backend.service.competencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.competencia.TipoVaraDistribuicao;
import com.tcc.pjb.backend.model.entity.competencia.Tribunal;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
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

    @Test
    void aderenciaTerritorialMinima_ufIgualDaUnidadeEDoProcesso_pontuaDoisQuandoComarcaNaoCoincide() {
        MapaCompetenciaDinamicoEngine engine = criarEngine();
        UnidadeJudiciariaCompetencia unidade = unidadeComUf("VARA-CE", "CE");
        MapaCompetenciaDinamicoEngine.DynamicRequest request = requestComUfEComarcaReu("CE", "Sobral");

        int score = engine.aderenciaTerritorialMinima(unidade, request);

        assertThat(score).isEqualTo(2);
    }

    @Test
    void aderenciaTerritorialMinima_ufDesconhecidaDaUnidade_mantemElegivelSemFavorecer() {
        MapaCompetenciaDinamicoEngine engine = criarEngine();
        UnidadeJudiciariaCompetencia unidade = unidadeComUf("VARA-SEM-UF", null);
        MapaCompetenciaDinamicoEngine.DynamicRequest request = requestComUfEComarcaReu("CE", null);

        int score = engine.aderenciaTerritorialMinima(unidade, request);

        assertThat(score).isEqualTo(1);
    }

    @Test
    void aderenciaTerritorialMinima_ufDivergenteEntreUnidadeEProcesso_excluiComScoreZero() {
        MapaCompetenciaDinamicoEngine engine = criarEngine();
        UnidadeJudiciariaCompetencia unidade = unidadeComUf("VARA-AC", "AC");
        MapaCompetenciaDinamicoEngine.DynamicRequest request = requestComUfEComarcaReu("CE", null);

        int score = engine.aderenciaTerritorialMinima(unidade, request);

        assertThat(score).isEqualTo(0);
    }

    private static MapaCompetenciaDinamicoEngine criarEngine() {
        return new MapaCompetenciaDinamicoEngine(
                mock(UnidadeJudiciariaCompetenciaRepository.class),
                mock(ProcessoDistribuicaoCompetenciaRepository.class),
                mock(ProcessoRepository.class),
                mock(AuditLedgerService.class),
                mock(OutboxPublisher.class),
                mock(CompetenceResolverService.class),
                mock(ConfiguracaoDistribuicaoVaraService.class),
                mock(ProceduralCanonicalResolver.class)
        );
    }

    private static UnidadeJudiciariaCompetencia unidadeComUf(String codigo, String uf) {
        Tribunal tribunal = new Tribunal("TJCE", "Tribunal de Justica do Ceara", TipoJustica.ESTADUAL, GrauJurisdicao.SEGUNDO_GRAU, "CE");
        return new UnidadeJudiciariaCompetencia(
                codigo, tribunal, null, uf, TipoJustica.ESTADUAL, RamoDireito.CIVIL, TipoVaraDistribuicao.CIVEL_GERAL);
    }

    private static MapaCompetenciaDinamicoEngine.DynamicRequest requestComUfEComarcaReu(String ufReu, String comarcaReu) {
        return new MapaCompetenciaDinamicoEngine.DynamicRequest(
                null, null, null, null, null, null, null, ufReu, comarcaReu, false, false, null, null, false, false, null);
    }

    private static UnidadeJudiciariaCompetencia unidade(String codigo) {
        Tribunal tribunal = new Tribunal("TJCE", "Tribunal de Justica do Ceara", TipoJustica.ESTADUAL, GrauJurisdicao.SEGUNDO_GRAU, "CE");
        Comarca comarca = new Comarca("Fortaleza", "CE", "2304400", null, tribunal);
        UnidadeJudiciariaCompetencia unidade = new UnidadeJudiciariaCompetencia(
                codigo,
                tribunal,
                comarca,
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
