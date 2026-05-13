package com.tcc.pjb.backend.service.processual.substituicao.federativa.warroom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoFederativaWarRoomApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomRamo;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomRito;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoFederativaWarRoomTribunal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoFederativaWarRoomFacadeServiceTest {

    @Test
    void deveMapearWarRoomFederativo() {
        PjbSubstituicaoFederativaWarRoomApplicationService applicationService = mock(PjbSubstituicaoFederativaWarRoomApplicationService.class);
        PjbSubstituicaoFederativaWarRoomFacadeService facadeService = new PjbSubstituicaoFederativaWarRoomFacadeService(applicationService);

        PjbSubstituicaoFederativaWarRoomTribunal tribunal = new PjbSubstituicaoFederativaWarRoomTribunal(
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "ESTADUAL",
                "operacao-assistida",
                "PARCIAL",
                86,
                true,
                false,
                true,
                "janela-corte-controlado",
                List.of(
                        new PjbSubstituicaoFederativaWarRoomRamo(
                                "PENAL",
                                "Direito Penal",
                                82,
                                false,
                                true,
                                "freeze-rigor-penal",
                                List.of(
                                        new PjbSubstituicaoFederativaWarRoomRito(
                                                "PROCEDIMENTO_PENAL_COMUM",
                                                77,
                                                "PARTIAL_READY",
                                                "OBSERVAR",
                                                "ATTENTION",
                                                "freeze-rigor-penal",
                                                false,
                                                true,
                                                List.of("b1"),
                                                List.of("a1"),
                                                10L,
                                                "0001"
                                        )
                                ),
                                List.of("e1"),
                                List.of("a1")
                        )
                ),
                List.of("g1"),
                List.of("r1"),
                List.of("b1"),
                List.of("p1")
        );

        when(applicationService.avaliar()).thenReturn(new PjbSubstituicaoFederativaWarRoomAggregate(
                84,
                false,
                true,
                2,
                1,
                List.of("crit1"),
                List.of(tribunal),
                List.of("f1"),
                Instant.now()
        ));
        when(applicationService.avaliarTribunal("TJCE")).thenReturn(tribunal);

        var response = facadeService.avaliar();
        var tribunalResponse = facadeService.avaliarTribunal("TJCE");

        assertEquals(84, response.scoreGeral());
        assertFalse(response.freezeNacionalAtivo());
        assertEquals(1, response.tribunais().size());
        assertEquals("TJCE", tribunalResponse.tribunalCodigo());
        assertNotNull(response.geradoEm());
    }
}
