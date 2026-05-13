package com.tcc.pjb.backend.service.processual.substituicao.federativa.malhajulgadora;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaMalhaJulgadoraApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraTribunal;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaMalhaJulgadoraUnidade;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoFederativaMalhaJulgadoraFacadeServiceTest {

    @Test
    void deveMapearMalhaJulgadoraFederativa() {
        PjbSubstituicaoFederativaMalhaJulgadoraApplicationService applicationService = mock(PjbSubstituicaoFederativaMalhaJulgadoraApplicationService.class);
        PjbSubstituicaoFederativaMalhaJulgadoraFacadeService facadeService = new PjbSubstituicaoFederativaMalhaJulgadoraFacadeService(applicationService);

        PjbSubstituicaoFederativaMalhaJulgadoraTribunal tribunal = new PjbSubstituicaoFederativaMalhaJulgadoraTribunal(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJE",
                "operacao-assistida",
                87,
                81,
                79,
                83,
                true,
                false,
                1,
                List.of(new PjbSubstituicaoFederativaMalhaJulgadoraUnidade(
                        "1VARA-CRIMINAL",
                        "1ª Vara Criminal",
                        "PENAL",
                        "PROCEDIMENTO_PENAL_COMUM",
                        5,
                        82,
                        77,
                        74,
                        false,
                        true,
                        true,
                        "janela-unidade-assistida",
                        List.of("g1"),
                        List.of("f1"),
                        10L,
                        "0001"
                )),
                List.of("b1"),
                List.of("a1"),
                List.of("f1")
        );

        when(applicationService.avaliar()).thenReturn(new PjbSubstituicaoFederativaMalhaJulgadoraAggregate(
                85,
                false,
                true,
                true,
                true,
                4,
                List.of(tribunal),
                List.of("crit1"),
                List.of("f1"),
                Instant.now()
        ));
        when(applicationService.avaliarTribunal("TJCE")).thenReturn(tribunal);

        var response = facadeService.avaliar();
        var tribunalResponse = facadeService.avaliarTribunal("TJCE");

        assertEquals(85, response.scoreNacional());
        assertFalse(response.malhaJulgadoraPronta());
        assertEquals(1, response.tribunais().size());
        assertEquals("TJCE", tribunalResponse.tribunalCodigo());
        assertNotNull(response.geradoEm());
    }
}
