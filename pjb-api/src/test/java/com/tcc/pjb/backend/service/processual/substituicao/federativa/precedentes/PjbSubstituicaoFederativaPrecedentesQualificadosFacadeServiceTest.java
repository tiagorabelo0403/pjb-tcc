package com.tcc.pjb.backend.service.processual.substituicao.federativa.precedentes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaPrecedentesQualificadosTribunal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoFederativaPrecedentesQualificadosFacadeServiceTest {

    @Test
    void deveMapearMalhaDePrecedentesQualificados() {
        PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService applicationService = mock(PjbSubstituicaoFederativaPrecedentesQualificadosApplicationService.class);
        PjbSubstituicaoFederativaPrecedentesQualificadosFacadeService facadeService = new PjbSubstituicaoFederativaPrecedentesQualificadosFacadeService(applicationService);

        PjbSubstituicaoFederativaPrecedentesQualificadosTribunal tribunal = new PjbSubstituicaoFederativaPrecedentesQualificadosTribunal(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJE",
                "operacao-assistida",
                88,
                84,
                79,
                82,
                86,
                true,
                false,
                1,
                List.of(new PjbSubstituicaoFederativaPrecedentesQualificadosCompetencia(
                        "CIVIL:COMUM_ORDINARIO",
                        "CIVIL",
                        "Cível",
                        "COMUM_ORDINARIO",
                        12,
                        83,
                        76,
                        80,
                        88,
                        false,
                        true,
                        true,
                        true,
                        true,
                        true,
                        "janela-precedentes-assistida",
                        List.of("g1"),
                        List.of("f1"),
                        11L,
                        "0001"
                )),
                List.of("b1"),
                List.of("a1"),
                List.of("f1")
        );

        when(applicationService.avaliar()).thenReturn(new PjbSubstituicaoFederativaPrecedentesQualificadosAggregate(
                87,
                false,
                true,
                true,
                true,
                true,
                5,
                List.of(tribunal),
                List.of("crit1"),
                List.of("f1"),
                Instant.now()
        ));
        when(applicationService.avaliarTribunal("TJCE")).thenReturn(tribunal);

        var response = facadeService.avaliar();
        var tribunalResponse = facadeService.avaliarTribunal("TJCE");

        assertEquals(87, response.scoreNacional());
        assertFalse(response.malhaPrecedentesPronta());
        assertEquals(1, response.tribunais().size());
        assertEquals("TJCE", tribunalResponse.tribunalCodigo());
        assertNotNull(response.geradoEm());
    }
}
