package com.tcc.pjb.backend.service.processual.substituicao.federativa.tutelacoletiva;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaTutelaColetivaApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaTutelaColetivaTribunal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoFederativaTutelaColetivaFacadeServiceTest {

    @Test
    void deveMapearMalhaTutelaColetiva() {
        PjbSubstituicaoFederativaTutelaColetivaApplicationService applicationService = mock(PjbSubstituicaoFederativaTutelaColetivaApplicationService.class);
        PjbSubstituicaoFederativaTutelaColetivaFacadeService facadeService = new PjbSubstituicaoFederativaTutelaColetivaFacadeService(applicationService);

        PjbSubstituicaoFederativaTutelaColetivaTribunal tribunal = new PjbSubstituicaoFederativaTutelaColetivaTribunal(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJE",
                "operacao-assistida",
                86,
                84,
                82,
                78,
                76,
                true,
                false,
                1,
                List.of(new PjbSubstituicaoFederativaTutelaColetivaCompetencia(
                        "AMBIENTAL:AMBIENTAL_ACP",
                        "AMBIENTAL",
                        "Ambiental",
                        "AMBIENTAL_ACP",
                        9,
                        82,
                        80,
                        74,
                        71,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        "janela-coletiva-assistida",
                        List.of("g1"),
                        List.of("f1"),
                        77L,
                        "0001"
                )),
                List.of("b1"),
                List.of("a1"),
                List.of("f1")
        );

        when(applicationService.avaliar()).thenReturn(new PjbSubstituicaoFederativaTutelaColetivaAggregate(
                84,
                false,
                true,
                true,
                true,
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

        assertEquals(84, response.scoreNacional());
        assertFalse(response.malhaTutelaColetivaPronta());
        assertEquals(1, response.tribunais().size());
        assertEquals("TJCE", tribunalResponse.tribunalCodigo());
        assertNotNull(response.geradoEm());
    }
}
