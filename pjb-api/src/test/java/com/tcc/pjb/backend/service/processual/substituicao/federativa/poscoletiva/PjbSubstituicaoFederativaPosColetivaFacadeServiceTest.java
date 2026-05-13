package com.tcc.pjb.backend.service.processual.substituicao.federativa.poscoletiva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaPosColetivaApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPosColetivaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPosColetivaCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPosColetivaTribunal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoFederativaPosColetivaFacadeServiceTest {

    @Test
    void deveMapearMalhaPosColetiva() {
        PjbSubstituicaoFederativaPosColetivaApplicationService applicationService = mock(PjbSubstituicaoFederativaPosColetivaApplicationService.class);
        PjbSubstituicaoFederativaPosColetivaFacadeService facadeService = new PjbSubstituicaoFederativaPosColetivaFacadeService(applicationService);

        PjbSubstituicaoFederativaPosColetivaTribunal tribunal = new PjbSubstituicaoFederativaPosColetivaTribunal(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJE",
                "operacao-assistida",
                84,
                82,
                80,
                77,
                74,
                true,
                false,
                1,
                List.of(new PjbSubstituicaoFederativaPosColetivaCompetencia(
                        "AMBIENTAL:AMBIENTAL_ACP",
                        "AMBIENTAL",
                        "Ambiental",
                        "AMBIENTAL_ACP",
                        9,
                        82,
                        80,
                        77,
                        74,
                        false,
                        true,
                        true,
                        true,
                        true,
                        "janela-pos-coletiva-assistida",
                        List.of("g1"),
                        List.of("f1"),
                        77L,
                        "0001"
                )),
                List.of("b1"),
                List.of("a1"),
                List.of("f1")
        );

        when(applicationService.avaliar()).thenReturn(new PjbSubstituicaoFederativaPosColetivaAggregate(
                83,
                false,
                true,
                true,
                false,
                false,
                4,
                List.of(tribunal),
                List.of("crit1"),
                List.of("f1"),
                Instant.now()
        ));
        when(applicationService.avaliarTribunal("TJCE")).thenReturn(tribunal);

        var response = facadeService.avaliar();
        var tribunalResponse = facadeService.avaliarTribunal("TJCE");

        assertEquals(83, response.scoreNacional());
        assertFalse(response.malhaPosColetivaPronta());
        assertEquals(1, response.tribunais().size());
        assertEquals("TJCE", tribunalResponse.tribunalCodigo());
        assertNotNull(response.geradoEm());
    }
}
