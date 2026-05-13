package com.tcc.pjb.backend.service.processual.substituicao.federativa.cutover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaCutoverMatrixApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverCompetencia;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverMatrixAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaCutoverTribunal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoFederativaCutoverMatrixFacadeServiceTest {

    @Test
    void deveMapearCutoverMatrixFederativa() {
        PjbSubstituicaoFederativaCutoverMatrixApplicationService applicationService = mock(PjbSubstituicaoFederativaCutoverMatrixApplicationService.class);
        PjbSubstituicaoFederativaCutoverMatrixFacadeService facadeService = new PjbSubstituicaoFederativaCutoverMatrixFacadeService(applicationService);

        PjbSubstituicaoFederativaCutoverTribunal tribunal = new PjbSubstituicaoFederativaCutoverTribunal(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "PJE",
                "operacao-assistida",
                87,
                85,
                82,
                80,
                88,
                true,
                false,
                "janela-corte-controlado",
                1,
                List.of(new PjbSubstituicaoFederativaCutoverCompetencia(
                        "PENAL",
                        "Direito Penal",
                        "PROCEDIMENTO_PENAL_COMUM",
                        5,
                        84,
                        79,
                        76,
                        true,
                        "janela-corte-controlado",
                        List.of("g1"),
                        List.of("a1"),
                        10L,
                        "0001"
                )),
                List.of("b1"),
                List.of("f1")
        );

        when(applicationService.avaliar()).thenReturn(new PjbSubstituicaoFederativaCutoverMatrixAggregate(
                86,
                false,
                true,
                3,
                7,
                List.of(tribunal),
                List.of("crit1"),
                List.of("f1"),
                Instant.now()
        ));
        when(applicationService.avaliarTribunal("TJCE")).thenReturn(tribunal);

        var response = facadeService.avaliar();
        var tribunalResponse = facadeService.avaliarTribunal("TJCE");

        assertEquals(86, response.scoreGeral());
        assertFalse(response.freezeNacionalAtivo());
        assertEquals(1, response.tribunais().size());
        assertEquals("TJCE", tribunalResponse.tribunalCodigo());
        assertNotNull(response.geradoEm());
    }
}
