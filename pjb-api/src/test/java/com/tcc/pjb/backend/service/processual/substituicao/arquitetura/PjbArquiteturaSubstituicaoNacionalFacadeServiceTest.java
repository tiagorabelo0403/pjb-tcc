package com.tcc.pjb.backend.service.processual.substituicao.arquitetura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbArquiteturaSubstituicaoNacionalApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoCapacidade;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoNacionalAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbArquiteturaSubstituicaoNacionalFacadeServiceTest {

    @Test
    void deveMapearArquiteturaNacionalComQuatroPilares() {
        PjbArquiteturaSubstituicaoNacionalApplicationService applicationService = mock(PjbArquiteturaSubstituicaoNacionalApplicationService.class);
        PjbArquiteturaSubstituicaoNacionalFacadeService facadeService = new PjbArquiteturaSubstituicaoNacionalFacadeService(applicationService);

        PjbArquiteturaSubstituicaoCapacidade capacidade = new PjbArquiteturaSubstituicaoCapacidade(
                "cap",
                "Capacidade",
                PjbFechamentoStatus.CONCLUIDA,
                90,
                "ok",
                List.of("evidencia"),
                List.of()
        );
        List<PjbArquiteturaSubstituicaoPilar> pilares = List.of(
                new PjbArquiteturaSubstituicaoPilar("motor", "Motor", PjbFechamentoStatus.PARCIAL, 90, false, List.of(capacidade), List.of()),
                new PjbArquiteturaSubstituicaoPilar("interop", "Interop", PjbFechamentoStatus.PARCIAL, 88, false, List.of(capacidade), List.of()),
                new PjbArquiteturaSubstituicaoPilar("conf", "Conf", PjbFechamentoStatus.PARCIAL, 87, false, List.of(capacidade), List.of()),
                new PjbArquiteturaSubstituicaoPilar("gov", "Gov", PjbFechamentoStatus.PARCIAL, 86, false, List.of(capacidade), List.of())
        );
        when(applicationService.avaliar()).thenReturn(new PjbArquiteturaSubstituicaoNacionalAggregate(
                88,
                false,
                true,
                100,
                10,
                2,
                95,
                180,
                pilares,
                "ok",
                List.of("f1"),
                Instant.now()
        ));

        var response = facadeService.avaliar();

        assertEquals(4, response.pilares().size());
        assertEquals(88, response.scoreGeral());
        assertFalse(response.prontoParaSubstituicaoImediata());
        assertNotNull(response.geradoEm());
    }
}
