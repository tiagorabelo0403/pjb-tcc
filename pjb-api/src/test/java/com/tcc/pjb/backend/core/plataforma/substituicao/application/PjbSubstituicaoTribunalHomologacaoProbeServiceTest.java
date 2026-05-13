package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoModo;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoTribunalHomologacaoProbeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoTribunalHomologacaoProbeServiceTest {

    @Test
    void devePersistirProbesDeHomologacao() {
        PjbSubstituicaoTribunalHomologacaoProbeRepository repository = mock(PjbSubstituicaoTribunalHomologacaoProbeRepository.class);
        when(repository.findByExecucaoIdAndProbeCodigo(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PjbSubstituicaoNacionalExecucaoRepository execucaoRepository = mock(PjbSubstituicaoNacionalExecucaoRepository.class);
        PjbSubstituicaoTribunalHomologacaoProbeService service = new PjbSubstituicaoTribunalHomologacaoProbeService(repository, execucaoRepository, new ObjectMapper());
        PjbSubstituicaoNacionalExecucaoEntity execucao = new PjbSubstituicaoNacionalExecucaoEntity(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                NationalCompetenceMatrix.TJCE.ramo().name(),
                PjbSubstituicaoExecucaoAcao.HOMOLOGAR_TRIBUNAL,
                PjbSubstituicaoExecucaoModo.ASSISTIDA,
                false,
                "hash-3",
                "tester",
                "teste",
                null,
                "{}"
        );
        when(execucaoRepository.getReferenceById(3L)).thenReturn(execucao);
        PjbSubstituicaoGateSnapshot gate = new PjbSubstituicaoGateSnapshot(NationalCompetenceMatrix.TJCE, 92, 86, true, true, 2, 1, 4, 0, 0, 1, List.of());

        PjbSubstituicaoTribunalHomologacaoProbeService.ProbeExecutionResult result = service.executar(3L, "TJCE", false, "hash-3", gate);

        assertEquals(5, result.details().get("totalProbes"));
        assertEquals(5, result.aprovadas());
        assertEquals(0, result.bloqueadas());
    }
}
