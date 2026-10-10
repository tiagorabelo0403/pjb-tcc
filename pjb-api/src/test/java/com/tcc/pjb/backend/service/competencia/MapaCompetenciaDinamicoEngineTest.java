package com.tcc.pjb.backend.service.competencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceRedistributionResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

class MapaCompetenciaDinamicoEngineTest {

    @Test
    void deveReutilizarSnapshotCurtoDeUnidadesEntreChamadasConsecutivas() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        when(unidadeRepository.findAll()).thenReturn(
                List.of(congestionada("VARA-01", 1L)),
                List.of(congestionada("VARA-01", 1L), livre("VARA-02", 2L)));
        MapaCompetenciaDinamicoEngine engine = engineCom(unidadeRepository);

        assertThat(engine.analisarRedistribuicao(0.95d).propostas()).isEmpty();
        assertThat(engine.analisarRedistribuicao(0.95d).propostas()).isEmpty();

        verify(unidadeRepository, times(1)).findAll();
    }

    @Test
    void alteracaoDeUnidadesDescartaOSnapshotEmCache() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        when(unidadeRepository.findAll()).thenReturn(
                List.of(congestionada("VARA-01", 1L)),
                List.of(congestionada("VARA-01", 1L), livre("VARA-02", 2L)));
        MapaCompetenciaDinamicoEngine engine = engineCom(unidadeRepository);

        assertThat(engine.analisarRedistribuicao(0.95d).propostas()).isEmpty();
        engine.invalidarSnapshotDeUnidades(new UnidadesJudiciariasAlteradasEvent());

        assertThat(engine.analisarRedistribuicao(0.95d).propostas())
                .extracting(DynamicCompetenceRedistributionResponse.Proposal::origemCodigo,
                        DynamicCompetenceRedistributionResponse.Proposal::destinoCodigo)
                .containsExactly(tuple("VARA-01", "VARA-02"));
        verify(unidadeRepository, times(2)).findAll();
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

    @Test
    void scoreTerritorial_unidadeForaDoCatalogoDeComarcaPontuaAcimaDoLimiarDeAutomacaoPorTextoDeComarca() throws Exception {
        MapaCompetenciaDinamicoEngine engine = criarEngine();
        UnidadeJudiciariaCompetencia unidade = unidadeSemFkComComarcaTextual("TJAC-CIVEL-AC-CAP", "AC", "Rio Branco");
        MapaCompetenciaDinamicoEngine.DynamicRequest request =
                requestComTerritorioAutorEReu("AC", "Rio Branco", "AC", "Rio Branco");

        double score = invokeScoreTerritorial(engine, unidade, request);

        assertThat(score).isGreaterThanOrEqualTo(18.0d);
    }

    @Test
    void scoreTerritorial_ufDoReuBateMasComarcaDaUnidadeEDesconhecidaNaoRebaixaScorePeloBlocoDoAutor() throws Exception {
        MapaCompetenciaDinamicoEngine engine = criarEngine();
        UnidadeJudiciariaCompetencia unidade = unidadeComUf("VARA-AC-SEM-COMARCA", "AC");
        MapaCompetenciaDinamicoEngine.DynamicRequest request =
                requestComTerritorioAutorEReu("AC", "Rio Branco", "AC", "Rio Branco");

        double score = invokeScoreTerritorial(engine, unidade, request);

        assertThat(score).isGreaterThanOrEqualTo(18.0d);
    }

    private static double invokeScoreTerritorial(MapaCompetenciaDinamicoEngine engine,
                                                 UnidadeJudiciariaCompetencia unidade,
                                                 MapaCompetenciaDinamicoEngine.DynamicRequest request) throws Exception {
        java.lang.reflect.Method metodo = MapaCompetenciaDinamicoEngine.class.getDeclaredMethod(
                "scoreTerritorial",
                UnidadeJudiciariaCompetencia.class,
                MapaCompetenciaDinamicoEngine.DynamicRequest.class,
                List.class);
        metodo.setAccessible(true);
        return (double) metodo.invoke(engine, unidade, request, new java.util.ArrayList<String>());
    }

    private static UnidadeJudiciariaCompetencia unidadeSemFkComComarcaTextual(String codigo, String uf, String comarca) {
        Tribunal tribunal = new Tribunal("TJAC", "Tribunal de Justica do Acre", TipoJustica.ESTADUAL, GrauJurisdicao.SEGUNDO_GRAU, uf);
        UnidadeJudiciariaCompetencia unidade = new UnidadeJudiciariaCompetencia(
                codigo, tribunal, null, uf, TipoJustica.ESTADUAL, RamoDireito.CIVIL, TipoVaraDistribuicao.CIVEL_GERAL);
        unidade.setComarca(comarca);
        return unidade;
    }

    private static MapaCompetenciaDinamicoEngine.DynamicRequest requestComTerritorioAutorEReu(String ufAutor,
                                                                                             String comarcaAutor,
                                                                                             String ufReu,
                                                                                             String comarcaReu) {
        return new MapaCompetenciaDinamicoEngine.DynamicRequest(
                null, null, null, null, null, ufAutor, comarcaAutor, ufReu, comarcaReu,
                false, false, null, null, false, false, null);
    }

    private static MapaCompetenciaDinamicoEngine criarEngine() {
        return engineCom(mock(UnidadeJudiciariaCompetenciaRepository.class));
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

    private static MapaCompetenciaDinamicoEngine engineCom(UnidadeJudiciariaCompetenciaRepository unidadeRepository) {
        return new MapaCompetenciaDinamicoEngine(
                unidadeRepository,
                mock(ProcessoDistribuicaoCompetenciaRepository.class),
                mock(ProcessoRepository.class),
                mock(AuditLedgerService.class),
                mock(OutboxPublisher.class),
                mock(CompetenceResolverService.class),
                mock(ConfiguracaoDistribuicaoVaraService.class),
                mock(ProceduralCanonicalResolver.class)
        );
    }

    private static UnidadeJudiciariaCompetencia congestionada(String codigo, Long id) {
        UnidadeJudiciariaCompetencia unidade = unidade(codigo);
        ReflectionTestUtils.setField(unidade, "id", id);
        unidade.setProcessosAtivos(95);
        unidade.setIndiceCongestionamento(new BigDecimal("0.95"));
        return unidade;
    }

    private static UnidadeJudiciariaCompetencia livre(String codigo, Long id) {
        UnidadeJudiciariaCompetencia unidade = unidade(codigo);
        ReflectionTestUtils.setField(unidade, "id", id);
        return unidade;
    }
}
