package com.tcc.pjb.backend.service.competencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceRedistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.competencia.Comarca;
import com.tcc.pjb.backend.model.entity.competencia.ProcessoDistribuicaoCompetencia;
import com.tcc.pjb.backend.model.entity.competencia.StatusDistribuicaoCompetencia;
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
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
    void registrarCargaDaDistribuicaoAplicada_processoJaRoteado_gravaCargaNaUnidadeSemReescolher() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        UnidadeJudiciariaCompetencia unidade = unidade("VARA-10");
        when(unidadeRepository.findByCodigo("VARA-10")).thenReturn(Optional.of(unidade));
        when(distribuicaoRepository.findTopByProcesso_IdOrderByIdDesc(anyLong())).thenReturn(Optional.empty());
        MapaCompetenciaDinamicoEngine engine = engineCom(unidadeRepository, distribuicaoRepository);

        engine.registrarCargaDaDistribuicaoAplicada(processoRoteado(77L, "0009876-54.2026.8.06.0001", "VARA-10"));

        ArgumentCaptor<ProcessoDistribuicaoCompetencia> captor = ArgumentCaptor.forClass(ProcessoDistribuicaoCompetencia.class);
        verify(distribuicaoRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUnidade()).isSameAs(unidade);
        assertThat(captor.getValue().isDistribuicaoAutomatica()).isFalse();
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusDistribuicaoCompetencia.APLICADA);
    }

    @Test
    void registrarCargaDaDistribuicaoAplicada_processoJaTemDistribuicao_naoDuplica() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        when(distribuicaoRepository.findTopByProcesso_IdOrderByIdDesc(anyLong()))
                .thenReturn(Optional.of(mock(ProcessoDistribuicaoCompetencia.class)));
        MapaCompetenciaDinamicoEngine engine = engineCom(unidadeRepository, distribuicaoRepository);

        engine.registrarCargaDaDistribuicaoAplicada(processoRoteado(77L, "0009876-54.2026.8.06.0001", "VARA-10"));

        verify(distribuicaoRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registrarCargaDaDistribuicaoAplicada_unidadeNaoResolvidaPorCodigo_naoGrava() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        when(distribuicaoRepository.findTopByProcesso_IdOrderByIdDesc(anyLong())).thenReturn(Optional.empty());
        when(unidadeRepository.findByCodigo(anyString())).thenReturn(Optional.empty());
        MapaCompetenciaDinamicoEngine engine = engineCom(unidadeRepository, distribuicaoRepository);

        engine.registrarCargaDaDistribuicaoAplicada(processoRoteado(77L, "0009876-54.2026.8.06.0001", "VARA-INEXISTENTE"));

        verify(distribuicaoRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void registrarCargaDaDistribuicaoAplicada_processoSemNumeroUnificado_naoGrava() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        MapaCompetenciaDinamicoEngine engine = engineCom(unidadeRepository, distribuicaoRepository);

        engine.registrarCargaDaDistribuicaoAplicada(processoRoteado(77L, null, "VARA-10"));

        verify(distribuicaoRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    private static Processo processoRoteado(Long id, String nupn, String codigoUnidade) {
        Processo processo = new Processo();
        ReflectionTestUtils.setField(processo, "id", id);
        processo.setNumeroUnificado(nupn);
        processo.setUnidadeJudiciariaCodigo(codigoUnidade);
        return processo;
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

    @Test
    void mediaDeParesUsaOGrupoDaComarcaDoCadastroNaoAMediaGlobal() throws Exception {
        UnidadeJudiciariaCompetencia fortaleza1 = unidadeSemFkComComarcaTextual("VARA-FOR-1", "CE", "Fortaleza");
        UnidadeJudiciariaCompetencia fortaleza2 = unidadeSemFkComComarcaTextual("VARA-FOR-2", "CE", "Fortaleza");
        UnidadeJudiciariaCompetencia sobral = unidadeSemFkComComarcaTextual("VARA-SOB-1", "CE", "Sobral");
        ReflectionTestUtils.setField(fortaleza1, "id", 1L);
        ReflectionTestUtils.setField(fortaleza2, "id", 2L);
        ReflectionTestUtils.setField(sobral, "id", 3L);
        Map<Long, Long> recentes = Map.of(1L, 10L, 2L, 4L, 3L, 1L);

        Map<Long, Double> medias = invokeMediaDePares(criarEngine(), List.of(fortaleza1, fortaleza2, sobral), recentes, 5.0d);

        assertThat(medias.get(1L)).isEqualTo(7.0d);
        assertThat(medias.get(2L)).isEqualTo(7.0d);
        assertThat(medias.get(3L)).isEqualTo(1.0d);
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Double> invokeMediaDePares(MapaCompetenciaDinamicoEngine engine,
                                                        List<UnidadeJudiciariaCompetencia> unidades,
                                                        Map<Long, Long> recentes,
                                                        double mediaGlobal) throws Exception {
        java.lang.reflect.Method metodo = MapaCompetenciaDinamicoEngine.class.getDeclaredMethod(
                "mediaDeParesPorUnidade", List.class, Map.class, double.class);
        metodo.setAccessible(true);
        return (Map<Long, Double>) metodo.invoke(engine, unidades, recentes, mediaGlobal);
    }

    @Test
    void carregarDistribuicoesRecentesSomaTresJanelasComPesoDecrescente() throws Exception {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        when(distribuicaoRepository.contarRecentesPorUnidade(any(), any(java.time.Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 2L}));
        MapaCompetenciaDinamicoEngine engine = engineCom(unidadeRepository, distribuicaoRepository);
        UnidadeJudiciariaCompetencia unidade = unidade("VARA-1");
        ReflectionTestUtils.setField(unidade, "id", 1L);

        Map<Long, Long> pesos = invokeCarregarRecentes(engine, List.of(unidade));

        assertThat(pesos.get(1L)).isEqualTo(6L);
        ArgumentCaptor<java.time.Instant> captor = ArgumentCaptor.forClass(java.time.Instant.class);
        verify(distribuicaoRepository, times(3)).contarRecentesPorUnidade(any(), captor.capture());
        java.time.Instant agora = java.time.Instant.now();
        List<Long> idadesEmDias = captor.getAllValues().stream()
                .map(limite -> ChronoUnit.DAYS.between(limite, agora))
                .sorted()
                .toList();
        assertThat(idadesEmDias).containsExactly(1L, 3L, 7L);
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, Long> invokeCarregarRecentes(MapaCompetenciaDinamicoEngine engine,
                                                          List<UnidadeJudiciariaCompetencia> unidades) throws Exception {
        java.lang.reflect.Method metodo = MapaCompetenciaDinamicoEngine.class.getDeclaredMethod(
                "carregarDistribuicoesRecentes", List.class);
        metodo.setAccessible(true);
        return (Map<Long, Long>) metodo.invoke(engine, unidades);
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
        return engineCom(unidadeRepository, mock(ProcessoDistribuicaoCompetenciaRepository.class));
    }

    private static MapaCompetenciaDinamicoEngine engineCom(UnidadeJudiciariaCompetenciaRepository unidadeRepository,
                                                           ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository) {
        return new MapaCompetenciaDinamicoEngine(
                unidadeRepository,
                distribuicaoRepository,
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
