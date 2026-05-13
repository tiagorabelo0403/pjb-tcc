package com.tcc.pjb.backend.service.processual.substituicao.federativa.nucleoduro;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaNucleoDuroApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaNucleoDuroTribunal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoFederativaNucleoDuroFacadeServiceTest {

    @Test
    void deveMapearNucleoDuroFederativo() {
        PjbSubstituicaoFederativaNucleoDuroApplicationService applicationService = mock(PjbSubstituicaoFederativaNucleoDuroApplicationService.class);
        PjbSubstituicaoFederativaNucleoDuroFacadeService facadeService = new PjbSubstituicaoFederativaNucleoDuroFacadeService(applicationService);

        PjbSubstituicaoFederativaNucleoDuroTribunal tribunal = new PjbSubstituicaoFederativaNucleoDuroTribunal(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJE",
                "operacao-assistida",
                86,
                82,
                79,
                83,
                77,
                true,
                false,
                true,
                true,
                true,
                1,
                List.of(new PjbSubstituicaoFederativaNucleoDuroCompetencia(
                        "PENAL",
                        "Direito Penal",
                        "PROCEDIMENTO_PENAL_COMUM",
                        6,
                        80,
                        74,
                        71,
                        82,
                        false,
                        "1a Vara Criminal",
                        "janela-corte-controlado",
                        List.of("g1"),
                        List.of("f1"),
                        10L,
                        "0001"
                )),
                List.of("b1"),
                List.of("a1"),
                List.of("f1")
        );

        when(applicationService.avaliar()).thenReturn(new PjbSubstituicaoFederativaNucleoDuroAggregate(
                84,
                false,
                true,
                true,
                true,
                3,
                List.of(tribunal),
                List.of("crit1"),
                List.of("f1"),
                Instant.now()
        ));
        when(applicationService.avaliarTribunal("TJCE")).thenReturn(tribunal);

        var response = facadeService.avaliar();
        var tribunalResponse = facadeService.avaliarTribunal("TJCE");

        assertEquals(84, response.scoreNacional());
        assertFalse(response.prontoNucleoDuro());
        assertEquals(1, response.tribunais().size());
        assertEquals("TJCE", tribunalResponse.tribunalCodigo());
        assertNotNull(response.geradoEm());
    }
}
