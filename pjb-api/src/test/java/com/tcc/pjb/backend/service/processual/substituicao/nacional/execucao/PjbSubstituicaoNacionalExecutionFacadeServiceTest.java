package com.tcc.pjb.backend.service.processual.substituicao.nacional.execucao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoNacionalCommandApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoNacionalExecutionQueryApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoNacionalOperationalCockpitApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoFase;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoModo;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalExecucaoAggregate;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalCockpitResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalCockpitResumoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoOperacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.execucao.PjbSubstituicaoNacionalExecucaoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit.PjbSubstituicaoNacionalOperacionalResumoResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal.PjbSubstituicaoTribunalEvidenciaExportavelResponse;
import com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal.PjbSubstituicaoTribunalReconciliacaoResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoNacionalExecutionFacadeServiceTest {

    @Test
    void deveDelegarConsultasOperacionaisAoCockpitService() {
        PjbSubstituicaoNacionalCommandApplicationService command = mock(PjbSubstituicaoNacionalCommandApplicationService.class);
        PjbSubstituicaoNacionalExecutionQueryApplicationService query = mock(PjbSubstituicaoNacionalExecutionQueryApplicationService.class);
        PjbSubstituicaoNacionalOperationalCockpitApplicationService operational = mock(PjbSubstituicaoNacionalOperationalCockpitApplicationService.class);
        PjbSubstituicaoNacionalExecutionFacadeService facade = new PjbSubstituicaoNacionalExecutionFacadeService(command, query, operational);

        PjbSubstituicaoNacionalExecucaoOperacionalResponse operacionalResponse = new PjbSubstituicaoNacionalExecucaoOperacionalResponse(
                null,
                new PjbSubstituicaoNacionalOperacionalResumoResponse(1, 2, 1, 0, 1, 3, 2, 0, 0, 4, 5, 3, 1, 1),
                List.of(),
                List.of(),
                List.of(),
                null
        );
        PjbSubstituicaoNacionalCockpitResponse cockpitResponse = new PjbSubstituicaoNacionalCockpitResponse(
                new PjbSubstituicaoNacionalCockpitResumoResponse(1, 2, 1, 1, 0, 0, 0),
                List.of(),
                List.of(),
                Instant.now()
        );
        PjbSubstituicaoTribunalEvidenciaExportavelResponse evidenciaResponse = new PjbSubstituicaoTribunalEvidenciaExportavelResponse(
                "TJCE",
                "evidencia.json",
                "abc",
                10,
                5,
                Instant.now(),
                Map.of("ok", true)
        );
        PjbSubstituicaoTribunalReconciliacaoResponse reconciliacaoResponse = new PjbSubstituicaoTribunalReconciliacaoResponse(
                "TJCE",
                "Tribunal",
                1,
                2,
                3,
                4,
                5,
                6,
                2,
                1,
                1,
                0,
                "ESTAVEL",
                List.of(),
                evidenciaResponse,
                Instant.now()
        );

        when(operational.detalharOperacional(99L)).thenReturn(operacionalResponse);
        when(operational.cockpit("TJCE")).thenReturn(cockpitResponse);
        when(operational.reconciliarTribunal("TJCE")).thenReturn(reconciliacaoResponse);
        when(operational.evidenciaExportavelTribunal("TJCE")).thenReturn(evidenciaResponse);

        assertSame(operacionalResponse, facade.detalharOperacional(99L));
        assertSame(cockpitResponse, facade.cockpit("TJCE"));
        assertSame(reconciliacaoResponse, facade.reconciliarTribunal("TJCE"));
        assertSame(evidenciaResponse, facade.evidenciasExportaveisTribunal("TJCE"));
    }

    @Test
    void deveMapearDetalheBasicoDaExecucao() {
        PjbSubstituicaoNacionalCommandApplicationService command = mock(PjbSubstituicaoNacionalCommandApplicationService.class);
        PjbSubstituicaoNacionalExecutionQueryApplicationService query = mock(PjbSubstituicaoNacionalExecutionQueryApplicationService.class);
        PjbSubstituicaoNacionalOperationalCockpitApplicationService operational = mock(PjbSubstituicaoNacionalOperationalCockpitApplicationService.class);
        PjbSubstituicaoNacionalExecutionFacadeService facade = new PjbSubstituicaoNacionalExecutionFacadeService(command, query, operational);

        Instant now = Instant.now();
        UUID jobId = UUID.randomUUID();
        when(query.detalhar(7L)).thenReturn(new PjbSubstituicaoNacionalExecucaoAggregate(
                7L,
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                PjbSubstituicaoExecucaoAcao.HOMOLOGAR_TRIBUNAL,
                PjbSubstituicaoExecucaoSituacao.CONCLUIDA,
                PjbSubstituicaoExecucaoFase.FINALIZACAO,
                PjbSubstituicaoExecucaoModo.ASSISTIDA,
                false,
                true,
                true,
                91,
                jobId,
                "corr",
                "hash",
                "admin",
                "teste",
                "onda-1",
                Map.of("payload", true),
                Map.of("resultado", true),
                List.of(),
                now,
                now,
                now,
                now
        ));

        PjbSubstituicaoNacionalExecucaoResponse response = facade.detalhar(7L);

        assertEquals(7L, response.execucaoId());
        assertEquals("TJCE", response.tribunalCodigo());
        assertEquals(PjbSubstituicaoExecucaoAcao.HOMOLOGAR_TRIBUNAL, response.acao());
        assertEquals(PjbSubstituicaoExecucaoSituacao.CONCLUIDA, response.situacao());
        assertEquals(91, response.gateScore());
    }
}
