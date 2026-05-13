package com.tcc.pjb.backend.core.distribuicao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService;
import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService.RoutingDecision;
import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualTrackSupport;
import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualProcessoSupport;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class DistribuicaoProcessualNacionalEngineTest {

    @Test
    void reforcaPrevencaoEDeskDeRevisaoNoPrimeiroAjuizamento() {
        NationalProcessRoutingService routingService = mock(NationalProcessRoutingService.class);
        when(routingService.route(any())).thenReturn(routingDecision("PREVENCAO_REFERENCIADA", "MODERADO", true));
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessoRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        DistribuicaoProcessualNacionalEngine engine = new DistribuicaoProcessualNacionalEngine(
                routingService,
                new DistributionConstraintSnapshotService(),
                new DistributionGovernanceResolver(),
                new DistribuicaoProcessualTrackSupport(),
                new DistribuicaoProcessualProcessoSupport(),
                provider
        );

        var request = new DistribuicaoProcessualNacionalEngine.DistribuicaoRequest(
                "0000001-00.2026.8.06.0001",
                "CE",
                "Fortaleza",
                RitoProcessual.COMUM_ORDINARIO,
                1500.00d,
                "Autor",
                "Réu",
                GrauJurisdicao.PRIMEIRO_GRAU,
                "Fortaleza",
                "TJCE",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "0000002-00.2026.8.06.0001",
                null,
                "PROCEDIMENTO COMUM",
                "RESPONSABILIDADE CIVIL",
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false
        );

        var result = engine.distribuir(request);

        assertEquals("REDIRECIONAMENTO_PREVENTIVO_EM_REVISAO", result.status());
        assertTrue(result.filaDistribuicao().startsWith("FILA_REVISAO_"));
        assertTrue(result.inboxKey().startsWith("INBOX_DISTRIBUICAO_") || result.inboxKey().startsWith("INBOX_SIGILO_"));
        assertNotNull(result.workItemId());
        assertTrue(result.trilhoCompetencia().contains("TJCE") || result.trilhoCompetencia().contains("PREVENCAO"));
    }

    @Test
    void consultaProcessoPersistidoEReconstruiTrilhaDeDistribuicao() {
        NationalProcessRoutingService routingService = mock(NationalProcessRoutingService.class);
        when(routingService.route(any())).thenReturn(routingDecision("AUTONOMA", "CONTROLADO", false));
        ProcessoRepository repository = mock(ProcessoRepository.class);
        when(repository.findByNumero("0000003-00.2026.8.06.0001")).thenReturn(Optional.of(
                Processo.builder()
                        .id(30L)
                        .numeroUnificado("0000003-00.2026.8.06.0001")
                        .uf("CE")
                        .comarca("Fortaleza")
                        .tribunalCodigoRoteado("TJCE")
                        .unidadeJudiciariaCodigo("1VCIV")
                        .preventionMode("SEM_PREVENCAO_ATIVA")
                        .linkageMode("AUTONOMA")
                        .routingRiskLevel("CONTROLADO")
                        .preProtocoloStatus("DISTRIBUICAO_AUTOMATICA_APLICADA")
                        .classeProcessual("PROCEDIMENTO COMUM")
                        .assunto("RESPONSABILIDADE CIVIL")
                        .parteAutoraNome("Autor")
                        .parteReuNome("Réu")
                        .rito(RitoProcessual.COMUM_ORDINARIO)
                        .valorCausa(BigDecimal.valueOf(5000))
                        .build()
        ));
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessoRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(repository);
        DistribuicaoProcessualNacionalEngine engine = new DistribuicaoProcessualNacionalEngine(
                routingService,
                new DistributionConstraintSnapshotService(),
                new DistributionGovernanceResolver(),
                new DistribuicaoProcessualTrackSupport(),
                new DistribuicaoProcessualProcessoSupport(),
                provider
        );

        Map<String, Object> payload = engine.consultarDistribuicao("0000003-00.2026.8.06.0001");

        assertEquals(Boolean.TRUE, payload.get("encontrado"));
        assertEquals("DISTRIBUICAO_AUTOMATICA_APLICADA", payload.get("status"));
        assertEquals("TJCE", payload.get("tribunalCodigoRoteado"));
        assertTrue(String.valueOf(payload.get("ultimaFilaDistribuicao")).startsWith("FILA_"));
        assertTrue(String.valueOf(payload.get("ultimaInboxKey")).startsWith("INBOX_"));
        assertEquals("1VCIV", payload.get("unidadeJudiciariaCodigo"));
    }

    @Test
    void classificaFluxoCustodiaDesdeOPrimeiroDiagnostico() {
        NationalProcessRoutingService routingService = mock(NationalProcessRoutingService.class);
        when(routingService.route(any())).thenReturn(routingDecision("AUTONOMA", "CONTROLADO", false));
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessoRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        DistribuicaoProcessualNacionalEngine engine = new DistribuicaoProcessualNacionalEngine(
                routingService,
                new DistributionConstraintSnapshotService(),
                new DistributionGovernanceResolver(),
                new DistribuicaoProcessualTrackSupport(),
                new DistribuicaoProcessualProcessoSupport(),
                provider
        );

        Map<String, Object> payload = engine.diagnosticarCompetencia(new DistribuicaoProcessualNacionalEngine.DiagnosticoRequest(
                RitoProcessual.PROCEDIMENTO_PENAL_COMUM,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "Fortaleza",
                "CE",
                0.0d,
                "Fortaleza",
                "TJCE",
                null,
                null,
                "AUDIENCIA DE CUSTODIA",
                "Fortaleza",
                "Fortaleza",
                "Fortaleza",
                "Fortaleza",
                null,
                null,
                "AUTO DE PRISAO EM FLAGRANTE",
                "AUDIENCIA DE CUSTODIA",
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        assertEquals("CUSTODIA", payload.get("specializedTrack"));
        assertTrue(String.valueOf(payload.get("filaDistribuicao")).startsWith("FILA_CUSTODIA_"));
        assertTrue(String.valueOf(payload.get("inboxKey")).startsWith("INBOX_CUSTODIA_"));
        assertEquals("DISTRIBUICAO_PRIORIDADE_CUSTODIA", payload.get("status"));
        assertEquals(0, ((Number) payload.get("priority")).intValue());
    }

    @Test
    void classificaExecucaoFiscalComTrilhaEspecializadaNaEntrada() {
        NationalProcessRoutingService routingService = mock(NationalProcessRoutingService.class);
        when(routingService.route(any())).thenReturn(routingDecision("AUTONOMA", "CONTROLADO", false));
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessoRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        DistribuicaoProcessualNacionalEngine engine = new DistribuicaoProcessualNacionalEngine(
                routingService,
                new DistributionConstraintSnapshotService(),
                new DistributionGovernanceResolver(),
                new DistribuicaoProcessualTrackSupport(),
                new DistribuicaoProcessualProcessoSupport(),
                provider
        );

        Map<String, Object> payload = engine.diagnosticarCompetencia(new DistribuicaoProcessualNacionalEngine.DiagnosticoRequest(
                RitoProcessual.EXECUCAO_FISCAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "Fortaleza",
                "CE",
                150000.0d,
                "Fortaleza",
                "TJCE",
                null,
                null,
                "FAZENDA PUBLICA",
                "Fortaleza",
                "Fortaleza",
                null,
                null,
                null,
                null,
                "EXECUCAO FISCAL",
                "CERTIDAO DE DIVIDA ATIVA",
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        assertEquals("EXECUCAO_FISCAL", payload.get("specializedTrack"));
        assertTrue(String.valueOf(payload.get("filaDistribuicao")).startsWith("FILA_EXECUCAO_FISCAL_"));
        assertTrue(String.valueOf(payload.get("inboxKey")).startsWith("INBOX_EXECUCAO_FISCAL_"));
        assertEquals("DISTRIBUICAO_ESPECIALIZADA_EXECUCAO_FISCAL", payload.get("status"));
    }

    @Test
    void classificaFamiliaESucessoesComTrilhaEspecializadaNaEntrada() {
        NationalProcessRoutingService routingService = mock(NationalProcessRoutingService.class);
        when(routingService.route(any())).thenReturn(routingDecision("AUTONOMA", "CONTROLADO", false));
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessoRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        DistribuicaoProcessualNacionalEngine engine = new DistribuicaoProcessualNacionalEngine(
                routingService,
                new DistributionConstraintSnapshotService(),
                new DistributionGovernanceResolver(),
                new DistribuicaoProcessualTrackSupport(),
                new DistribuicaoProcessualProcessoSupport(),
                provider
        );

        Map<String, Object> payload = engine.diagnosticarCompetencia(new DistribuicaoProcessualNacionalEngine.DiagnosticoRequest(
                RitoProcessual.CIVIL_FAMILIA_DIVORCIO,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "Fortaleza",
                "CE",
                20000.0d,
                "Fortaleza",
                "TJCE",
                null,
                null,
                "FAMILIA",
                "Fortaleza",
                "Fortaleza",
                null,
                null,
                null,
                null,
                "DIVORCIO LITIGIOSO",
                "DISSOLUCAO DE CASAMENTO",
                false,
                false,
                false,
                false,
                false,
                true,
                false
        ));

        assertEquals("FAMILIA_SUCESSOES", payload.get("specializedTrack"));
        assertTrue(String.valueOf(payload.get("filaDistribuicao")).startsWith("FILA_FAMILIA_SUCESSOES_"));
        assertEquals("DISTRIBUICAO_ESPECIALIZADA_FAMILIA_SUCESSOES", payload.get("status"));
    }

    @Test
    void classificaAmbientalComTrilhaEspecializadaNaEntrada() {
        NationalProcessRoutingService routingService = mock(NationalProcessRoutingService.class);
        when(routingService.route(any())).thenReturn(routingDecision("AUTONOMA", "CONTROLADO", false));
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessoRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        DistribuicaoProcessualNacionalEngine engine = new DistribuicaoProcessualNacionalEngine(
                routingService,
                new DistributionConstraintSnapshotService(),
                new DistributionGovernanceResolver(),
                new DistribuicaoProcessualTrackSupport(),
                new DistribuicaoProcessualProcessoSupport(),
                provider
        );

        Map<String, Object> payload = engine.diagnosticarCompetencia(new DistribuicaoProcessualNacionalEngine.DiagnosticoRequest(
                RitoProcessual.AMBIENTAL_ACP,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "Fortaleza",
                "CE",
                350000.0d,
                "Fortaleza",
                "TJCE",
                null,
                null,
                "AMBIENTAL",
                "Fortaleza",
                "Fortaleza",
                "Fortaleza",
                "Fortaleza",
                null,
                null,
                "ACAO CIVIL PUBLICA",
                "DANO AMBIENTAL EM UNIDADE DE CONSERVACAO",
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        assertEquals("AMBIENTAL", payload.get("specializedTrack"));
        assertTrue(String.valueOf(payload.get("filaDistribuicao")).startsWith("FILA_AMBIENTAL_"));
        assertEquals("DISTRIBUICAO_ESPECIALIZADA_AMBIENTAL", payload.get("status"));
    }

    @Test
    void classificaEmpresarialComTrilhaEspecializadaNaEntrada() {
        NationalProcessRoutingService routingService = mock(NationalProcessRoutingService.class);
        when(routingService.route(any())).thenReturn(routingDecision("AUTONOMA", "CONTROLADO", false));
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessoRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        DistribuicaoProcessualNacionalEngine engine = new DistribuicaoProcessualNacionalEngine(
                routingService,
                new DistributionConstraintSnapshotService(),
                new DistributionGovernanceResolver(),
                new DistribuicaoProcessualTrackSupport(),
                new DistribuicaoProcessualProcessoSupport(),
                provider
        );

        Map<String, Object> payload = engine.diagnosticarCompetencia(new DistribuicaoProcessualNacionalEngine.DiagnosticoRequest(
                RitoProcessual.RECUPERACAO_JUDICIAL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "Fortaleza",
                "CE",
                980000.0d,
                "Fortaleza",
                "TJCE",
                null,
                null,
                "EMPRESARIAL",
                "Fortaleza",
                "Fortaleza",
                null,
                null,
                null,
                null,
                "RECUPERACAO JUDICIAL",
                "REESTRUTURACAO EMPRESARIAL",
                false,
                false,
                false,
                false,
                false,
                false,
                false
        ));

        assertEquals("EMPRESARIAL", payload.get("specializedTrack"));
        assertTrue(String.valueOf(payload.get("filaDistribuicao")).startsWith("FILA_EMPRESARIAL_"));
        assertEquals("DISTRIBUICAO_ESPECIALIZADA_EMPRESARIAL", payload.get("status"));
    }

    @Test
    void classificaInternacionalComTrilhaEspecializadaNaEntrada() {
        NationalProcessRoutingService routingService = mock(NationalProcessRoutingService.class);
        when(routingService.route(any())).thenReturn(routingDecision("AUTONOMA", "CONTROLADO", false));
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessoRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        DistribuicaoProcessualNacionalEngine engine = new DistribuicaoProcessualNacionalEngine(
                routingService,
                new DistributionConstraintSnapshotService(),
                new DistributionGovernanceResolver(),
                new DistribuicaoProcessualTrackSupport(),
                new DistribuicaoProcessualProcessoSupport(),
                provider
        );

        Map<String, Object> payload = engine.diagnosticarCompetencia(new DistribuicaoProcessualNacionalEngine.DiagnosticoRequest(
                RitoProcessual.CARTA_ROGATORIA,
                GrauJurisdicao.PRIMEIRO_GRAU,
                "Fortaleza",
                "CE",
                0.0d,
                "Fortaleza",
                "TRF5",
                null,
                null,
                "COOPERACAO INTERNACIONAL",
                null,
                null,
                null,
                null,
                null,
                null,
                "CARTA ROGATORIA",
                "COOPERACAO JURIDICA INTERNACIONAL",
                false,
                false,
                false,
                false,
                false,
                true,
                false
        ));

        assertEquals("INTERNACIONAL", payload.get("specializedTrack"));
        assertTrue(String.valueOf(payload.get("filaDistribuicao")).startsWith("FILA_INTERNACIONAL_"));
        assertEquals("DISTRIBUICAO_ESPECIALIZADA_INTERNACIONAL", payload.get("status"));
    }


    private RoutingDecision routingDecision(String linkageMode, String riskLevel, boolean strictLock) {
        LinkedHashMap<String, Object> binding = new LinkedHashMap<>();
        binding.put("relationMode", linkageMode);
        binding.put("preventionFingerprint", "PREV|TJCE|FORTALEZA");
        binding.put("dependencyFingerprint", "DEP|TJCE|FORTALEZA");
        binding.put("bindingStrength", strictLock ? "ESTRITA" : "NORMAL");
        binding.put("strictLock", strictLock);
        binding.put("triageOverride", strictLock ? "GATE_PREVENCAO_REFERENCIADA" : null);
        LinkedHashMap<String, Object> relational = new LinkedHashMap<>();
        relational.put("binding", binding);
        relational.put("attachmentMode", strictLock ? "AUTO_ATTACH_DISABLED" : "AUTO_ATTACH_ALLOWED");
        relational.put("registryBucket", strictLock ? "PREVENTO_FORTALEZA" : "FORTALEZA_BASE");
        LinkedHashMap<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("materialityAxis", "CIVEL_GERAL");
        coverage.put("territorialAnchor", "FORTALEZA");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("relational", relational);
        metadata.put("coverage", coverage);
        return new RoutingDecision(
                RitoProcessual.COMUM_ORDINARIO,
                com.tcc.pjb.backend.model.entity.enums.RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU,
                com.tcc.pjb.backend.domain.enums.TipoJustica.ESTADUAL,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJE",
                "EPROC",
                "PRIMEIRO_GRAU",
                "1ª Vara Cível de Fortaleza",
                "1VCIV",
                "FILA_DISTRIBUICAO_FORTALEZA",
                false,
                true,
                true,
                24,
                BigDecimal.valueOf(40000),
                "Fortaleza",
                "Fortaleza",
                "Fórum Clóvis Beviláqua",
                null,
                null,
                null,
                "COMARCA_EXATA",
                strictLock ? "PREVENCAO_ESTRITA" : "SEM_PREVENCAO_ATIVA",
                strictLock ? "MANUAL_ASSISTIDA" : "AUTO_DIRETA",
                "CIVEL_GERAL",
                strictLock ? "SORTEIO_ASSISTIDO_COM_VINCULO" : "SORTEIO_EQUILIBRADO",
                linkageMode,
                "ESTADUAL/PRIMEIRO_GRAU/TJCE/FORTALEZA/CIVEL_GERAL",
                riskLevel,
                "SECRETARIA_ESTADUAL_FORTALEZA_CIVEL_GERAL",
                "MESA_TRIAGEM_FORTALEZA",
                List.of(),
                List.of(),
                List.of(),
                metadata
        );
    }
}
