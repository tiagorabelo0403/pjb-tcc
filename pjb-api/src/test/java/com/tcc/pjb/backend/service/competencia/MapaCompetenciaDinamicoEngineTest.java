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
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionRequest;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
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
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
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
    void pontuacaoDeCargaComparaSoComParesDaComarcaEVaraUnicaFicaNeutra() throws Exception {
        UnidadeJudiciariaCompetencia fortaleza1 = unidadeSemFkComComarcaTextual("VARA-FOR-1", "CE", "Fortaleza");
        UnidadeJudiciariaCompetencia fortaleza2 = unidadeSemFkComComarcaTextual("VARA-FOR-2", "CE", "Fortaleza");
        UnidadeJudiciariaCompetencia sobral = unidadeSemFkComComarcaTextual("VARA-SOB-1", "CE", "Sobral");
        ReflectionTestUtils.setField(fortaleza1, "id", 1L);
        ReflectionTestUtils.setField(fortaleza2, "id", 2L);
        ReflectionTestUtils.setField(sobral, "id", 3L);
        Map<Long, Long> recentes = Map.of(1L, 10L, 2L, 4L, 3L, 1L);

        Map<Long, MapaCompetenciaDinamicoEngine.ReferenciaDePares> naComarca =
                invokeReferenciaDePares(criarEngine(), List.of(fortaleza1, fortaleza2, sobral), recentes, false);

        assertThat(naComarca.get(1L)).isEqualTo(referencia(7.0d, MapaCompetenciaDinamicoEngine.NivelDePares.COMARCA, 2));
        assertThat(naComarca.get(2L)).isEqualTo(referencia(7.0d, MapaCompetenciaDinamicoEngine.NivelDePares.COMARCA, 2));
        assertThat(naComarca.get(3L)).isEqualTo(referencia(1.0d, MapaCompetenciaDinamicoEngine.NivelDePares.COMARCA, 1));
    }

    @Test
    void leituraDeGestaoEscalaVaraUnicaParaAUfEDepoisParaOPais() throws Exception {
        UnidadeJudiciariaCompetencia fortaleza1 = unidadeSemFkComComarcaTextual("VARA-FOR-1", "CE", "Fortaleza");
        UnidadeJudiciariaCompetencia fortaleza2 = unidadeSemFkComComarcaTextual("VARA-FOR-2", "CE", "Fortaleza");
        UnidadeJudiciariaCompetencia sobral = unidadeSemFkComComarcaTextual("VARA-SOB-1", "CE", "Sobral");
        UnidadeJudiciariaCompetencia rioBranco = unidadeSemFkComComarcaTextual("VARA-RBR-1", "AC", "Rio Branco");
        ReflectionTestUtils.setField(fortaleza1, "id", 1L);
        ReflectionTestUtils.setField(fortaleza2, "id", 2L);
        ReflectionTestUtils.setField(sobral, "id", 3L);
        ReflectionTestUtils.setField(rioBranco, "id", 4L);
        Map<Long, Long> recentes = Map.of(1L, 10L, 2L, 4L, 3L, 1L, 4L, 9L);

        Map<Long, MapaCompetenciaDinamicoEngine.ReferenciaDePares> gestao =
                invokeReferenciaDePares(criarEngine(), List.of(fortaleza1, fortaleza2, sobral, rioBranco), recentes, true);

        assertThat(gestao.get(3L)).isEqualTo(referencia(5.0d, MapaCompetenciaDinamicoEngine.NivelDePares.UF, 3));
        assertThat(gestao.get(4L)).isEqualTo(referencia(6.0d, MapaCompetenciaDinamicoEngine.NivelDePares.NACIONAL, 4));
    }

    @Test
    void varaInaptaEVaraDeOutraJusticaNaoContamComoPares() throws Exception {
        UnidadeJudiciariaCompetencia ativa = unidadeSemFkComComarcaTextual("VARA-FOR-1", "CE", "Fortaleza");
        UnidadeJudiciariaCompetencia suspensa = unidadeSemFkComComarcaTextual("VARA-FOR-2", "CE", "Fortaleza");
        suspensa.setAceitaDistribuicao(false);
        UnidadeJudiciariaCompetencia federal = unidadeSemFkComComarcaTextual("VARA-FED-1", "CE", "Fortaleza");
        ReflectionTestUtils.setField(federal, "tipoJustica", TipoJustica.FEDERAL);
        ReflectionTestUtils.setField(ativa, "id", 1L);
        ReflectionTestUtils.setField(suspensa, "id", 2L);
        ReflectionTestUtils.setField(federal, "id", 3L);

        Map<Long, MapaCompetenciaDinamicoEngine.ReferenciaDePares> gestao =
                invokeReferenciaDePares(criarEngine(), List.of(ativa, suspensa, federal), Map.of(1L, 8L, 2L, 0L, 3L, 2L), true);

        assertThat(gestao.get(1L)).isEqualTo(referencia(8.0d, MapaCompetenciaDinamicoEngine.NivelDePares.COMARCA, 1));
        assertThat(gestao).doesNotContainKey(2L);
    }

    @Test
    void relatorioDeGestaoApontaVaraComCargaMuitoAcimaDosParesSemPropostaDeRedistribuicao() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        UnidadeJudiciariaCompetencia sobrecarregada = livre("VARA-01", 1L);
        UnidadeJudiciariaCompetencia par = livre("VARA-02", 2L);
        UnidadeJudiciariaCompetencia outroPar = livre("VARA-03", 3L);
        when(unidadeRepository.findAll()).thenReturn(List.of(sobrecarregada, par, outroPar));
        when(distribuicaoRepository.contarRecentesPorUnidade(any(), any(java.time.Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 12L}, new Object[]{2L, 2L}, new Object[]{3L, 1L}));
        MapaCompetenciaDinamicoEngine engine = engineCom(unidadeRepository, distribuicaoRepository);

        DynamicCompetenceRedistributionResponse resposta = engine.analisarRedistribuicao(0.95d);

        assertThat(resposta.propostas()).isEmpty();
        assertThat(resposta.saturacoes())
                .extracting(DynamicCompetenceRedistributionResponse.Saturacao::unidadeCodigo,
                        DynamicCompetenceRedistributionResponse.Saturacao::nivelDosPares,
                        DynamicCompetenceRedistributionResponse.Saturacao::quantidadeDePares)
                .containsExactly(tuple("VARA-01", "COMARCA", 3));
        assertThat(resposta.saturacoes().getFirst().razaoSobreOsPares()).isEqualTo(2.4d);
    }

    @Test
    void varaUnicaCompetenteDaComarcaSobrecarregadaNaoPerdeOProcessoParaVaraDeOutraComarca() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        ConfiguracaoDistribuicaoVaraService configuracao = mock(ConfiguracaoDistribuicaoVaraService.class);
        ProceduralCanonicalResolver canonico = mock(ProceduralCanonicalResolver.class);
        when(configuracao.consultar(anyString(), any())).thenAnswer(inv -> new ConfiguracaoDistribuicaoVaraService.ResultadoConsulta(
                inv.getArgument(0), true, null, null, List.of(), java.time.Instant.now()));
        when(canonico.resolve(any())).thenReturn(new ProceduralCanonicalResolver.CanonicalContext());
        UnidadeJudiciariaCompetencia sobral = unidadeSemFkComComarcaTextual("VARA-SOB-1", "CE", "Sobral");
        UnidadeJudiciariaCompetencia fortaleza1 = unidadeSemFkComComarcaTextual("VARA-FOR-1", "CE", "Fortaleza");
        UnidadeJudiciariaCompetencia fortaleza2 = unidadeSemFkComComarcaTextual("VARA-FOR-2", "CE", "Fortaleza");
        ReflectionTestUtils.setField(sobral, "id", 3L);
        ReflectionTestUtils.setField(fortaleza1, "id", 1L);
        ReflectionTestUtils.setField(fortaleza2, "id", 2L);
        when(unidadeRepository.findAll()).thenReturn(List.of(sobral, fortaleza1, fortaleza2));
        when(distribuicaoRepository.contarRecentesPorUnidade(any(), any(java.time.Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[]{3L, 30L}, new Object[]{1L, 1L}, new Object[]{2L, 1L}));
        MapaCompetenciaDinamicoEngine engine = new MapaCompetenciaDinamicoEngine(
                unidadeRepository, distribuicaoRepository, mock(ProcessoRepository.class), mock(AuditLedgerService.class),
                mock(OutboxPublisher.class), configuracao, canonico, inferencia(canonico, mock(CompetenceResolverService.class)));

        var resposta = engine.distribuir(new DynamicCompetenceDistributionRequest(
                null, null, null, "CIVIL", null, "CE", "Sobral", "CE", "Sobral",
                false, false, null, "ESTADUAL", false, false, null)).orElseThrow();

        assertThat(resposta.unidadeCodigo()).isEqualTo("VARA-SOB-1");
        assertThat(resposta.scoreBreakdown().equilibrio()).isEqualTo(7.0d);
    }

    @Test
    void processoDeRitoComumNaoVaiParaJuizadoEspecialExclusivoMesmoComMelhorPontuacao() {
        UnidadeJudiciariaCompetencia jec = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-CAP", "CE", "Fortaleza");
        jec.setExclusivoJuizadoEspecial(true);
        jec.setPrioridadeEstrategica(95);
        UnidadeJudiciariaCompetencia civel = unidadeSemFkComComarcaTextual("TJCE-CIVEL-CE-CAP", "CE", "Fortaleza");
        ReflectionTestUtils.setField(jec, "id", 1L);
        ReflectionTestUtils.setField(civel, "id", 2L);

        assertThat(distribuirEntre(List.of(jec, civel), false).unidadeCodigo()).isEqualTo("TJCE-CIVEL-CE-CAP");
        assertThat(distribuirEntre(List.of(jec, civel), true).unidadeCodigo()).isEqualTo("TJCE-JEC-CE-CAP");
    }

    private static DynamicCompetenceDistributionResponse distribuirEntre(
            List<UnidadeJudiciariaCompetencia> unidades, boolean requerJuizado) {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ConfiguracaoDistribuicaoVaraService configuracao = mock(ConfiguracaoDistribuicaoVaraService.class);
        ProceduralCanonicalResolver canonico = mock(ProceduralCanonicalResolver.class);
        when(unidadeRepository.findAll()).thenReturn(unidades);
        when(configuracao.consultar(anyString(), any())).thenAnswer(inv -> new ConfiguracaoDistribuicaoVaraService.ResultadoConsulta(
                inv.getArgument(0), true, null, null, List.of(), java.time.Instant.now()));
        when(canonico.resolve(any())).thenReturn(new ProceduralCanonicalResolver.CanonicalContext());
        MapaCompetenciaDinamicoEngine engine = new MapaCompetenciaDinamicoEngine(
                unidadeRepository, mock(ProcessoDistribuicaoCompetenciaRepository.class), mock(ProcessoRepository.class),
                mock(AuditLedgerService.class), mock(OutboxPublisher.class), configuracao, canonico, inferencia(canonico, mock(CompetenceResolverService.class)));
        return engine.distribuir(new DynamicCompetenceDistributionRequest(
                null, null, null, "CIVIL", null, "CE", "Fortaleza", "CE", "Fortaleza",
                requerJuizado, false, null, "ESTADUAL", false, false, null)).orElseThrow();
    }

    @Test
    void distribuicaoMinimaDeProcessoDeRitoComumNaoCaiNoJuizadoExclusivoDeMaiorPrioridade() {
        UnidadeJudiciariaCompetencia jec = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-CAP", "CE", "Fortaleza");
        jec.setExclusivoJuizadoEspecial(true);
        jec.setPrioridadeEstrategica(95);
        UnidadeJudiciariaCompetencia civel = unidadeSemFkComComarcaTextual("TJCE-CIVEL-CE-CAP", "CE", "Fortaleza");
        civel.setPrioridadeEstrategica(80);

        assertThat(registrarSoPelaDistribuicaoMinima(List.of(jec, civel), RitoProcessual.COMUM_ORDINARIO, RamoDireito.CIVIL, mock(AuditLedgerService.class)).orElseThrow().unidadeCodigo())
                .isEqualTo("TJCE-CIVEL-CE-CAP");
    }

    @Test
    void processoDeJuizadoEspecialCriminalSegueParaOJecrimMesmoNaDistribuicaoMinima() {
        assertThat(registrarSoPelaDistribuicaoMinima(varasCriminais(), RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL, RamoDireito.PENAL, mock(AuditLedgerService.class))
                .orElseThrow().unidadeCodigo())
                .isEqualTo("TJCE-JECRIM-CE-CAP");
    }

    @Test
    void ritoSumarissimoPenalDeMenorPotencialOfensivoTambemSegueParaOJecrim() {
        assertThat(registrarSoPelaDistribuicaoMinima(varasCriminais(), RitoProcessual.PROCEDIMENTO_PENAL_SUMARISSIMO, RamoDireito.PENAL, mock(AuditLedgerService.class))
                .orElseThrow().unidadeCodigo())
                .isEqualTo("TJCE-JECRIM-CE-CAP");
    }

    @Test
    void juizadoNaoInstaladoNaComarcaVaiParaVaraComumDeMesmaNaturezaComRegistroAuditavel() {
        UnidadeJudiciariaCompetencia civel = unidadeSemFkComComarcaTextual("TJCE-CIVEL-CE-CAP", "CE", "Fortaleza");
        AuditLedgerService auditoria = mock(AuditLedgerService.class);

        assertThat(registrarSoPelaDistribuicaoMinima(List.of(civel), RitoProcessual.JUIZADO_ESPECIAL_CIVEL, RamoDireito.CIVIL, auditoria)
                .orElseThrow().unidadeCodigo()).isEqualTo("TJCE-CIVEL-CE-CAP");
        verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_JUIZADO_EM_UNIDADE_COMUM"),
                org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("90"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.contains("unidade=TJCE-CIVEL-CE-CAP"));
    }

    @Test
    void processoDeJuizadoSemNenhumaUnidadeDeMesmaNaturezaFicaRegistradoParaRevisaoEmVezDeSumir() {
        UnidadeJudiciariaCompetencia criminal = varaCriminal("TJCE-CRIMINAL-CE-CAP");
        AuditLedgerService auditoria = mock(AuditLedgerService.class);

        assertThat(registrarSoPelaDistribuicaoMinima(List.of(criminal), RitoProcessual.JUIZADO_ESPECIAL_CIVEL, RamoDireito.CIVIL, auditoria)).isEmpty();
        verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_JUIZADO_SEM_DESTINO"),
                org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("90"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.contains("rito=JUIZADO_ESPECIAL_CIVEL"));
    }

    @Test
    void processoDeJuizadoCriminalSemJecrimNaComarcaVaiParaVaraCriminalENuncaParaOJuizadoCivel() {
        UnidadeJudiciariaCompetencia jecCivel = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-CAP", "CE", "Fortaleza");
        jecCivel.setExclusivoJuizadoEspecial(true);
        jecCivel.setPrioridadeEstrategica(99);
        List<UnidadeJudiciariaCompetencia> unidades = List.of(varaCriminal("TJCE-CRIMINAL-CE-CAP"), jecCivel);

        for (RamoDireito ramo : java.util.Arrays.asList(RamoDireito.PENAL, RamoDireito.CIVIL, null)) {
            assertThat(registrarSoPelaDistribuicaoMinima(unidades, RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL, ramo, mock(AuditLedgerService.class))
                    .orElseThrow().unidadeCodigo()).isEqualTo("TJCE-CRIMINAL-CE-CAP");
        }
    }

    @Test
    void juizadoCriminalComRamoCivilOuAusenteEscolheOJecrimEmVezDoJecCivelNoCaminhoCompleto() {
        UnidadeJudiciariaCompetencia jecCivel = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-CAP", "CE", "Fortaleza");
        jecCivel.setExclusivoJuizadoEspecial(true);
        jecCivel.setPrioridadeEstrategica(99);
        ReflectionTestUtils.setField(jecCivel, "id", 1L);
        List<UnidadeJudiciariaCompetencia> unidades = new java.util.ArrayList<>(varasCriminais());
        unidades.add(jecCivel);

        for (RamoDireito ramo : java.util.Arrays.asList(RamoDireito.CIVIL, null)) {
            assertThat(registrarDistribuicaoInicial(unidades, RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL, ramo, TipoJustica.ESTADUAL,
                    mock(AuditLedgerService.class), true).orElseThrow().unidadeCodigo()).isEqualTo("TJCE-JECRIM-CE-CAP");
        }
    }

    @Test
    void juizadoCivelChegaAoJecMesmoQuandoORamoDaUnidadeDifereDoRamoDoProcesso() {
        UnidadeJudiciariaCompetencia jec = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-CAP", "CE", "Fortaleza");
        ReflectionTestUtils.setField(jec, "ramoDireito", RamoDireito.CONSUMIDOR);
        jec.setExclusivoJuizadoEspecial(true);
        jec.setPrioridadeEstrategica(10);
        UnidadeJudiciariaCompetencia civel = unidadeSemFkComComarcaTextual("TJCE-CIVEL-CE-CAP", "CE", "Fortaleza");
        civel.setPrioridadeEstrategica(90);

        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            assertThat(registrarDistribuicaoInicial(List.of(jec, civel), RitoProcessual.JUIZADO_ESPECIAL_CIVEL, RamoDireito.CIVIL,
                    TipoJustica.ESTADUAL, mock(AuditLedgerService.class), caminhoCompleto).orElseThrow().unidadeCodigo())
                    .isEqualTo("TJCE-JEC-CE-CAP");
        }
    }

    @Test
    void juizadoFederalNaoPrevidenciarioChegaAoJefEmVezDaVaraFederalComum() {
        UnidadeJudiciariaCompetencia jef = unidadeFederal("JFCE-JEF-CE-CAP", RamoDireito.PREVIDENCIARIO, 10);
        jef.setPermiteJuizadoEspecial(true);
        UnidadeJudiciariaCompetencia federal = unidadeFederal("JFCE-FEDERAL-CE-CAP", RamoDireito.TRIBUTARIO, 90);

        assertThat(registrarDistribuicaoInicial(List.of(jef, federal), RitoProcessual.JUIZADO_ESPECIAL_FEDERAL, RamoDireito.CIVIL,
                TipoJustica.FEDERAL, mock(AuditLedgerService.class), false).orElseThrow().unidadeCodigo())
                .isEqualTo("JFCE-JEF-CE-CAP");
    }

    @Test
    void juizadoDaFazendaPublicaNaoInstaladoVaiParaAVaraDaFazendaENuncaParaOJecCivel() {
        UnidadeJudiciariaCompetencia jec = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-CAP", "CE", "Fortaleza");
        jec.setExclusivoJuizadoEspecial(true);
        jec.setPrioridadeEstrategica(99);
        UnidadeJudiciariaCompetencia fazenda = unidadeSemFkComComarcaTextual("TJCE-FAZENDA-CE-CAP", "CE", "Fortaleza");
        ReflectionTestUtils.setField(fazenda, "tipoVara", TipoVaraDistribuicao.FAZENDA_PUBLICA);
        ReflectionTestUtils.setField(fazenda, "ramoDireito", RamoDireito.ADMINISTRATIVO);
        fazenda.setPrioridadeEstrategica(10);

        assertThat(registrarSoPelaDistribuicaoMinima(List.of(jec, fazenda), RitoProcessual.JUIZADO_ESPECIAL_FAZENDA_PUBLICA, RamoDireito.CIVIL,
                mock(AuditLedgerService.class)).orElseThrow().unidadeCodigo()).isEqualTo("TJCE-FAZENDA-CE-CAP");
    }

    @Test
    void redistribuicaoDeVaraQueProcessaJuizadoSoPropoeDestinoQueTambemProcessaJuizado() {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        UnidadeJudiciariaCompetencia adjunta = congestionada("VARA-ADJUNTA", 1L);
        adjunta.setPermiteJuizadoEspecial(true);
        UnidadeJudiciariaCompetencia semJuizado = livre("VARA-SEM-JUIZADO", 2L);
        UnidadeJudiciariaCompetencia exclusiva = livre("VARA-JEC", 3L);
        exclusiva.setExclusivoJuizadoEspecial(true);
        UnidadeJudiciariaCompetencia outraAdjunta = livre("VARA-OUTRA-ADJUNTA", 4L);
        outraAdjunta.setPermiteJuizadoEspecial(true);
        outraAdjunta.setPrioridadeEstrategica(1);
        when(unidadeRepository.findAll()).thenReturn(List.of(adjunta, semJuizado, exclusiva, outraAdjunta));

        assertThat(engineCom(unidadeRepository).analisarRedistribuicao(0.95d).propostas())
                .extracting(DynamicCompetenceRedistributionResponse.Proposal::origemCodigo,
                        DynamicCompetenceRedistributionResponse.Proposal::destinoCodigo)
                .containsExactly(tuple("VARA-ADJUNTA", "VARA-OUTRA-ADJUNTA"));
    }

    @Test
    void juizadoDeOutraComarcaNaoVenceAVaraComumDaPropriaComarcaEADistribuicaoExigeConferenciaHumana() {
        UnidadeJudiciariaCompetencia jecSobral = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-SOB", "CE", "Sobral");
        jecSobral.setExclusivoJuizadoEspecial(true);
        jecSobral.setPrioridadeEstrategica(99);
        ReflectionTestUtils.setField(jecSobral, "id", 1L);
        UnidadeJudiciariaCompetencia civelFortaleza = unidadeSemFkComComarcaTextual("TJCE-CIVEL-CE-CAP", "CE", "Fortaleza");
        civelFortaleza.setPrioridadeEstrategica(10);
        ReflectionTestUtils.setField(civelFortaleza, "id", 2L);
        AuditLedgerService auditoria = mock(AuditLedgerService.class);

        DynamicCompetenceDistributionResponse resposta = registrarDistribuicaoInicial(List.of(jecSobral, civelFortaleza),
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL, RamoDireito.CIVIL, TipoJustica.ESTADUAL, auditoria, true).orElseThrow();

        assertThat(resposta.unidadeCodigo()).isEqualTo("TJCE-CIVEL-CE-CAP");
        assertThat(resposta.distribuicaoAutomatica()).isFalse();
        assertThat(resposta.fatoresRevisaoHumana()).anyMatch(fator -> fator.startsWith("Juizado especial nao instalado"));
        verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_JUIZADO_EM_UNIDADE_COMUM"),
                org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("90"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.contains("unidade=TJCE-CIVEL-CE-CAP"));
    }

    @Test
    void juizadoCriminalNuncaVaiParaVaraDoJuriNemDeViolenciaDomestica() {
        UnidadeJudiciariaCompetencia juri = varaCriminal("TJCE-JURI-CE-CAP");
        ReflectionTestUtils.setField(juri, "tipoVara", TipoVaraDistribuicao.TRIBUNAL_JURI);
        UnidadeJudiciariaCompetencia violencia = varaCriminal("TJCE-VD-CE-CAP");
        ReflectionTestUtils.setField(violencia, "tipoVara", TipoVaraDistribuicao.VIOLENCIA_DOMESTICA);
        AuditLedgerService auditoria = mock(AuditLedgerService.class);

        assertThat(registrarSoPelaDistribuicaoMinima(List.of(juri, violencia), RitoProcessual.JUIZADO_ESPECIAL_CRIMINAL, RamoDireito.PENAL, auditoria)).isEmpty();
        verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_JUIZADO_SEM_DESTINO"),
                org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("90"),
                org.mockito.ArgumentMatchers.isNull(), anyString());
    }

    @Test
    void previdenciarioDeJefEmCompetenciaDelegadaNaoCaiNoJuizadoEstadual() {
        UnidadeJudiciariaCompetencia jec = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-CAP", "CE", "Fortaleza");
        jec.setExclusivoJuizadoEspecial(true);
        jec.setPrioridadeEstrategica(99);
        UnidadeJudiciariaCompetencia civel = unidadeSemFkComComarcaTextual("TJCE-CIVEL-CE-CAP", "CE", "Fortaleza");
        civel.setPrioridadeEstrategica(10);

        assertThat(registrarSoPelaDistribuicaoMinima(List.of(jec, civel), RitoProcessual.PREVIDENCIARIO_JEF, RamoDireito.PREVIDENCIARIO,
                mock(AuditLedgerService.class)).orElseThrow().unidadeCodigo()).isEqualTo("TJCE-CIVEL-CE-CAP");
    }

    @Test
    void distribuicaoPreviaDoAjuizamentoUsaORitoParaChegarAoJecDeOutroRamo() {
        UnidadeJudiciariaCompetencia jec = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-CAP", "CE", "Fortaleza");
        ReflectionTestUtils.setField(jec, "ramoDireito", RamoDireito.CONSUMIDOR);
        jec.setExclusivoJuizadoEspecial(true);
        ReflectionTestUtils.setField(jec, "id", 1L);
        UnidadeJudiciariaCompetencia civel = unidadeSemFkComComarcaTextual("TJCE-CIVEL-CE-CAP", "CE", "Fortaleza");
        civel.setPrioridadeEstrategica(99);
        ReflectionTestUtils.setField(civel, "id", 2L);

        assertThat(distribuirPeloAjuizamento(List.of(jec, civel), "Fortaleza", RitoProcessual.JUIZADO_ESPECIAL_CIVEL)
                .orElseThrow().unidadeCodigo()).isEqualTo("TJCE-JEC-CE-CAP");
    }

    @Test
    void juizadoDoInteriorSemUnidadeNaComarcaChegaAoJuizadoDaCapitalPelaRotaDoAjuizamento() {
        UnidadeJudiciariaCompetencia jecCapital = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-CAP", "CE", "Fortaleza");
        jecCapital.setExclusivoJuizadoEspecial(true);
        ReflectionTestUtils.setField(jecCapital, "id", 1L);

        assertThat(distribuirPeloAjuizamento(List.of(jecCapital), "Quixada", RitoProcessual.JUIZADO_ESPECIAL_CIVEL)
                .orElseThrow().unidadeCodigo()).isEqualTo("TJCE-JEC-CE-CAP");
    }

    @Test
    void rotaDoAjuizamentoPrefereAVaraComumLocalAoJuizadoDeOutraComarcaEPedeConferencia() {
        UnidadeJudiciariaCompetencia jecSobral = unidadeSemFkComComarcaTextual("TJCE-JEC-CE-SOB", "CE", "Sobral");
        jecSobral.setExclusivoJuizadoEspecial(true);
        jecSobral.setPrioridadeEstrategica(99);
        ReflectionTestUtils.setField(jecSobral, "id", 1L);
        UnidadeJudiciariaCompetencia civelFortaleza = unidadeSemFkComComarcaTextual("TJCE-CIVEL-CE-CAP", "CE", "Fortaleza");
        ReflectionTestUtils.setField(civelFortaleza, "id", 2L);

        DynamicCompetenceDistributionResponse resposta = distribuirPeloAjuizamento(List.of(jecSobral, civelFortaleza), "Fortaleza",
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL).orElseThrow();

        assertThat(resposta.unidadeCodigo()).isEqualTo("TJCE-CIVEL-CE-CAP");
        assertThat(resposta.distribuicaoAutomatica()).isFalse();
        assertThat(resposta.fatoresRevisaoHumana()).anyMatch(fator -> fator.startsWith("Juizado especial nao instalado"));
    }

    private static Optional<DynamicCompetenceDistributionResponse> distribuirPeloAjuizamento(List<UnidadeJudiciariaCompetencia> unidades,
                                                                                            String comarca,
                                                                                            RitoProcessual rito) {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ConfiguracaoDistribuicaoVaraService configuracao = mock(ConfiguracaoDistribuicaoVaraService.class);
        ProceduralCanonicalResolver canonico = mock(ProceduralCanonicalResolver.class);
        when(unidadeRepository.findAll()).thenReturn(unidades);
        when(configuracao.consultar(anyString(), any())).thenAnswer(inv -> new ConfiguracaoDistribuicaoVaraService.ResultadoConsulta(
                inv.getArgument(0), true, null, null, List.of(), java.time.Instant.now()));
        when(canonico.resolve(any())).thenReturn(new ProceduralCanonicalResolver.CanonicalContext());
        MapaCompetenciaDinamicoEngine engine = new MapaCompetenciaDinamicoEngine(
                unidadeRepository, mock(ProcessoDistribuicaoCompetenciaRepository.class), mock(ProcessoRepository.class),
                mock(AuditLedgerService.class), mock(OutboxPublisher.class), configuracao, canonico, inferencia(canonico, mock(CompetenceResolverService.class)));
        return engine.distribuir(new DynamicCompetenceDistributionRequest(
                null, null, null, "CIVIL", null, "CE", comarca, "CE", comarca,
                false, false, "CIVIL", "ESTADUAL", false, false, null), rito);
    }

    @Test
    void cargaAplicadaDeJuizadoEmVaraComumFicaMarcadaParaConferenciaEAuditada() {
        UnidadeJudiciariaCompetencia civel = unidade("VARA-CIVEL");
        AuditLedgerService auditoria = mock(AuditLedgerService.class);

        ProcessoDistribuicaoCompetencia gravada = registrarCargaAplicada(civel, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, auditoria);

        assertThat(ReflectionTestUtils.getField(gravada, "fatoresRevisao")).asString().startsWith("Juizado especial nao instalado");
        verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_JUIZADO_EM_UNIDADE_COMUM"),
                org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("77"),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.contains("unidade=VARA-CIVEL"));
    }

    @Test
    void cargaAplicadaDeRitoComumEmJuizadoExclusivoFicaMarcadaComoIncompativel() {
        UnidadeJudiciariaCompetencia jec = unidade("VARA-JEC");
        jec.setExclusivoJuizadoEspecial(true);
        AuditLedgerService auditoria = mock(AuditLedgerService.class);

        ProcessoDistribuicaoCompetencia gravada = registrarCargaAplicada(jec, RitoProcessual.COMUM_ORDINARIO, auditoria);

        assertThat(ReflectionTestUtils.getField(gravada, "fatoresRevisao")).asString().startsWith("Unidade aplicada antes do commit");
        verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_APLICADA_INCOMPATIVEL_COM_JUIZADO"),
                org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("77"),
                org.mockito.ArgumentMatchers.isNull(), anyString());
    }

    @Test
    void reprocessamentoDeCargaAplicadaQueExigiaConferenciaDevolveAConferenciaDeNovo() {
        UnidadeJudiciariaCompetencia civel = unidade("VARA-CIVEL");
        Processo processo = processoRoteado(77L, "0009876-54.2026.8.06.0001", "VARA-CIVEL");
        ProcessoDistribuicaoCompetencia jaGravada = new ProcessoDistribuicaoCompetencia(processo, civel, "0009876-54.2026.8.06.0001",
                0d, 0d, 0d, 0d, 0d, 0d, false, StatusDistribuicaoCompetencia.APLICADA, "aplicada", "",
                "Juizado especial nao instalado ao alcance do processo; conferir", "hash");
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        when(distribuicaoRepository.findTopByProcesso_IdOrderByIdDesc(77L)).thenReturn(Optional.of(jaGravada));
        MapaCompetenciaDinamicoEngine engine = engineCom(mock(UnidadeJudiciariaCompetenciaRepository.class), distribuicaoRepository);

        assertThat(engine.registrarCargaDaDistribuicaoAplicada(processo)).hasValueSatisfying(resposta -> {
            assertThat(resposta.distribuicaoAutomatica()).isFalse();
            assertThat(resposta.unidadeCodigo()).isEqualTo("VARA-CIVEL");
        });
        verify(distribuicaoRepository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void falhaDoResolverCanonicoNaoImpedeRegistrarACargaNemAConferenciaPeloRitoDoProcesso() {
        UnidadeJudiciariaCompetencia civel = unidade("VARA-CIVEL");
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        when(unidadeRepository.findByCodigo("VARA-CIVEL")).thenReturn(Optional.of(civel));
        when(distribuicaoRepository.findTopByProcesso_IdOrderByIdDesc(anyLong())).thenReturn(Optional.empty());
        ProceduralCanonicalResolver canonico = mock(ProceduralCanonicalResolver.class);
        when(canonico.resolve(any())).thenThrow(new IllegalStateException("classe TPU invalida"));
        MapaCompetenciaDinamicoEngine engine = new MapaCompetenciaDinamicoEngine(
                unidadeRepository, distribuicaoRepository, mock(ProcessoRepository.class), mock(AuditLedgerService.class),
                mock(OutboxPublisher.class), mock(ConfiguracaoDistribuicaoVaraService.class), canonico, inferencia(canonico, mock(CompetenceResolverService.class)));
        Processo processo = processoRoteado(77L, "0009876-54.2026.8.06.0001", "VARA-CIVEL");
        processo.setRito(RitoProcessual.JUIZADO_ESPECIAL_CIVEL);

        assertThat(engine.registrarCargaDaDistribuicaoAplicada(processo)).hasValueSatisfying(resposta ->
                assertThat(resposta.distribuicaoAutomatica()).isFalse());
        verify(distribuicaoRepository).saveAndFlush(any(ProcessoDistribuicaoCompetencia.class));
    }

    @Test
    void cargaAplicadaCompativelNaoGeraConferenciaNemAuditoria() {
        UnidadeJudiciariaCompetencia jec = unidade("VARA-JEC");
        jec.setExclusivoJuizadoEspecial(true);
        AuditLedgerService auditoria = mock(AuditLedgerService.class);

        ProcessoDistribuicaoCompetencia gravada = registrarCargaAplicada(jec, RitoProcessual.JUIZADO_ESPECIAL_CIVEL, auditoria);

        assertThat(ReflectionTestUtils.getField(gravada, "fatoresRevisao")).isEqualTo("");
        org.mockito.Mockito.verifyNoInteractions(auditoria);
    }

    private static ProcessoDistribuicaoCompetencia registrarCargaAplicada(UnidadeJudiciariaCompetencia unidade,
                                                                         RitoProcessual rito,
                                                                         AuditLedgerService auditoria) {
        return registrarCargaAplicada(unidade, List.of(unidade), rito, TipoJustica.ESTADUAL, null, auditoria);
    }

    private static ProcessoDistribuicaoCompetencia registrarCargaAplicada(UnidadeJudiciariaCompetencia unidade,
                                                                         List<UnidadeJudiciariaCompetencia> malha,
                                                                         RitoProcessual rito,
                                                                         TipoJustica tipoJustica,
                                                                         BigDecimal valorCausa,
                                                                         AuditLedgerService auditoria) {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        when(unidadeRepository.findByCodigo(unidade.getCodigo())).thenReturn(Optional.of(unidade));
        when(unidadeRepository.findAll()).thenReturn(malha);
        when(distribuicaoRepository.findTopByProcesso_IdOrderByIdDesc(anyLong())).thenReturn(Optional.empty());
        ProceduralCanonicalResolver canonico = mock(ProceduralCanonicalResolver.class);
        when(canonico.resolve(any())).thenReturn(new ProceduralCanonicalResolver.CanonicalContext());
        MapaCompetenciaDinamicoEngine engine = new MapaCompetenciaDinamicoEngine(
                unidadeRepository, distribuicaoRepository, mock(ProcessoRepository.class), auditoria,
                mock(OutboxPublisher.class), mock(ConfiguracaoDistribuicaoVaraService.class), canonico, inferencia(canonico, mock(CompetenceResolverService.class)));
        Processo processo = processoRoteado(77L, "0009876-54.2026.8.06.0001", unidade.getCodigo());
        processo.setRito(rito);
        processo.setTipoJustica(tipoJustica);
        processo.setValorCausa(valorCausa);
        processo.setUf("CE");
        processo.setComarca("Fortaleza");

        Optional<DynamicCompetenceDistributionResponse> devolvida = engine.registrarCargaDaDistribuicaoAplicada(processo);

        ArgumentCaptor<ProcessoDistribuicaoCompetencia> captor = ArgumentCaptor.forClass(ProcessoDistribuicaoCompetencia.class);
        verify(distribuicaoRepository).saveAndFlush(captor.capture());
        String fatorGravado = String.valueOf(ReflectionTestUtils.getField(captor.getValue(), "fatoresRevisao"));
        if (fatorGravado.isEmpty()) {
            assertThat(devolvida).isEmpty();
        } else {
            assertThat(devolvida).hasValueSatisfying(resposta -> {
                assertThat(resposta.distribuicaoAutomatica()).isFalse();
                assertThat(resposta.fatoresRevisaoHumana()).containsExactly(fatorGravado);
            });
        }
        return captor.getValue();
    }

    private static com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService salarioMinimo() {
        com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService salarioMinimo =
                mock(com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService.class);
        when(salarioMinimo.multiplicar(any(BigDecimal.class), any(java.time.LocalDate.class)))
                .thenAnswer(inv -> new BigDecimal(inv.<java.time.LocalDate>getArgument(1).getYear() < 2026 ? "1518.00" : "1621.00")
                        .multiply(inv.getArgument(0)));
        return salarioMinimo;
    }

    @Test
    void previdenciarioFederalDentroDaAlcadaVaiAoJefPelaCompetenciaAbsoluta() {
        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.PREVIDENCIARIO_BPC_LOAS, RamoDireito.PREVIDENCIARIO,
                    TipoJustica.FEDERAL, new BigDecimal("50000.00"), mock(AuditLedgerService.class), caminhoCompleto)
                    .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-JEF-CE-CAP");
        }
    }

    @Test
    void previdenciarioFederalAcimaDaAlcadaVaiAVaraFederalComumENaoAoJefExclusivo() {
        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.PREVIDENCIARIO_APOSENTADORIA, RamoDireito.PREVIDENCIARIO,
                    TipoJustica.FEDERAL, new BigDecimal("150000.00"), mock(AuditLedgerService.class), caminhoCompleto)
                    .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
        }
    }

    @Test
    void acidentarioNaJusticaFederalNaoVaiAoJefEPedeConferenciaDaCompetenciaEstadual() {
        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            AuditLedgerService auditoria = mock(AuditLedgerService.class);
            DynamicCompetenceDistributionResponse resposta = registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.PREVIDENCIARIO_ACIDENTARIO,
                    RamoDireito.PREVIDENCIARIO, TipoJustica.FEDERAL, new BigDecimal("20000.00"), auditoria, caminhoCompleto).orElseThrow();

            assertThat(resposta.unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
            assertThat(resposta.distribuicaoAutomatica()).isFalse();
            assertThat(resposta.fatoresRevisaoHumana()).anyMatch(fator -> fator.contains("Sumula 15 do STJ"));
            verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_ACIDENTARIO_NA_JUSTICA_FEDERAL"),
                    org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("90"),
                    org.mockito.ArgumentMatchers.isNull(), anyString());
        }
    }

    @Test
    void previdenciarioFederalDeRitoComumDentroDaAlcadaTambemVaiAoJef() {
        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.COMUM_ORDINARIO, RamoDireito.PREVIDENCIARIO,
                    TipoJustica.FEDERAL, new BigDecimal("50000.00"), mock(AuditLedgerService.class), caminhoCompleto)
                    .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-JEF-CE-CAP");
        }
    }

    @Test
    void mandadoDeSegurancaPrevidenciarioNaoVaiAoJefPorExclusaoLegal() {
        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.ESPECIAL_MANDADO_SEGURANCA, RamoDireito.PREVIDENCIARIO,
                    TipoJustica.FEDERAL, new BigDecimal("20000.00"), mock(AuditLedgerService.class), caminhoCompleto)
                    .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
        }
    }

    @Test
    void causaFederalComumDentroDaAlcadaDoJefFicaNaVaraComumComConferenciaDaCompetenciaAbsoluta() {
        for (RitoProcessual rito : List.of(RitoProcessual.COMUM_ORDINARIO, RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO,
                RitoProcessual.TRIBUTARIO_REPETICAO_INDEBITO, RitoProcessual.ADMINISTRATIVO_SERVIDORES)) {
            for (boolean caminhoCompleto : new boolean[]{true, false}) {
                AuditLedgerService auditoria = mock(AuditLedgerService.class);
                DynamicCompetenceDistributionResponse resposta = registrarDistribuicaoInicial(malhaFederal(), rito,
                        RamoDireito.TRIBUTARIO, TipoJustica.FEDERAL, new BigDecimal("20000.00"), auditoria, caminhoCompleto).orElseThrow();

                assertThat(resposta.unidadeCodigo()).as(rito.name()).isEqualTo("JFCE-FEDERAL-CE-CAP");
                assertThat(resposta.distribuicaoAutomatica()).as(rito.name()).isFalse();
                assertThat(resposta.fatoresRevisaoHumana()).as(rito.name()).containsOnlyOnce(fatorCausaFederalNaAlcada(resposta));
                verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_CAUSA_FEDERAL_NA_ALCADA_FORA_DO_JUIZADO"),
                        org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("90"),
                        org.mockito.ArgumentMatchers.isNull(), anyString());
            }
        }
    }

    @Test
    void causaFederalNaAlcadaSemJefInstaladoNaComarcaNaoPedeConferencia() {
        UnidadeJudiciariaCompetencia federal = unidadeFederal("JFCE-FEDERAL-CE-CAP", RamoDireito.TRIBUTARIO, 90);
        ReflectionTestUtils.setField(federal, "id", 2L);
        UnidadeJudiciariaCompetencia jefDeOutraComarca = unidadeSemFkComComarcaTextual("JFCE-JEF-CE-JUA", "CE", "Juazeiro do Norte");
        ReflectionTestUtils.setField(jefDeOutraComarca, "tipoJustica", TipoJustica.FEDERAL);
        jefDeOutraComarca.setExclusivoJuizadoEspecial(true);
        ReflectionTestUtils.setField(jefDeOutraComarca, "id", 3L);

        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            DynamicCompetenceDistributionResponse resposta = registrarDistribuicaoInicial(List.of(federal, jefDeOutraComarca),
                    RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO, RamoDireito.TRIBUTARIO, TipoJustica.FEDERAL,
                    new BigDecimal("20000.00"), mock(AuditLedgerService.class), caminhoCompleto).orElseThrow();

            assertThat(resposta.unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
            assertThat(resposta.fatoresRevisaoHumana()).noneMatch(fator -> fator.contains("alcada do juizado especial federal"));
        }
    }

    @Test
    void jefInativoOuCriminalNaComarcaNaoContaComoJuizadoInstalado() {
        UnidadeJudiciariaCompetencia federal = unidadeFederal("JFCE-FEDERAL-CE-CAP", RamoDireito.TRIBUTARIO, 90);
        ReflectionTestUtils.setField(federal, "id", 2L);
        UnidadeJudiciariaCompetencia jefFechado = unidadeFederal("JFCE-JEF-CE-CAP", RamoDireito.PREVIDENCIARIO, 10);
        jefFechado.setExclusivoJuizadoEspecial(true);
        ReflectionTestUtils.setField(jefFechado, "aceitaDistribuicao", false);
        ReflectionTestUtils.setField(jefFechado, "id", 1L);
        UnidadeJudiciariaCompetencia jefCriminalPeloRamo = unidadeFederal("JFCE-JEFCRIM-CE-CAP", RamoDireito.PENAL, 10);
        jefCriminalPeloRamo.setExclusivoJuizadoEspecial(true);
        ReflectionTestUtils.setField(jefCriminalPeloRamo, "id", 5L);
        UnidadeJudiciariaCompetencia jefCriminalPelaVara = unidadeFederal("JFCE-JEFCRIM-2-CE-CAP", RamoDireito.PREVIDENCIARIO, 10);
        jefCriminalPelaVara.setExclusivoJuizadoEspecial(true);
        ReflectionTestUtils.setField(jefCriminalPelaVara, "tipoVara", TipoVaraDistribuicao.CRIMINAL_JUIZADO);
        ReflectionTestUtils.setField(jefCriminalPelaVara, "id", 6L);
        for (UnidadeJudiciariaCompetencia jefNaoCivel : List.of(jefFechado, jefCriminalPeloRamo, jefCriminalPelaVara)) {
            ProcessoDistribuicaoCompetencia gravada = registrarCargaAplicada(federal, List.of(federal, jefNaoCivel),
                    RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO, TipoJustica.FEDERAL, new BigDecimal("20000.00"), mock(AuditLedgerService.class));

            assertThat(ReflectionTestUtils.getField(gravada, "fatoresRevisao")).as(jefNaoCivel.getCodigo()).isEqualTo("");
        }
    }

    @Test
    void jefCivelAptoNaComarcaContaComoJuizadoInstalado() {
        List<UnidadeJudiciariaCompetencia> malha = malhaFederal();

        ProcessoDistribuicaoCompetencia gravada = registrarCargaAplicada(malha.get(1), malha, RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO,
                TipoJustica.FEDERAL, new BigDecimal("20000.00"), mock(AuditLedgerService.class));

        assertThat(ReflectionTestUtils.getField(gravada, "fatoresRevisao")).asString().contains("alcada do juizado especial federal");
    }

    @Test
    void jefInstaladoEhProcuradoNaComarcaDoAutorENaoNaSedeDoReu() {
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(malhaFederal(), mock(AuditLedgerService.class), true,
                new ProceduralCanonicalResolver.CanonicalContext(), null);
        DynamicCompetenceDistributionRequest contraAUniaoNaSede = new DynamicCompetenceDistributionRequest(null, null, null,
                "TRIBUTARIO", new BigDecimal("20000.00"), "CE", "Fortaleza", "DF", "Brasilia",
                false, false, null, "FEDERAL", false, false, null);

        assertThat(engine.distribuir(contraAUniaoNaSede, RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO).orElseThrow()
                .fatoresRevisaoHumana()).anyMatch(fator -> fator.contains("alcada do juizado especial federal"));
    }

    @Test
    void ritoNaoInformadoNaDistribuicaoPreviaDeixaAConferenciaParaORitoCanonico() {
        ProceduralCanonicalResolver.CanonicalContext canonicoDeMandado = new ProceduralCanonicalResolver.CanonicalContext(
                java.time.Instant.now(), RitoProcessual.TRIBUTARIO_MANDADO_SEGURANCA, null, null, null, null, null, null, null,
                List.of(), List.of(), List.of(), Map.of());
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(malhaFederal(), mock(AuditLedgerService.class), true,
                canonicoDeMandado, null);

        assertThat(engine.distribuir(pedidoFederal("TRIBUTARIO", "FEDERAL"), null).orElseThrow()
                .fatoresRevisaoHumana()).noneMatch(fator -> fator.contains("alcada do juizado especial federal"));
    }

    @Test
    void causaFederalSemValorDaCausaEmVaraComumPedeConferenciaDaCompetenciaDoJef() {
        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            AuditLedgerService auditoria = mock(AuditLedgerService.class);
            DynamicCompetenceDistributionResponse resposta = registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.PREVIDENCIARIO_APOSENTADORIA,
                    RamoDireito.PREVIDENCIARIO, TipoJustica.FEDERAL, null, auditoria, caminhoCompleto).orElseThrow();

            assertThat(resposta.unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
            assertThat(resposta.distribuicaoAutomatica()).isFalse();
            assertThat(resposta.fatoresRevisaoHumana()).anyMatch(fator -> fator.startsWith("Causa federal sem valor da causa"));
            verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_CAUSA_FEDERAL_SEM_VALOR_DA_CAUSA"),
                    org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("90"),
                    org.mockito.ArgumentMatchers.isNull(), anyString());
        }
    }

    @Test
    void cargaAplicadaDeAcidentarioEmVaraFederalFicaMarcadaParaConferencia() {
        AuditLedgerService auditoria = mock(AuditLedgerService.class);
        List<UnidadeJudiciariaCompetencia> malha = malhaFederal();

        ProcessoDistribuicaoCompetencia gravada = registrarCargaAplicada(malha.get(1), malha, RitoProcessual.PREVIDENCIARIO_ACIDENTARIO,
                TipoJustica.FEDERAL, new BigDecimal("20000.00"), auditoria);

        assertThat(ReflectionTestUtils.getField(gravada, "fatoresRevisao")).asString().contains("Sumula 15 do STJ");
        verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_ACIDENTARIO_NA_JUSTICA_FEDERAL"),
                org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("77"),
                org.mockito.ArgumentMatchers.isNull(), anyString());
    }

    @Test
    void conferenciaFederalSemProcessoEhAuditadaComoDistribuicaoENaoComoProcesso() {
        AuditLedgerService auditoria = mock(AuditLedgerService.class);
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(malhaFederal(), auditoria, true,
                new ProceduralCanonicalResolver.CanonicalContext(), null);
        DynamicCompetenceDistributionRequest pedido = new DynamicCompetenceDistributionRequest("0001234-55.2026.4.05.8100", null, null,
                "TRIBUTARIO", new BigDecimal("20000.00"), "CE", "Fortaleza", "CE", "Fortaleza",
                false, false, null, "FEDERAL", false, false, null);

        engine.distribuirERegistrar(pedido);

        verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_CAUSA_FEDERAL_NA_ALCADA_FORA_DO_JUIZADO"),
                org.mockito.ArgumentMatchers.eq("COMPETENCIA_DISTRIBUICAO"), org.mockito.ArgumentMatchers.eq("0001234-55.2026.4.05.8100"),
                org.mockito.ArgumentMatchers.isNull(), anyString());
    }

    @Test
    void alcadaDoJefIncluiExatamenteSessentaSalariosMinimosEExcluiOCentavoSeguinte() {
        assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.PREVIDENCIARIO_APOSENTADORIA, RamoDireito.PREVIDENCIARIO,
                TipoJustica.FEDERAL, new BigDecimal("97260.00"), mock(AuditLedgerService.class), false)
                .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-JEF-CE-CAP");
        assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.PREVIDENCIARIO_APOSENTADORIA, RamoDireito.PREVIDENCIARIO,
                TipoJustica.FEDERAL, new BigDecimal("97260.01"), mock(AuditLedgerService.class), false)
                .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
    }

    @Test
    void alcadaDoJefUsaOSalarioMinimoDaDataDoAjuizamento() {
        assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.PREVIDENCIARIO_APOSENTADORIA, RamoDireito.PREVIDENCIARIO,
                TipoJustica.FEDERAL, new BigDecimal("95000.00"), java.time.LocalDateTime.of(2025, 12, 15, 10, 0),
                mock(AuditLedgerService.class), false)
                .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
        assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.PREVIDENCIARIO_APOSENTADORIA, RamoDireito.PREVIDENCIARIO,
                TipoJustica.FEDERAL, new BigDecimal("95000.00"), java.time.LocalDateTime.of(2026, 1, 15, 10, 0),
                mock(AuditLedgerService.class), false)
                .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-JEF-CE-CAP");
    }

    private static String fatorCausaFederalNaAlcada(DynamicCompetenceDistributionResponse resposta) {
        return resposta.fatoresRevisaoHumana().stream()
                .filter(fator -> fator.contains("alcada do juizado especial federal"))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void causaFederalComumAcimaDaAlcadaDoJefNaoPedeConferenciaDeCompetenciaAbsoluta() {
        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            AuditLedgerService auditoria = mock(AuditLedgerService.class);
            DynamicCompetenceDistributionResponse resposta = registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.COMUM_ORDINARIO,
                    RamoDireito.TRIBUTARIO, TipoJustica.FEDERAL, new BigDecimal("150000.00"), auditoria, caminhoCompleto).orElseThrow();

            assertThat(resposta.unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
            assertThat(resposta.fatoresRevisaoHumana()).noneMatch(fator -> fator.contains("alcada do juizado especial federal"));
            verify(auditoria, org.mockito.Mockito.never()).appendSafely(
                    org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_CAUSA_FEDERAL_NA_ALCADA_FORA_DO_JUIZADO"), any(), any(), any(), any());
        }
    }

    @Test
    void mandadoDeSegurancaFederalDentroDaAlcadaNaoPedeConferenciaPorqueOJefNaoOProcessa() {
        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            DynamicCompetenceDistributionResponse resposta = registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.ESPECIAL_MANDADO_SEGURANCA,
                    RamoDireito.TRIBUTARIO, TipoJustica.FEDERAL, new BigDecimal("20000.00"), mock(AuditLedgerService.class), caminhoCompleto).orElseThrow();

            assertThat(resposta.unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
            assertThat(resposta.fatoresRevisaoHumana()).noneMatch(fator -> fator.contains("alcada do juizado especial federal"));
        }
    }

    @Test
    void distribuicaoPreviaComRitoInformadoPedeConferenciaDaAlcadaEMandaPrevidenciarioAoJef() {
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(malhaFederal(), mock(AuditLedgerService.class), true,
                new ProceduralCanonicalResolver.CanonicalContext(), null);

        DynamicCompetenceDistributionResponse tributaria = engine.distribuir(pedidoFederal("TRIBUTARIO", "FEDERAL"),
                RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO).orElseThrow();
        DynamicCompetenceDistributionResponse previdenciaria = engine.distribuir(pedidoFederal("PREVIDENCIARIO", "FEDERAL"),
                RitoProcessual.PREVIDENCIARIO_BPC_LOAS).orElseThrow();
        DynamicCompetenceDistributionResponse mandado = engine.distribuir(pedidoFederal("TRIBUTARIO", "FEDERAL"),
                RitoProcessual.TRIBUTARIO_MANDADO_SEGURANCA).orElseThrow();

        assertThat(tributaria.unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
        assertThat(tributaria.fatoresRevisaoHumana()).anyMatch(fator -> fator.contains("alcada do juizado especial federal"));
        assertThat(previdenciaria.unidadeCodigo()).isEqualTo("JFCE-JEF-CE-CAP");
        assertThat(mandado.fatoresRevisaoHumana()).noneMatch(fator -> fator.contains("alcada do juizado especial federal"));
    }

    @Test
    void inferenciaSemRitoAvaliaAAlcadaComOTipoDeJusticaDoPedidoDoCanonicoOuDoResolvedor() {
        ProceduralCanonicalResolver.CanonicalContext canonicoFederal = new ProceduralCanonicalResolver.CanonicalContext(
                java.time.Instant.now(), null, null, null, null, "FEDERAL", null, null, null, List.of(), List.of(), List.of(), Map.of());

        DynamicCompetenceDistributionResponse peloPedido = engineDaMalha(malhaFederal(), mock(AuditLedgerService.class), true,
                new ProceduralCanonicalResolver.CanonicalContext(), null)
                .distribuir(pedidoFederal("TRIBUTARIO", "FEDERAL")).orElseThrow();
        DynamicCompetenceDistributionResponse peloCanonico = engineDaMalha(malhaFederal(), mock(AuditLedgerService.class), true,
                canonicoFederal, null)
                .distribuir(pedidoFederal(null, null)).orElseThrow();
        DynamicCompetenceDistributionResponse peloResolvedor = engineDaMalha(malhaFederal(), mock(AuditLedgerService.class), true,
                new ProceduralCanonicalResolver.CanonicalContext(), "FEDERAL")
                .distribuir(pedidoFederal(null, null)).orElseThrow();

        for (DynamicCompetenceDistributionResponse resposta : List.of(peloPedido, peloCanonico, peloResolvedor)) {
            assertThat(resposta.unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
            assertThat(resposta.fatoresRevisaoHumana()).anyMatch(fator -> fator.contains("alcada do juizado especial federal"));
        }
    }

    @Test
    void juizadoEmUnidadeComumNaoSomaAConferenciaDaAlcadaNemDuplicaAuditoria() {
        UnidadeJudiciariaCompetencia federal = unidadeFederal("JFCE-FEDERAL-CE-CAP", RamoDireito.TRIBUTARIO, 90);
        ReflectionTestUtils.setField(federal, "id", 2L);
        UnidadeJudiciariaCompetencia jefDeOutraComarca = unidadeSemFkComComarcaTextual("JFCE-JEF-CE-JUA", "CE", "Juazeiro do Norte");
        ReflectionTestUtils.setField(jefDeOutraComarca, "tipoJustica", TipoJustica.FEDERAL);
        jefDeOutraComarca.setExclusivoJuizadoEspecial(true);
        ReflectionTestUtils.setField(jefDeOutraComarca, "id", 3L);
        AuditLedgerService auditoria = mock(AuditLedgerService.class);

        DynamicCompetenceDistributionResponse resposta = registrarDistribuicaoInicial(List.of(federal, jefDeOutraComarca),
                RitoProcessual.PREVIDENCIARIO_BPC_LOAS, RamoDireito.PREVIDENCIARIO, TipoJustica.FEDERAL,
                new BigDecimal("50000.00"), auditoria, false).orElseThrow();

        assertThat(resposta.unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
        assertThat(resposta.fatoresRevisaoHumana()).anyMatch(fator -> fator.startsWith("Juizado especial nao instalado"));
        assertThat(resposta.fatoresRevisaoHumana()).noneMatch(fator -> fator.contains("alcada do juizado especial federal"));
        verify(auditoria, times(1)).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_JUIZADO_EM_UNIDADE_COMUM"),
                any(), any(), any(), any());
        verify(auditoria, org.mockito.Mockito.never()).appendSafely(
                org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_CAUSA_FEDERAL_NA_ALCADA_FORA_DO_JUIZADO"), any(), any(), any(), any());
    }

    @Test
    void varaEstadualComCompetenciaDelegadaNaoRecebeConferenciaDeCausaFederal() {
        AuditLedgerService auditoria = mock(AuditLedgerService.class);
        UnidadeJudiciariaCompetencia estadual = unidade("VARA-CIVEL");
        ReflectionTestUtils.setField(estadual, "tipoJustica", TipoJustica.ESTADUAL);
        List<UnidadeJudiciariaCompetencia> malha = new java.util.ArrayList<>(malhaFederal());
        malha.add(estadual);

        ProcessoDistribuicaoCompetencia gravada = registrarCargaAplicada(estadual, malha, RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO,
                TipoJustica.FEDERAL, new BigDecimal("20000.00"), auditoria);

        assertThat(ReflectionTestUtils.getField(gravada, "fatoresRevisao")).isEqualTo("");
        org.mockito.Mockito.verifyNoInteractions(auditoria);
    }

    @Test
    void dataDoAjuizamentoEhLidaNoFusoLegalAntesDeEscolherOSalarioMinimo() {
        java.time.LocalDateTime virada = java.time.LocalDateTime.ofInstant(
                java.time.Instant.parse("2026-01-01T02:00:00Z"), java.time.ZoneId.systemDefault());

        assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.PREVIDENCIARIO_APOSENTADORIA, RamoDireito.PREVIDENCIARIO,
                TipoJustica.FEDERAL, new BigDecimal("95000.00"), virada, mock(AuditLedgerService.class), false)
                .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-FEDERAL-CE-CAP");
    }

    @Test
    void conferenciaFederalComoUnicoFatorPersisteAEscolhaDoCaminhoCompletoEmVezDeCairNaDistribuicaoMinima() {
        ConferenciaFederal alcada = ConferenciaFederal.ALCADA_DO_JEF;
        ConferenciaFederal acidentario = ConferenciaFederal.ACIDENTARIO_DA_JUSTICA_ESTADUAL;

        assertThat(MapaCompetenciaDinamicoEngine.soConferenciaFederal(alcada, List.of(alcada.fator()))).isTrue();
        assertThat(MapaCompetenciaDinamicoEngine.soConferenciaFederal(acidentario, List.of(acidentario.fator()))).isTrue();
        assertThat(MapaCompetenciaDinamicoEngine.soConferenciaFederal(alcada,
                List.of(alcada.fator(), "Confianca operacional inferior ao limiar de automacao"))).isFalse();
        assertThat(MapaCompetenciaDinamicoEngine.soConferenciaFederal(alcada, List.of(acidentario.fator()))).isFalse();
        assertThat(MapaCompetenciaDinamicoEngine.soConferenciaFederal(null, List.of(alcada.fator()))).isFalse();
        assertThat(MapaCompetenciaDinamicoEngine.soConferenciaFederal(ConferenciaFederal.NENHUMA, List.of())).isFalse();
    }

    @Test
    void competenciaDelegadaInformadaNoPedidoPrevaleceSobreOTipoCanonicoNaInferenciaDoJuizado() {
        UnidadeJudiciariaCompetencia estadual = unidade("TJCE-VARA-CIVEL-CE-CAP");
        ReflectionTestUtils.setField(estadual, "ramoDireito", RamoDireito.PREVIDENCIARIO);
        ReflectionTestUtils.setField(estadual, "id", 7L);
        ProceduralCanonicalResolver.CanonicalContext canonicoFederal = new ProceduralCanonicalResolver.CanonicalContext(
                java.time.Instant.now(), null, null, null, null, "FEDERAL", null, null, null, List.of(), List.of(), List.of(), Map.of());
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(List.of(estadual), mock(AuditLedgerService.class), true, canonicoFederal, null);

        Optional<DynamicCompetenceDistributionResponse> resposta = engine.distribuir(pedidoFederal("PREVIDENCIARIO", "ESTADUAL"));

        assertThat(resposta).hasValueSatisfying(distribuicao -> {
            assertThat(distribuicao.unidadeCodigo()).isEqualTo("TJCE-VARA-CIVEL-CE-CAP");
            assertThat(distribuicao.fatoresRevisaoHumana()).noneMatch(fator -> fator.startsWith("Juizado especial nao instalado"));
        });
    }

    @Test
    void ritoExplicitoExcluidoDoJefNaoGanhaConferenciaMesmoSemTipoDeJusticaNoPedido() {
        ProceduralCanonicalResolver.CanonicalContext canonicoFederalSemRito = new ProceduralCanonicalResolver.CanonicalContext(
                java.time.Instant.now(), null, null, null, null, "FEDERAL", null, null, null, List.of(), List.of(), List.of(), Map.of());
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(malhaFederal(), mock(AuditLedgerService.class), true,
                canonicoFederalSemRito, null);

        assertThat(engine.distribuir(pedidoFederal("TRIBUTARIO", null), RitoProcessual.ESPECIAL_MANDADO_SEGURANCA).orElseThrow()
                .fatoresRevisaoHumana()).noneMatch(fator -> fator.contains("alcada do juizado especial federal"));
    }

    @Test
    void ritoExplicitoQueCabeNoJefPrevaleceSobreORitoCanonicoQuandoFaltaOTipoDeJustica() {
        ProceduralCanonicalResolver.CanonicalContext canonicoDeMandadoFederal = new ProceduralCanonicalResolver.CanonicalContext(
                java.time.Instant.now(), RitoProcessual.TRIBUTARIO_MANDADO_SEGURANCA, null, null, null, "FEDERAL", null, null, null,
                List.of(), List.of(), List.of(), Map.of());
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(malhaFederal(), mock(AuditLedgerService.class), true,
                canonicoDeMandadoFederal, null);

        assertThat(engine.distribuir(pedidoFederal("TRIBUTARIO", null), RitoProcessual.COMUM_ORDINARIO).orElseThrow()
                .fatoresRevisaoHumana()).anyMatch(fator -> fator.contains("alcada do juizado especial federal"));
    }

    @Test
    void ritoEfetivoDoProcessoSegueAteOEnriquecimentoQuandoOTipoDeJusticaSoEhInferidoDepois() {
        List<UnidadeJudiciariaCompetencia> unidades = malhaFederal();
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        when(unidadeRepository.findAll()).thenReturn(unidades);
        when(unidadeRepository.findByCodigo(anyString())).thenAnswer(inv -> unidades.stream()
                .filter(unidade -> unidade.getCodigo().equals(inv.getArgument(0))).findFirst());
        ConfiguracaoDistribuicaoVaraService configuracao = mock(ConfiguracaoDistribuicaoVaraService.class);
        when(configuracao.consultar(anyString(), any())).thenAnswer(inv -> new ConfiguracaoDistribuicaoVaraService.ResultadoConsulta(
                inv.getArgument(0), true, null, null, List.of(), java.time.Instant.now()));
        ProceduralCanonicalResolver canonico = mock(ProceduralCanonicalResolver.class);
        when(canonico.resolve(any())).thenAnswer(inv -> {
            Map<String, Object> payload = inv.getArgument(0);
            RitoProcessual rito = payload.containsKey("rito") ? RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO : RitoProcessual.TRIBUTARIO_MANDADO_SEGURANCA;
            return new ProceduralCanonicalResolver.CanonicalContext(java.time.Instant.now(), rito, null, null, null, null, null, null, null,
                    List.of(), List.of(), List.of(), Map.of());
        });
        CompetenceResolverService resolvedor = mock(CompetenceResolverService.class);
        com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse resolucao =
                mock(com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse.class);
        when(resolucao.tipoJusticaSugerida()).thenReturn("FEDERAL");
        when(resolvedor.resolve(any())).thenReturn(resolucao);
        MapaCompetenciaDinamicoEngine engine = new MapaCompetenciaDinamicoEngine(
                unidadeRepository, mock(ProcessoDistribuicaoCompetenciaRepository.class), mock(ProcessoRepository.class),
                mock(AuditLedgerService.class), mock(OutboxPublisher.class), configuracao, canonico, inferencia(canonico, resolvedor));
        Processo processo = processoRoteado(90L, "0001234-55.2026.4.05.8100", null);
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setValorCausa(new BigDecimal("20000.00"));

        assertThat(engine.registrarDistribuicaoInicial(processo).orElseThrow().fatoresRevisaoHumana())
                .anyMatch(fator -> fator.contains("alcada do juizado especial federal"));
    }

    @Test
    void tipoDeJusticaDoPedidoSemRamoNaoEhSubstituidoPeloSugeridoPeloResolvedor() {
        UnidadeJudiciariaCompetencia estadual = unidade("TJCE-VARA-CIVEL-CE-CAP");
        ReflectionTestUtils.setField(estadual, "id", 7L);
        List<UnidadeJudiciariaCompetencia> malha = new java.util.ArrayList<>(malhaFederal());
        malha.add(estadual);
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(malha, mock(AuditLedgerService.class), true,
                new ProceduralCanonicalResolver.CanonicalContext(), "FEDERAL");

        assertThat(engine.distribuir(pedidoFederal(null, "ESTADUAL")).orElseThrow().unidadeCodigo())
                .isEqualTo("TJCE-VARA-CIVEL-CE-CAP");
    }

    @Test
    void comarcaComEsemAcentoContaComoAMesmaParaReconhecerOJefInstalado() {
        for (String[] comarcas : List.of(new String[]{"Maceió", "Maceio"}, new String[]{"Maceio", "Maceió"})) {
            String comarcaDoProcesso = comarcas[0];
            UnidadeJudiciariaCompetencia federal = unidadeSemFkComComarcaTextual("JFAL-FEDERAL-AL-CAP", "AL", comarcaDoProcesso);
            ReflectionTestUtils.setField(federal, "tipoJustica", TipoJustica.FEDERAL);
            ReflectionTestUtils.setField(federal, "ramoDireito", RamoDireito.TRIBUTARIO);
            ReflectionTestUtils.setField(federal, "tipoVara", TipoVaraDistribuicao.VARA_UNICA);
            ReflectionTestUtils.setField(federal, "id", 2L);
            UnidadeJudiciariaCompetencia jef = unidadeSemFkComComarcaTextual("JFAL-JEF-AL-CAP", "AL", comarcas[1]);
            ReflectionTestUtils.setField(jef, "tipoJustica", TipoJustica.FEDERAL);
            ReflectionTestUtils.setField(jef, "ramoDireito", RamoDireito.PREVIDENCIARIO);
            ReflectionTestUtils.setField(jef, "tipoVara", TipoVaraDistribuicao.VARA_UNICA);
            jef.setExclusivoJuizadoEspecial(true);
            ReflectionTestUtils.setField(jef, "id", 1L);
            MapaCompetenciaDinamicoEngine engine = engineDaMalha(List.of(federal, jef), mock(AuditLedgerService.class), true,
                    new ProceduralCanonicalResolver.CanonicalContext(), null);
            DynamicCompetenceDistributionRequest pedido = new DynamicCompetenceDistributionRequest(null, null, null, "TRIBUTARIO",
                    new BigDecimal("20000.00"), "AL", comarcaDoProcesso, "AL", comarcaDoProcesso, false, false, null, "FEDERAL", false, false, null);

            assertThat(engine.distribuir(pedido, RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO).orElseThrow().fatoresRevisaoHumana())
                    .as(comarcaDoProcesso).anyMatch(fator -> fator.contains("alcada do juizado especial federal"));
        }
    }

    @Test
    void distribuicaoDeReservaInfereOJefPeloRamoCanonicoQuandoOProcessoNaoTemRamo() {
        ProceduralCanonicalResolver.CanonicalContext canonicoPrevidenciario = new ProceduralCanonicalResolver.CanonicalContext(
                java.time.Instant.now(), RitoProcessual.COMUM_ORDINARIO, "PREVIDENCIARIO", null, null, "FEDERAL", null, null, null,
                List.of(), List.of(), List.of(), Map.of());
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(malhaFederal(), mock(AuditLedgerService.class), false,
                canonicoPrevidenciario, null);
        Processo processo = processoRoteado(90L, "0001234-55.2026.4.05.8100", null);
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setTipoJustica(TipoJustica.FEDERAL);
        processo.setValorCausa(new BigDecimal("20000.00"));

        assertThat(engine.registrarDistribuicaoInicial(processo).orElseThrow().unidadeCodigo()).isEqualTo("JFCE-JEF-CE-CAP");
    }

    @Test
    void cargaAplicadaDeProcessoEstadualNaoCarregaAMalhaDeUnidades() {
        UnidadeJudiciariaCompetencia civel = unidade("VARA-CIVEL");
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        when(unidadeRepository.findByCodigo("VARA-CIVEL")).thenReturn(Optional.of(civel));
        when(distribuicaoRepository.findTopByProcesso_IdOrderByIdDesc(anyLong())).thenReturn(Optional.empty());
        ProceduralCanonicalResolver canonico = mock(ProceduralCanonicalResolver.class);
        when(canonico.resolve(any())).thenReturn(new ProceduralCanonicalResolver.CanonicalContext());
        MapaCompetenciaDinamicoEngine engine = new MapaCompetenciaDinamicoEngine(
                unidadeRepository, distribuicaoRepository, mock(ProcessoRepository.class), mock(AuditLedgerService.class),
                mock(OutboxPublisher.class), mock(ConfiguracaoDistribuicaoVaraService.class), canonico, inferencia(canonico, mock(CompetenceResolverService.class)));
        Processo processo = processoRoteado(77L, "0009876-54.2026.8.06.0001", "VARA-CIVEL");
        processo.setRito(RitoProcessual.COMUM_ORDINARIO);
        processo.setTipoJustica(TipoJustica.ESTADUAL);
        processo.setValorCausa(new BigDecimal("20000.00"));

        engine.registrarCargaDaDistribuicaoAplicada(processo);

        verify(unidadeRepository, org.mockito.Mockito.never()).findAll();
    }

    private static DynamicCompetenceDistributionRequest pedidoFederal(String ramo, String tipoJustica) {
        return new DynamicCompetenceDistributionRequest(null, null, null, ramo, new BigDecimal("20000.00"),
                "CE", "Fortaleza", "CE", "Fortaleza", false, false, null, tipoJustica, false, false, null);
    }

    @Test
    void causaEstadualDentroDoValorDoJefNaoPedeConferenciaDeCompetenciaFederal() {
        AuditLedgerService auditoria = mock(AuditLedgerService.class);
        UnidadeJudiciariaCompetencia civel = unidade("VARA-CIVEL");

        ProcessoDistribuicaoCompetencia gravada = registrarCargaAplicada(civel, List.of(civel), RitoProcessual.COMUM_ORDINARIO,
                TipoJustica.ESTADUAL, new BigDecimal("20000.00"), auditoria);

        assertThat(ReflectionTestUtils.getField(gravada, "fatoresRevisao")).isEqualTo("");
        org.mockito.Mockito.verifyNoInteractions(auditoria);
    }

    @Test
    void processoComRitoDeJuizadoCivelNaJusticaFederalVaiAoJef() {
        for (boolean caminhoCompleto : new boolean[]{true, false}) {
            assertThat(registrarDistribuicaoInicial(malhaFederal(), RitoProcessual.JUIZADO_ESPECIAL_CIVEL, RamoDireito.CIVIL,
                    TipoJustica.FEDERAL, new BigDecimal("20000.00"), mock(AuditLedgerService.class), caminhoCompleto)
                    .orElseThrow().unidadeCodigo()).isEqualTo("JFCE-JEF-CE-CAP");
        }
    }

    @Test
    void cargaAplicadaDeCausaFederalNaAlcadaEmVaraComumFicaMarcadaParaConferencia() {
        AuditLedgerService auditoria = mock(AuditLedgerService.class);
        List<UnidadeJudiciariaCompetencia> malha = malhaFederal();

        ProcessoDistribuicaoCompetencia gravada = registrarCargaAplicada(malha.get(1), malha, RitoProcessual.FAZENDA_PUBLICA_CONHECIMENTO,
                TipoJustica.FEDERAL, new BigDecimal("20000.00"), auditoria);

        assertThat(ReflectionTestUtils.getField(gravada, "fatoresRevisao")).asString().contains("alcada do juizado especial federal");
        verify(auditoria).appendSafely(org.mockito.ArgumentMatchers.eq("DISTRIBUICAO_CAUSA_FEDERAL_NA_ALCADA_FORA_DO_JUIZADO"),
                org.mockito.ArgumentMatchers.eq("PROCESSO"), org.mockito.ArgumentMatchers.eq("77"),
                org.mockito.ArgumentMatchers.isNull(), anyString());
    }

    private static List<UnidadeJudiciariaCompetencia> malhaFederal() {
        UnidadeJudiciariaCompetencia jef = unidadeFederal("JFCE-JEF-CE-CAP", RamoDireito.PREVIDENCIARIO, 10);
        jef.setExclusivoJuizadoEspecial(true);
        ReflectionTestUtils.setField(jef, "id", 1L);
        UnidadeJudiciariaCompetencia federal = unidadeFederal("JFCE-FEDERAL-CE-CAP", RamoDireito.TRIBUTARIO, 90);
        ReflectionTestUtils.setField(federal, "id", 2L);
        return List.of(jef, federal);
    }

    private static UnidadeJudiciariaCompetencia varaCriminal(String codigo) {
        UnidadeJudiciariaCompetencia criminal = unidadeSemFkComComarcaTextual(codigo, "CE", "Fortaleza");
        ReflectionTestUtils.setField(criminal, "tipoVara", TipoVaraDistribuicao.CRIMINAL_GERAL);
        ReflectionTestUtils.setField(criminal, "ramoDireito", RamoDireito.PENAL);
        return criminal;
    }

    private static UnidadeJudiciariaCompetencia unidadeFederal(String codigo, RamoDireito ramo, int prioridade) {
        UnidadeJudiciariaCompetencia unidade = unidadeSemFkComComarcaTextual(codigo, "CE", "Fortaleza");
        ReflectionTestUtils.setField(unidade, "tipoJustica", TipoJustica.FEDERAL);
        ReflectionTestUtils.setField(unidade, "ramoDireito", ramo);
        ReflectionTestUtils.setField(unidade, "tipoVara", TipoVaraDistribuicao.VARA_UNICA);
        unidade.setPrioridadeEstrategica(prioridade);
        return unidade;
    }

    private static List<UnidadeJudiciariaCompetencia> varasCriminais() {
        UnidadeJudiciariaCompetencia jecrim = unidadeSemFkComComarcaTextual("TJCE-JECRIM-CE-CAP", "CE", "Fortaleza");
        ReflectionTestUtils.setField(jecrim, "tipoVara", TipoVaraDistribuicao.CRIMINAL_JUIZADO);
        ReflectionTestUtils.setField(jecrim, "ramoDireito", RamoDireito.PENAL);
        jecrim.setExclusivoJuizadoEspecial(true);
        jecrim.setPrioridadeEstrategica(10);
        UnidadeJudiciariaCompetencia criminal = unidadeSemFkComComarcaTextual("TJCE-CRIMINAL-CE-CAP", "CE", "Fortaleza");
        ReflectionTestUtils.setField(criminal, "tipoVara", TipoVaraDistribuicao.CRIMINAL_GERAL);
        ReflectionTestUtils.setField(criminal, "ramoDireito", RamoDireito.PENAL);
        criminal.setPrioridadeEstrategica(90);
        return List.of(jecrim, criminal);
    }

    private static Optional<DynamicCompetenceDistributionResponse> registrarSoPelaDistribuicaoMinima(List<UnidadeJudiciariaCompetencia> unidades,
                                                                                                    RitoProcessual rito,
                                                                                                    RamoDireito ramo,
                                                                                                    AuditLedgerService auditoria) {
        return registrarDistribuicaoInicial(unidades, rito, ramo, TipoJustica.ESTADUAL, auditoria, false);
    }

    private static Optional<DynamicCompetenceDistributionResponse> registrarDistribuicaoInicial(List<UnidadeJudiciariaCompetencia> unidades,
                                                                                               RitoProcessual rito,
                                                                                               RamoDireito ramo,
                                                                                               TipoJustica tipoJustica,
                                                                                               AuditLedgerService auditoria,
                                                                                               boolean caminhoCompleto) {
        return registrarDistribuicaoInicial(unidades, rito, ramo, tipoJustica, null, auditoria, caminhoCompleto);
    }

    private static Optional<DynamicCompetenceDistributionResponse> registrarDistribuicaoInicial(List<UnidadeJudiciariaCompetencia> unidades,
                                                                                               RitoProcessual rito,
                                                                                               RamoDireito ramo,
                                                                                               TipoJustica tipoJustica,
                                                                                               BigDecimal valorCausa,
                                                                                               AuditLedgerService auditoria,
                                                                                               boolean caminhoCompleto) {
        return registrarDistribuicaoInicial(unidades, rito, ramo, tipoJustica, valorCausa, null, auditoria, caminhoCompleto);
    }

    private static Optional<DynamicCompetenceDistributionResponse> registrarDistribuicaoInicial(List<UnidadeJudiciariaCompetencia> unidades,
                                                                                               RitoProcessual rito,
                                                                                               RamoDireito ramo,
                                                                                               TipoJustica tipoJustica,
                                                                                               BigDecimal valorCausa,
                                                                                               java.time.LocalDateTime dataCriacao,
                                                                                               AuditLedgerService auditoria,
                                                                                               boolean caminhoCompleto) {
        MapaCompetenciaDinamicoEngine engine = engineDaMalha(unidades, auditoria, caminhoCompleto,
                new ProceduralCanonicalResolver.CanonicalContext(), null);
        Processo processo = processoRoteado(90L, "0001234-55.2026.8.06.0001", null);
        processo.setRito(rito);
        processo.setUf("CE");
        processo.setComarca("Fortaleza");
        processo.setRamoDireito(ramo);
        processo.setTipoJustica(tipoJustica);
        processo.setValorCausa(valorCausa);
        processo.setDataCriacao(dataCriacao);
        return engine.registrarDistribuicaoInicial(processo);
    }

    private static MapaCompetenciaDinamicoEngine engineDaMalha(List<UnidadeJudiciariaCompetencia> unidades,
                                                               AuditLedgerService auditoria,
                                                               boolean caminhoCompleto,
                                                               ProceduralCanonicalResolver.CanonicalContext contextoCanonico,
                                                               String tipoJusticaSugerida) {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ConfiguracaoDistribuicaoVaraService configuracao = mock(ConfiguracaoDistribuicaoVaraService.class);
        ProceduralCanonicalResolver canonico = mock(ProceduralCanonicalResolver.class);
        when(unidadeRepository.findAll()).thenReturn(unidades);
        when(unidadeRepository.findByCodigo(anyString())).thenAnswer(inv -> unidades.stream()
                .filter(unidade -> unidade.getCodigo().equals(inv.getArgument(0))).findFirst());
        when(configuracao.consultar(anyString(), any())).thenAnswer(inv -> new ConfiguracaoDistribuicaoVaraService.ResultadoConsulta(
                inv.getArgument(0), caminhoCompleto, caminhoCompleto ? null : "fora", null, List.of(), java.time.Instant.now()));
        when(canonico.resolve(any())).thenReturn(contextoCanonico);
        CompetenceResolverService resolvedorCompetencia = mock(CompetenceResolverService.class);
        com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse resolucao =
                mock(com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse.class);
        when(resolucao.tipoJusticaSugerida()).thenReturn(tipoJusticaSugerida);
        when(resolvedorCompetencia.resolve(any())).thenReturn(resolucao);
        return new MapaCompetenciaDinamicoEngine(
                unidadeRepository, mock(ProcessoDistribuicaoCompetenciaRepository.class), mock(ProcessoRepository.class),
                auditoria, mock(OutboxPublisher.class), configuracao, canonico, inferencia(canonico, resolvedorCompetencia));
    }

    @Test
    void leituraDeGestaoIgnoraComoParVaraSemNenhumaDistribuicaoNoPjbEm90Dias() throws Exception {
        UnidadeJudiciariaCompetenciaRepository unidadeRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        ProcessoDistribuicaoCompetenciaRepository distribuicaoRepository = mock(ProcessoDistribuicaoCompetenciaRepository.class);
        UnidadeJudiciariaCompetencia emOperacao = livre("VARA-01", 1L);
        UnidadeJudiciariaCompetencia outraEmOperacao = livre("VARA-02", 2L);
        UnidadeJudiciariaCompetencia foraDoPjb = livre("VARA-03", 3L);
        when(unidadeRepository.findAll()).thenReturn(List.of(emOperacao, outraEmOperacao, foraDoPjb));
        when(distribuicaoRepository.contarRecentesPorUnidade(any(), any(java.time.Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 4L}, new Object[]{2L, 2L}));
        MapaCompetenciaDinamicoEngine engine = engineCom(unidadeRepository, distribuicaoRepository);

        DynamicCompetenceRedistributionResponse resposta = engine.analisarRedistribuicao(0.95d);

        assertThat(resposta.saturacoes()).isEmpty();
    }

    private static MapaCompetenciaDinamicoEngine.ReferenciaDePares referencia(double media,
                                                                              MapaCompetenciaDinamicoEngine.NivelDePares nivel,
                                                                              int pares) {
        return new MapaCompetenciaDinamicoEngine.ReferenciaDePares(media, nivel, pares);
    }

    @SuppressWarnings("unchecked")
    private static Map<Long, MapaCompetenciaDinamicoEngine.ReferenciaDePares> invokeReferenciaDePares(MapaCompetenciaDinamicoEngine engine,
                                                                                                       List<UnidadeJudiciariaCompetencia> unidades,
                                                                                                       Map<Long, Long> recentes,
                                                                                                       boolean escalonar) throws Exception {
        java.lang.reflect.Method metodo = MapaCompetenciaDinamicoEngine.class.getDeclaredMethod(
                "referenciaDePares", List.class, Map.class, boolean.class);
        metodo.setAccessible(true);
        return (Map<Long, MapaCompetenciaDinamicoEngine.ReferenciaDePares>) metodo.invoke(engine, unidades, recentes, escalonar);
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
        ProceduralCanonicalResolver canonico = mock(ProceduralCanonicalResolver.class);
        return new MapaCompetenciaDinamicoEngine(
                unidadeRepository,
                distribuicaoRepository,
                mock(ProcessoRepository.class),
                mock(AuditLedgerService.class),
                mock(OutboxPublisher.class),
                mock(ConfiguracaoDistribuicaoVaraService.class),
                canonico,
                inferencia(canonico, mock(CompetenceResolverService.class)));
    }

    private static InferenciaCompetenciaDistribuicaoService inferencia(ProceduralCanonicalResolver canonico,
                                                                        CompetenceResolverService resolvedorCompetencia) {
        return new InferenciaCompetenciaDistribuicaoService(canonico, resolvedorCompetencia, salarioMinimo());
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
