package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoModo;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoMigracaoLoteRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;
import com.tcc.pjb.backend.platform.hash.CanonicalJsonHasher;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoMigracaoIndustrialBatchServiceTest {

    @Test
    void devePlanejarLotesDeterministicos() throws Exception {
        PjbSubstituicaoMigracaoLoteRepository repository = mock(PjbSubstituicaoMigracaoLoteRepository.class);
        when(repository.findByExecucaoIdAndLoteCodigo(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ObjectMapper objectMapper = new ObjectMapper();
        CanonicalJsonHasher hasher = new CanonicalJsonHasher(objectMapper, Clock.systemUTC());
        PjbSubstituicaoNacionalExecucaoRepository execucaoRepository = mock(PjbSubstituicaoNacionalExecucaoRepository.class);
        PjbSubstituicaoMigracaoIndustrialBatchService service = new PjbSubstituicaoMigracaoIndustrialBatchService(repository, execucaoRepository, hasher, objectMapper);
        String payloadJson = objectMapper.writeValueAsString(Map.of(
                "metadados", Map.of(
                        "processosEstimados", 510,
                        "tamanhoLote", 250
                )
        ));
        PjbSubstituicaoNacionalExecucaoEntity execucao = new PjbSubstituicaoNacionalExecucaoEntity(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                NationalCompetenceMatrix.TJCE.ramo().name(),
                PjbSubstituicaoExecucaoAcao.INICIAR_MIGRACAO_SOMBRA,
                PjbSubstituicaoExecucaoModo.ASSISTIDA,
                false,
                "hash-1",
                "tester",
                "teste",
                "shadow-mode-governado",
                payloadJson
        );
        when(execucaoRepository.getReferenceById(1L)).thenReturn(execucao);
        when(execucaoRepository.getReferenceById(2L)).thenReturn(execucao);
        PjbSubstituicaoGateSnapshot gate = new PjbSubstituicaoGateSnapshot(NationalCompetenceMatrix.TJCE, 90, 88, true, true, 2, 1, 3, 0, 0, 1, List.of());

        PjbSubstituicaoMigracaoIndustrialBatchService.MigrationExecutionResult result = service.executar(1L, "TJCE", false, "hash-1", payloadJson, gate);

        assertEquals(3, result.details().get("totalLotes"));
        assertEquals(510, result.details().get("totalItensPlanejados"));
        assertEquals(3, result.reconciliados());
    }

    @Test
    void deveUsarLoteControleQuandoMassaNaoVierNoPayload() {
        PjbSubstituicaoMigracaoLoteRepository repository = mock(PjbSubstituicaoMigracaoLoteRepository.class);
        when(repository.findByExecucaoIdAndLoteCodigo(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ObjectMapper objectMapper = new ObjectMapper();
        CanonicalJsonHasher hasher = new CanonicalJsonHasher(objectMapper, Clock.systemUTC());
        PjbSubstituicaoNacionalExecucaoRepository execucaoRepository = mock(PjbSubstituicaoNacionalExecucaoRepository.class);
        PjbSubstituicaoMigracaoIndustrialBatchService service = new PjbSubstituicaoMigracaoIndustrialBatchService(repository, execucaoRepository, hasher, objectMapper);
        PjbSubstituicaoNacionalExecucaoEntity execucao = new PjbSubstituicaoNacionalExecucaoEntity(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                NationalCompetenceMatrix.TJCE.ramo().name(),
                PjbSubstituicaoExecucaoAcao.INICIAR_MIGRACAO_SOMBRA,
                PjbSubstituicaoExecucaoModo.ASSISTIDA,
                true,
                "hash-2",
                "tester",
                null,
                null,
                "{}"
        );
        when(execucaoRepository.getReferenceById(1L)).thenReturn(execucao);
        PjbSubstituicaoGateSnapshot gate = new PjbSubstituicaoGateSnapshot(NationalCompetenceMatrix.TJCE, 80, 60, false, true, 0, 0, 0, 1, 1, 0, List.of("BLOCK"));
        String payloadJson = "{}";

        PjbSubstituicaoMigracaoIndustrialBatchService.MigrationExecutionResult result = service.executar(1L, "TJCE", false, "hash-1", payloadJson, gate);

        assertEquals(1, result.details().get("totalLotes"));
        assertTrue((Boolean) result.details().get("loteControleTecnico"));
        assertEquals(1, result.simulados());
    }
}
