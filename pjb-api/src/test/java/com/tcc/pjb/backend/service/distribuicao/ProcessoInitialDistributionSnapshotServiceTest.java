package com.tcc.pjb.backend.service.distribuicao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualNacionalEngine;
import com.tcc.pjb.backend.core.distribuicao.DistributionConstraintSnapshotService;
import com.tcc.pjb.backend.core.distribuicao.DistributionGovernanceResolver;
import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService;
import com.tcc.pjb.backend.core.processual.routing.NationalProcessRoutingService.RoutingDecision;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class ProcessoInitialDistributionSnapshotServiceTest {

    @Test
    void consolidaSnapshotInicialNoProcessoSemCriarFluxoParalelo() {
        NationalProcessRoutingService routingService = mock(NationalProcessRoutingService.class);
        when(routingService.route(any())).thenReturn(routingDecision());
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<ProcessoRepository> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        DistribuicaoProcessualNacionalEngine engine = instantiateEngine(
                routingService,
                new DistributionConstraintSnapshotService(),
                new DistributionGovernanceResolver(),
                provider
        );
        ProcessoInitialDistributionSnapshotService service = new ProcessoInitialDistributionSnapshotService(engine, processoRepository);

        Processo processo = Processo.builder()
                .id(77L)
                .numeroUnificado("0001111-00.2026.8.06.0001")
                .uf("CE")
                .comarca("Fortaleza")
                .rito(RitoProcessual.EXECUCAO_FISCAL)
                .classeProcessual("EXECUCAO FISCAL")
                .assunto("CERTIDAO DE DIVIDA ATIVA")
                .parteAutoraNome("Município")
                .parteReuNome("Executado")
                .valorCausa(BigDecimal.valueOf(150000))
                .build();

        var snapshot = service.consolidar(processo);

        assertNotNull(snapshot);
        assertEquals("EXECUCAO_FISCAL", snapshot.specializedTrack());
        assertEquals("TJCE", processo.getTribunalCodigoRoteado());
        assertEquals("1VCIV", processo.getUnidadeJudiciariaCodigo());
        assertEquals("DISTRIBUICAO_ESPECIALIZADA_EXECUCAO_FISCAL", processo.getPreProtocoloStatus());
        verify(processoRepository).save(processo);
    }

    @SuppressWarnings("unchecked")
    private DistribuicaoProcessualNacionalEngine instantiateEngine(NationalProcessRoutingService routingService,
                                                                   DistributionConstraintSnapshotService constraintSnapshotService,
                                                                   DistributionGovernanceResolver governanceResolver,
                                                                   ObjectProvider<ProcessoRepository> provider) {
        try {
            Class<?> trackSupportType = Class.forName("com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualTrackSupport");
            var trackSupportConstructor = trackSupportType.getDeclaredConstructor();
            trackSupportConstructor.setAccessible(true);
            Object trackSupport = trackSupportConstructor.newInstance();
            Class<?> processoSupportType = Class.forName("com.tcc.pjb.backend.core.distribuicao.DistribuicaoProcessualProcessoSupport");
            var processoSupportConstructor = processoSupportType.getDeclaredConstructor();
            processoSupportConstructor.setAccessible(true);
            Object processoSupport = processoSupportConstructor.newInstance();
            var constructor = DistribuicaoProcessualNacionalEngine.class.getConstructor(
                    NationalProcessRoutingService.class,
                    DistributionConstraintSnapshotService.class,
                    DistributionGovernanceResolver.class,
                    trackSupportType,
                    processoSupportType,
                    ObjectProvider.class
            );
            return constructor.newInstance(routingService, constraintSnapshotService, governanceResolver, trackSupport, processoSupport, provider);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private RoutingDecision routingDecision() {
        LinkedHashMap<String, Object> binding = new LinkedHashMap<>();
        binding.put("relationMode", "AUTONOMA");
        binding.put("strictLock", false);
        LinkedHashMap<String, Object> relational = new LinkedHashMap<>();
        relational.put("binding", binding);
        relational.put("registryBucket", "FORTALEZA_BASE");
        LinkedHashMap<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("materialityAxis", "FAZENDA_PUBLICA");
        coverage.put("territorialAnchor", "FORTALEZA");
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("relational", relational);
        metadata.put("coverage", coverage);
        return new RoutingDecision(
                RitoProcessual.EXECUCAO_FISCAL,
                com.tcc.pjb.backend.model.entity.enums.RamoDireito.TRIBUTARIO,
                GrauJurisdicao.PRIMEIRO_GRAU,
                com.tcc.pjb.backend.domain.enums.TipoJustica.ESTADUAL,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJE",
                "EPROC",
                "PRIMEIRO_GRAU",
                "1ª Vara da Fazenda Pública de Fortaleza",
                "1VCIV",
                "FILA_DISTRIBUICAO_FORTALEZA",
                false,
                false,
                false,
                24,
                BigDecimal.valueOf(40000),
                "Fortaleza",
                "Fortaleza",
                "Fórum Clóvis Beviláqua",
                null,
                null,
                null,
                "COMARCA_EXATA",
                "SEM_PREVENCAO_ATIVA",
                "AUTO_DIRETA",
                "FAZENDA_PUBLICA",
                "SORTEIO_EQUILIBRADO",
                "AUTONOMA",
                "ESTADUAL/PRIMEIRO_GRAU/TJCE/FORTALEZA/FAZENDA_PUBLICA",
                "CONTROLADO",
                "SECRETARIA_ESTADUAL_FORTALEZA_FAZENDA_PUBLICA",
                "MESA_TRIAGEM_FORTALEZA",
                List.of(),
                List.of(),
                List.of(),
                metadata
        );
    }
}
