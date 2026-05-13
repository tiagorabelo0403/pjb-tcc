package com.tcc.pjb.backend.service.processual.substituicao.federativa.centrocomando;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaCentroComandoApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCentroComandoAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTribunal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoFederativaCentroComandoFacadeServiceTest {

    @Test
    void deveMapearCentroComandoFederativo() {
        PjbSubstituicaoFederativaCentroComandoApplicationService applicationService = mock(PjbSubstituicaoFederativaCentroComandoApplicationService.class);
        PjbSubstituicaoFederativaCentroComandoFacadeService facadeService = new PjbSubstituicaoFederativaCentroComandoFacadeService(applicationService);

        when(applicationService.avaliar()).thenReturn(new PjbSubstituicaoFederativaCentroComandoAggregate(
                84,
                true,
                false,
                3,
                1,
                2,
                List.of("pend1"),
                List.of(
                        new PjbSubstituicaoFederativaTribunal(
                                "TJCE",
                                "Tribunal de Justiça do Ceará",
                                "ESTADUAL",
                                "PJE",
                                "PDPJ",
                                "operacao-assistida",
                                PjbFechamentoStatus.PARCIAL,
                                82,
                                true,
                                true,
                                List.of("PJE"),
                                List.of("PJE"),
                                List.of("g1"),
                                List.of("r1"),
                                List.of("b1"),
                                List.of("a1")
                        )
                ),
                List.of("f1"),
                Instant.now()
        ));

        when(applicationService.avaliarTribunal("TJCE")).thenReturn(new PjbSubstituicaoFederativaTribunal(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJE",
                "PDPJ",
                "operacao-assistida",
                PjbFechamentoStatus.CONCLUIDA,
                88,
                true,
                true,
                List.of("PJE"),
                List.of("PJE"),
                List.of("g1"),
                List.of("r1"),
                List.of(),
                List.of("a1")
        ));

        var response = facadeService.avaliar();
        var tribunal = facadeService.avaliarTribunal("TJCE");

        assertEquals(84, response.scoreNacional());
        assertFalse(response.prontoRollbackGovernado());
        assertEquals(1, response.tribunais().size());
        assertEquals("TJCE", tribunal.tribunalCodigo());
        assertNotNull(response.geradoEm());
    }
}
